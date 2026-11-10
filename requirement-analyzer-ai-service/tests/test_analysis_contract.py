from fastapi.testclient import TestClient

from app.main import app


def test_analyze_endpoint_returns_structured_requirement_payload() -> None:
    response = TestClient(app).post(
        "/api/v1/analyze",
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
    assert payload["correlationId"]
    assert payload["requirements"][0]["type"] == "FUNCTIONAL"
    assert payload["requirements"][0]["title"]
    assert payload["issues"]


def test_analyze_endpoint_requires_text_or_documents() -> None:
    response = TestClient(app).post(
        "/api/v1/analyze",
        json={
            "projectId": "22222222-2222-4222-8222-222222222222",
            "projectName": "Empty Project",
            "text": "",
            "documentIds": [],
        },
    )

    assert response.status_code == 422
