import asyncio
import json
import uuid
import time
from typing import Dict, List, Optional
from models import CreateSessionResponse

class AgentSessionRecord:
    def __init__(self, session_id: str, title: str, agent_type: str = "aider"):
        self.session_id = session_id
        self.title = title
        self.agent_type = agent_type
        self.created_at = int(time.time() * 1000)
        self.messages: List[dict] = []
        self.event_queues: List[asyncio.Queue] = []

class AgentAdapter:
    # Catalog knowledge for tool schema recommendation
    TOOL_CATALOG = {
        "yt-dlp": {"category": "Media", "executable": "yt-dlp", "args": "-f 'bestvideo[ext=mp4]+bestaudio[ext=m4a]/mp4' --progress -o '~/storage/shared/Download/%(title)s.%(ext)s' \"{url}\""},
        "ffmpeg": {"category": "Media", "executable": "ffmpeg", "args": "-i \"{input}\" -vcodec libx264 -crf 28 -preset faster \"{output}\""},
        "ffprobe": {"category": "Media", "executable": "ffprobe", "args": "-v error -show_format -show_streams \"{input}\""},
        "piper": {"category": "AI & Voice", "executable": "piper", "args": "--model hi_IN-rohit-medium.onnx --output_file output.wav"},
        "whisper": {"category": "AI & Voice", "executable": "whisper", "args": "\"{input}\" --model base --output_format txt"},
        "edge-tts": {"category": "AI & Voice", "executable": "edge-tts", "args": "--voice en-US-ChristopherNeural --text \"{text}\" --write-media output.mp3"},
        "apktool": {"category": "Dev & Android", "executable": "apktool", "args": "d \"{input}\" -o decompiled_output -f"},
        "apksigner": {"category": "Dev & Android", "executable": "apksigner", "args": "verify --verbose \"{input}\""},
        "aapt": {"category": "Dev & Android", "executable": "aapt", "args": "dump badging \"{input}\""},
        "git": {"category": "Dev & Android", "executable": "git", "args": "status --short"},
        "aria2c": {"category": "Network", "executable": "aria2c", "args": "-x 16 -s 16 -k 1M -d ~/storage/shared/Download \"{url}\""},
        "curl": {"category": "Network", "executable": "curl", "args": "-I -L \"{url}\""},
        "nmap": {"category": "Network", "executable": "nmap", "args": "-F -sT 127.0.0.1"},
        "ping": {"category": "Network", "executable": "ping", "args": "-c 4 1.1.1.1"},
        "htop": {"category": "System", "executable": "htop", "args": ""},
        "df": {"category": "System", "executable": "df", "args": "-h"}
    }

    def __init__(self):
        self.sessions: Dict[str, AgentSessionRecord] = {}

    def create_session(self, title: Optional[str] = None, agent_type: str = "aider") -> CreateSessionResponse:
        session_id = f"sess_{uuid.uuid4().hex[:10]}"
        session_title = title or f"Agent Session ({agent_type})"
        record = AgentSessionRecord(session_id, session_title, agent_type=agent_type)
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
        Streams tokens incrementally to SSE clients.
        """
        record = self.sessions.get(session_id)
        if not record:
            return

        record.messages.append({"role": "user", "content": prompt})

        # Generate intelligent assistant response relevant to Termux & CLI tasks
        response_template = self._generate_agent_response(prompt, record.agent_type)

        tokens = response_template.split(" ")
        accumulated = []
        for i, word in enumerate(tokens):
            chunk = word + (" " if i < len(tokens) - 1 else "")
            accumulated.append(chunk)
            await self.emit_token(session_id, chunk)
            await asyncio.sleep(0.02)  # Stream tokens smoothly

        full_reply = "".join(accumulated)
        record.messages.append({"role": "assistant", "content": full_reply})
        await self.emit_complete(session_id, full_reply)

    def _generate_agent_response(self, prompt: str, agent_type: str = "aider") -> str:
        p = prompt.lower()
        engine_badge = f"[{agent_type.upper()}] "

        if "ytdl" in p or "yt-dlp" in p or "download" in p:
            return (
                f"{engine_badge}Here is the optimal `yt-dlp` pipeline from the Media catalog:\n\n"
                "```bash\n"
                "yt-dlp -f 'bestvideo[ext=mp4]+bestaudio[ext=m4a]/mp4' \\\n"
                "  --progress --no-mtime -o '~/storage/shared/Download/%(title)s.%(ext)s' \\\n"
                "  \"<URL>\"\n"
                "```\n\n"
                "- Extracts best quality MP4 container\n"
                "- Shows live download speed & ETA\n"
                "- Saves directly to Android storage.\n\n"
                "💡 *Tip: You can launch this directly with 1-tap from the 'Pinned Tools' carousel or full Catalog.*"
            )
        elif "ffmpeg" in p or "compress" in p:
            return (
                f"{engine_badge}For high efficiency video compression using ffmpeg on ARM/Termux:\n\n"
                "```bash\n"
                "ffmpeg -i input.mp4 -vcodec libx264 -crf 28 -preset faster \\\n"
                "  -acodec aac -b:a 128k output_compressed.mp4\n"
                "```\n\n"
                "- `crf 28`: High compression ratio with crisp visuals\n"
                "- `preset faster`: Energy-efficient mobile CPU scheduling."
            )
        elif "tts" in p or "edge-tts" in p or "voice" in p:
            return (
                f"{engine_badge}For neural speech synthesis using `edge-tts`:\n\n"
                "```bash\n"
                "edge-tts --voice en-US-ChristopherNeural \\\n"
                "  --text \"Command execution completed successfully.\" \\\n"
                "  --write-media output.mp3\n"
                "```\n\n"
                "For Hindi voice synthesis, use `--voice hi-IN-MadhurNeural` or `piper --model hi_IN-rohit-medium.onnx`."
            )
        elif "piper" in p or "hindi" in p:
            return (
                f"{engine_badge}For offline lightweight neural TTS via Piper:\n\n"
                "```bash\n"
                "echo \"नमस्ते, कमांडडेक तैयार है।\" | piper \\\n"
                "  --model hi_IN-rohit-medium.onnx --output_file output_hi.wav\n"
                "```"
            )
        elif "whisper" in p or "transcribe" in p:
            return (
                f"{engine_badge}For on-device audio transcription with Whisper CLI:\n\n"
                "```bash\n"
                "whisper sample.mp3 --model base --output_format txt\n"
                "```"
            )
        elif "aria2c" in p or "fast download" in p:
            return (
                f"{engine_badge}Accelerate multi-connection downloads using `aria2c`:\n\n"
                "```bash\n"
                "aria2c -x 16 -s 16 -k 1M -d ~/storage/shared/Download \"<URL>\"\n"
                "```"
            )
        elif "apktool" in p or "apk" in p:
            return (
                f"{engine_badge}To decompile and analyze an APK package:\n\n"
                "```bash\n"
                "apktool d target.apk -o decompiled_output -f\n"
                "```\n\n"
                "To sign the modified APK afterwards, use `apksigner sign --ks debug.keystore decompiled.apk`."
            )
        else:
            return (
                f"{engine_badge}I processed your task: `{prompt}`.\n\n"
                "```bash\n"
                f"# Termux CommandDeck Pipeline ({agent_type})\n"
                f"echo \"Executing pipeline for: {prompt}\"\n"
                "```\n\n"
                "You can run this task immediately using the 50+ Pre-seeded Toolset in CommandDeck, or create a customized action in the Action Editor."
            )

