package com.aichat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aichat.config.AdminProperties;
import com.aichat.config.AiLimitProperties;
import com.aichat.config.AuthProperties;
import com.aichat.config.TreeProperties;
import com.aichat.exception.ApiException;
import com.aichat.manager.AiPromptManager;
import com.aichat.manager.AiResponseParser;
import com.aichat.manager.DeepSeekManager;
import com.aichat.manager.TreeStructureManager;
import com.aichat.model.domain.KnowledgeNode;
import com.aichat.model.dto.RegisterDTO;
import com.aichat.model.entity.LoginSessionDO;
import com.aichat.model.enums.AiOperationEnum;
import com.aichat.repository.AiUsageRepository;
import com.aichat.repository.LoginSessionRepository;
import com.aichat.repository.UserAccountRepository;
import com.aichat.security.LoginAttemptLimiter;
import com.aichat.service.impl.AuthServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

/** 用非默认规则验证消费者，防止配置只完成绑定却没有实际生效。 */
class RuntimePolicyTest {
    @Test
    void loginWindowUsesConfiguredLimitAndResetsExactlyAtExpiry() {
        AuthProperties properties = new AuthProperties();
        properties.setLoginWindow(Duration.ofSeconds(20));
        properties.setLoginMaxAttempts(2);
        Clock clock = mock(Clock.class);
        Instant start = Instant.parse("2026-11-01T00:00:00Z");
        when(clock.instant()).thenReturn(start);
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(properties, clock);
        limiter.check("source-a");
        limiter.check("source-a");
        assertThatThrownBy(() -> limiter.check("source-a")).isInstanceOf(ApiException.class)
            .hasMessageContaining("20 秒");
        assertThatCode(() -> limiter.check("source-b")).doesNotThrowAnyException();
        when(clock.instant()).thenReturn(start.plusSeconds(20));
        assertThatCode(() -> limiter.check("source-a")).doesNotThrowAnyException();
    }

    @Test
    void sessionLifetimeIsConfigurableAndExpiryIsExclusive() {
        AuthProperties properties = new AuthProperties();
        properties.setSessionTtl(Duration.ofHours(2));
        UserAccountRepository users = mock(UserAccountRepository.class);
        LoginSessionRepository sessions = mock(LoginSessionRepository.class);
        Clock clock = mock(Clock.class);
        Instant start = Instant.parse("2026-11-01T00:00:00Z");
        when(clock.instant()).thenReturn(start);
        AuthServiceImpl auth = new AuthServiceImpl(users, sessions, properties, new AdminProperties(), clock);
        var result = auth.register(new RegisterDTO("test@example.test", "TestPassword123!", "测试"));
        ArgumentCaptor<LoginSessionDO> saved = ArgumentCaptor.forClass(LoginSessionDO.class);
        verify(sessions).save(saved.capture());
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(start.plus(Duration.ofHours(2)));
        when(sessions.findById(any())).thenReturn(Optional.of(saved.getValue()));
        when(clock.instant()).thenReturn(saved.getValue().getExpiresAt());
        assertThatThrownBy(() -> auth.authenticate("Bearer " + result.token())).isInstanceOf(ApiException.class)
            .hasMessageContaining("过期");
        verify(sessions).delete(saved.getValue());
    }

    @Test
    void treeCapacityAndMessagesFollowConfiguredLimits() {
        TreeProperties properties = new TreeProperties();
        properties.setMaxNodes(2);
        properties.setMaxDepth(2);
        TreeStructureManager structure = new TreeStructureManager(new ObjectMapper(), properties);
        KnowledgeNode root = new KnowledgeNode("root", "根", "范围");
        assertThatCode(() -> structure.ensureCapacity(root, "root", 1)).doesNotThrowAnyException();
        root.getChildren().add(new KnowledgeNode("child", "子", "范围"));
        assertThatThrownBy(() -> structure.ensureCapacity(root, "root", 1)).hasMessageContaining("最多 2 个节点");
        properties.setMaxNodes(10);
        assertThatThrownBy(() -> structure.ensureCapacity(root, "child", 1)).hasMessageContaining("2 层");
    }

    @Test
    void quotaDayUsesConfiguredZoneBeforeCallingProvider() {
        AiLimitProperties properties = new AiLimitProperties();
        properties.setQuotaZone(ZoneId.of("America/Los_Angeles"));
        properties.setDailyLimit(2);
        Clock clock = Clock.fixed(Instant.parse("2026-11-01T01:00:00Z"), ZoneOffset.UTC);
        Instant dayStart = Instant.parse("2026-10-31T07:00:00Z");
        AiUsageRepository usage = mock(AiUsageRepository.class);
        when(usage.countByUserIdAndCreatedAtAfter("user", dayStart)).thenReturn(2L);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);
        when(builder.build()).thenReturn(client);
        DeepSeekManager manager = new DeepSeekManager(builder, usage, new AiResponseParser(new ObjectMapper()),
            properties, "test-key", "test-model", mock(AiPromptManager.class), clock);
        assertThatThrownBy(() -> manager.generate("user", AiOperationEnum.EXTRACT, "输入", 3))
            .isInstanceOf(ApiException.class).hasMessageContaining("今日");
        verify(usage).countByUserIdAndCreatedAtAfter("user", dayStart);
        verifyNoInteractions(client);
    }
}
