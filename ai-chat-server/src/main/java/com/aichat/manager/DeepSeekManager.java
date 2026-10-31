package com.aichat.manager;

import com.aichat.config.AiLimitProperties;
import com.aichat.exception.ApiException;
import com.aichat.exception.SafeLog;
import com.aichat.model.dto.CandidateDTO;
import com.aichat.model.entity.AiUsageDO;
import com.aichat.model.enums.AiOperationEnum;
import com.aichat.repository.AiUsageRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 封装 Spring AI 的 DeepSeek 调用、额度保护、协议校验与用量记录。 */
@Component
public class DeepSeekManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekManager.class);
    private final ChatClient chatClient;
    private final AiUsageRepository usageRepository;
    private final AiResponseParser responseParser;
    private final AiLimitProperties limits;
    private final Set<String> runningUsers = ConcurrentHashMap.newKeySet();
    private final String apiKey;
    private final String model;
    private final AiPromptManager prompts;
    private final Clock clock;

    public DeepSeekManager(ChatClient.Builder builder, AiUsageRepository usageRepository,
        AiResponseParser responseParser, AiLimitProperties limits, @Value("${spring.ai.openai.api-key}") String apiKey,
        @Value("${spring.ai.openai.chat.options.model}") String model, AiPromptManager prompts, Clock clock) {
        this.chatClient = builder.build();
        this.usageRepository = usageRepository;
        this.responseParser = responseParser;
        this.limits = limits;
        this.apiKey = apiKey;
        this.model = model;
        this.prompts = prompts;
        this.clock = clock;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !"not-configured".equals(apiKey);
    }

    /** 调用期间不持有知识树数据库事务，避免模型等待时间占用连接和行锁。 */
    public List<CandidateDTO> generate(String userId, AiOperationEnum operation, String input, int maxCandidates) {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "尚未配置 DeepSeek 密钥，请联系管理员；仍可手动编辑知识树");
        }
        if (!runningUsers.add(userId)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "已有 AI 请求正在处理，请稍候");
        }
        AiUsageDO usage = new AiUsageDO();
        usage.setUserId(userId);
        usage.setOperation(operation.name());
        usage.setModel(model);
        long started = System.nanoTime();
        RuntimeException failure = null;
        try {
            checkDailyQuota(userId);
            LOGGER.info("AI started operation={} userId={} model={}", operation, userId, model);
            ChatResponse response = requestCompletion(operation, input, maxCandidates);
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 返回空结果，请重试");
            }
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                Integer totalTokens = response.getMetadata().getUsage().getTotalTokens();
                usage.setTotalTokens(totalTokens == null ? 0 : totalTokens);
            }
            List<CandidateDTO> result = responseParser.parse(response.getResult().getOutput().getText(), maxCandidates);
            usage.setSuccess(true);
            return result;
        } catch (RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            usage.setDurationMs(Duration.ofNanos(System.nanoTime() - started).toMillis());
            LOGGER.info("AI finished operation={} userId={} model={} success={} durationMs={} totalTokens={}",
                operation, userId, model, usage.getSuccess(), usage.getDurationMs(), usage.getTotalTokens());
            saveUsageAndRelease(usage, failure);
        }
    }

    private ChatResponse requestCompletion(AiOperationEnum operation, String input, int maxCandidates) {
        String prompt = prompts.render(operation, maxCandidates);
        try {
            return chatClient.prompt().system(prompt).user(input).call().chatResponse();
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek 调用失败，请稍后重试；已保存的知识树不受影响", exception);
        }
    }

    private void checkDailyQuota(String userId) {
        ZoneId zone = limits.getQuotaZone();
        Instant start = LocalDate.now(clock.withZone(zone)).atStartOfDay(zone).toInstant();
        if (usageRepository.countByUserIdAndCreatedAtAfter(userId, start) >= limits.getDailyLimit()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "今日 AI 调用次数已用完，明天再来继续探索");
        }
    }

    private void saveUsageAndRelease(AiUsageDO usage, RuntimeException originalFailure) {
        try {
            usageRepository.save(usage);
        } catch (RuntimeException recordingFailure) {
            // 统计写入失败不能覆盖原始业务错误，二者通过异常链保留。
            if (originalFailure == null) {
                throw recordingFailure;
            }
            originalFailure.addSuppressed(recordingFailure);
            LOGGER.error("AI usage persistence failed userId={}", usage.getUserId(),
                SafeLog.forLogging(recordingFailure));
        } finally {
            runningUsers.remove(usage.getUserId());
        }
    }
}
