#!/usr/bin/env python3
"""플레이스토어 그래픽 재생성 — 에네 콘셉트 단색 트윈테일 실루엣.

런처 벡터(app/src/main/res/drawable/ic_launcher_foreground.xml)와 같은 지오메트리를
여기서도 그린다(108 뷰포트 좌표 공유). 모티프를 바꾸면 두 곳을 같이 바꿀 것.
산출물: icon-512.png (512×512), feature-1024x500.png (1024×500, 알파 없음)
"""
import math
from PIL import Image, ImageDraw, ImageFont

NAVY = '#0A141F'
NAVY2 = '#071019'
CYAN = '#53D1FF'

# ── 런처 벡터와 공유하는 지오메트리 (108 뷰포트) ──
HEAD = (54, 48, 15.5)
HALO_R = 18.5
AHOGE = [('M', (50.5, 34)), ('Q', (45, 23), (57, 20)), ('Q', (52, 27), (55.5, 33.5)), ('Q', (52.5, 34.5), (50.5, 34))]
LTAIL = [('M', (43, 34)), ('Q', (28, 21), (22.5, 35)), ('Q', (20.5, 54), (31, 76)), ('Q', (32.5, 60), (38, 50)), ('Q', (42.5, 44), (43, 38))]
RTAIL = [('M', (65, 34)), ('Q', (80, 21), (85.5, 35)), ('Q', (87.5, 54), (77, 77)), ('Q', (75.5, 60), (70, 50)), ('Q', (65.5, 44), (65, 38))]
EYE_R = 1.15
EYE_SEGS = [((46.8, 44.0), (50.2, 46.4)), ((46.8, 48.8), (50.2, 46.4)),
            ((61.2, 44.0), (57.8, 46.4)), ((61.2, 48.8), (57.8, 46.4))]
DEV_R = 1.15
_th = math.radians(-55)
_u = (math.cos(_th), -math.sin(_th)); _nv = (-_u[1], _u[0])
def _bars(mcx, mcy):
    out = []
    for off, L in ((-2.7, 2.9), (0, 4.3), (2.7, 2.9)):
        c = (mcx + off * _nv[0], mcy + off * _nv[1])
        out.append(((c[0] - L * _u[0], c[1] - L * _u[1]), (c[0] + L * _u[0], c[1] + L * _u[1])))
    return out
DEV_SEGS = _bars(60.6, 56.0) + _bars(47.4, 56.0)


def qbez(p0, p1, p2, n=28):
    return [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
             (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1])
            for t in [i / n for i in range(n + 1)]]


def poly(cmds):
    pts, cur = [], None
    for c in cmds:
        if c[0] == 'M':
            cur = c[1]; pts.append(cur)
        elif c[0] == 'Q':
            pts += qbez(cur, c[1], c[2])[1:]; cur = c[2]
    return pts


def draw_motif(dr, ox, oy, scale):
    """(ox,oy) 기준 108 뷰포트 지오메트리를 scale 배로 그린다."""
    T = lambda p: (ox + p[0] * scale, oy + p[1] * scale)
    for sh in (LTAIL, RTAIL):
        dr.polygon([T(p) for p in poly(sh)], fill=CYAN)
    cx, cy, r = HEAD
    hr = HALO_R * scale
    hc = T((cx, cy))
    dr.ellipse([hc[0] - hr, hc[1] - hr, hc[0] + hr, hc[1] + hr], fill=NAVY)
    dr.polygon([T(p) for p in poly(AHOGE)], fill=CYAN)
    rr = r * scale
    dr.ellipse([hc[0] - rr, hc[1] - rr, hc[0] + rr, hc[1] + rr], fill=CYAN)
    def seg(a, b, r2):
        A, B = T(a), T(b); cr = r2 * scale
        dr.line([A, B], fill=NAVY, width=int(2 * cr))
        for p in (A, B):
            dr.ellipse([p[0] - cr, p[1] - cr, p[0] + cr, p[1] + cr], fill=NAVY)
    for a, b in EYE_SEGS:
        seg(a, b, EYE_R)
    for a, b in DEV_SEGS:
        seg(a, b, DEV_R)


def make_icon():
    S, SS = 512, 4
    img = Image.new('RGBA', (S * SS, S * SS), NAVY)
    dr = ImageDraw.Draw(img)
    dr.polygon([(S * SS, 0), (S * SS, S * SS), (0, S * SS)], fill=NAVY2)
    draw_motif(dr, 0, 0, S * SS / 108)
    img.resize((S, S), Image.LANCZOS).save('icon-512.png')


def korean_font(size):
    for path, idx in [('/System/Library/Fonts/AppleSDGothicNeo.ttc', 1),
                      ('/System/Library/Fonts/AppleSDGothicNeo.ttc', 0)]:
        try:
            return ImageFont.truetype(path, size, index=idx)
        except Exception:
            continue
    return ImageFont.load_default()


def make_feature():
    W, H, SS = 1024, 500, 2
    img = Image.new('RGB', (W * SS, H * SS), NAVY)
    dr = ImageDraw.Draw(img)
    # 좌→우 미세 그라데이션
    c1 = tuple(int(NAVY[i:i + 2], 16) for i in (1, 3, 5))
    c2 = tuple(int(NAVY2[i:i + 2], 16) for i in (1, 3, 5))
    for x in range(W * SS):
        t = x / (W * SS)
        col = tuple(int(c1[k] + (c2[k] - c1[k]) * t) for k in range(3))
        dr.line([(x, 0), (x, H * SS)], fill=col)
    # 우측 대형 모티프 (세로 높이의 ~86%)
    scale = H * SS * 0.86 / 108
    draw_motif(dr, W * SS - 108 * scale - 60 * SS, (H * SS - 108 * scale) / 2, scale)
    # 좌측 텍스트
    title = korean_font(86 * SS)
    sub = korean_font(34 * SS)
    dr.text((64 * SS, 160 * SS), '합주실 예약 AI', font=title, fill='#FFFFFF')
    dr.text((66 * SS, 282 * SS), '말로 찾는 밴드 합주실 빈 시간', font=sub, fill=CYAN)
    img.resize((W, H), Image.LANCZOS).save('feature-1024x500.png')


if __name__ == '__main__':
    import os
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    make_icon()
    make_feature()
    print('icon-512.png / feature-1024x500.png 재생성 완료')
