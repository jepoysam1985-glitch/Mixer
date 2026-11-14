import os
import re
import shutil
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
    logo.resize((s, s), Image.LANCZOS).save(p + '/ic_launcher.png')
    n = int(s * 0.80)
    rd = Image.new('RGBA', (s, s), (0, 0, 0, 255))
    rd.paste(logo.resize((n, n), Image.LANCZOS), ((s - n) // 2, (s - n) // 2))
    circle(rd).save(p + '/ic_launcher_round.png')
    fg = Image.new('RGBA', (f, f), (0, 0, 0, 0))
    m = int(f * 0.56)
    fg.paste(logo.resize((m, m), Image.LANCZOS), ((f - m) // 2, (f - m) // 2))
    fg.save(p + '/ic_launcher_foreground.png')

os.makedirs(res + '/values', exist_ok=True)
open(res + '/values/ic_launcher_background.xml', 'w').write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">#000000</color>\n</resources>\n')
print('Icons done')

pub = 'build-app/android/app/src/main/assets/public'
idx = pub + '/index.html'
if os.path.exists(idx):
    html = open(idx, encoding='utf-8').read()
    img = '<img src="logo.jpg" alt="" style="width:34px;height:34px;border-radius:8px;vertical-align:middle;margin-right:8px"> '
    new, n = re.subn(r'<h1>[^<]*JEIANNE FM MIXER</h1>', '<h1>' + img + 'JEIANNE FM MIXER</h1>', html, count=1)
    if n:
        open(idx, 'w', encoding='utf-8').write(new)
        shutil.copy('logo.jpg', pub + '/logo.jpg')
        print('Logo added to header')
    else:
        print('Header not found, logo not added')
