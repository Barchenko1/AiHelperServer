package com.helper.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "APP_JWT_SECRET=integration-test-secret-long-enough-for-hs256",
        "APP_NOOP_SECRET=1",
        "GPT_API_KEY=test",
        "ANTHROPIC_API_KEY=test"
})
@AutoConfigureMockMvc
class AiHelperServerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private JsonObject login() throws Exception {
        String body = mvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "pa")
                        .param("password", "pa1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonParser.parseString(body).getAsJsonObject();
    }

    @Test
    void loginIssuesThreeHourSessionUsableByTokenAndCode() throws Exception {
        Instant before = Instant.now();
        JsonObject session = login();
        String token = session.get("token").getAsString();
        String code = session.get("code").getAsString();

        assertThat(Instant.parse(session.get("expiresAt").getAsString()))
                .isAfterOrEqualTo(before.plus(Duration.ofHours(3)).minusSeconds(1))
                .isBefore(before.plus(Duration.ofHours(3)).plusSeconds(60));

        mvc.perform(get("/api/v1/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session").value(true));

        mvc.perform(post("/api/v1/ping").header("X-Auth-Code", code).header("X-Client", "cli"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/status").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.clients.cli").isNumber());
    }

    @Test
    void secondBrowserJoinsWithTheCodeWithoutAPassword() throws Exception {
        JsonObject session = login();

        mvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("code", session.get("code").getAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(session.get("token").getAsString()))
                .andExpect(jsonPath("$.expiresAt").value(session.get("expiresAt").getAsString()));

        mvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("code", "000000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "pa")
                        .param("password", "nope"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void statusIsPublicButReportsNoSession() throws Exception {
        mvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session").value(false));
    }

    @Test
    void protectedEndpointsNeedAValidCode() throws Exception {
        mvc.perform(post("/api/v1/ping").header("X-Client", "cli").header("X-Auth-Code", "000000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightIsAllowed() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/v1/status")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }
}
