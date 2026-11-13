import os
from PIL import Image, ImageDraw

res = 'build-app/android/app/src/main/res'
logo = Image.open('logo.jpg').convert('RGBA')
dens = {'mdpi': (48, 108), 'hdpi': (72, 162), 'xhdpi': (96, 216), 'xxhdpi': (144, 324), 'xxxhdpi': (192, 432)}

def circle(img):
    big = img.size[0] * 4
    mask = Image.new('L', (big, big), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, big - 1, big - 1), fill=255)
    mask = mask.resize(img.size, Image.LANCZOS)
    out = Image.new('RGBA', img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out

for d, (s, f) in dens.items():
    p = res + '/mipmap-' + d
    os.makedirs(p, exist_ok=True)
    for w in ('ic_launcher.webp', 'ic_launcher_round.webp', 'ic_launcher_foreground.webp'):
        if os.path.exists(p + '/' + w):
            os.remove(p + '/' + w)
    sq = logo.resize((s, s), Image.LANCZOS)
    sq.save(p + '/ic_launcher.png')
    circle(sq).save(p + '/ic_launcher_round.png')
    fg = Image.new('RGBA', (f, f), (0, 0, 0, 0))
    n = int(f * 0.62)
    inner = circle(logo.resize((n, n), Image.LANCZOS))
    fg.paste(inner, ((f - n) // 2, (f - n) // 2), inner)
    fg.save(p + '/ic_launcher_foreground.png')

os.makedirs(res + '/values', exist_ok=True)
open(res + '/values/ic_launcher_background.xml', 'w').write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">#000000</color>\n</resources>\n')
print('Icons done')
