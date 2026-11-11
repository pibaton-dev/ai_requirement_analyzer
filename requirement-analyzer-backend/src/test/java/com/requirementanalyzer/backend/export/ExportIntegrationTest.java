package com.requirementanalyzer.backend.export;

import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;

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

        mockMvc.perform(get("/api/v1/projects/{projectId}/export/pdf", projectId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType("application/pdf"));
        mockMvc.perform(get("/api/v1/projects/{projectId}/export/xlsx", projectId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

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

        MvcResult pdfResult = mockMvc.perform(get("/api/v1/projects/{projectId}/analysis-versions/{versionId}/export/pdf", projectId, versionId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType("application/pdf"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")))
                .andReturn();
        try (var pdf = Loader.loadPDF(pdfResult.getResponse().getContentAsByteArray())) {
            org.junit.jupiter.api.Assertions.assertTrue(new PDFTextStripper().getText(pdf).contains("REQ-001"));
        }

        MvcResult xlsxResult = mockMvc.perform(get("/api/v1/projects/{projectId}/analysis-versions/{versionId}/export/xlsx", projectId, versionId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
                .andReturn();
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxResult.getResponse().getContentAsByteArray()))) {
            org.junit.jupiter.api.Assertions.assertEquals("REQ-001",
                    workbook.getSheet("Requirements").getRow(1).getCell(0).getStringCellValue());
        }

        String otherOwnerToken = register("export-other-owner@example.com");
        mockMvc.perform(get("/api/v1/projects/{projectId}/analysis-versions/{versionId}/export/pdf", projectId, versionId)
                        .header("Authorization", "Bearer " + otherOwnerToken))
                .andExpect(status().isNotFound());
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
