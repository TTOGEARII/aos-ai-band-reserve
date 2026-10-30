#!/usr/bin/env python3
"""플레이스토어·런처 그래픽 생성 — 사용자 제작 캐릭터 아트(source-icon.png) 기반.

원본: play/source-icon.png (1367x768, 시안 캐릭터 + 투톤 대각 배경).
산출물:
  - icon-512.png            스토어 아이콘 (캐릭터 중심 정방형 크롭)
  - feature-1024x500.png    피처 그래픽 (배경 재구성 + 캐릭터 우측 + 타이틀)
  - ../app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png
                            어댑티브 런처 전경 (캐릭터 추출, 432px, 안전영역 맞춤)
런처 배경(ic_launcher_background.xml)은 원본 배경색 2톤 대각 벡터로 별도 유지.
"""
import os
from PIL import Image, ImageDraw, ImageFont

CYAN = (82, 203, 248)      # 캐릭터 본색
BG1 = '#072B37'            # 배경 좌상(틸)
BG2 = '#03060B'            # 배경 우하(흑청)


def load_source():
    return Image.open('source-icon.png').convert('RGB')


def extract_character(im):
    """시안 캐릭터만 알파로 추출. 내부 디테일(배경색 선)은 뚫려서 배경이 비친다 — 의도된 것."""
    w, h = im.size
    src = im.load()
    out = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    dst = out.load()
    lo, hi = 45, 95
    for y in range(h):
        for x in range(w):
            r, g, b = src[x, y]
            d = ((r - CYAN[0]) ** 2 + (g - CYAN[1]) ** 2 + (b - CYAN[2]) ** 2) ** 0.5
            if d < lo:
                a = 255
            elif d > hi:
                continue
            else:
                a = int(255 * (hi - d) / (hi - lo))
            dst[x, y] = (r, g, b, a)
    bbox = out.getbbox()
    return out.crop(bbox)


def char_center_bbox(im):
    ch = extract_character(im)
    return ch


def make_icon(im):
    """캐릭터 중심 정방형 크롭 → 512. 원본 배경(대각 투톤) 그대로 유지."""
    w, h = im.size
    ch = extract_character(im)
    # 캐릭터 가로 중심 찾기: 추출 전 bbox를 다시 계산하기 위해 알파 투영
    full = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    # extract_character가 crop해버리므로 중심은 원본에서 다시 측정
    # (간단히: 시안 픽셀 x 평균)
    src = im.load()
    sx = n = 0
    for y in range(0, h, 4):
        for x in range(0, w, 4):
            r, g, b = src[x, y]
            if abs(r - CYAN[0]) < 60 and abs(g - CYAN[1]) < 60 and abs(b - CYAN[2]) < 60:
                sx += x; n += 1
    cx = sx // max(n, 1)
    side = h
    x0 = max(0, min(w - side, cx - side // 2))
    im.crop((x0, 0, x0 + side, side)).resize((512, 512), Image.LANCZOS).save('icon-512.png')


def make_launcher_fg(im):
    ch = extract_character(im)
    S = 432; SAFE = 252   # 108dp 캔버스(4x), 안전영역 66dp(264px)보다 살짝 안쪽
    scale = min(SAFE / ch.width, SAFE / ch.height)
    nw, nh = int(ch.width * scale), int(ch.height * scale)
    ch = ch.resize((nw, nh), Image.LANCZOS)
    canvas = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    canvas.paste(ch, ((S - nw) // 2, (S - nh) // 2), ch)
    out = os.path.join('..', 'app', 'src', 'main', 'res', 'drawable-xxxhdpi')
    os.makedirs(out, exist_ok=True)
    canvas.save(os.path.join(out, 'ic_launcher_foreground.png'))


def korean_font(size):
    for path, idx in [('/System/Library/Fonts/AppleSDGothicNeo.ttc', 1),
                      ('/System/Library/Fonts/AppleSDGothicNeo.ttc', 0)]:
        try:
            return ImageFont.truetype(path, size, index=idx)
        except Exception:
            continue
    return ImageFont.load_default()


def make_feature(im):
    W, H, SS = 1024, 500, 2
    img = Image.new('RGB', (W * SS, H * SS), BG1)
    dr = ImageDraw.Draw(img)
    c1 = tuple(int(BG1[i:i + 2], 16) for i in (1, 3, 5))
    c2 = tuple(int(BG2[i:i + 2], 16) for i in (1, 3, 5))
    for x in range(W * SS):
        t = x / (W * SS)
        col = tuple(int(c1[k] + (c2[k] - c1[k]) * t) for k in range(3))
        dr.line([(x, 0), (x, H * SS)], fill=col)
    ch = extract_character(im)
    target_h = int(H * SS * 0.88)
    scale = target_h / ch.height
    ch = ch.resize((int(ch.width * scale), target_h), Image.LANCZOS)
    img.paste(ch, (W * SS - ch.width - 50 * SS, (H * SS - target_h) // 2), ch)
    title = korean_font(86 * SS)
    sub = korean_font(34 * SS)
    dr.text((64 * SS, 160 * SS), '에네쨩해줘!', font=title, fill='#FFFFFF')
    dr.text((66 * SS, 282 * SS), '말로 찾는 밴드 합주실 빈 시간', font=sub, fill='#52CBF8')
    img.resize((W, H), Image.LANCZOS).save('feature-1024x500.png')


if __name__ == '__main__':
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    im = load_source()
    make_icon(im)
    make_launcher_fg(im)
    make_feature(im)
    print('icon-512 / feature / launcher-fg 생성 완료')
