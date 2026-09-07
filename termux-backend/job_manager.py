import asyncio
import os
import re
import signal
import subprocess
import time
import uuid
from pathlib import Path
from typing import Dict, List, Optional
import mimetypes

from models import JobModel, JobStatus, OutputFileModel
from security import SecurityValidator

class JobManager:
    def __init__(self, base_dir: Optional[str] = None):
        if base_dir:
            self.base_dir = Path(base_dir)
        else:
            home = os.environ.get("HOME", "/data/data/com.termux/files/home")
            self.base_dir = Path(home) / "termux-commanddeck"

        self.jobs_dir = self.base_dir / "jobs"
        self.jobs_dir.mkdir(parents=True, exist_ok=True)

        self.jobs: Dict[str, JobModel] = {}
        self.processes: Dict[str, subprocess.Popen] = {}
        self.event_queues: Dict[str, List[asyncio.Queue]] = {}
        self.security = SecurityValidator()

    def get_job_dir(self, job_id: str) -> Path:
        return self.jobs_dir / job_id

    def create_job(self, job_type: str, executable: str, arguments: List[str], description: str = "") -> JobModel:
        # Validate executable structurally
        resolved_bin = self.security.validate_executable(executable)
        sanitized_args = self.security.sanitize_arguments(arguments)

        job_id = f"job_{uuid.uuid4().hex[:12]}"
        job_dir = self.get_job_dir(job_id)

        # Create structured subdirectories
        (job_dir / "input").mkdir(parents=True, exist_ok=True)
        (job_dir / "output").mkdir(parents=True, exist_ok=True)
        (job_dir / "logs").mkdir(parents=True, exist_ok=True)

        command_display = f"{executable} {' '.join(sanitized_args)}".strip()

        job = JobModel(
            jobId=job_id,
            type=job_type,
            command=command_display,
            status=JobStatus.QUEUED,
            progress=0.0
        )
        self.jobs[job_id] = job
        self.event_queues[job_id] = []

        # Start execution in background task
        asyncio.create_task(self._run_job_process(job_id, resolved_bin, sanitized_args))

        return job

    async def _emit_event(self, job_id: str, event_type: str, payload: dict):
        queues = self.event_queues.get(job_id, [])
        for q in list(queues):
            await q.put({"event": event_type, "data": payload})

    def subscribe_events(self, job_id: str) -> asyncio.Queue:
        if job_id not in self.event_queues:
            self.event_queues[job_id] = []
        q = asyncio.Queue()
        self.event_queues[job_id].append(q)
        return q

    def unsubscribe_events(self, job_id: str, q: asyncio.Queue):
        if job_id in self.event_queues and q in self.event_queues[job_id]:
            self.event_queues[job_id].remove(q)

    async def _run_job_process(self, job_id: str, executable_path: str, arguments: List[str]):
        job = self.jobs.get(job_id)
        if not job:
            return

        job_dir = self.get_job_dir(job_id)
        log_file_path = job_dir / "logs" / "stdout.log"

        job.status = JobStatus.RUNNING
        job.updatedAt = int(time.time() * 1000)
        await self._emit_event(job_id, "status", {"status": "RUNNING"})

        cmd = [executable_path] + arguments

        try:
            # We execute structurally with subprocess.Popen, NEVER shell=True
            proc = subprocess.Popen(
                cmd,
                cwd=str(job_dir),
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                bufsize=1,
                preexec_fn=os.setsid if hasattr(os, "setsid") else None
            )
            self.processes[job_id] = proc

            loop = asyncio.get_running_loop()

            async def read_stream(stream, is_stderr: bool):
                with open(log_file_path, "a", encoding="utf-8", errors="replace") as log_file:
                    while True:
                        line = await loop.run_in_executor(None, stream.readline)
                        if not line:
                            break
                        line_clean = line.rstrip("\r\n")
                        if not line_clean:
                            continue

                        log_file.write(line_clean + "\n")
                        log_file.flush()

                        if is_stderr:
                            job.stderr.append(line_clean)
                            if len(job.stderr) > 200:
                                job.stderr.pop(0)
                            await self._emit_event(job_id, "stderr", {"line": line_clean})
                        else:
                            job.stdout.append(line_clean)
                            if len(job.stdout) > 200:
                                job.stdout.pop(0)
                            await self._emit_event(job_id, "stdout", {"line": line_clean})

                        # Progress parser for yt-dlp, ffmpeg, edge-tts
                        self._parse_progress(job, line_clean)
                        if job.progress > 0:
                            await self._emit_event(job_id, "progress", {
                                "percent": job.progress,
                                "speed": job.speed,
                                "eta": job.eta
                            })

            await asyncio.gather(
                read_stream(proc.stdout, False),
                read_stream(proc.stderr, True)
            )

            exit_code = await loop.run_in_executor(None, proc.wait)
            job.exitCode = exit_code
            job.completedAt = int(time.time() * 1000)
            job.updatedAt = int(time.time() * 1000)

            # Discover output files in output/
            self._discover_output_files(job, job_dir / "output")

            for out_file in job.outputFiles:
                await self._emit_event(job_id, "file", out_file.model_dump())

            if exit_code == 0:
                job.status = JobStatus.COMPLETED
                job.progress = 100.0
                await self._emit_event(job_id, "completed", {"exitCode": 0})
            else:
                if job.status != JobStatus.CANCELLED:
                    job.status = JobStatus.FAILED
                    job.errorMessage = f"Process exited with non-zero code {exit_code}"
                    await self._emit_event(job_id, "failed", {"exitCode": exit_code, "error": job.errorMessage})

        except Exception as e:
            job.status = JobStatus.FAILED
            job.errorMessage = str(e)
            job.completedAt = int(time.time() * 1000)
            await self._emit_event(job_id, "failed", {"exitCode": None, "error": str(e)})
        finally:
            self.processes.pop(job_id, None)

    def _parse_progress(self, job: JobModel, line: str):
        # yt-dlp: [download]  42.1% of ~ 50.00MiB at  5.20MiB/s ETA 00:05
        ytdl_match = re.search(r"\[download\]\s+([\d\.]+)%\s+of\s+.*?at\s+([\w\.\/]+)\s+ETA\s+([\d:]+)", line)
        if ytdl_match:
            try:
                job.progress = float(ytdl_match.group(1))
                job.speed = ytdl_match.group(2)
                job.eta = ytdl_match.group(3)
                return
            except ValueError:
                pass

        # ffmpeg: size= 1024kB time=00:01:23.45 bitrate= 100.0kbits/s speed= 2.5x
        ffmpeg_speed = re.search(r"speed=\s*([\d\.]+x)", line)
        if ffmpeg_speed:
            job.speed = ffmpeg_speed.group(1)

        ffmpeg_pct = re.search(r"progress=\s*([\d\.]+)%", line)
        if ffmpeg_pct:
            try:
                job.progress = float(ffmpeg_pct.group(1))
            except ValueError:
                pass

    def _discover_output_files(self, job: JobModel, output_dir: Path):
        if not output_dir.exists():
            return
        files = []
        for p in output_dir.rglob("*"):
            if p.is_file():
                file_id = f"f_{uuid.uuid4().hex[:10]}"
                mime, _ = mimetypes.guess_type(p.name)
                files.append(OutputFileModel(
                    fileId=file_id,
                    filename=p.name,
                    sizeBytes=p.stat().st_size,
                    mimeType=mime or "application/octet-stream",
                    downloadUrl=f"/files/{file_id}"
                ))
        job.outputFiles = files

    def cancel_job(self, job_id: str) -> bool:
        job = self.jobs.get(job_id)
        proc = self.processes.get(job_id)

        if not job:
            return False

        if job.status in [JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.CANCELLED]:
            return True

        job.status = JobStatus.CANCELLED
        job.completedAt = int(time.time() * 1000)
        job.errorMessage = "Cancelled by user"

        if proc:
            try:
                if hasattr(os, "killpg") and hasattr(os, "getpgid"):
                    os.killpg(os.getpgid(proc.pid), signal.SIGTERM)
                else:
                    proc.terminate()
            except Exception:
                try:
                    proc.kill()
                except Exception:
                    pass

        asyncio.create_task(self._emit_event(job_id, "cancelled", {"reason": "Cancelled by user"}))
        return True
