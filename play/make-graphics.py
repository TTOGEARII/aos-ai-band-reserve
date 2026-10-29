#!/usr/bin/env python3
"""플레이스토어 그래픽 생성 스크립트.

생성물:
  icon-512.png        512x512 32-bit PNG (앱 아이콘, 어댑티브 아이콘과 같은 디자인)
  feature-1024x500.png 1024x500 알파 없는 PNG (피처 그래픽)

디자인: 에네 볼 단자 모티프 — 시안 오디오 잭 탭 + 네이비 그릴 슬릿
(ic_launcher_foreground.xml과 같은 좌표를 그대로 그린다)

재생성: python3 make-graphics.py
"""
import math
import os
import re
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))

BG = (0x0A, 0x14, 0x1F)        # #0A141F 네이비
BG_DARK = (0x07, 0x10, 0x19)   # #071019 더 어두운 네이비(대각 절반 / 그라데이션 끝)
CYAN = (0x53, 0xD1, 0xFF)      # #53D1FF 에네 시안
WHITE = (255, 255, 255)

SS = 4  # 안티앨리어싱용 슈퍼샘플 배율

# ic_launcher_foreground.xml(108 뷰포트)과 동일한 패스. 색, pathData 순.
JACK_PATHS = [
    # 탭 몸체 (오디오 잭 단면), 시안
    ("#53D1FF", "M40,30 L68,30 Q74,30 72.62,35.84 L65.92,64.11 Q65,68 61,68 "
                "L47,68 Q43,68 42.08,64.11 L35.38,35.84 Q34,30 40,30 Z"),
    # 그릴 슬릿 1 (네이비로 파냄)
    ("#0A141F", "M44,39.5 L64,39.5 A2.5,2.5 0 0 1 64,44.5 L44,44.5 A2.5,2.5 0 0 1 44,39.5 Z"),
    # 그릴 슬릿 2 (몸체 테이퍼를 따라 더 좁게)
    ("#0A141F", "M46,49.5 L62,49.5 A2.5,2.5 0 0 1 62,54.5 L46,54.5 A2.5,2.5 0 0 1 46,49.5 Z"),
    # 커넥터 핀, 짙은 파랑
    ("#29B6F6", "M49,68 L59,68 L59,74 Q59,77 56,77 L52,77 Q49,77 49,74 Z"),
    # 모서리 하이라이트
    ("#E1F5FE", "M41.5,33.5 L45.5,33.5 A1.5,1.5 0 0 1 45.5,36.5 L41.5,36.5 A1.5,1.5 0 0 1 41.5,33.5 Z"),
]


def _arc_points(p0, p1, rx, ry, fa, fs, n=32):
    """W3C 끝점→중심 타원 호 변환(x-rotation 0), n등분 샘플."""
    x1, y1 = p0
    x2, y2 = p1
    dx = (x1 - x2) / 2.0
    dy = (y1 - y2) / 2.0
    rx, ry = abs(rx), abs(ry)
    lam = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry)
    if lam > 1:
        s = math.sqrt(lam)
        rx *= s
        ry *= s
    num = rx * rx * ry * ry - rx * rx * dy * dy - ry * ry * dx * dx
    den = rx * rx * dy * dy + ry * ry * dx * dx
    co = math.sqrt(max(0.0, num / den))
    if fa == fs:
        co = -co
    cxp = co * rx * dy / ry
    cyp = -co * ry * dx / rx
    cx = cxp + (x1 + x2) / 2.0
    cy = cyp + (y1 + y2) / 2.0

    def ang(ux, uy, vx, vy):
        d = math.hypot(ux, uy) * math.hypot(vx, vy)
        c = max(-1.0, min(1.0, (ux * vx + uy * vy) / d))
        a = math.acos(c)
        if ux * vy - uy * vx < 0:
            a = -a
        return a

    ux, uy = (dx - cxp) / rx, (dy - cyp) / ry
    vx, vy = (-dx - cxp) / rx, (-dy - cyp) / ry
    th1 = ang(1.0, 0.0, ux, uy)
    dth = ang(ux, uy, vx, vy)
    if fs == 0 and dth > 0:
        dth -= 2 * math.pi
    if fs == 1 and dth < 0:
        dth += 2 * math.pi
    return [
        (cx + rx * math.cos(th1 + dth * k / n), cy + ry * math.sin(th1 + dth * k / n))
        for k in range(1, n + 1)
    ]


