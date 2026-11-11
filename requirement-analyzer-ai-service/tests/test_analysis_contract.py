import base64

from fastapi.testclient import TestClient

from app.main import app


def test_analyze_endpoint_returns_structured_requirement_payload(monkeypatch) -> None:
    monkeypatch.setenv("AI_SERVICE_API_KEY", "test-service-token")
    response = TestClient(app).post(
        "/api/v1/analyze",
        headers={
            "X-Service-Token": "test-service-token",
            "X-Correlation-ID": "analysis-test-001",
        },
        json={
            "projectId": "11111111-1111-4111-8111-111111111111",
            "projectName": "Attendance Platform",
            "text": "Users must sign in to check attendance and view reports.",
            "documentIds": [],
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["projectId"] == "11111111-1111-4111-8111-111111111111"
    assert payload["correlationId"] == "analysis-test-001"
    assert response.headers["X-Correlation-ID"] == "analysis-test-001"
    assert payload["requirements"][0]["type"] == "FUNCTIONAL"
    assert payload["requirements"][0]["title"]
    assert payload["issues"]


def test_analyze_endpoint_requires_text_or_documents(monkeypatch) -> None:
    monkeypatch.setenv("AI_SERVICE_API_KEY", "test-service-token")
    response = TestClient(app).post(
        "/api/v1/analyze",
        headers={"X-Service-Token": "test-service-token"},
        json={
            "projectId": "22222222-2222-4222-8222-222222222222",
            "projectName": "Empty Project",
            "text": "",
            "documentIds": [],
        },
    )

    assert response.status_code == 422


def test_analyze_endpoint_parses_uploaded_txt_content(monkeypatch) -> None:
    monkeypatch.setenv("AI_SERVICE_API_KEY", "test-service-token")
    document_id = "33333333-3333-4333-8333-333333333333"
    response = TestClient(app).post(
        "/api/v1/analyze",
        headers={"X-Service-Token": "test-service-token"},
        json={
            "projectId": "11111111-1111-4111-8111-111111111111",
            "projectName": "Attendance Platform",
            "text": "",
            "documentIds": [document_id],
            "documents": [{
                "documentId": document_id,
                "fileName": "attendance.txt",
                "contentType": "text/plain",
                "contentBase64": base64.b64encode(b"Teachers can submit attendance for each class.").decode(),
            }],
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["summary"] == "Teachers can submit attendance for each class."
    assert payload["requirements"][0]["title"] == "Teachers can submit attendance for each class"
    assert payload["requirements"][0]["description"] == "Teachers can submit attendance for each class"


def test_analyze_endpoint_rejects_mismatched_document_metadata(monkeypatch) -> None:
    monkeypatch.setenv("AI_SERVICE_API_KEY", "test-service-token")
    response = TestClient(app).post(
        "/api/v1/analyze",
        headers={"X-Service-Token": "test-service-token"},
        json={
            "projectId": "11111111-1111-4111-8111-111111111111",
            "projectName": "Attendance Platform",
            "text": "",
            "documentIds": [],
            "documents": [{
                "documentId": "33333333-3333-4333-8333-333333333333",
                "fileName": "attendance.txt",
                "contentType": "text/plain",
                "contentBase64": base64.b64encode(b"Teachers take attendance.").decode(),
            }],
        },
    )

    assert response.status_code == 422


def test_analyze_endpoint_rejects_invalid_document_content(monkeypatch) -> None:
    monkeypatch.setenv("AI_SERVICE_API_KEY", "test-service-token")
    document_id = "33333333-3333-4333-8333-333333333333"
    response = TestClient(app).post(
        "/api/v1/analyze",
        headers={"X-Service-Token": "test-service-token"},
        json={
            "projectId": "11111111-1111-4111-8111-111111111111",
            "projectName": "Attendance Platform",
            "text": "",
            "documentIds": [document_id],
            "documents": [{
                "documentId": document_id,
                "fileName": "attendance.pdf",
                "contentType": "application/pdf",
                "contentBase64": base64.b64encode(b"not a PDF").decode(),
            }],
        },
    )

    assert response.status_code == 422
    assert response.json()["detail"] == "One or more documents could not be parsed."


def test_analyze_endpoint_rejects_missing_or_invalid_service_credentials(monkeypatch) -> None:
    monkeypatch.setenv("AI_SERVICE_API_KEY", "test-service-token")
    client = TestClient(app)
    payload = {
        "projectId": "11111111-1111-4111-8111-111111111111",
        "projectName": "Attendance Platform",
        "text": "Users must sign in.",
        "documentIds": [],
    }

    assert client.post("/api/v1/analyze", json=payload).status_code == 401
    assert client.post(
        "/api/v1/analyze",
        headers={"X-Service-Token": "wrong-token"},
        json=payload,
    ).status_code == 401


def test_analyze_endpoint_fails_closed_when_service_token_is_unconfigured(monkeypatch) -> None:
    monkeypatch.delenv("AI_SERVICE_API_KEY", raising=False)
    response = TestClient(app).post(
        "/api/v1/analyze",
        headers={"X-Service-Token": "test-service-token"},
        json={
            "projectId": "11111111-1111-4111-8111-111111111111",
            "projectName": "Attendance Platform",
            "text": "Users must sign in.",
            "documentIds": [],
        },
    )

    assert response.status_code == 503
