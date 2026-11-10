package com.requirementanalyzer.backend.export;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ownerCanExportProjectReviewAsJson() throws Exception {
        String accessToken = register("export-owner@example.com");

        MvcResult projectResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Export project\",\"description\":\"Project used for export validation\"}"))
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
                .andReturn();
        String versionId = JsonPath.read(versionResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/projects/{projectId}/analysis-versions/{versionId}/requirements", projectId, versionId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirementId\":\"REQ-001\",\"title\":\"Users sign in\",\"description\":\"Users must sign in to view reports.\",\"priority\":\"HIGH\",\"status\":\"PENDING_REVIEW\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/projects/{projectId}/analysis-versions/{versionId}/export/json", projectId, versionId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.id").value(projectId))
                .andExpect(jsonPath("$.version.id").value(versionId))
                .andExpect(jsonPath("$.requirements[0].requirementId").value("REQ-001"));
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correct-horse-battery","displayName":"Export owner"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
