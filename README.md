<div align="center">

# 🎙️ CaptionPop

**Free, private, karaoke-style captions — 100% in your browser.**

No upload. No account. No watermark. No subscription.

Your video never leaves your device. AI transcription runs entirely locally.

[🎬 Try the live demo](https://vibecode404.github.io/CaptionPop/) · [How it works](#-how-it-works) · [Privacy](#-privacy)

</div>

---

## ✨ What it does

1. **Drop a video** — MP4, WebM, MOV
2. **Generate captions** — Whisper AI transcribes speech right in your browser, with per-word timestamps
3. **Edit & style** — Fix the transcript, pick fonts, colors, highlight style, position
4. **Preview karaoke** — Watch words light up as they're spoken, CapCut-style
5. **Export MP4** — Captions burned into the video, ready for TikTok / Reels / Shorts

## 🚀 Try it

**🎬 Live demo: https://vibecode404.github.io/CaptionPop/**

No install. No build step. Just open the link in Chrome or Edge (or open `index.html` locally).

> First run downloads the AI model once (~150MB), then it's cached and works fully offline.

## 🔒 Privacy

This is the whole point. Other caption tools upload your video to their servers. CaptionPop:

- Transcribes with Whisper running **locally** via [Transformers.js](https://huggingface.co/docs/transformers.js)
- Renders and exports with **WebCodecs** — also local
- Makes **zero** network requests except downloading the open-source AI model (once) and CDN libraries
- Has no analytics, no tracking, no account

Open DevTools → Network tab and see for yourself.

## 🛠️ How it works

| Step | Tech |
|---|---|
| Speech → text | Whisper (base/tiny/small) via Transformers.js, word-level timestamps |
| Caption timing | Phrases segmented by pauses & punctuation; timestamps monotonically smoothed |
| Preview | HTML overlay synced to video playback |
| Export | WebCodecs decodes frames → captions drawn on canvas → H.264 + AAC re-encoded to MP4 (via mp4box.js + mp4-muxer) |

## 💻 Development

It's a single `index.html` — no bundler, no framework. Dependencies load from CDN:

- `@xenova/transformers` — in-browser Whisper
- `mp4box.js` — MP4 demuxing
- `mp4-muxer` — MP4 muxing

Just edit and refresh. To test export you need Chrome/Edge (WebCodecs).

## 🗺️ Roadmap

- [ ] More caption styles & animations
- [ ] Hindi + other language quality pass
- [ ] SRT/VTT subtitle export
- [ ] Mobile app (Play Store)

## 📄 License

MIT — do whatever you want with it.


## ⚡ Built in a day

This entire project went from idea to launch in ~12 hours on Oct 13, 2026:

- **Morning** — Idea + competitive research + CapCut paywall verification
- **Afternoon** — Whisper transcription spike, validated word-perfect with karaoke timing
- **Afternoon** — Full app built (single HTML file, 29KB)
- **Evening** — Bug fixes (export pipeline, audio encoding, burned-in captions)
- **Night** — Shipped to GitHub + live demo on GitHub Pages

*vibeCode404. You can't find me.* 🥷
