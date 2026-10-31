package com.aichat;

import static org.assertj.core.api.Assertions.assertThat;

import com.aichat.config.AdminProperties;
import com.aichat.config.AiLimitProperties;
import com.aichat.config.AiPromptProperties;
import com.aichat.config.AuthProperties;
import com.aichat.config.TopicProperties;
import com.aichat.config.TreeProperties;
import java.time.Duration;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** 验证外部覆盖可绑定，错误配置在处理请求前被拒绝。 */
class RuntimeConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Binding.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({AiLimitProperties.class, AuthProperties.class, TreeProperties.class,
        AdminProperties.class, TopicProperties.class, AiPromptProperties.class})
    static class Binding {}

    @Test
    void bindsRuntimeOverridesWithUnitsAndResources() {
        runner
            .withPropertyValues("app.ai.daily-limit=7", "app.ai.quota-zone=America/Los_Angeles",
                "app.ai.connect-timeout=250ms", "app.ai.read-timeout=45s", "app.ai.extract-candidates=5",
                "app.auth.session-ttl=2h", "app.auth.login-window=20s", "app.auth.login-max-attempts=2",
                "app.tree.max-nodes=12", "app.tree.max-depth=4", "app.admin.recent-usage-limit=3",
                "app.topic.seed-enabled=false", "app.topic.default-color=#123456",
                "app.topic.seed-location=file:./custom-topics.json", "app.ai.prompts.extract=file:./extract.txt")
            .run(context -> {
                assertThat(context).hasNotFailed();
                AiLimitProperties ai = context.getBean(AiLimitProperties.class);
                assertThat(ai.getDailyLimit()).isEqualTo(7);
                assertThat(ai.getQuotaZone()).isEqualTo(ZoneId.of("America/Los_Angeles"));
                assertThat(ai.getConnectTimeout()).isEqualTo(Duration.ofMillis(250));
                assertThat(ai.getReadTimeout()).isEqualTo(Duration.ofSeconds(45));
                assertThat(ai.getExtractCandidates()).isEqualTo(5);
                assertThat(context.getBean(AuthProperties.class).getSessionTtl()).isEqualTo(Duration.ofHours(2));
                assertThat(context.getBean(AuthProperties.class).getLoginWindow()).isEqualTo(Duration.ofSeconds(20));
                assertThat(context.getBean(AuthProperties.class).getLoginMaxAttempts()).isEqualTo(2);
                assertThat(context.getBean(TreeProperties.class).getMaxNodes()).isEqualTo(12);
                assertThat(context.getBean(TreeProperties.class).getMaxDepth()).isEqualTo(4);
                assertThat(context.getBean(AdminProperties.class).getRecentUsageLimit()).isEqualTo(3);
                TopicProperties topic = context.getBean(TopicProperties.class);
                assertThat(topic.getSeedEnabled()).isFalse();
                assertThat(topic.getDefaultColor()).isEqualTo("#123456");
                assertThat(topic.getSeedLocation().getFilename()).isEqualTo("custom-topics.json");
                assertThat(context.getBean(AiPromptProperties.class).getExtract().getFilename())
                    .isEqualTo("extract.txt");
            });
    }

    @ParameterizedTest
    @ValueSource(strings = {"app.ai.daily-limit=0", "app.ai.quota-zone=Invalid/Zone", "app.ai.connect-timeout=0s",
        "app.ai.read-timeout=-1s", "app.ai.extract-candidates=11", "app.auth.session-ttl=0s",
        "app.auth.login-window=-1s", "app.auth.login-max-attempts=0", "app.tree.max-nodes=0", "app.tree.max-depth=81",
        "app.admin.recent-usage-limit=0", "app.admin.recent-usage-limit=1001", "app.topic.default-color=red"})
    void rejectsInvalidConfigurationAtStartup(String property) {
        runner.withPropertyValues(property).run(context -> assertThat(context).hasFailed());
    }
}
