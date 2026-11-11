package com.requirementanalyzer.backend.analysis;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalysisJobIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ownerCanSubmitAnalysisJobAndViewStatus() throws Exception {
        String accessToken = register("analysis-owner@example.com");

        MvcResult projectResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Requirements project\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId = JsonPath.read(projectResult.getResponse().getContentAsString(), "$.id");

        MvcResult jobResult = mockMvc.perform(post("/api/v1/projects/{projectId}/analyses", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Users must sign in and view attendance reports.\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andReturn();
        String jobId = JsonPath.read(jobResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/analyses/{jobId}", jobId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId));
    }

    @Test
    void analysisSubmissionRequiresTextOrAuthorizedDocuments() throws Exception {
        String accessToken = register("analysis-validation@example.com");

        MvcResult projectResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Validation project\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId = JsonPath.read(projectResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/projects/{projectId}/analyses", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"   \", \"documentIds\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void analysisCannotUseAnotherUsersDocument() throws Exception {
        String documentOwnerToken = register("analysis-document-owner@example.com");
        String otherOwnerToken = register("analysis-other-owner@example.com");

        MvcResult documentProjectResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + documentOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Private document project\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String documentProjectId = JsonPath.read(documentProjectResult.getResponse().getContentAsString(), "$.id");

        MvcResult documentResult = mockMvc.perform(multipart("/api/v1/projects/{projectId}/documents", documentProjectId)
                        .file(new MockMultipartFile("file", "requirements.txt", "text/plain",
                                "Users can view their attendance.".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + documentOwnerToken))
                .andExpect(status().isCreated())
                .andReturn();
        String documentId = JsonPath.read(documentResult.getResponse().getContentAsString(), "$.id");

        MvcResult otherProjectResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + otherOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other user's project\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String otherProjectId = JsonPath.read(otherProjectResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/projects/{projectId}/analyses", otherProjectId)
                        .header("Authorization", "Bearer " + otherOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentIds\":[\"%s\"]}".formatted(documentId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correct-horse-battery","displayName":"Analysis owner"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
