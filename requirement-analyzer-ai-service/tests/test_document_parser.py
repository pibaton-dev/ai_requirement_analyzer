import base64
from io import BytesIO
from uuid import uuid4

import fitz
import pytest
from docx import Document as WordDocument
from openpyxl import Workbook

from app.document_parser import DocumentParsingError, extract_document_text
from app.schemas import AnalysisDocument


def analysis_document(file_name: str, content_type: str, content: bytes) -> AnalysisDocument:
    return AnalysisDocument(
        documentId=uuid4(),
        fileName=file_name,
        contentType=content_type,
        contentBase64=base64.b64encode(content).decode("ascii"),
    )


def test_extracts_plain_text() -> None:
    document = analysis_document("requirements.txt", "text/plain", b"Users can download reports.")

    assert extract_document_text(document) == "Users can download reports."


def test_extracts_pdf_text() -> None:
    pdf = fitz.open()
    page = pdf.new_page()
    page.insert_text((72, 72), "Users can download reports.")
    content = pdf.tobytes()
    pdf.close()

    document = analysis_document("requirements.pdf", "application/pdf", content)

    assert "Users can download reports." in extract_document_text(document)


def test_extracts_docx_paragraphs_and_tables() -> None:
    word_document = WordDocument()
    word_document.add_paragraph("Users can download reports.")
    table = word_document.add_table(rows=1, cols=2)
    table.cell(0, 0).text = "Manager"
    table.cell(0, 1).text = "Approves report access"
    content = BytesIO()
    word_document.save(content)

    document = analysis_document(
        "requirements.docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        content.getvalue(),
    )

    extracted = extract_document_text(document)
    assert "Users can download reports." in extracted
    assert "Manager | Approves report access" in extracted


def test_extracts_xlsx_sheet_and_cell_values() -> None:
    workbook = Workbook()
    worksheet = workbook.active
    worksheet.title = "Permissions"
    worksheet.append(["Role", "Access"])
    worksheet.append(["Manager", "Approve reports"])
    content = BytesIO()
    workbook.save(content)

    document = analysis_document(
        "requirements.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        content.getvalue(),
    )

    extracted = extract_document_text(document)
    assert "Permissions" in extracted
    assert "Manager | Approve reports" in extracted


@pytest.mark.parametrize(
    ("file_name", "content_type", "content"),
    [
        ("requirements.pdf", "application/pdf", b"not a PDF"),
        ("requirements.txt", "text/plain", b"\xff\xfe"),
        ("requirements.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", b"not a ZIP"),
    ],
)
def test_rejects_invalid_document_content(file_name: str, content_type: str, content: bytes) -> None:
    with pytest.raises(DocumentParsingError):
        extract_document_text(analysis_document(file_name, content_type, content))


def test_rejects_extension_and_mime_mismatch() -> None:
    document = analysis_document("requirements.pdf", "text/plain", b"Users can download reports.")

    with pytest.raises(DocumentParsingError, match="extension and content type"):
        extract_document_text(document)
