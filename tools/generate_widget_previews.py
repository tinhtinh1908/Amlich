#!/usr/bin/env python3
"""Draw launcher-picker previews matching the native widget layouts.
Illustrative content: 14 October 2026 (5/9 lunar); no runtime dependency.
"""
from pathlib import Path
import calendar
from datetime import date as Date
from PIL import Image, ImageDraw, ImageFont
OUT = Path(__file__).resolve().parents[1] / 'res/drawable-nodpi'
S = 3
BG, PRIMARY, SECONDARY, MUTED, ACCENT, HOLIDAY = '#242424','#F2F2F2','#A0A0A0','#737373','#277AF7','#F12522'
REGULAR='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
BOLD='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
def font(size,bold=False):return ImageFont.truetype(BOLD if bold else REGULAR,round(size*S))
def label(draw,box,value,size,color=PRIMARY,bold=False):
 x,y,w,h=[v*S for v in box];f=font(size,bold);b=draw.textbbox((0,0),value,font=f)
 draw.text((x+(w-b[2]+b[0])/2-b[0],y+(h-b[3]+b[1])/2-b[1]),value,font=f,fill=color)
def base(w,h):
 im=Image.new('RGBA',(w*S,h*S));d=ImageDraw.Draw(im)
 d.rounded_rectangle((0,0,w*S-1,h*S-1),radius=24*S,fill=BG)
 return im,d
def save(im,name):im.save(OUT/name,'WEBP',lossless=True,method=6,exact=True)
def dates():
 im,d=base(180,80)
 label(d,(10,12,56,13),'T4',11,SECONDARY,True)
 label(d,(10,26,56,44),'14',40,ACCENT)
 # Native horizontal column is left aligned.
 for y,value,size,color,bold in [(16,'Tháng 10 · 2026',10,SECONDARY,True),(34,'Âm 5/9',13,PRIMARY,True),(55,'Bính Ngọ',10,SECONDARY,False)]:d.text((76*S,y*S),value,font=font(size,bold),fill=color)
 save(im,'widget_preview_2x1.webp')
 im,d=base(80,160)
 label(d,(10,12,60,14),'T4',11,SECONDARY,True)
 label(d,(10,27,60,75),'14',42,ACCENT)
 label(d,(10,108,60,12),'Tháng 10',10,SECONDARY,True)
 label(d,(10,125,60,16),'Âm 5/9',13,PRIMARY,True)
 label(d,(10,139,60,12),'Bính Ngọ',10,SECONDARY)
 save(im,'widget_preview_1x2.webp')
def month():
 im,d=base(220,220)
 cw=200/7
 for col,day in enumerate(['T2','T3','T4','T5','T6','T7','CN']):label(d,(10+col*cw,10,cw,28),day,11,HOLIDAY if col==6 else SECONDARY)
 cells=calendar.Calendar(0).monthdatescalendar(2026,10)
 rowh=(220-20-28)/len(cells)
 events={10:'Giải phóng',13:'Doanh nhân',20:'Phụ nữ VN'}
 for row,days in enumerate(cells):
  for col,date in enumerate(days):
   x,y=10+col*cw,38+row*rowh
   selected=date.month==10 and date.day==14
   color=PRIMARY if date.month==10 else MUTED
   if date.month==10 and col==6:color=HOLIDAY
   if selected:
    cy=y+(rowh-12)/2
    d.ellipse(((x+cw/2-13)*S,(cy-13)*S,(x+cw/2+13)*S,(cy+13)*S),fill=ACCENT);color='#FFFFFF'
   label(d,(x,y,cw,rowh-12),str(date.day),14,color,True)
   lunar = 5 + (date - Date(2026, 10, 14)).days
   if lunar <= 0: lunar += 29
   detail=events.get(date.day,str(lunar)) if date.month==10 else str(lunar)
   # Preview abbreviates event labels to the same single-line cell width.
   if len(detail)>6:detail=detail[:4]+'…'
   label(d,(x,y+rowh-11,cw,11),detail,8,HOLIDAY if date.month==10 and date.day in events else (SECONDARY if date.month==10 else MUTED))
 save(im,'widget_preview_3x3.webp')
def tear():
 im,d=base(160,160)
 binding=Image.new('RGBA',im.size,ACCENT)
 mask=Image.new('L',im.size);md=ImageDraw.Draw(mask)
 md.rounded_rectangle((0,0,160*S-1,160*S-1),radius=12*S,fill=255)
 md.rectangle((0,28*S,160*S,160*S),fill=0)
 im.paste(binding,(0,0),mask);d=ImageDraw.Draw(im)
 for x in [160*.23,160*.77]:d.ellipse(((x-2)*S,3*S,(x+2)*S,7*S),fill='#FFFFFF')
 label(d,(8,8,144,18),'THÁNG 10 · 2026',9,'#FFFFFF',True)
 label(d,(8,26,86,65),'14',54,ACCENT,True)
 label(d,(8,91,86,12),'Thứ Tư',9,SECONDARY,True)
 label(d,(94,46,58,11),'ÂM LỊCH',7,SECONDARY)
 label(d,(94,57,58,22),'5/9',18,PRIMARY,True)
 label(d,(8,103,144,13),'Tân Dậu · Bính Ngọ',8,SECONDARY)
 label(d,(8,116,72,11),'Hoàng đạo',8,ACCENT)
 label(d,(8,127,72,13),'Kim Đường',8,ACCENT,True)
 label(d,(80,116,72,11),'Tiết khí',8,SECONDARY)
 label(d,(80,127,72,13),'Hàn lộ',8,PRIMARY,True)
 label(d,(8,140,144,12),'Tháng Mậu Tuất',8,SECONDARY)
 d.polygon([(146*S,160*S),(146*S,146*S),(160*S,146*S)],fill='#434343')
 save(im,'widget_preview_2x2.webp')
if __name__=='__main__':
 OUT.mkdir(parents=True,exist_ok=True);dates();month();tear()
