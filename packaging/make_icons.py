#!/usr/bin/env python3
"""Draw the Calendar app icon and generate AppIcon.icns (macOS only) and app.ico.

Run from anywhere:  python3 packaging/make_icons.py
Needs Pillow; the .icns step needs macOS (iconutil).
"""
import os
import shutil
import subprocess
import sys
import tempfile

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
S = 1024
SS = 2  # supersampling factor for smooth edges


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def draw_master():
    n = S * SS
    k = SS
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))

    # Page geometry (macOS-style margin around the tile).
    x0, y0, x1, y1 = 100 * k, 84 * k, 924 * k, 908 * k
    radius = 170 * k

    # Soft shadow.
    shadow = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        (x0, y0 + 26 * k, x1, y1 + 26 * k), radius, fill=(0, 0, 0, 120))
    shadow = shadow.filter(ImageFilter.GaussianBlur(26 * k))
    img = Image.alpha_composite(img, shadow)

    # Page mask.
    mask = Image.new("L", (n, n), 0)
    ImageDraw.Draw(mask).rounded_rectangle((x0, y0, x1, y1), radius, fill=255)

    page = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    pd = ImageDraw.Draw(page)
    pd.rectangle((x0, y0, x1, y1), fill=(255, 255, 255, 255))

    # Header band with vertical gradient.
    header_h = 235 * k
    top, bot = (0x46, 0x82, 0xb4), (0x2e, 0x6d, 0xa4)
    for y in range(y0, y0 + header_h):
        t = (y - y0) / header_h
        pd.line((x0, y, x1, y), fill=lerp(top, bot, t) + (255,))

    # Grid: 5 columns by 4 rows of faint squares, one highlighted.
    gx0, gx1 = x0 + 62 * k, x1 - 62 * k
    gy0, gy1 = y0 + header_h + 56 * k, y1 - 62 * k
    cols, rows, gap = 5, 4, 24 * k
    cw = (gx1 - gx0 - gap * (cols - 1)) / cols
    ch = (gy1 - gy0 - gap * (rows - 1)) / rows
    for r in range(rows):
        for c in range(cols):
            cx = gx0 + c * (cw + gap)
            cy = gy0 + r * (ch + gap)
            box = (cx, cy, cx + cw, cy + ch)
            if (r, c) == (1, 3):
                pd.rounded_rectangle(box, 22 * k, fill=(0xff, 0xf6, 0xd6, 255),
                                     outline=(0xd9, 0xa4, 0x1c, 255), width=9 * k)
            else:
                pd.rounded_rectangle(box, 22 * k, fill=(0xe9, 0xee, 0xf4, 255))

    # Clip page to rounded square, then add a hairline border.
    clipped = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    clipped.paste(page, (0, 0), mask)
    ImageDraw.Draw(clipped).rounded_rectangle(
        (x0, y0, x1, y1), radius, outline=(0, 0, 0, 40), width=3 * k)
    img = Image.alpha_composite(img, clipped)

    # Binder rings (drawn over the header edge).
    d = ImageDraw.Draw(img)
    for cx in (x0 + (x1 - x0) * 0.30, x0 + (x1 - x0) * 0.70):
        w, h = 46 * k, 120 * k
        top_y = y0 - 34 * k
        d.rounded_rectangle((cx - w / 2 + 3 * k, top_y + 8 * k, cx + w / 2 + 3 * k, top_y + h + 8 * k),
                            w // 2, fill=(0, 0, 0, 70))
        d.rounded_rectangle((cx - w / 2, top_y, cx + w / 2, top_y + h), w // 2,
                            fill=(0xf4, 0xf6, 0xf9, 255), outline=(0x8a, 0x97, 0xa6, 255), width=5 * k)
        # hole shading at the bottom where the ring enters the page
        d.rounded_rectangle((cx - 9 * k, top_y + h - 52 * k, cx + 9 * k, top_y + h - 8 * k), 9 * k,
                            fill=(0x1f, 0x4e, 0x7c, 255))

    return img.resize((S, S), Image.LANCZOS)


def make_icns(master):
    if sys.platform != "darwin" or not shutil.which("iconutil"):
        print("skipping .icns (needs macOS iconutil)")
        return
    out = os.path.join(HERE, "macos", "AppIcon.icns")
    with tempfile.TemporaryDirectory() as tmp:
        iconset = os.path.join(tmp, "AppIcon.iconset")
        os.mkdir(iconset)
        for base in (16, 32, 128, 256, 512):
            for scale in (1, 2):
                px = base * scale
                name = f"icon_{base}x{base}{'@2x' if scale == 2 else ''}.png"
                master.resize((px, px), Image.LANCZOS).save(os.path.join(iconset, name))
        subprocess.run(["iconutil", "-c", "icns", iconset, "-o", out], check=True)
    print("wrote", out)


def make_ico(master):
    out = os.path.join(HERE, "windows", "app.ico")
    sizes = [16, 24, 32, 48, 64, 128, 256]
    master.save(out, format="ICO", sizes=[(s, s) for s in sizes])
    print("wrote", out)


def main():
    os.makedirs(os.path.join(HERE, "macos"), exist_ok=True)
    os.makedirs(os.path.join(HERE, "windows"), exist_ok=True)
    master = draw_master()
    png = os.path.join(HERE, "icon-1024.png")
    master.save(png)
    print("wrote", png)
    make_icns(master)
    make_ico(master)


if __name__ == "__main__":
    main()
