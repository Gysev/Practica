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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "app.seed-demo-users=true")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SecurityApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void demoLoginIssuesJwt() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"teacher\",\"password\":\"password\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotCallXmlApi() throws Exception {
        String token = loginToken("student", "password");
        mockMvc.perform(post("/api/xml/parse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_XML)
                        .content("<root>ok</root>"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanCallXmlApi() throws Exception {
        String token = loginToken("teacher", "password");
        mockMvc.perform(post("/api/xml/parse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_XML)
                        .content("<root>ok</root>"))
                .andExpect(status().isOk());
    }

    private String loginToken(String user, String pass) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode n = objectMapper.readTree(res.getResponse().getContentAsString());
        assertThat(n.has("accessToken")).isTrue();
        return n.get("accessToken").asText();
    }
}
