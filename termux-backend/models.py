from enum import Enum
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field
import time

class JobStatus(str, Enum):
    QUEUED = "QUEUED"
    RUNNING = "RUNNING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"

class OutputFileModel(BaseModel):
    fileId: str
    filename: str
    sizeBytes: int = 0
    mimeType: str = "application/octet-stream"
    downloadUrl: str = ""

class JobCreateRequest(BaseModel):
    type: str = "CLI"
    executable: str
    arguments: List[str] = Field(default_factory=list)
    description: Optional[str] = ""

class JobModel(BaseModel):
    jobId: str
    type: str
    command: str
    status: JobStatus
    progress: float = 0.0
    speed: Optional[str] = None
    eta: Optional[str] = None
    startedAt: int = Field(default_factory=lambda: int(time.time() * 1000))
    updatedAt: int = Field(default_factory=lambda: int(time.time() * 1000))
    completedAt: Optional[int] = None
    exitCode: Optional[int] = None
    stdout: List[str] = Field(default_factory=list)
    stderr: List[str] = Field(default_factory=list)
    outputFiles: List[OutputFileModel] = Field(default_factory=list)
    errorMessage: Optional[str] = None

class JobCancelResponse(BaseModel):
    success: bool
    message: str

class HealthResponse(BaseModel):
    status: str = "ok"
    version: str = "1.0.0"
    timestamp: int = Field(default_factory=lambda: int(time.time() * 1000))

class CreateSessionRequest(BaseModel):
    title: Optional[str] = "CLI Assistant Session"
    agentType: Optional[str] = "aider"

class CreateSessionResponse(BaseModel):
    sessionId: str
    title: str
    createdAt: int

class SendMessageRequest(BaseModel):
    message: str
