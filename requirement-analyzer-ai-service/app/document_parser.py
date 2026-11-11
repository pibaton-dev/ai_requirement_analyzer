from __future__ import annotations

import base64
import binascii
from io import BytesIO
from zipfile import BadZipFile, ZipFile

import fitz
from docx import Document as WordDocument
from docx.opc.exceptions import PackageNotFoundError
from lxml.etree import XMLSyntaxError
from openpyxl import load_workbook
from openpyxl.utils.exceptions import InvalidFileException

from .schemas import AnalysisDocument

MAX_FILE_BYTES = 10 * 1024 * 1024
MAX_EXPANDED_ARCHIVE_BYTES = 50 * 1024 * 1024
MAX_ARCHIVE_ENTRIES = 2_000
MAX_PDF_PAGES = 200
MAX_WORKSHEETS = 100
MAX_SPREADSHEET_CELLS = 50_000
MAX_EXTRACTED_CHARACTERS = 200_000


class DocumentParsingError(ValueError):
    pass


def decode_document_content(document: AnalysisDocument) -> bytes:
    try:
        content = base64.b64decode(document.contentBase64, validate=True)
    except (binascii.Error, ValueError) as error:
        raise DocumentParsingError("Document content is not valid base64.") from error

    if not content or len(content) > MAX_FILE_BYTES:
        raise DocumentParsingError("Document is empty or exceeds the 10MB limit.")
    return content


def extract_document_text(document: AnalysisDocument, content: bytes | None = None) -> str:
    content = decode_document_content(document) if content is None else content
    if not content or len(content) > MAX_FILE_BYTES:
        raise DocumentParsingError("Document is empty or exceeds the 10MB limit.")

    extension = document.fileName.rsplit(".", 1)[-1].lower() if "." in document.fileName else ""
    expected_types = {
        "pdf": "application/pdf",
        "docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "txt": "text/plain",
        "xlsx": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    }
    if extension not in expected_types or document.contentType.lower() != expected_types[extension]:
        raise DocumentParsingError("Document extension and content type are not supported or do not match.")

    if extension == "pdf":
        text = _extract_pdf(content)
    elif extension == "docx":
        _validate_archive(content, {"[Content_Types].xml", "word/document.xml"})
        text = _extract_docx(content)
    elif extension == "xlsx":
        _validate_archive(content, {"[Content_Types].xml", "xl/workbook.xml"})
        text = _extract_xlsx(content)
    else:
        try:
            text = content.decode("utf-8-sig")
        except UnicodeDecodeError as error:
            raise DocumentParsingError("TXT documents must use UTF-8 encoding.") from error
        if "\x00" in text:
            raise DocumentParsingError("TXT documents cannot contain binary data.")

    text = text.strip()
    if not text:
        raise DocumentParsingError("Document contains no extractable text.")
    if len(text) > MAX_EXTRACTED_CHARACTERS:
        raise DocumentParsingError("Extracted document text exceeds the 200,000 character limit.")
    return text


def _validate_archive(content: bytes, required_entries: set[str]) -> None:
    try:
        with ZipFile(BytesIO(content)) as archive:
            entries = archive.infolist()
            if len(entries) > MAX_ARCHIVE_ENTRIES:
                raise DocumentParsingError("Office document contains too many archive entries.")
            if sum(entry.file_size for entry in entries) > MAX_EXPANDED_ARCHIVE_BYTES:
                raise DocumentParsingError("Office document expands beyond the supported size.")
            names = {entry.filename for entry in entries}
            if not required_entries.issubset(names):
                raise DocumentParsingError("Office document structure is invalid.")
    except BadZipFile as error:
        raise DocumentParsingError("Office document archive is invalid.") from error


def _extract_pdf(content: bytes) -> str:
    if not content.startswith(b"%PDF-"):
        raise DocumentParsingError("PDF signature is invalid.")
    try:
        with fitz.open(stream=content, filetype="pdf") as pdf:
            if pdf.needs_pass:
                raise DocumentParsingError("Password-protected PDF files are not supported.")
            if len(pdf) > MAX_PDF_PAGES:
                raise DocumentParsingError("PDF documents cannot exceed 200 pages.")
            return "\n".join(page.get_text() for page in pdf)
    except (fitz.FileDataError, RuntimeError) as error:
        raise DocumentParsingError("PDF document is invalid.") from error


def _extract_docx(content: bytes) -> str:
    try:
        document = WordDocument(BytesIO(content))
    except (BadZipFile, KeyError, OSError, PackageNotFoundError, ValueError, XMLSyntaxError) as error:
        raise DocumentParsingError("DOCX document is invalid.") from error
    parts = [paragraph.text for paragraph in document.paragraphs if paragraph.text.strip()]
    for table in document.tables:
        for row in table.rows:
            values = [cell.text.strip() for cell in row.cells if cell.text.strip()]
            if values:
                parts.append(" | ".join(values))
    return "\n".join(parts)


def _extract_xlsx(content: bytes) -> str:
    workbook = None
    try:
        workbook = load_workbook(BytesIO(content), read_only=True, data_only=True)
        if len(workbook.worksheets) > MAX_WORKSHEETS:
            raise DocumentParsingError("XLSX documents cannot exceed 100 worksheets.")
        parts: list[str] = []
        cell_count = 0
        for worksheet in workbook.worksheets:
            if worksheet.max_row and worksheet.max_column and worksheet.max_row * worksheet.max_column > MAX_SPREADSHEET_CELLS:
                raise DocumentParsingError("XLSX documents cannot exceed 50,000 cells.")
            parts.append(worksheet.title)
            for row in worksheet.iter_rows(values_only=True):
                cell_count += len(row)
                if cell_count > MAX_SPREADSHEET_CELLS:
                    raise DocumentParsingError("XLSX documents cannot exceed 50,000 cells.")
                values = [str(value).strip() for value in row if value is not None and str(value).strip()]
                if values:
                    parts.append(" | ".join(values))
        return "\n".join(parts)
    except DocumentParsingError:
        raise
    except (BadZipFile, InvalidFileException, KeyError, OSError, TypeError, ValueError, XMLSyntaxError) as error:
        raise DocumentParsingError("XLSX document is invalid.") from error
    finally:
        if workbook is not None:
            workbook.close()
