#!/usr/bin/env python3
"""Convert Android PNG drawables to pixel-identical lossless WebP files."""

from pathlib import Path

from PIL import Image, ImageChops


ROOT = Path(__file__).resolve().parents[1]


def convert(path: Path) -> None:
    target = path.with_suffix(".webp")
    with Image.open(path) as source:
        expected = source.convert("RGBA")
        expected.save(target, "WEBP", lossless=True, method=6, exact=True)
    with Image.open(target) as encoded:
        if ImageChops.difference(expected, encoded.convert("RGBA")).getbbox():
            target.unlink(missing_ok=True)
            raise RuntimeError(f"Pixel verification failed: {path}")
    path.unlink()
    print(f"{path.relative_to(ROOT)} -> {target.relative_to(ROOT)}")


def main() -> None:
    for path in sorted((ROOT / "res").rglob("*.png")):
        convert(path)


if __name__ == "__main__":
    main()
