package com.aichat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aichat.config.SeedData;
import com.aichat.repository.AiUsageRepository;
import com.aichat.repository.LoginSessionRepository;
import com.aichat.repository.TopicRepository;
import com.aichat.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {"spring.profiles.active=default",
    "spring.datasource.url=jdbc:h2:mem:workflow;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop", "app.admin.email=admin@test.local",
    "app.admin.password=TestAdminPassword123!", "spring.ai.retry.max-attempts=1", "app.ai.extract-candidates=2",
    "app.auth.session-ttl=2h", "app.auth.login-max-attempts=100", "app.admin.recent-usage-limit=2"})
@AutoConfigureMockMvc
class WorkflowIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    UserAccountRepository users;
    @Autowired
    LoginSessionRepository sessions;
    @Autowired
    AiUsageRepository usage;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    SeedData seedData;
    @Autowired
    TopicRepository topics;
    static final AtomicReference<String> responseContent = new AtomicReference<>();
    static final AtomicReference<String> lastRequest = new AtomicReference<>();
    static HttpServer provider;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry properties) throws Exception {
        provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        provider.createContext("/chat/completions", exchange -> {
            lastRequest.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            ObjectMapper json = new ObjectMapper();
            String payload = json.writeValueAsString(Map.of("id", "test-completion", "object", "chat.completion",
                "created", 1, "model", "deepseek-test", "choices",
                List.of(Map.of("index", 0, "message", Map.of("role", "assistant", "content", responseContent.get()),
                    "finish_reason", "stop")),
                "usage", Map.of("prompt_tokens", 30, "completion_tokens", 40, "total_tokens", 70)));
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        provider.start();
        properties.add("spring.ai.openai.base-url", () -> "http://127.0.0.1:" + provider.getAddress().getPort());
        properties.add("spring.ai.openai.api-key", () -> "test-key-not-a-secret");
    }

    @AfterAll
    static void stop() {
        if (provider != null) {
            provider.stop(0);
        }
    }

    @BeforeEach
    void reply() {
        responseContent.set(
            "{\"children\":[{\"name\":\"基础认知\",\"description\":\"基本概念和知识边界\"},{\"name\":\"日常管理\",\"description\":\"日常生活相关知识\"}],\"reason\":\"按知识类型拆分\"}");
    }

    private record Account(String token, String id, String email) {
    }

    private Account register() throws Exception {
        String email = "u" + UUID.randomUUID() + "@test.local";
        JsonNode data = call(post("/api/auth/register"), null,
            Map.of("email", email, "password", "TestPassword123!", "name", "测试用户"), 200);
        return new Account(data.path("token").asText(), data.path("user").path("id").asText(), email);
    }

    private JsonNode create(Account account) throws Exception {
        return call(post("/api/trees"), account.token,
            Map.of("name", "医疗科普", "description", "面向公众的科普知识结构", "topicId", "health"), 200);
    }

    private JsonNode call(MockHttpServletRequestBuilder req, String token, Object body, int status) throws Exception {
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body));
        }
        MvcResult result = mvc.perform(req).andExpect(status().is(status)).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void registrationLoginAndLogoutUseHashedSecrets() throws Exception {
        Instant before = Instant.now();
        Account a = register();
        assertThat(sessions.findAll().stream().filter(session -> session.getUserId().equals(a.id)).findFirst()
            .orElseThrow().getExpiresAt())
            .isBetween(before.plus(Duration.ofHours(2)), Instant.now().plus(Duration.ofHours(2)));
        assertThat(users.findById(a.id).orElseThrow().getPasswordHash()).startsWith("$2a$");
        assertThat(sessions.findAll()).noneMatch(s -> s.getTokenHash().equals(a.token));
        call(post("/api/auth/login"), null, Map.of("email", a.email, "password", "incorrect"), 401);
        JsonNode login =
            call(post("/api/auth/login"), null, Map.of("email", a.email, "password", "TestPassword123!"), 200);
        String token = login.path("token").asText();
        call(get("/api/auth/me"), token, null, 200);
        call(post("/api/auth/logout"), token, null, 200);
        call(get("/api/trees"), token, null, 401);
    }

    @Test
    void allTreeActionsAreIsolatedBetweenUsers() throws Exception {
        Account a = register(), b = register();
        JsonNode tree = create(a);
        String id = tree.path("id").asText(), node = tree.path("root").path("id").asText();
        assertThat(call(get("/api/trees"), b.token, null, 200).size()).isZero();
        call(get("/api/trees/" + id), b.token, null, 404);
        call(patch("/api/trees/" + id + "/nodes/" + node), b.token,
            Map.of("revision", 0, "name", "侵入", "description", "", "status", "DRAFT"), 404);
        call(post("/api/trees/" + id + "/nodes/" + node + "/expand"), b.token,
            Map.of("revision", 0, "count", 3, "instruction", ""), 404);
        call(get("/api/trees/" + id + "/suggestions"), b.token, null, 404);
        call(get("/api/trees/" + id + "/export-preview"), b.token, null, 404);
        call(post("/api/trees/" + id + "/copy"), b.token, null, 404);
        call(delete("/api/trees/" + id + "?revision=0"), b.token, null, 404);
    }

    @Test
    void nodeEditsInvalidateDescendantsAndRejectStaleWrites() throws Exception {
        Account a = register();
        JsonNode tree = create(a);
        String id = tree.path("id").asText(), root = tree.path("root").path("id").asText();
        JsonNode added = call(post("/api/trees/" + id + "/nodes/" + root + "/children"), a.token,
            Map.of("revision", 0, "name", "慢性病", "description", "慢性疾病认知"), 200);
        String child = added.path("root").path("children").get(0).path("id").asText();
        call(post("/api/trees/" + id + "/nodes/" + root + "/children"), a.token,
            Map.of("revision", 0, "name", "过时写入", "description", ""), 409);
        call(patch("/api/trees/" + id + "/nodes/" + child), a.token,
            Map.of("revision", 1, "name", "慢性病", "description", "慢性疾病认知", "status", "CONFIRMED"), 200);
        JsonNode edited = call(patch("/api/trees/" + id + "/nodes/" + root), a.token,
            Map.of("revision", 2, "name", "成人医疗科普", "description", "成人健康知识", "status", "DRAFT"), 200);
        assertThat(edited.path("root").path("children").get(0).path("status").asText()).isEqualTo("REVIEW");
        assertThat(edited.path("title").asText()).isEqualTo("成人医疗科普");
        call(delete("/api/trees/" + id + "/nodes/" + root + "?revision=3"), a.token, null, 400);
        JsonNode deleted = call(delete("/api/trees/" + id + "/nodes/" + child + "?revision=3"), a.token, null, 200);
        assertThat(deleted.path("root").path("children").size()).isZero();
    }

    @Test
    void springAiHttpRoundTripRequiresExplicitAcceptanceAndKeepsContext() throws Exception {
        Account a = register();
        JsonNode tree = create(a);
        String id = tree.path("id").asText(), root = tree.path("root").path("id").asText();
        JsonNode suggestion = call(post("/api/trees/" + id + "/nodes/" + root + "/expand"), a.token,
            Map.of("revision", 0, "count", 3, "instruction", "按知识类型拆分"), 200);
        assertThat(lastRequest.get()).contains("医疗科普", "existingChildren", "按知识类型拆分", "本次最多 3 个")
            .doesNotContain("{{max");
        assertThat(call(get("/api/trees/" + id), a.token, null, 200).path("root").path("children").size()).isZero();
        assertThat(call(get("/api/trees/" + id + "/suggestions"), a.token, null, 200).size()).isEqualTo(1);
        List<Map<String, String>> selected = List.of(Map.of("name", "基础知识（已编辑）", "description", "用户确认后的范围"));
        JsonNode accepted =
            call(post("/api/trees/" + id + "/suggestions/" + suggestion.path("id").asText() + "/accept"), a.token,
                Map.of("revision", 0, "children", selected), 200);
        assertThat(accepted.path("root").path("children").size()).isEqualTo(1);
        assertThat(accepted.path("root").path("children").get(0).path("name").asText()).isEqualTo("基础知识（已编辑）");
        call(post("/api/trees/" + id + "/suggestions/" + suggestion.path("id").asText() + "/accept"), a.token,
            Map.of("revision", 1, "children", selected), 409);
        assertThat(usage.findAll())
            .anyMatch(u -> u.getUserId().equals(a.id) && u.getSuccess() && u.getTotalTokens() == 70);
    }

    @Test
    void extractionAndInvalidProviderOutputAreHandled() throws Exception {
        Account a = register();
        responseContent.set(
            "{\"children\":[{\"name\":\"一\",\"description\":\"范围一\"},{\"name\":\"二\",\"description\":\"范围二\"},{\"name\":\"三\",\"description\":\"范围三\"}]}");
        JsonNode extracted = call(post("/api/ai/extract"), a.token, Map.of("text", "希望构建慢病科普主题"), 200);
        assertThat(extracted.size()).isEqualTo(2);
        assertThat(lastRequest.get()).contains("1 至 2").doesNotContain("{{max");
        // 带 Markdown 代码块的回答不符合纯 JSON 协议，必须拒绝。
        responseContent.set("```json\n" + responseContent.get() + "\n```");
        call(post("/api/ai/extract"), a.token, Map.of("text", "慢病"), 502);
        responseContent.set("not json");
        call(post("/api/ai/extract"), a.token, Map.of("text", "慢病"), 502);
        responseContent.set("{\"children\":[]}");
        call(post("/api/ai/extract"), a.token, Map.of("text", "慢病"), 422);
        responseContent.set("{\"children\":[{\"name\":\"缺少范围\"}]}");
        call(post("/api/ai/extract"), a.token, Map.of("text", "慢病"), 502);
        assertThat(usage.findAll()).anyMatch(u -> u.getUserId().equals(a.id) && !u.getSuccess());
    }

    @Test
    void staleSuggestionsCannotBeAccepted() throws Exception {
        Account a = register();
        JsonNode tree = create(a);
        String id = tree.path("id").asText(), root = tree.path("root").path("id").asText();
        JsonNode suggestion = call(post("/api/trees/" + id + "/nodes/" + root + "/expand"), a.token,
            Map.of("revision", 0, "count", 3, "instruction", ""), 200);
        call(post("/api/trees/" + id + "/nodes/" + root + "/children"), a.token,
            Map.of("revision", 0, "name", "新范围", "description", "改变结构"), 200);
        call(post("/api/trees/" + id + "/suggestions/" + suggestion.path("id").asText() + "/accept"), a.token,
            Map.of("revision", 1, "children", List.of(Map.of("name", "基础认知", "description", "说明"))), 409);
        assertThat(call(get("/api/trees/" + id + "/suggestions"), a.token, null, 200).size()).isZero();
    }

    @Test
    void exportsAreStableSnapshotsWithCleanSchemaAndPrivateHistory() throws Exception {
        Account a = register(), b = register();
        JsonNode tree = create(a);
        String id = tree.path("id").asText(), root = tree.path("root").path("id").asText();
        JsonNode exported = call(post("/api/trees/" + id + "/exports?revision=0"), a.token, null, 200);
        assertThat(exported.path("schemaVersion").asText()).isEqualTo("1.0");
        assertThat(exported.path("root").path("children").size()).isZero();
        assertThat(exported.toString()).doesNotContain("ownerId", "password", "status");
        JsonNode history = call(get("/api/trees/" + id + "/exports"), a.token, null, 200);
        String version = history.get(0).path("id").asText();
        call(get("/api/trees/" + id + "/exports/" + version), b.token, null, 404);
        call(patch("/api/trees/" + id + "/nodes/" + root), a.token,
            Map.of("revision", 0, "name", "已修改主题", "description", "新范围", "status", "DRAFT"), 200);
        assertThat(call(get("/api/trees/" + id + "/exports/" + version), a.token, null, 200)).isEqualTo(exported);
        assertThat(
            call(get("/api/trees/" + id + "/export-preview"), a.token, null, 200).path("root").path("id").asText())
            .isEqualTo(root);
    }

    @Test
    void duplicateSiblingNamesAndInvalidInputsAreRejected() throws Exception {
        Account a = register();
        JsonNode tree = create(a);
        String id = tree.path("id").asText(), root = tree.path("root").path("id").asText();
        call(post("/api/trees"), a.token, Map.of("name", " ", "description", ""), 400);
        call(post("/api/trees/" + id + "/nodes/" + root + "/children"), a.token,
            Map.of("revision", 0, "name", "同名", "description", ""), 200);
        call(post("/api/trees/" + id + "/nodes/" + root + "/children"), a.token,
            Map.of("revision", 1, "name", "同名", "description", ""), 400);
        call(post("/api/trees/" + id + "/nodes/" + root + "/expand"), a.token,
            Map.of("revision", 1, "count", 20, "instruction", ""), 400);
        call(patch("/api/trees/" + id + "/nodes/" + root), a.token,
            Map.of("revision", 1, "name", "名称", "description", "", "status", "INVALID"), 400);
        call(patch("/api/trees/" + id + "/nodes/" + root), a.token,
            Map.of("revision", 1, "name", "名称", "description", "", "status", 0), 400);
    }

    @Test
    void copyHasIndependentIdsAndDeletingTreeRemovesHistory() throws Exception {
        Account a = register();
        JsonNode tree = create(a);
        String id = tree.path("id").asText();
        call(post("/api/trees/" + id + "/exports?revision=0"), a.token, null, 200);
        JsonNode copied = call(post("/api/trees/" + id + "/copy"), a.token, null, 200);
        assertThat(copied.path("id")).isNotEqualTo(tree.path("id"));
        assertThat(copied.path("root").path("id")).isNotEqualTo(tree.path("root").path("id"));
        call(delete("/api/trees/" + id + "?revision=0"), a.token, null, 200);
        call(get("/api/trees/" + id), a.token, null, 404);
        call(get("/api/trees/" + copied.path("id").asText()), a.token, null, 200);
    }

    @Test
    void adminCanManageTopicsAndDisableAccountsButRegularUsersCannot() throws Exception {
        Account a = register();
        call(get("/api/admin/users"), a.token, null, 403);
        call(get("/api/admin;ignored=1/users"), a.token, null, 403);
        String admin = call(post("/api/auth/login"), null,
            Map.of("email", "admin@test.local", "password", "TestAdminPassword123!"), 200).path("token").asText();
        assertThat(call(get("/api/topics"), a.token, null, 200).size()).isGreaterThanOrEqualTo(12);
        JsonNode topic = call(post("/api/admin/topics"), admin, Map.of("name", "测试分类", "description", "测试范围", "icon",
            "BookOpen", "color", "#123456", "sortOrder", 99, "enabled", false), 200);
        assertThat(call(get("/api/topics"), a.token, null, 200).toString()).doesNotContain(topic.path("id").asText());
        call(patch("/api/admin/users/" + a.id), admin, Map.of("enabled", false), 200);
        call(get("/api/trees"), a.token, null, 401);
        JsonNode stats = call(get("/api/admin/stats"), admin, null, 200);
        assertThat(stats.has("recentUsage")).isTrue();
        assertThat(stats.path("recentUsage").size()).isLessThanOrEqualTo(2);
    }

    @Test
    void rerunningSeedPreservesAdministratorEdits() throws Exception {
        var topic = topics.findById("health").orElseThrow();
        String originalName = topic.getName();
        Boolean originalEnabled = topic.getEnabled();
        long count = topics.count();
        try {
            topic.setName("管理员已修改");
            topic.setEnabled(false);
            topics.saveAndFlush(topic);
            seedData.run(null);
            var persisted = topics.findById("health").orElseThrow();
            assertThat(persisted.getName()).isEqualTo("管理员已修改");
            assertThat(persisted.getEnabled()).isFalse();
            assertThat(topics.count()).isEqualTo(count);
        } finally {
            topic.setName(originalName);
            topic.setEnabled(originalEnabled);
            topics.saveAndFlush(topic);
        }
    }

    @Test
    void suggestionsUseValueEqualityForLargeRevisions() throws Exception {
        Account account = register();
        JsonNode tree = create(account);
        String treeId = tree.path("id").asText();
        String rootId = tree.path("root").path("id").asText();
        jdbc.update("update knowledge_tree set revision = ? where id = ?", 128L, treeId);
        JsonNode suggestion = call(post("/api/trees/" + treeId + "/nodes/" + rootId + "/expand"), account.token,
            Map.of("revision", 128, "count", 3, "instruction", ""), 200);
        assertThat(call(get("/api/trees/" + treeId + "/suggestions"), account.token, null, 200).size()).isEqualTo(1);
        JsonNode accepted = call(
            post("/api/trees/" + treeId + "/suggestions/" + suggestion.path("id").asText() + "/accept"), account.token,
            Map.of("revision", 128, "children", List.of(Map.of("name", "基础认知", "description", "基本概念"))), 200);
        assertThat(accepted.path("revision").asLong()).isEqualTo(129L);
    }
}
