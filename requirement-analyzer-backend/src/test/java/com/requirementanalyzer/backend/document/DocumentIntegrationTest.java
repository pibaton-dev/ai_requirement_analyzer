package com.requirementanalyzer.backend.document;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ownerCanUploadListAndDeleteDocument() throws Exception {
        String accessToken = register("doc-owner@example.com");

        MvcResult projectResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Spec project\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId = JsonPath.read(projectResult.getResponse().getContentAsString(), "$.id");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "requirements.txt",
                "text/plain",
                "User story: login\nAcceptance: user can sign in".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/projects/{projectId}/documents", projectId)
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("requirements.txt"))
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andReturn();
        String documentId = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/projects/{projectId}/documents", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/documents/{documentId}", documentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("requirements.txt"));

        mockMvc.perform(get("/api/v1/documents/{documentId}/download", documentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().string("User story: login\nAcceptance: user can sign in"));

        mockMvc.perform(delete("/api/v1/documents/{documentId}", documentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/documents/{documentId}", documentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void documentAccessIsScopedToProjectOwner() throws Exception {
        String firstUserToken = register("first-doc-owner@example.com");
        String secondUserToken = register("second-doc-owner@example.com");

        MvcResult projectResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + firstUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Project shared nowhere\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId = JsonPath.read(projectResult.getResponse().getContentAsString(), "$.id");

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/projects/{projectId}/documents", projectId)
                        .file(new MockMultipartFile("file", "shared.txt", "text/plain", "secret".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + firstUserToken))
                .andExpect(status().isCreated())
                .andReturn();
        String documentId = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/documents/{documentId}", documentId)
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/documents/{documentId}", documentId)
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isNotFound());
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correct-horse-battery","displayName":"Document owner"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
