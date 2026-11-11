package com.requirementanalyzer.backend.export;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import com.requirementanalyzer.backend.common.DomainException;
import com.requirementanalyzer.backend.review.ReviewDtos;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExportDocumentRenderer {

    public byte[] toPdf(ExportDtos.ProjectExportResponse export) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            List<String> lines = pdfLines(export);
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float y = page.getMediaBox().getHeight() - 50;

            for (String line : lines) {
                for (String wrappedLine : wrap(line, 92)) {
                    if (y < 50) {
                        content.close();
                        page = new PDPage(PDRectangle.LETTER);
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        y = page.getMediaBox().getHeight() - 50;
                    }
                    content.beginText();
                    content.setFont(font, 10);
                    content.newLineAtOffset(50, y);
                    content.showText(toPdfText(wrappedLine));
                    content.endText();
                    y -= 14;
                }
            }

            content.close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw renderingFailure();
        }
    }

    public byte[] toXlsx(ExportDtos.ProjectExportResponse export) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            Sheet project = workbook.createSheet("Project");
            addRows(project, headerStyle, List.of(
                    List.of("Field", "Value"),
                    List.of("Project ID", value(export.project().id())),
                    List.of("Project", value(export.project().name())),
                    List.of("Description", value(export.project().description())),
                    List.of("Domain", value(export.project().domain())),
                    List.of("Status", value(export.project().status())),
                    List.of("Exported at", value(export.exportedAt())),
                    List.of("Version", export.version() == null ? "" : Integer.toString(export.version().versionNumber())),
                    List.of("Version ID", export.version() == null ? "" : value(export.version().id())),
                    List.of("Analysis job ID", export.version() == null ? "" : value(export.version().analysisJobId())),
                    List.of("Version title", export.version() == null ? "" : value(export.version().title())),
                    List.of("Version summary", export.version() == null ? "" : value(export.version().summary()))
            ));

            addRows(workbook.createSheet("Requirements"), headerStyle, rows(
                    List.of("Requirement ID", "Title", "Description", "Priority", "Status", "Source", "Created at"),
                    list(export.requirements()).stream().map(requirement -> List.of(
                            value(requirement.requirementId()), value(requirement.title()), value(requirement.description()),
                            value(requirement.priority()), value(requirement.status()), value(requirement.sourceType()),
                            value(requirement.createdAt())
                    )).toList()
            ));

            addRows(workbook.createSheet("Questions"), headerStyle, rows(
                    List.of("Question ID", "Question", "Related requirement", "Reason", "Impact", "Status", "Answer"),
                    list(export.questions()).stream().map(question -> List.of(
                            value(question.questionId()), value(question.question()), value(question.relatedRequirementId()),
                            value(question.reason()), value(question.impact()), value(question.status()), value(question.answer())
                    )).toList()
            ));

            addRows(workbook.createSheet("Audit"), headerStyle, rows(
                    List.of("Action", "Target type", "Target", "Previous status", "New status", "Details", "Created at"),
                    list(export.auditEvents()).stream().map(event -> List.of(
                            value(event.action()), value(event.targetType()), value(event.targetKey()),
                            value(event.previousStatus()), value(event.newStatus()), value(event.details()),
                            value(event.createdAt())
                    )).toList()
            ));
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw renderingFailure();
        }
    }

    private List<String> pdfLines(ExportDtos.ProjectExportResponse export) {
        List<String> lines = new ArrayList<>();
        lines.add("Requirement Analyzer - Project Export");
        lines.add("Project ID: " + value(export.project().id()));
        lines.add("Project: " + value(export.project().name()));
        lines.add("Description: " + value(export.project().description()));
        lines.add("Domain: " + value(export.project().domain()));
        lines.add("Project status: " + value(export.project().status()));
        lines.add("Exported at: " + value(export.exportedAt()));
        if (export.version() != null) {
            lines.add("Version " + export.version().versionNumber() + " (" + export.version().id() + "): "
                    + value(export.version().title()));
            lines.add("Analysis job ID: " + value(export.version().analysisJobId()));
            lines.add("Version status: " + value(export.version().status()));
            lines.add("Summary: " + value(export.version().summary()));
        }
        lines.add("");
        lines.add("Requirements");
        List<ReviewDtos.RequirementResponse> requirements = list(export.requirements());
        List<ReviewDtos.RequirementQuestionResponse> questions = list(export.questions());
        List<ReviewDtos.ReviewAuditResponse> auditEvents = list(export.auditEvents());
        if (requirements.isEmpty()) {
            lines.add("No requirements.");
        }
        for (ReviewDtos.RequirementResponse requirement : requirements) {
            lines.add(requirement.requirementId() + " | " + requirement.title() + " | "
                    + requirement.priority() + " | " + requirement.status());
            lines.add(value(requirement.description()));
            lines.add("");
        }
        lines.add("Questions");
        if (questions.isEmpty()) {
            lines.add("No questions.");
        }
        for (ReviewDtos.RequirementQuestionResponse question : questions) {
            lines.add(question.questionId() + " | " + question.status() + " | " + question.question());
            lines.add("Answer: " + value(question.answer()));
            lines.add("");
        }
        lines.add("Audit history");
        if (auditEvents.isEmpty()) {
            lines.add("No audit events.");
        }
        for (ReviewDtos.ReviewAuditResponse event : auditEvents) {
            lines.add(value(event.createdAt()) + " | " + event.action() + " | "
                    + value(event.targetKey()) + " | " + value(event.details()));
        }
        return lines;
    }

    private void addRows(Sheet sheet, CellStyle headerStyle, List<List<String>> rows) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex);
            for (int columnIndex = 0; columnIndex < rows.get(rowIndex).size(); columnIndex++) {
                Cell cell = row.createCell(columnIndex);
                cell.setCellValue(rows.get(rowIndex).get(columnIndex));
                if (rowIndex == 0) {
                    cell.setCellStyle(headerStyle);
                }
            }
        }
        if (!rows.isEmpty() && !rows.getFirst().isEmpty()) {
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, rows.size() - 1, 0, rows.getFirst().size() - 1));
            for (int columnIndex = 0; columnIndex < rows.getFirst().size(); columnIndex++) {
                sheet.setColumnWidth(columnIndex, 24 * 256);
            }
        }
    }

    private List<List<String>> rows(List<String> headers, List<List<String>> data) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(headers);
        rows.addAll(data);
        return rows;
    }

    private List<String> wrap(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return List.of(value);
        }
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split("\\s+")) {
            if (!line.isEmpty() && line.length() + word.length() + 1 > maxLength) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private String toPdfText(String value) {
        return value.replaceAll("\\s+", " ").replaceAll("[^\\x20-\\x7E]", "?");
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private <T> List<T> list(List<T> value) {
        return value == null ? List.of() : value;
    }

    private DomainException renderingFailure() {
        return new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "EXPORT_RENDER_FAILED",
                "The export could not be generated.");
    }
}
