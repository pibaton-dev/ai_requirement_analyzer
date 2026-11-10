package com.requirementanalyzer.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

        @Autowired
        private UserAccountRepository userRepository;

    @Test
    void registersAndAllowsProfileUpdatesWithAccessToken() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"analyst@example.com","password":"correct-horse-battery","displayName":"Analyst"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(jsonPath("$.user.email").value("analyst@example.com"))
                .andReturn();

        String accessToken = com.jayway.jsonpath.JsonPath.read(
                registration.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Analyst"));

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Business Analyst\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Business Analyst"));
    }

    @Test
    void rotatesRefreshTokensAndRejectsThePreviousToken() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"refresh@example.com","password":"correct-horse-battery","displayName":"Refresh"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        jakarta.servlet.http.Cookie oldRefreshCookie = registration.getResponse().getCookie("refresh_token");

        MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh").cookie(oldRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(oldRefreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        String newRefresh = refresh.getResponse().getCookie("refresh_token").getValue();
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(refresh.getResponse().getCookie("refresh_token")))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", newRefresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnauthenticatedProfileRequestsAndDuplicateAccounts() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", containsString("Authentication is required")));

        String request = """
                {"email":"duplicate@example.com","password":"correct-horse-battery","displayName":"First"}
                """;
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

        @Test
        void disabledAccountCannotUseAnExistingAccessToken() throws Exception {
                MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {"email":"disabled@example.com","password":"correct-horse-battery","displayName":"Disabled"}
                                                                """))
                                .andExpect(status().isCreated())
                                .andReturn();
                String response = registration.getResponse().getContentAsString();
                String accessToken = com.jayway.jsonpath.JsonPath.read(response, "$.accessToken");
                String email = com.jayway.jsonpath.JsonPath.read(response, "$.user.email");
                UserAccount user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
                user.changeStatus(AccountStatus.DISABLED);
                userRepository.saveAndFlush(user);

                mockMvc.perform(get("/api/v1/users/me")
                                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isUnauthorized());
        }
}