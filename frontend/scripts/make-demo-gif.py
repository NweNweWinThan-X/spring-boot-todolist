#!/usr/bin/env python3
"""Assemble docs/demo.gif from the frames captured by e2e/demo-video.spec.ts.

Pillow only — no ffmpeg, no ImageMagick. GitHub renders an animated GIF inline in a
README; it will not play a .webm committed to the repository, which is why this is a GIF.

    python3 scripts/make-demo-gif.py <frames-dir> <output.gif>
"""
import sys
from pathlib import Path

from PIL import Image

WIDTH = 900          # downscaled from the 1280px capture to keep the file small
FRAME_MS = 700       # 22 frames * 700ms ~= 15s
LAST_FRAME_MS = 1800  # hold the final frame so the loop does not feel abrupt
MAX_BYTES = 8 * 1024 * 1024


def load(path: Path) -> Image.Image:
    img = Image.open(path).convert("RGB")
    height = round(img.height * WIDTH / img.width)
    return img.resize((WIDTH, height), Image.LANCZOS)


def main() -> int:
    frames_dir = Path(sys.argv[1])
    out = Path(sys.argv[2])

    paths = sorted(frames_dir.glob("*.png"))
    if not paths:
        print(f"no frames in {frames_dir}", file=sys.stderr)
        return 1

    frames = [load(p) for p in paths]
    # A shared adaptive palette keeps colours stable between frames; per-frame palettes
    # make flat UI backgrounds shimmer.
    palette = frames[0].quantize(colors=128, method=Image.MEDIANCUT)
    quantized = [f.quantize(palette=palette, dither=Image.FLOYDSTEINBERG) for f in frames]

    durations = [FRAME_MS] * len(quantized)
    durations[-1] = LAST_FRAME_MS

    out.parent.mkdir(parents=True, exist_ok=True)
    quantized[0].save(
        out,
        save_all=True,
        append_images=quantized[1:],
        duration=durations,
        loop=0,
        optimize=True,
    )

    size = out.stat().st_size
    seconds = sum(durations) / 1000
    print(f"{out}: {len(quantized)} frames, {seconds:.1f}s, {size / 1024 / 1024:.2f} MB")
    if size > MAX_BYTES:
        print(f"WARNING: larger than {MAX_BYTES // 1024 // 1024} MB", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
