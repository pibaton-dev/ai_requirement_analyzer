package com.requirementanalyzer.backend.review;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewDomainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ownerCanCreateReviewVersionAndManageRequirementsAndQuestions() throws Exception {
        String accessToken = register("review-owner@example.com");

        MvcResult projectResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Review project\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId = JsonPath.read(projectResult.getResponse().getContentAsString(), "$.id");

        MvcResult analysisResult = mockMvc.perform(post("/api/v1/projects/{projectId}/analyses", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Users must sign in to view attendance reports.\"}"))
                .andExpect(status().isAccepted())
                .andReturn();
        String analysisJobId = JsonPath.read(analysisResult.getResponse().getContentAsString(), "$.id");

        MvcResult versionResult = mockMvc.perform(post("/api/v1/projects/{projectId}/analyses/{analysisJobId}/versions", projectId, analysisJobId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andReturn();
        String versionId = JsonPath.read(versionResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/projects/{projectId}/analysis-versions/{versionId}/requirements", projectId, versionId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirementId\":\"REQ-001\",\"title\":\"Users sign in\",\"description\":\"Users must sign in to view reports.\",\"priority\":\"HIGH\",\"status\":\"PENDING_REVIEW\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        mockMvc.perform(put("/api/v1/projects/{projectId}/analysis-versions/{versionId}/requirements/REQ-001", projectId, versionId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Users must sign in to view attendance reports.\",\"priority\":\"CRITICAL\",\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"));

        MvcResult questionResult = mockMvc.perform(post("/api/v1/projects/{projectId}/analysis-versions/{versionId}/questions", projectId, versionId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":\"Q-001\",\"question\":\"Who approves attendance corrections?\",\"relatedRequirementId\":\"REQ-001\",\"reason\":\"Needed for workflow\",\"impact\":\"High\",\"status\":\"OPEN\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String questionId = JsonPath.read(questionResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/projects/{projectId}/analysis-versions/{versionId}/questions/{questionId}/answer", projectId, versionId, questionId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"The department manager approves corrections.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANSWERED"));

        mockMvc.perform(get("/api/v1/projects/{projectId}/analysis-versions/{versionId}/review", projectId, versionId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.questions[0].status").value("ANSWERED"));
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correct-horse-battery","displayName":"Review owner"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
