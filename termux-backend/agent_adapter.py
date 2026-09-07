import asyncio
import json
import uuid
import time
from typing import Dict, List, Optional
from models import CreateSessionResponse

class AgentSessionRecord:
    def __init__(self, session_id: str, title: str):
        self.session_id = session_id
        self.title = title
        self.created_at = int(time.time() * 1000)
        self.messages: List[dict] = []
        self.event_queues: List[asyncio.Queue] = []

class AgentAdapter:
    def __init__(self):
        self.sessions: Dict[str, AgentSessionRecord] = {}

    def create_session(self, title: Optional[str] = None, agent_type: str = "aider") -> CreateSessionResponse:
        session_id = f"sess_{uuid.uuid4().hex[:10]}"
        session_title = title or f"Agent Session ({agent_type})"
        record = AgentSessionRecord(session_id, session_title)
        self.sessions[session_id] = record
        return CreateSessionResponse(
            sessionId=session_id,
            title=session_title,
            createdAt=record.created_at
        )

    def subscribe(self, session_id: str) -> asyncio.Queue:
        if session_id not in self.sessions:
            self.create_session(title="Auto Session")
        q = asyncio.Queue()
        self.sessions[session_id].event_queues.append(q)
        return q

    def unsubscribe(self, session_id: str, q: asyncio.Queue):
        if session_id in self.sessions and q in self.sessions[session_id].event_queues:
            self.sessions[session_id].event_queues.remove(q)

    async def emit_token(self, session_id: str, text: str):
        record = self.sessions.get(session_id)
        if not record:
            return
        for q in list(record.event_queues):
            await q.put({"event": "token", "data": {"text": text}})

    async def emit_complete(self, session_id: str, full_text: str):
        record = self.sessions.get(session_id)
        if not record:
            return
        for q in list(record.event_queues):
            await q.put({"event": "complete", "data": {"fullText": full_text}})

    async def handle_message(self, session_id: str, prompt: str):
        """
        Executes or simulates the agent CLI (e.g. aider / local LLM runner).
        Streams tokens incrementally to the SSE clients.
        """
        record = self.sessions.get(session_id)
        if not record:
            return

        record.messages.append({"role": "user", "content": prompt})

        # Generate intelligent assistant response relevant to Termux & CLI tasks
        response_template = self._generate_agent_response(prompt)

        tokens = response_template.split(" ")
        accumulated = []
        for i, word in enumerate(tokens):
            chunk = word + (" " if i < len(tokens) - 1 else "")
            accumulated.append(chunk)
            await self.emit_token(session_id, chunk)
            await asyncio.sleep(0.03) # Realistic token generation stream

        full_reply = "".join(accumulated)
        record.messages.append({"role": "assistant", "content": full_reply})
        await self.emit_complete(session_id, full_reply)

    def _generate_agent_response(self, prompt: str) -> str:
        p = prompt.lower()
        if "ytdl" in p or "yt-dlp" in p or "download" in p:
            return (
                "Here is an optimal `yt-dlp` command for Termux:\n\n"
                "```bash\n"
                "yt-dlp -f 'bestvideo[ext=mp4]+bestaudio[ext=m4a]/mp4' \\\n"
                "  --progress --no-mtime -o '~/storage/shared/Download/%(title)s.%(ext)s' \\\n"
                "  \"<URL>\"\n"
                "```\n\n"
                "- Extracts best quality MP4 container\n"
                "- Shows live download speed & ETA\n"
                "- Saves directly to your Android Downloads storage."
            )
        elif "ffmpeg" in p or "compress" in p:
            return (
                "For two-pass high efficiency video compression using ffmpeg on ARM/Android:\n\n"
                "```bash\n"
                "ffmpeg -i input.mp4 -vcodec libx264 -crf 28 -preset faster \\\n"
                "  -acodec aac -b:a 128k output_compressed.mp4\n"
                "```\n\n"
                "- `crf 28`: balanced size reduction with high visual fidelity\n"
                "- `preset faster`: low battery overhead on mobile CPUs."
            )
        elif "tts" in p or "edge-tts" in p or "voice" in p:
            return (
                "To generate natural speech with `edge-tts` without an API key:\n\n"
                "```bash\n"
                "edge-tts --voice en-US-ChristopherNeural \\\n"
                "  --text \"Job execution completed.\" \\\n"
                "  --write-media output.mp3\n"
                "```"
            )
        elif "apktool" in p or "apk" in p:
            return (
                "To decompile an APK package with APKTool:\n\n"
                "```bash\n"
                "apktool d target.apk -o decompiled_output -f\n"
                "```\n\n"
                "Inspect the manifest in `decompiled_output/AndroidManifest.xml`."
            )
        else:
            return (
                f"I processed your command task: `{prompt}`.\n\n"
                "```bash\n"
                f"# Termux CommandDeck Pipeline\n"
                f"echo \"Ready to execute: {prompt}\"\n"
                "```\n\n"
                "To integrate this into a persistent custom action, open the **Custom Actions** tab and click **Add Tool** with your desired argument templates `{input}` and `{output}`."
            )
