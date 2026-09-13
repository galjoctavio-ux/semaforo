"""Generate the landing's social card from typography and shapes, without user photos."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parents[1]
canvas = Image.new('RGB', (1200, 630), '#102822')
d = ImageDraw.Draw(canvas)
font_dir = Path('C:/Windows/Fonts')
regular = str(font_dir / 'arial.ttf')
bold = str(font_dir / 'arialbd.ttf')
d.rounded_rectangle((70, 65, 120, 162), 17, fill='#294636')
for y, color in [(88, '#e68977'), (114, '#eac358'), (140, '#b9ef69')]:
    d.ellipse((88, y-8, 104, y+8), fill=color)
d.text((143, 78), 'semáforo', font=ImageFont.truetype(bold, 43), fill='#ffffff')
d.text((145, 133), 'POR TESIVIL', font=ImageFont.truetype(regular, 15), fill='#bcd0c2')
d.text((70, 218), 'Unos segundos.', font=ImageFont.truetype(bold, 80), fill='#ffffff')
d.text((70, 311), 'Una decisión más clara.', font=ImageFont.truetype(bold, 80), fill='#b9ef69')
d.text((74, 453), 'Tus costos. Tu tiempo. Tus reglas.', font=ImageFont.truetype(regular, 31), fill='#c6d7ce')
d.line((74, 520, 1126, 520), fill='#3b5647', width=2)
d.text((74, 548), 'BETA PÚBLICA · ANDROID 14+ · DESCARGA GRATUITA', font=ImageFont.truetype(regular, 22), fill='#c6d7ce')
canvas.save(root / 'landing/social.png', optimize=True)
