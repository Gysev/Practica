package ru.mtuci.coursemanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.mtuci.coursemanagement.antivirus.crypto.AntivirusEdsPayload;
import ru.mtuci.coursemanagement.eds.EdsDetachedSigner;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "app.seed-demo-users=true")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AntivirusSignatureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EdsDetachedSigner edsDetachedSigner;

    @Test
    void createUpdateSoftDeleteExportsHistoryAuditAndEds() throws Exception {
        String teacher = bearerToken("teacher", "password");
        String uniq = "sig-" + UUID.randomUUID();

        Instant beforeCreate = Instant.now().minusSeconds(2);

        MvcResult createRes = mockMvc.perform(post("/api/antivirus-signatures")
                        .header("Authorization", "Bearer " + teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + uniq + "\",\"content\":\"pattern-1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode created = objectMapper.readTree(createRes.getResponse().getContentAsString());
        long id = created.get("id").asLong();
        assertThat(created.get("version").asInt()).isEqualTo(1);
        verifyEdsAgainstResponse(created);

        MvcResult updRes = mockMvc.perform(put("/api/antivirus-signatures/" + id)
                        .header("Authorization", "Bearer " + teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + uniq + "\",\"content\":\"pattern-2\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode updated = objectMapper.readTree(updRes.getResponse().getContentAsString());
        assertThat(updated.get("version").asInt()).isEqualTo(2);
        verifyEdsAgainstResponse(updated);

        Instant afterUpdate = Instant.parse(updated.get("updatedAt").asText());

        mockMvc.perform(delete("/api/antivirus-signatures/" + id)
                        .header("Authorization", "Bearer " + teacher))
                .andExpect(status().isOk());

        JsonNode deleted = objectMapper.readTree(
                mockMvc.perform(get("/api/antivirus-signatures/" + id)
                                .header("Authorization", "Bearer " + teacher))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        assertThat(deleted.get("deleted").asBoolean()).isTrue();

        JsonNode full = objectMapper.readTree(
                mockMvc.perform(get("/api/antivirus-signatures/export/full")
                                .header("Authorization", "Bearer " + teacher))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        assertThat(full.isArray()).isTrue();
        assertThat(full).noneMatch(row -> row.get("id").asLong() == id);

        Instant since = beforeCreate;
        JsonNode incr = objectMapper.readTree(
                mockMvc.perform(get("/api/antivirus-signatures/export/incremental")
                                .param("since", since.toString())
                                .header("Authorization", "Bearer " + teacher))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        boolean sawDeletedRow = false;
        for (JsonNode row : incr) {
            if (row.get("id").asLong() != id) {
                continue;
            }
            sawDeletedRow = true;
            assertThat(row.get("deleted").asBoolean()).isTrue();
        }
        assertThat(sawDeletedRow).as("increment должен включать удалённую запись").isTrue();

        Instant sinceExclusive = afterUpdate;
        JsonNode incrAfterOnlyDelete = objectMapper.readTree(
                mockMvc.perform(get("/api/antivirus-signatures/export/incremental")
                                .param("since", sinceExclusive.toString())
                                .header("Authorization", "Bearer " + teacher))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        JsonNode oursAfterUpdate = findRowById(incrAfterOnlyDelete, id);
        assertThat(oursAfterUpdate)
                .as("инкремент с since после обновления должен включить удалённую запись этого id")
                .isNotNull();
        assertThat(oursAfterUpdate.get("deleted").asBoolean()).isTrue();

        JsonNode history = objectMapper.readTree(
                mockMvc.perform(get("/api/antivirus-signatures/" + id + "/history")
                                .header("Authorization", "Bearer " + teacher))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        assertThat(history).hasSize(3);

        JsonNode audit = objectMapper.readTree(
                mockMvc.perform(get("/api/antivirus-signatures/" + id + "/audit")
                                .header("Authorization", "Bearer " + teacher))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        assertThat(audit).hasSize(3);

    }

    @Test
    void exportFullRequiresAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/antivirus-signatures/export/full")).andExpect(status().isForbidden());
    }

    @Test
    void studentCannotCreateSignature() throws Exception {
        String token = bearerToken("student", "password");
        String uniq = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/antivirus-signatures")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x-" + uniq + "\",\"content\":\"y\"}"))
                .andExpect(status().isForbidden());
    }

    private static JsonNode findRowById(JsonNode rows, long id) {
        for (JsonNode row : rows) {
            if (row.has("id") && row.get("id").asLong() == id) {
                return row;
            }
        }
        return null;
    }

    private void verifyEdsAgainstResponse(JsonNode n) throws Exception {
        long id = n.get("id").asLong();
        String name = n.get("name").asText();
        String content = n.get("content").asText();
        int version = n.get("version").asInt();
        String eds = n.get("edsSignature").asText();
        byte[] payload = AntivirusEdsPayload.canonicalUtf8(id, name, content, version);
        assertThat(edsDetachedSigner.verifyRawPayloadUtf8(payload, eds)).isTrue();
    }

    private String bearerToken(String user, String pass) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tokenNode = objectMapper.readTree(res.getResponse().getContentAsString());
        return tokenNode.get("accessToken").asText();
    }
}