def _parse_path(d):
    """M/L/Q/A/Z pathData → 폴리곤 점 목록 (Q는 24분할, A는 호 샘플)."""
    tokens = re.findall(r"[MLQAZ]|-?\d*\.?\d+", d)
    i = 0
    pts = []
    cur = None

    def num():
        nonlocal i
        v = float(tokens[i])
        i += 1
        return v

    while i < len(tokens):
        t = tokens[i]
        i += 1
        if t in ("M", "L"):
            cur = (num(), num())
            pts.append(cur)
        elif t == "Q":
            cx, cy = num(), num()
            x, y = num(), num()
            x0, y0 = cur
            for k in range(1, 25):
                u = k / 24.0
                pts.append((
                    (1 - u) ** 2 * x0 + 2 * (1 - u) * u * cx + u ** 2 * x,
                    (1 - u) ** 2 * y0 + 2 * (1 - u) * u * cy + u ** 2 * y,
                ))
            cur = (x, y)
        elif t == "A":
            rx, ry = num(), num()
            _rot = num()
            fa, fs = int(num()), int(num())
            x, y = num(), num()
            pts.extend(_arc_points(cur, (x, y), rx, ry, fa, fs))
            cur = (x, y)
        elif t == "Z":
            pass
    return pts


def draw_jack(draw, scale, ox=0.0, oy=0.0):
    """잭 탭 모티프를 그린다. scale: 108 뷰포트 단위 1 → 픽셀 수. ox/oy: 픽셀 오프셋."""
    for color, d in JACK_PATHS:
        poly = [(x * scale + ox, y * scale + oy) for x, y in _parse_path(d)]
        draw.polygon(poly, fill=color)


def make_icon(path):
    size = 512
    s = size * SS
    img = Image.new("RGBA", (s, s), BG + (255,))
    draw = ImageDraw.Draw(img)
    # 우하단 대각선 절반 (좌상→우하 대각선 아래쪽)
    draw.polygon([(s, 0), (s, s), (0, s)], fill=BG_DARK + (255,))
    # 전경: 108 캔버스 → 512 스케일
    draw_jack(draw, scale=s / 108.0)
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
    # 가로 그라데이션 #0A141F → #071019
    for x in range(w):
        t = x / (w - 1)
        c = tuple(round(BG[i] + (BG_DARK[i] - BG[i]) * t) for i in range(3))
        ImageDraw.Draw(img).line([(x, 0), (x, h)], fill=c)

    # 우측 잭 모티프 (슈퍼샘플 레이어에 크게)
    ov = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
    od = ImageDraw.Draw(ov)
    mark_h = 440 * SS           # 모티프 영역 높이(px)
    scale = mark_h / 108.0
    ox = w * SS - 108 * scale * 0.94   # 오른쪽에 붙이되 살짝 여유
    oy = (h * SS - mark_h) / 2
    draw_jack(od, scale=scale, ox=ox, oy=oy)
    ov = ov.resize((w, h), Image.LANCZOS)
    img = Image.alpha_composite(img.convert("RGBA"), ov).convert("RGB")

    # 텍스트: 좌측 여백 64px, 수직 중앙 — 흰색 제목 + 시안 부제
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
              fill=CYAN)  # 에네 시안 #53D1FF

    img.save(path, "PNG")


if __name__ == "__main__":
    icon_path = os.path.join(HERE, "icon-512.png")
    feature_path = os.path.join(HERE, "feature-1024x500.png")
    make_icon(icon_path)
    make_feature(feature_path)
    print(icon_path)
    print(feature_path)
