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
import ru.mtuci.coursemanagement.license.dto.TicketResponse;
import ru.mtuci.coursemanagement.license.service.TicketSignatureService;
import ru.mtuci.coursemanagement.repository.AppUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "app.seed-demo-users=true")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class LicenseLifecycleIntegrationTest {

    private static final String DEVICE = "dev-workstation-test-001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketSignatureService ticketSignatureService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void lifecycleCreateActivateVerifyRenew() throws Exception {
        long teacherId = appUserRepository.findByUsername("teacher").orElseThrow().getId();
        String adminToken = bearerToken("admin", "password");
        String teacherToken = bearerToken("teacher", "password");

        mockMvc.perform(get("/api/licenses/signing-public-key.pem"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/licenses/signing-certificate.pem"))
                .andExpect(status().isOk());

        MvcResult createRes = mockMvc.perform(post("/api/licenses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + teacherId + ",\"validityPeriodDays\":365}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode created = objectMapper.readTree(createRes.getResponse().getContentAsString());
        String licenseKey = created.get("licenseKey").asText();

        mockMvc.perform(post("/api/licenses/" + licenseKey + "/activate")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceExternalId\":\"" + DEVICE + "\"}"))
                .andExpect(status().isOk());

        MvcResult verifyRes = mockMvc.perform(post("/api/licenses/verify")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licenseKey\":\"" + licenseKey + "\",\"deviceExternalId\":\"" + DEVICE + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        TicketResponse tr = objectMapper.readValue(
                verifyRes.getResponse().getContentAsString(), TicketResponse.class);
        assertThat(tr.ticket().deviceId()).isEqualTo(DEVICE);
        assertThat(tr.ticket().licenseBlocked()).isFalse();
        assertThat(ticketSignatureService.verifyTicket(tr.ticket(), tr.electronicSignatureBase64())).isTrue();

        mockMvc.perform(post("/api/licenses/" + licenseKey + "/renew")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"additionalDays\":10}"))
                .andExpect(status().isOk());
    }

    private String bearerToken(String user, String pass) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode n = objectMapper.readTree(res.getResponse().getContentAsString());
        return n.get("accessToken").asText();
    }
}
