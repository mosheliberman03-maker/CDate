"""Makes the Android launcher icons and splash from native/assets (replaces @capacitor/assets, whose image library
failed to download on GitHub's servers). Every icon/splash file the Android template has is redrawn at its own size."""
import os, sys
from PIL import Image
RES = 'android/app/src/main/res'
icon = Image.open('assets/icon.png').convert('RGBA')
splash = Image.open('assets/splash.png').convert('RGBA')
BG = (10, 17, 49, 255)  # navy, like the character icon (v55)
def fit(src, w, h, scale=1.0, bg=BG):
    out = Image.new('RGBA', (w, h), bg)
    s = min(w, h) * scale
    im = src.copy(); im.thumbnail((int(s), int(s)), Image.LANCZOS)
    out.paste(im, ((w - im.width) // 2, (h - im.height) // 2), im)
    return out
def cover(src, w, h):
    r = max(w / src.width, h / src.height)
    im = src.resize((max(1, round(src.width * r)), max(1, round(src.height * r))), Image.LANCZOS)
    l, t = (im.width - w) // 2, (im.height - h) // 2
    return im.crop((l, t, l + w, t + h))
n = 0
for root, _, files in os.walk(RES):
    for f in files:
        p = os.path.join(root, f)
        if not f.endswith('.png'): continue
        if f.startswith('ic_launcher'):
            w, h = Image.open(p).size
            if 'foreground' in f: im = fit(icon, w, h, 0.72)            # adaptive icon: keep it inside the safe zone
            elif 'round' in f:
                im = fit(icon, w, h, 1.0); m = Image.new('L', (w, h), 0)
                from PIL import ImageDraw; ImageDraw.Draw(m).ellipse((0, 0, w - 1, h - 1), fill=255); im.putalpha(m)
            else: im = fit(icon, w, h, 1.0)
            im.save(p); n += 1
        elif f == 'splash.png':
            w, h = Image.open(p).size
            cover(splash, w, h).save(p); n += 1
for root, _, files in os.walk(RES):
    for f in files:
        if f == 'ic_launcher_background.xml':
            p = os.path.join(root, f)
            open(p, 'w').write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">#0A1131</color>\n</resources>\n')
print('make-icons: redrew', n, 'files')
if n == 0: sys.exit('make-icons: no icon files found')
