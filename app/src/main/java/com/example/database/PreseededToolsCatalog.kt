package com.example.database

import com.example.database.entity.CustomToolEntity
import java.util.UUID

object PreseededToolsCatalog {

    private fun generateDeterministicId(seed: String): String {
        return UUID.nameUUIDFromBytes(seed.toByteArray()).toString()
    }

    fun getAllPreseededTools(): List<CustomToolEntity> {
        val tools = mutableListOf<CustomToolEntity>()
        var sortIndex = 0

        fun addTool(
            seed: String,
            title: String,
            category: String,
            executable: String,
            argsTemplate: String,
            inputType: String,
            accentColor: String,
            description: String,
            icon: String = "terminal",
            isPinned: Boolean = false
        ) {
            tools.add(
                CustomToolEntity(
                    id = generateDeterministicId(seed),
                    title = title,
                    category = category,
                    executable = executable,
                    argsTemplate = argsTemplate,
                    inputType = inputType,
                    accentColor = accentColor,
                    isPinned = isPinned,
                    description = description,
                    icon = icon,
                    sortOrder = sortIndex++,
                    enabled = true,
                    createdAt = System.currentTimeMillis() - (1000L * sortIndex),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // ==========================================
        // 1. MEDIA TOOLS (yt-dlp, ffmpeg, ffprobe)
        // First 6 tools are PINNED for instant dashboard access
        // ==========================================
        addTool(
            seed = "media_ytdlp_video",
            title = "Download Video (Best MP4)",
            category = "Media",
            executable = "yt-dlp",
            argsTemplate = "-f bestvideo[ext=mp4]+bestaudio[ext=m4a]/mp4 --progress --no-mtime -o output/%(title)s.%(ext)s {input}",
            inputType = "TEXT",
            accentColor = "#06B6D4",
            description = "Download best quality MP4 video from YouTube or supported streaming URL",
            icon = "cloud_download",
            isPinned = true // 1st pinned
        )

        addTool(
            seed = "media_ytdlp_audio",
            title = "Extract Audio (MP3 320k)",
            category = "Media",
            executable = "yt-dlp",
            argsTemplate = "-x --audio-format mp3 --audio-quality 0 --progress --no-mtime -o output/%(title)s.%(ext)s {input}",
            inputType = "TEXT",
            accentColor = "#A855F7",
            description = "Extract 320kbps high-fidelity MP3 audio from online stream URL",
            icon = "music_note",
            isPinned = true // 2nd pinned
        )

        addTool(
            seed = "media_ffmpeg_compress_medium",
            title = "FFmpeg Video Compression",
            category = "Media",
            executable = "ffmpeg",
            argsTemplate = "-i {input} -vcodec libx264 -crf 28 -preset faster -acodec aac -b:a 128k -y output/compressed.mp4",
            inputType = "FILE_PATH",
            accentColor = "#F59E0B",
            description = "Two-pass efficient H.264 compression optimized for mobile storage and playback",
            icon = "tune",
            isPinned = true // 3rd pinned
        )

        addTool(
            seed = "media_ffmpeg_gif",
            title = "Video to Animated GIF",
            category = "Media",
            executable = "ffmpeg",
            argsTemplate = "-i {input} -vf fps=12,scale=480:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse -loop 0 -y output/animated.gif",
            inputType = "FILE_PATH",
            accentColor = "#EC4899",
            description = "Convert short video clips into high quality optimized palette animated GIFs",
            icon = "gif",
            isPinned = true // 4th pinned
        )

        addTool(
            seed = "media_ffmpeg_extract_mp3",
            title = "Fast Audio Extraction",
            category = "Media",
            executable = "ffmpeg",
            argsTemplate = "-i {input} -vn -acodec libmp3lame -q:a 2 -y output/extracted_audio.mp3",
            inputType = "FILE_PATH",
            accentColor = "#10B981",
            description = "Strip video track and convert audio directly to variable bit-rate MP3",
            icon = "audiotrack",
            isPinned = true // 5th pinned
        )

        addTool(
            seed = "media_ffmpeg_volume_boost",
            title = "Audio Volume Booster (2x)",
            category = "Media",
            executable = "ffmpeg",
            argsTemplate = "-i {input} -filter:a volume=2.0 -vcodec copy -y output/boosted.mp4",
            inputType = "FILE_PATH",
            accentColor = "#3B82F6",
            description = "Double audio volume gain without touching or re-encoding video stream",
            icon = "volume_up",
            isPinned = true // 6th pinned
        )

        addTool(
            seed = "media_ffmpeg_compress_ultra",
            title = "Ultra Video Squeezer (CRF 34)",
            category = "Media",
            executable = "ffmpeg",
            argsTemplate = "-i {input} -vcodec libx264 -crf 34 -preset veryfast -acodec aac -b:a 64k -y output/tiny.mp4",
            inputType = "FILE_PATH",
            accentColor = "#F97316",
            description = "Maximum size reduction for fast messaging share and storage saving",
            icon = "compress"
        )

        addTool(
            seed = "media_ffprobe_inspect",
            title = "FFprobe Stream Inspector",
            category = "Media",
            executable = "ffprobe",
            argsTemplate = "-v quiet -print_format json -show_format -show_streams {input}",
            inputType = "FILE_PATH",
            accentColor = "#06B6D4",
            description = "Inspect video/audio container, bitrates, sample rates, and codecs in JSON format",
            icon = "info"
        )

        addTool(
            seed = "media_ffprobe_duration",
            title = "FFprobe Media Duration",
            category = "Media",
            executable = "ffprobe",
            argsTemplate = "-v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 {input}",
            inputType = "FILE_PATH",
            accentColor = "#14B8A6",
            description = "Extract exact duration in seconds from any media file without parsing full streams",
            icon = "timer"
        )

        addTool(
            seed = "media_ffmpeg_strip_audio",
            title = "Mute Video (Remove Audio)",
            category = "Media",
            executable = "ffmpeg",
            argsTemplate = "-i {input} -an -vcodec copy -y output/silent_video.mp4",
            inputType = "FILE_PATH",
            accentColor = "#EF4444",
            description = "Remove audio stream completely with zero video re-encoding latency",
            icon = "volume_off"
        )

        addTool(
            seed = "media_ffmpeg_extract_wav",
            title = "Lossless Audio to WAV",
            category = "Media",
            executable = "ffmpeg",
            argsTemplate = "-i {input} -vn -acodec pcm_s16le -ar 44100 -ac 2 -y output/lossless.wav",
            inputType = "FILE_PATH",
            accentColor = "#8B5CF6",
            description = "Extract uncompressed 16-bit 44.1kHz stereo PCM WAV for audio production and speech models",
            icon = "graphic_eq"
        )

        addTool(
            seed = "media_ffmpeg_rotate_cw",
            title = "Rotate Video 90° CW",
            category = "Media",
            executable = "ffmpeg",
            argsTemplate = "-i {input} -vf transpose=1 -c:a copy -y output/rotated.mp4",
            inputType = "FILE_PATH",
            accentColor = "#6366F1",
            description = "Transpose video orientation 90 degrees clockwise preserving original audio",
            icon = "rotate_right"
        )

        // ==========================================
        // 2. DEV & ANDROID TOOLS (apktool, apksigner, aapt, git)
        // ==========================================
        addTool(
            seed = "dev_apktool_decompile",
            title = "APK Decompile (Apktool)",
            category = "Dev",
            executable = "apktool",
            argsTemplate = "d {input} -o output/decompiled_apk -f",
            inputType = "FILE_PATH",
            accentColor = "#A855F7",
            description = "Decompile APK package into smali sources, AndroidManifest.xml, and resources",
            icon = "android"
        )

        addTool(
            seed = "dev_apktool_build",
            title = "APK Rebuild (Apktool)",
            category = "Dev",
            executable = "apktool",
            argsTemplate = "b {input} -o output/rebuilt_unsigned.apk",
            inputType = "DIRECTORY",
            accentColor = "#8B5CF6",
            description = "Reassemble modified decompiled directory back into a flashable APK binary",
            icon = "build"
        )

        addTool(
            seed = "dev_apksigner_verify",
            title = "Verify APK Signature",
            category = "Dev",
            executable = "apksigner",
            argsTemplate = "verify --verbose --print-certs {input}",
            inputType = "FILE_PATH",
            accentColor = "#10B981",
            description = "Verify APK v1, v2, v3 and v4 cryptographic signing certificates and signatures",
            icon = "verified_user"
        )

        addTool(
            seed = "dev_aapt_dump_badging",
            title = "AAPT Dump Badging",
            category = "Dev",
            executable = "aapt",
            argsTemplate = "dump badging {input}",
            inputType = "FILE_PATH",
            accentColor = "#06B6D4",
            description = "Inspect package name, launchable activity, sdk levels, and app metadata",
            icon = "badge"
        )

        addTool(
            seed = "dev_aapt_dump_permissions",
            title = "AAPT Dump Permissions",
            category = "Dev",
            executable = "aapt",
            argsTemplate = "dump permissions {input}",
            inputType = "FILE_PATH",
            accentColor = "#F59E0B",
            description = "List all Android permissions requested by target APK package",
            icon = "security"
        )

        addTool(
            seed = "dev_aapt_list",
            title = "AAPT List Contents",
            category = "Dev",
            executable = "aapt",
            argsTemplate = "list -v -a {input}",
            inputType = "FILE_PATH",
            accentColor = "#3B82F6",
            description = "Inspect complete internal resource tree and compiled xml contents",
            icon = "list"
        )

        addTool(
            seed = "dev_git_status",
            title = "Git Status",
            category = "Dev",
            executable = "git",
            argsTemplate = "status -s -b",
            inputType = "NONE",
            accentColor = "#EF4444",
            description = "Check git working directory status and uncommitted changes",
            icon = "commit"
        )

        addTool(
            seed = "dev_git_log",
            title = "Git Log (Compact)",
            category = "Dev",
            executable = "git",
            argsTemplate = "log -n 10 --oneline --decorate --graph",
            inputType = "NONE",
            accentColor = "#EC4899",
            description = "Display recent 10 commits with branch tags and commit hashes",
            icon = "history"
        )

        addTool(
            seed = "dev_git_diff",
            title = "Git Diff",
            category = "Dev",
            executable = "git",
            argsTemplate = "diff --stat",
            inputType = "NONE",
            accentColor = "#F97316",
            description = "Summarize modified lines and files in current repository",
            icon = "difference"
        )

        addTool(
            seed = "dev_git_branch",
            title = "Git Branch List",
            category = "Dev",
            executable = "git",
            argsTemplate = "branch -a -v",
            inputType = "NONE",
            accentColor = "#14B8A6",
            description = "List local and tracking remote git branches with latest commit hash",
            icon = "alt_route"
        )

        addTool(
            seed = "dev_git_commit_all",
            title = "Git Commit All",
            category = "Dev",
            executable = "git",
            argsTemplate = "commit -a -m \"{input}\"",
            inputType = "TEXT",
            accentColor = "#6366F1",
            description = "Stage modified files and commit with specified message",
            icon = "check_circle"
        )

        addTool(
            seed = "dev_python_run",
            title = "Run Python Script",
            category = "Dev",
            executable = "python3",
            argsTemplate = "{input}",
            inputType = "FILE_PATH",
            accentColor = "#EAB308",
            description = "Execute a local Python3 automation script inside Termux environment",
            icon = "code"
        )

        // ==========================================
        // 3. AI & VOICE TOOLS (piper, edge-tts, whisper, aider)
        // ==========================================
        addTool(
            seed = "ai_edge_tts_christopher",
            title = "Edge-TTS Christopher (en-US)",
            category = "AI",
            executable = "edge-tts",
            argsTemplate = "--voice en-US-ChristopherNeural --text \"{input}\" --write-media output/speech_en.mp3",
            inputType = "TEXT",
            accentColor = "#A855F7",
            description = "Generate crystal-clear American male speech with Microsoft Neural Voice",
            icon = "record_voice_over"
        )

        addTool(
            seed = "ai_edge_tts_madhur",
            title = "Edge-TTS Madhur (hi-IN)",
            category = "AI",
            executable = "edge-tts",
            argsTemplate = "--voice hi-IN-MadhurNeural --text \"{input}\" --write-media output/speech_hi_madhur.mp3",
            inputType = "TEXT",
            accentColor = "#06B6D4",
            description = "Synthesize natural sounding Hindi male narration without cloud API keys",
            icon = "mic"
        )

        addTool(
            seed = "ai_edge_tts_swara",
            title = "Edge-TTS Swara (hi-IN)",
            category = "AI",
            executable = "edge-tts",
            argsTemplate = "--voice hi-IN-SwaraNeural --text \"{input}\" --write-media output/speech_hi_swara.mp3",
            inputType = "TEXT",
            accentColor = "#EC4899",
            description = "Synthesize natural sounding Hindi female speech with neural cadence",
            icon = "record_voice_over"
        )

        addTool(
            seed = "ai_edge_tts_sonia",
            title = "Edge-TTS Sonia (en-GB)",
            category = "AI",
            executable = "edge-tts",
            argsTemplate = "--voice en-GB-SoniaNeural --text \"{input}\" --write-media output/speech_gb.mp3",
            inputType = "TEXT",
            accentColor = "#10B981",
            description = "British accent female narration for audio guides and podcasts",
            icon = "graphic_eq"
        )

        addTool(
            seed = "ai_piper_en",
            title = "Piper Offline Voice (English)",
            category = "AI",
            executable = "piper",
            argsTemplate = "--model en_US-lessac-medium.onnx --output_file output/piper_en.wav",
            inputType = "TEXT",
            accentColor = "#3B82F6",
            description = "Ultra-fast local neural TTS running entirely on device CPU via ONNX",
            icon = "volume_up"
        )

        addTool(
            seed = "ai_piper_hi",
            title = "Piper Offline Voice (Hindi)",
            category = "AI",
            executable = "piper",
            argsTemplate = "--model hi_IN-medium.onnx --output_file output/piper_hi.wav",
            inputType = "TEXT",
            accentColor = "#F59E0B",
            description = "Local Hindi TTS voice synthesized directly on Android device",
            icon = "mic"
        )

        addTool(
            seed = "ai_whisper_transcribe",
            title = "Whisper Audio Transcribe",
            category = "AI",
            executable = "whisper",
            argsTemplate = "{input} --model tiny.en --output_format txt --output_dir output/",
            inputType = "FILE_PATH",
            accentColor = "#8B5CF6",
            description = "Transcribe speech from audio or video files into accurate plaintext transcript",
            icon = "subtitles"
        )

        addTool(
            seed = "ai_whisper_translate",
            title = "Whisper Audio Translate",
            category = "AI",
            executable = "whisper",
            argsTemplate = "{input} --task translate --model base --output_dir output/",
            inputType = "FILE_PATH",
            accentColor = "#14B8A6",
            description = "Translate foreign language speech directly into English subtitles",
            icon = "translate"
        )

        addTool(
            seed = "ai_aider_refactor",
            title = "Aider Code Refactor",
            category = "AI",
            executable = "aider",
            argsTemplate = "--message \"{input}\" --no-git",
            inputType = "TEXT",
            accentColor = "#10B981",
            description = "Dispatch automated code refactoring or feature implementation to local CLI agent",
            icon = "smart_toy"
        )

        addTool(
            seed = "ai_aider_explain",
            title = "Aider Code Explain",
            category = "AI",
            executable = "aider",
            argsTemplate = "--message \"Explain the code architecture and data flow\" --no-git",
            inputType = "NONE",
            accentColor = "#06B6D4",
            description = "Analyze current repository architecture and generate executive summary",
            icon = "psychology"
        )

        // ==========================================
        // 4. NETWORK TOOLS (aria2c, curl, speedtest, ping, traceroute)
        // ==========================================
        addTool(
            seed = "net_aria2c_download",
            title = "Aria2c Turbo Multi-Download",
            category = "Network",
            executable = "aria2c",
            argsTemplate = "-s 16 -x 16 -j 4 -k 1M -d output/ {input}",
            inputType = "TEXT",
            accentColor = "#06B6D4",
            description = "High-performance segmented downloader using 16 concurrent TCP connections",
            icon = "download"
        )

        addTool(
            seed = "net_aria2c_torrent",
            title = "Aria2c Magnet / Torrent",
            category = "Network",
            executable = "aria2c",
            argsTemplate = "--seed-time=0 --max-connection-per-server=8 -d output/ \"{input}\"",
            inputType = "TEXT",
            accentColor = "#EC4899",
            description = "Download file payloads directly from magnet link or torrent URI",
            icon = "file_download"
        )

        addTool(
            seed = "net_curl_headers",
            title = "Inspect HTTP Headers",
            category = "Network",
            executable = "curl",
            argsTemplate = "-I -s -L {input}",
            inputType = "TEXT",
            accentColor = "#3B82F6",
            description = "Fetch and inspect server response headers, status codes, and redirect hops",
            icon = "http"
        )

        addTool(
            seed = "net_curl_post_json",
            title = "cURL POST JSON",
            category = "Network",
            executable = "curl",
            argsTemplate = "-X POST -H \"Content-Type: application/json\" -d '{input}' http://127.0.0.1:8080/api/echo",
            inputType = "TEXT",
            accentColor = "#10B981",
            description = "Send structured JSON payload over HTTP POST to test REST endpoints",
            icon = "send"
        )

        addTool(
            seed = "net_curl_download",
            title = "cURL Direct Download",
            category = "Network",
            executable = "curl",
            argsTemplate = "-O -L --progress-bar {input}",
            inputType = "TEXT",
            accentColor = "#8B5CF6",
            description = "Download target URL directly with redirect following and progress indicator",
            icon = "arrow_downward"
        )

        addTool(
            seed = "net_speedtest",
            title = "Speedtest CLI",
            category = "Network",
            executable = "speedtest",
            argsTemplate = "--simple",
            inputType = "NONE",
            accentColor = "#F59E0B",
            description = "Measure real-time ping latency, download bandwidth, and upload speed",
            icon = "speed"
        )

        addTool(
            seed = "net_ping_4",
            title = "Ping Test (4 Packets)",
            category = "Network",
            executable = "ping",
            argsTemplate = "-c 4 {input}",
            inputType = "TEXT",
            accentColor = "#14B8A6",
            description = "Transmit 4 ICMP echo requests to evaluate packet loss and latency",
            icon = "network_ping"
        )

        addTool(
            seed = "net_traceroute",
            title = "Traceroute Hops",
            category = "Network",
            executable = "traceroute",
            argsTemplate = "-m 15 {input}",
            inputType = "TEXT",
            accentColor = "#F97316",
            description = "Trace network packet gateway hops to identify routing bottlenecks",
            icon = "route"
        )

        addTool(
            seed = "net_wget_mirror",
            title = "Wget Mirror Webpage",
            category = "Network",
            executable = "wget",
            argsTemplate = "-p -k -E -P output/web_mirror/ {input}",
            inputType = "TEXT",
            accentColor = "#6366F1",
            description = "Download complete webpage with css, images, and converted links for offline reading",
            icon = "language"
        )

        addTool(
            seed = "net_listening_ports",
            title = "Check Open Ports (netstat)",
            category = "Network",
            executable = "netstat",
            argsTemplate = "-tuln",
            inputType = "NONE",
            accentColor = "#EF4444",
            description = "List all active TCP and UDP listening sockets and binding addresses",
            icon = "lan"
        )

        // ==========================================
        // 5. SYSTEM & TERMINAL TOOLS (ps, df, free, uptime, tar, du)
        // ==========================================
        addTool(
            seed = "sys_ps_aux",
            title = "Process Snapshot (ps aux)",
            category = "System",
            executable = "ps",
            argsTemplate = "aux",
            inputType = "NONE",
            accentColor = "#06B6D4",
            description = "Inspect all running user and system background processes with CPU/MEM metrics",
            icon = "memory"
        )

        addTool(
            seed = "sys_df_human",
            title = "Storage Breakdown (df -h)",
            category = "System",
            executable = "df",
            argsTemplate = "-h",
            inputType = "NONE",
            accentColor = "#10B981",
            description = "Inspect storage partitions, total capacity, and available free space",
            icon = "sd_storage"
        )

        addTool(
            seed = "sys_free_mem",
            title = "Memory Statistics (free -m)",
            category = "System",
            executable = "free",
            argsTemplate = "-m",
            inputType = "NONE",
            accentColor = "#A855F7",
            description = "Inspect physical RAM, swap space, buffers, and active memory allocation",
            icon = "pie_chart"
        )

        addTool(
            seed = "sys_uptime",
            title = "System Uptime & Load",
            category = "System",
            executable = "uptime",
            argsTemplate = "",
            inputType = "NONE",
            accentColor = "#F59E0B",
            description = "Display how long the Android Linux kernel has been running and load averages",
            icon = "schedule"
        )

        addTool(
            seed = "sys_du_breakdown",
            title = "Disk Usage Tree (du -sh *)",
            category = "System",
            executable = "du",
            argsTemplate = "-sh *",
            inputType = "NONE",
            accentColor = "#EC4899",
            description = "Calculate and summarize disk space consumed by files in current directory",
            icon = "folder_open"
        )

        addTool(
            seed = "sys_uname_info",
            title = "Kernel & Arch (uname -a)",
            category = "System",
            executable = "uname",
            argsTemplate = "-a",
            inputType = "NONE",
            accentColor = "#3B82F6",
            description = "Show Linux kernel version, device architecture (aarch64), and build hostname",
            icon = "computer"
        )

        addTool(
            seed = "sys_whoami",
            title = "Current User & UID",
            category = "System",
            executable = "whoami",
            argsTemplate = "",
            inputType = "NONE",
            accentColor = "#14B8A6",
            description = "Print current Termux process user ID and security context",
            icon = "person"
        )

        addTool(
            seed = "sys_tar_create",
            title = "Create Tarball (.tar.gz)",
            category = "System",
            executable = "tar",
            argsTemplate = "-czvf output/archive.tar.gz {input}",
            inputType = "FILE_PATH",
            accentColor = "#F97316",
            description = "Compress target directory or file list into a gzip-compressed tar archive",
            icon = "archive"
        )

        addTool(
            seed = "sys_tar_extract",
            title = "Extract Tarball Archive",
            category = "System",
            executable = "tar",
            argsTemplate = "-xzvf {input} -C output/",
            inputType = "FILE_PATH",
            accentColor = "#6366F1",
            description = "Decompress tar.gz archive into output directory maintaining permissions",
            icon = "unarchive"
        )

        addTool(
            seed = "sys_unzip_inspect",
            title = "Unzip Archive / Inspect",
            category = "System",
            executable = "unzip",
            argsTemplate = "-l {input}",
            inputType = "FILE_PATH",
            accentColor = "#EAB308",
            description = "List all internal contents, file sizes, and dates of a zip archive",
            icon = "folder_zip"
        )

        addTool(
            seed = "sys_pkill_process",
            title = "Terminate Process by Name",
            category = "System",
            executable = "pkill",
            argsTemplate = "-f {input}",
            inputType = "TEXT",
            accentColor = "#EF4444",
            description = "Gracefully stop running background daemon or CLI process by regex name",
            icon = "cancel"
        )

        return tools
    }
}
