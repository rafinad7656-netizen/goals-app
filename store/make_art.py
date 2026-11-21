"""Рисует графику для Google Play: значок 512x512 и обложку 1024x500.
Повторяет ic_launcher_foreground.xml (координаты в сетке 108x108)."""
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path(sys.argv[1])
ACCENT, PAPER = (0x2E, 0x9E, 0x6B), (0xFA, 0xFA, 0xF7)
SS = 4  # supersampling for smooth edges


def draw_mark(d: ImageDraw.ImageDraw, ox: float, oy: float, s: float):
    """Mark in 108-unit grid, scaled by s, offset by (ox, oy)."""
    p = lambda x, y: (ox + x * s, oy + y * s)
    w = round(5 * s)
    # 3/4 ring: from top (54,31) clockwise to left (31,54), r=23
    # PIL draws arc width inward from the bbox, so grow it by w/2 to centre the stroke on r
    cx, cy = p(54, 54); R = 23 * s + w / 2
    d.arc([cx - R, cy - R, cx + R, cy + R], start=270, end=180, fill=PAPER, width=w)
    for cx, cy in [(54, 31), (31, 54)]:  # round caps
        x, y = p(cx, cy); r = w / 2
        d.ellipse([x - r, y - r, x + r, y + r], fill=PAPER)
    cw = round(5.5 * s)
    pts = [p(44, 54.5), p(51, 61.5), p(65, 47)]
    d.line(pts, fill=PAPER, width=cw, joint="curve")
    for x, y in (pts[0], pts[-1]):
        r = cw / 2
        d.ellipse([x - r, y - r, x + r, y + r], fill=PAPER)


def icon(size=512):
    img = Image.new("RGB", (size * SS, size * SS), ACCENT)
    draw_mark(ImageDraw.Draw(img), 0, 0, size * SS / 108)
    img.resize((size, size), Image.LANCZOS).save(OUT / "icon-512.png")


def feature(w=1024, h=500):
    img = Image.new("RGB", (w * SS, h * SS), ACCENT)
    d = ImageDraw.Draw(img)
    mark = 300 * SS  # mark area in px; the 108 grid fits into it
    draw_mark(d, 60 * SS, (h * SS - mark) / 2, mark / 108)
    fonts = Path("C:/Windows/Fonts")
    title = ImageFont.truetype(str(fonts / "segoeuib.ttf"), 64 * SS)
    sub = ImageFont.truetype(str(fonts / "segoeui.ttf"), 30 * SS)
    x = 380 * SS
    d.text((x, 175 * SS), "Цели и задачи", font=title, fill=PAPER)
    d.text((x, 265 * SS), "День, цели и деньги —", font=sub, fill=PAPER)
    d.text((x, 305 * SS), "в одном простом приложении", font=sub, fill=PAPER)
    img.resize((w, h), Image.LANCZOS).save(OUT / "feature-graphic.png")


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    feature()
    for f in ("icon-512.png", "feature-graphic.png"):
        im = Image.open(OUT / f)
        assert im.size in [(512, 512), (1024, 500)], im.size
    print("ok")
