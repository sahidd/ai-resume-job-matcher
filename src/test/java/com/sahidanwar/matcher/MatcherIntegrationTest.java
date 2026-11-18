package com.sahidanwar.matcher;

import com.sahidanwar.matcher.application.MatchService;
import com.sahidanwar.matcher.domain.DocumentStatus;
import com.sahidanwar.matcher.infrastructure.persistence.StoredDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class MatcherIntegrationTest {

    private static final String RESUME = """
            Sahid Anwar
            Java developer. Spring Boot microservices, Kafka, PostgreSQL, Docker, JUnit, and Flyway.
            """;

    private static final String JOB = """
            Backend role. Required: Java, Spring Boot, Kafka, Kubernetes, and AWS.
            """;

    private static final String BAKING = """
            Baker. Sourdough, bread, and an oven starter. No software skills.
            """;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("matcher")
            .withUsername("matcher")
            .withPassword("matcher");

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379).toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoredDocumentRepository documents;

    @Autowired
    private MatchService matchService;

    @Test
    void matchesIndexesAndAnswersWithoutALanguageModel() throws Exception {
        String token = registerAndToken("ada");
        UUID resumeId = upload(token, "/api/resumes", "resume.txt", RESUME);
        UUID jobId = upload(token, "/api/jobs", "job.txt", JOB);
        UUID bakingId = upload(token, "/api/jobs", "baking.txt", BAKING);
        awaitReady(resumeId);
        awaitReady(jobId);
        awaitReady(bakingId);

        int before = matchService.computations();
        mockMvc.perform(post("/api/matches")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId":"%s","jobId":"%s"}
                                """.formatted(resumeId, jobId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestionSource").value("fallback"))
                .andExpect(jsonPath("$.matchedSkills", hasItem("kafka")))
                .andExpect(jsonPath("$.missingSkills", hasItem("kubernetes")))
                .andExpect(jsonPath("$.suggestions", containsString("kubernetes")));

        mockMvc.perform(post("/api/matches")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId":"%s","jobId":"%s"}
                                """.formatted(resumeId, jobId)))
                .andExpect(status().isOk());
        assertThat(matchService.computations()).isEqualTo(before + 1);

        mockMvc.perform(get("/api/resumes/{id}/jobs", resumeId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].documentId").value(jobId.toString()));

        mockMvc.perform(get("/api/jobs/{id}/resumes", jobId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].documentId").value(resumeId.toString()));

        mockMvc.perform(post("/api/ask")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentId":"%s","question":"Does this candidate know Kafka?"}
                                """.formatted(resumeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("fallback"))
                .andExpect(jsonPath("$.citations[0].excerpt", containsString("Kafka")))
                .andExpect(jsonPath("$.answer", containsString("chunk")));

        String ivy = registerAndToken("ivy");
        mockMvc.perform(get("/api/documents/{id}", resumeId).header("Authorization", "Bearer " + ivy))
                .andExpect(status().isNotFound());

        assertThat(bakingId).isNotNull();
    }

    @Test
    void marksAnEmptyUploadFailed() throws Exception {
        String token = registerAndToken("blank");
        UUID id = upload(token, "/api/resumes", "empty.txt", "");
        await().atMost(Duration.ofSeconds(20)).until(() ->
                documents.findById(id).orElseThrow().getStatus() == DocumentStatus.FAILED);
        mockMvc.perform(get("/api/documents/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage", containsString("No text could be extracted")));
    }

    @Test
    void rejectsMissingCredentialsAndDuplicateUsers() throws Exception {
        mockMvc.perform(get("/api/documents/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));

        registerAndToken("pat");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"pat\",\"password\":\"password\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", containsString("username-taken")));

        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"pat\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void limitsAuthenticatedCallsPerUser() throws Exception {
        String token = registerAndToken("burst");
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/api/documents/{id}", UUID.randomUUID())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
        mockMvc.perform(get("/api/documents/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type", containsString("rate-limit")));
    }

    private String registerAndToken(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password\"}"))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
        return body.substring(start, body.indexOf('"', start));
    }

    private UUID upload(String token, String path, String filename, String text) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "text/plain", text.getBytes(StandardCharsets.UTF_8));
        MvcResult result = mockMvc.perform(multipart(path).file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"id\":\"") + "\"id\":\"".length();
        return UUID.fromString(body.substring(start, body.indexOf('"', start)));
    }

    private void awaitReady(UUID id) {
        await().atMost(Duration.ofSeconds(20)).until(() ->
                documents.findById(id).orElseThrow().getStatus() == DocumentStatus.READY);
    }
}
