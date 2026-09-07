import pytest
from fastapi.testclient import TestClient
from main import app, job_manager

client = TestClient(app)

def test_health_endpoint():
    response = client.post("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert "version" in data
    assert "timestamp" in data

def test_job_creation_and_lookup():
    req = {
        "type": "TEST",
        "executable": "echo",
        "arguments": ["hello", "commanddeck"],
        "description": "Echo test job"
    }
    response = client.post("/jobs", json=req)
    assert response.status_code == 200
    data = response.json()
    assert "jobId" in data
    assert data["status"] in ["QUEUED", "RUNNING", "COMPLETED"]
    job_id = data["jobId"]

    # Lookup job
    get_res = client.get(f"/jobs/{job_id}")
    assert get_res.status_code == 200
    lookup_data = get_res.json()
    assert lookup_data["jobId"] == job_id

def test_invalid_executable_blocked():
    req = {
        "type": "MALICIOUS",
        "executable": "malicious_hack_script",
        "arguments": ["--exploit"]
    }
    response = client.post("/jobs", json=req)
    assert response.status_code == 403
    assert "whitelist" in response.json()["detail"].lower()

def test_job_cancel():
    req = {
        "type": "SLEEP_TEST",
        "executable": "python3",
        "arguments": ["-c", "import time; time.sleep(10)"]
    }
    create_res = client.post("/jobs", json=req)
    if create_res.status_code == 200:
        job_id = create_res.json()["jobId"]
        cancel_res = client.post(f"/jobs/{job_id}/cancel")
        assert cancel_res.status_code == 200
        assert cancel_res.json()["success"] is True

def test_list_jobs():
    response = client.get("/jobs")
    assert response.status_code == 200
    assert isinstance(response.json(), list)
