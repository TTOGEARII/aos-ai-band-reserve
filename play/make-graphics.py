#!/usr/bin/env python3
"""플레이스토어 그래픽 생성 스크립트.

생성물:
  icon-512.png        512x512 32-bit PNG (앱 아이콘, 어댑티브 아이콘과 같은 디자인)
  feature-1024x500.png 1024x500 알파 없는 PNG (피처 그래픽)

재생성: python3 make-graphics.py
"""
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))

BG = (0x6D, 0x28, 0xD9)        # #6D28D9
BG_DARK = (0x5B, 0x21, 0xB6)   # #5B21B6
GRAD_END = (0x4C, 0x1D, 0x95)  # #4C1D95
WHITE = (255, 255, 255)

SS = 4  # 안티앨리어싱용 슈퍼샘플 배율


def draw_notes(draw, scale, ox=0.0, oy=0.0, fill=WHITE):
    """ic_launcher_foreground.xml(108 뷰포트)의 8분음표 2개+빔을 그린다.

    scale: 108 뷰포트 단위 1 → 픽셀 수. ox/oy: 픽셀 오프셋.
    """
    def pt(x, y):
        return (x * scale + ox, y * scale + oy)

    # 왼쪽 음표 머리: 중심 (44,70) 반지름 6.5
    draw.ellipse([pt(44 - 6.5, 70 - 6.5), pt(44 + 6.5, 70 + 6.5)], fill=fill)
    # 왼쪽 기둥
    draw.polygon([pt(48.6, 42), pt(51.2, 42), pt(51.2, 70), pt(48.6, 70)], fill=fill)
    # 오른쪽 음표 머리: 중심 (64,66) 반지름 6.5
    draw.ellipse([pt(64 - 6.5, 66 - 6.5), pt(64 + 6.5, 66 + 6.5)], fill=fill)
    # 오른쪽 기둥
    draw.polygon([pt(68.6, 38), pt(71.2, 38), pt(71.2, 66), pt(68.6, 66)], fill=fill)
    # 빔
    draw.polygon([pt(48.6, 42), pt(71.2, 38), pt(71.2, 45.5), pt(48.6, 49.5)], fill=fill)


def make_icon(path):
    size = 512
    s = size * SS
    img = Image.new("RGBA", (s, s), BG + (255,))
    draw = ImageDraw.Draw(img)
    # 우하단 대각선 절반 (좌상→우하 대각선 아래쪽)
    draw.polygon([(s, 0), (s, s), (0, s)], fill=BG_DARK + (255,))
    # 전경: 108 캔버스 → 512 스케일
    draw_notes(draw, scale=s / 108.0)
    img = img.resize((size, size), Image.LANCZOS)
    img.save(path, "PNG")


def load_korean_font(size):
    candidates = [
        "/System/Library/Fonts/AppleSDGothicNeo.ttc",
        "/System/Library/Fonts/Supplemental/AppleGothic.ttf",
        "/Library/Fonts/AppleSDGothicNeo.ttc",
        "/System/Library/Fonts/Supplemental/NanumGothic.ttc",
    ]
    for p in candidates:
        if not os.path.exists(p):
            continue
        for idx in range(0, 12):
            try:
                return ImageFont.truetype(p, size, index=idx)
            except OSError:
                break
            except Exception:
                continue
    raise RuntimeError("한글 폰트를 찾지 못했습니다")


def make_feature(path):
    w, h = 1024, 500
    img = Image.new("RGB", (w, h))
    # 가로 그라데이션 #6D28D9 → #4C1D95
    for x in range(w):
        t = x / (w - 1)
        c = tuple(round(BG[i] + (GRAD_END[i] - BG[i]) * t) for i in range(3))
        ImageDraw.Draw(img).line([(x, 0), (x, h)], fill=c)

    # 우측 음표 모티프 (슈퍼샘플 레이어에 반투명 흰색으로)
    ov = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
    od = ImageDraw.Draw(ov)
    note_h = 420 * SS           # 음표 영역 높이(px)
    scale = note_h / 108.0
    ox = w * SS - 108 * scale * 0.92   # 오른쪽에 붙이되 살짝 여유
    oy = (h * SS - note_h) / 2
    draw_notes(od, scale=scale, ox=ox, oy=oy, fill=(255, 255, 255, 56))
    # 작은 음표 하나 더
    s2 = note_h * 0.45 / 108.0
    draw_notes(od, scale=s2, ox=ox - 150 * SS, oy=oy + 40 * SS, fill=(255, 255, 255, 36))
    ov = ov.resize((w, h), Image.LANCZOS)
    img = Image.alpha_composite(img.convert("RGBA"), ov).convert("RGB")

    # 텍스트: 좌측 여백 64px, 수직 중앙
    draw = ImageDraw.Draw(img)
    title_font = load_korean_font(84)
    sub_font = load_korean_font(36)
    margin = 64
    title = "합주실 예약 AI"
    sub = "말로 찾는 밴드 합주실 빈 시간"

    tb = draw.textbbox((0, 0), title, font=title_font)
    sb = draw.textbbox((0, 0), sub, font=sub_font)
    gap = 28
    total = (tb[3] - tb[1]) + gap + (sb[3] - sb[1])
    top = (h - total) / 2
    draw.text((margin, top - tb[1]), title, font=title_font, fill=WHITE)
    draw.text((margin, top + (tb[3] - tb[1]) + gap - sb[1]), sub, font=sub_font,
              fill=(237, 233, 254))  # 살짝 연한 흰색(#EDE9FE)

    img.save(path, "PNG")


if __name__ == "__main__":
    icon_path = os.path.join(HERE, "icon-512.png")
    feature_path = os.path.join(HERE, "feature-1024x500.png")
    make_icon(icon_path)
    make_feature(feature_path)
    print(icon_path)
    print(feature_path)
