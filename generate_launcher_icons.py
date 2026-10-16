"""
Generate fallback PNG launcher icons for pre-API-26 devices (minSdk = 24).

Renders the CredStow shield-and-lock mark on a navy field (#102A43),
matching the adaptive-icon foreground punched-lock design.
"""
from __future__ import annotations

import os

from PIL import Image, ImageDraw, ImageFilter

BG = (16, 42, 67)  # #102A43
FG = (255, 255, 255)

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Content scale inside the 24-unit viewport (legacy PNGs are full-bleed tiles).
SCALE = 0.72
PIVOT = 12.0
SUPERSAMPLE = 8


def v2px(vx: float, vy: float, icon_size: int) -> tuple[float, float]:
    sx = PIVOT + (vx - PIVOT) * SCALE
    sy = PIVOT + (vy - PIVOT) * SCALE
    return sx * (icon_size / 24.0), sy * (icon_size / 24.0)


def _cubic(p0, p1, p2, p3, steps: int) -> list[tuple[float, float]]:
    pts: list[tuple[float, float]] = []
    for i in range(steps + 1):
        t = i / steps
        u = 1.0 - t
        x = u**3 * p0[0] + 3 * u**2 * t * p1[0] + 3 * u * t**2 * p2[0] + t**3 * p3[0]
        y = u**3 * p0[1] + 3 * u**2 * t * p1[1] + 3 * u * t**2 * p2[1] + t**3 * p3[1]
        pts.append((x, y))
    return pts


def shield_points(icon_size: int) -> list[tuple[float, float]]:
    # Mirrors ic_app_mark.xml cubics so PNG fallbacks match the adaptive vector.
    raw: list[tuple[float, float]] = []
    raw += _cubic((12.0, 2.6), (15.55, 3.85), (18.55, 5.05), (19.05, 5.3), 10)
    raw += _cubic((19.05, 5.3), (19.35, 5.45), (19.5, 5.75), (19.5, 6.1), 6)[1:]
    raw.append((19.5, 12.35))
    raw += _cubic((19.5, 12.35), (19.5, 16.45), (16.4, 19.55), (12.0, 21.45), 12)[1:]
    raw += _cubic((12.0, 21.45), (7.6, 19.55), (4.5, 16.45), (4.5, 12.35), 12)[1:]
    raw.append((4.5, 6.1))
    raw += _cubic((4.5, 6.1), (4.5, 5.75), (4.65, 5.45), (4.95, 5.3), 6)[1:]
    raw += _cubic((4.95, 5.3), (5.45, 5.05), (8.45, 3.85), (12.0, 2.6), 10)[1:]
    return [v2px(x, y, icon_size) for x, y in raw]


def draw_mark(draw: ImageDraw.ImageDraw, icon_size: int) -> None:
    draw.polygon(shield_points(icon_size), fill=FG)
    body = [
        *v2px(8.7, 11.7, icon_size),
        *v2px(15.3, 17.65, icon_size),
    ]
    radius = 1.5 * SCALE * (icon_size / 24.0)
    draw.rounded_rectangle(body, radius=radius, fill=BG)
    # Shackle as a thick arc sitting on the lock body.
    cx, cy = v2px(12.0, 11.7, icon_size)
    outer_r = 2.45 * SCALE * (icon_size / 24.0)
    width = 1.2 * SCALE * (icon_size / 24.0)
    box = [cx - outer_r, cy - outer_r, cx + outer_r, cy + outer_r]
    draw.arc(box, start=180, end=0, fill=BG, width=max(2, int(round(width))))
    hole_r = 0.9 * SCALE * (icon_size / 24.0)
    hx, hy = v2px(12.0, 14.55, icon_size)
    draw.ellipse([hx - hole_r, hy - hole_r, hx + hole_r, hy + hole_r], fill=FG)


def render_icon(icon_size: int, circular: bool) -> Image.Image:
    hi = icon_size * SUPERSAMPLE
    img = Image.new("RGB", (hi, hi), BG)
    draw = ImageDraw.Draw(img)
    draw_mark(draw, hi)
    img = img.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
    if not circular:
        return img
    mask = Image.new("L", (icon_size, icon_size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, icon_size - 1, icon_size - 1), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(0.4))
    round_img = Image.new("RGBA", (icon_size, icon_size), (0, 0, 0, 0))
    rgba = img.convert("RGBA")
    round_img.paste(rgba, mask=mask)
    # Composite onto navy so launchers that ignore alpha still look solid.
    out = Image.new("RGB", (icon_size, icon_size), BG)
    out.paste(round_img, mask=round_img.split()[-1])
    return out


def generate() -> None:
    base = os.path.join("app", "src", "main", "res")
    for dirname, icon_size in DENSITIES.items():
        dirpath = os.path.join(base, dirname)
        os.makedirs(dirpath, exist_ok=True)
        square = render_icon(icon_size, circular=False)
        path = os.path.join(dirpath, "ic_launcher.png")
        square.save(path, "PNG", optimize=True)
        print(f"  [OK] {path}  ({icon_size}x{icon_size})")
        round_img = render_icon(icon_size, circular=True)
        path_r = os.path.join(dirpath, "ic_launcher_round.png")
        round_img.save(path_r, "PNG", optimize=True)
        print(f"  [OK] {path_r}  ({icon_size}x{icon_size})")


if __name__ == "__main__":
    generate()
