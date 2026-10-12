"""
Generate fallback PNG launcher icons for pre-API-26 devices (minSdk = 24).

Renders the CredStow key icon (same design as the adaptive-icon foreground)
at every Android density, drawn on a solid teal background (#0D9488).

The key paths mirror app/src/main/res/drawable/ic_launcher_foreground.xml
with the same 0.8 scale + centre-pivot so the fallback icon matches the
adaptive icon visually.
"""
import os
from PIL import Image, ImageDraw

# ---------------------------------------------------------------------------
# Colours (mirror ic_launcher_background.xml + ic_launcher_foreground.xml)
# ---------------------------------------------------------------------------
BG = (13, 148, 136)     # #0D9488  – teal background
FG = (255, 255, 255)    # #FFFFFF  – white key icon

# ---------------------------------------------------------------------------
# Density → pixel size (mdpi = 48 is the baseline)
# ---------------------------------------------------------------------------
DENSITIES = {
    "mipmap-mdpi":   48,
    "mipmap-hdpi":   72,
    "mipmap-xhdpi":  96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# ---------------------------------------------------------------------------
# Key geometry (same as the vector drawable, with 0.8 scale around pivot
# (12, 12.4) in viewport space).
#
# We work in a normalised 24×24 viewport and transform to pixel space.
# ---------------------------------------------------------------------------
PIVOT_VX, PIVOT_VY = 12.0, 12.4
SCALE = 0.8

def v2px(vx, vy, icon_size):
    """Transform a viewport coordinate to pixel coordinates with scale + pivot."""
    # Apply scale around pivot, then map viewport (24 units) → icon_size pixels
    sx = PIVOT_VX + (vx - PIVOT_VX) * SCALE
    sy = PIVOT_VY + (vy - PIVOT_VY) * SCALE
    px = sx * (icon_size / 24.0)
    py = sy * (icon_size / 24.0)
    return px, py


def draw_key(draw, icon_size):
    """Draw the key icon (wide bow, shaft, biting, teeth) scaled and centred."""
    s = icon_size / 24.0  # viewport-to-pixel scale (before the 0.8 content scale)

    # Bow: rounded rectangle (7,4)-(17,7) with corner radius 1 in viewport
    bx1, by1 = v2px(7, 4, icon_size)
    bx2, by2 = v2px(17, 7, icon_size)
    radius = 1 * SCALE * s
    draw.rounded_rectangle(
        [bx1, by1, bx2, by2],
        radius=radius,
        fill=FG,
    )

    # Shaft: rect (10.5,7)-(13.5,17.5)
    x1, y1 = v2px(10.5, 7, icon_size)
    x2, y2 = v2px(13.5, 17.5, icon_size)
    draw.rectangle([x1, y1, x2, y2], fill=FG)

    # Biting: rect (7,17.5)-(17,19)
    x1, y1 = v2px(7, 17.5, icon_size)
    x2, y2 = v2px(17, 19, icon_size)
    draw.rectangle([x1, y1, x2, y2], fill=FG)

    # Teeth: (7,19.2)-(8.8,21)  and  (15.2,19.2)-(17,21)
    x1, y1 = v2px(7, 19.2, icon_size)
    x2, y2 = v2px(8.8, 21, icon_size)
    draw.rectangle([x1, y1, x2, y2], fill=FG)
    x1, y1 = v2px(15.2, 19.2, icon_size)
    x2, y2 = v2px(17, 21, icon_size)
    draw.rectangle([x1, y1, x2, y2], fill=FG)


def generate():
    base = "app/src/main/res"
    for dirname, icon_size in DENSITIES.items():
        dirpath = os.path.join(base, dirname)
        os.makedirs(dirpath, exist_ok=True)

        img = Image.new("RGB", (icon_size, icon_size), BG)
        draw = ImageDraw.Draw(img)
        draw_key(draw, icon_size)

        path = os.path.join(dirpath, "ic_launcher.png")
        img.save(path, "PNG", optimize=True)
        print(f"  [OK] {path}  ({icon_size}x{icon_size})")

        # round variant — same image (pre-API-26 round is handled by the launcher)
        path_r = os.path.join(dirpath, "ic_launcher_round.png")
        img.save(path_r, "PNG", optimize=True)
        print(f"  [OK] {path_r}  ({icon_size}x{icon_size})")


if __name__ == "__main__":
    generate()
