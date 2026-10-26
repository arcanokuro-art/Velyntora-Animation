from pathlib import Path
import zipfile,io,json,re,struct,hashlib,math,base64
import xml.etree.ElementTree as E
from PIL import Image,ImageOps
import numpy as np
import argparse
parser=argparse.ArgumentParser();parser.add_argument('bundles',type=Path);args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
provenance=json.loads((root/'tools/brush/velyntora_brush/provenance.json').read_text())
for name,meta in provenance.items():
 assert hashlib.sha256((args.bundles/(name+'.bundle')).read_bytes()).hexdigest()==meta['sha256'], 'Unexpected bundle SHA-256'
out=root/'tools/brush/velyntora_brush'
groups=[('aerografos','Aerógrafos', [('collection4','b)_Airbrush_Soft'),('collection3','Airbrush_linear'),('collection3','Airbrush_pressure')]),('tinta','Tinta de escritura',[('collection4',n) for n in ['d)_Ink-1_Precision','d)_Ink-2_Fineliner','d)_Ink-3_Gpen','d)_Ink-4_Pen_Rough','d)_Ink-7_Brush_Rough']]),('marcadores','Marcadores',[('collection4',n) for n in ['e)_Marker_Chisel_Smooth','e)_Marker_Details','e)_Marker_Dry']]),('crayones_pastel','Crayones y pastel',[('collection3','Pastel_texture_large'),('collection3','Pastel_texture_thin')]),('carboncillo','Carboncillo',[('collection4',n) for n in ['f)_Charcoal_Rock_Soft','h)_Charcoal_pencil_large','h)_Charcoal_Pencil_Medium','h)_Charcoal_Pencil_Thin']]),('oleo','Óleo',[('collection3','Oils_bristle'),('collection3','Oils_knife')]+[('collection4',n) for n in ['f)_Bristles-2_Flat_Rough','f)_Bristles-5_Flat','i)_Wet_Paint']]),('acuarela','Acuarela',[('collection4',n) for n in ['j)_Watercolor_Fringe','j)_Watercolor_Texture','j)_Waterpaint_Hard_Edges','j)_Waterpaint_Soft_Edges']]),('puntas_especiales','Puntas especiales',[('collection4',n) for n in ['z)_Stamp_Grass','z)_Stamp_Sparkles','y)_Texture_Spray']]),('mezcla','Mezcla',[('collection4',n) for n in ['k)_Blender_Basic','k)_Blender_Knife_Edge','k)_Blender_Rake','k)_Blender_Smear','k)_Blender_Textured_Soft']])]
zs={n:zipfile.ZipFile(args.bundles/(n+'.bundle')) for n in ['collection3','collection4']}
def gbr(d,pos):
 hs,ver,w,h,bpp=struct.unpack_from('>5I',d,pos)
 assert 0<w<=4096 and 0<h<=4096 and bpp in [1,4],(hs,ver,w,h,bpp)
 a=d[pos+hs:pos+hs+w*h*bpp];assert len(a)==w*h*bpp
 im=Image.frombytes('L' if bpp==1 else 'RGBA',(w,h),a)
 return im,pos+hs+w*h*bpp

def mask(im,mono=False):
 if mono:return im.convert('L')
 a=np.array(im.convert('RGBA')).astype(float);l=a[:,:,:3].mean(2)
 return Image.fromarray(np.uint8(np.clip((255-l)*a[:,:,3]/255,0,255)))

def pattern(p):
 if p.get('Texture/Pattern/Enabled')!='true':return None
 try:
  b=base64.b64decode(p['Texture/Pattern/Pattern'])
  if not b.startswith(b'\x89PNG'):b=base64.b64decode(b)
  return Image.open(io.BytesIO(b)).convert('L').resize((128,128))
 except Exception:return None

catalog=[];nextid=100
for cat,label,entries in groups:
 for bun,name in entries:
  z=zs[bun];data=z.read('paintoppresets/'+name+'.kpp');im=Image.open(io.BytesIO(data));xml=im.info['preset'];r=E.fromstring(xml);p={n.get('name'):n.text or '' for n in r};b=E.fromstring(p['brush_definition']);m=b.find('MaskGenerator');typ=b.get('type');frames=[]
  if typ=='auto_brush':
   N=128;yy,xx=np.mgrid[0:N,0:N];xx=(xx+.5-N/2)/(N/2);yy=(yy+.5-N/2)/(N/2);ratio=float(m.get('ratio','1'));yy/=max(.02,ratio);dist=np.maximum(abs(xx),abs(yy)) if m.get('type')=='rect' else np.sqrt(xx*xx+yy*yy)
   curve=m.get('softness_curve')
   if curve:
    pts=[tuple(map(float,s.split(','))) for s in curve.strip(';').split(';')];a=np.interp(dist,[s[0] for s in pts],[s[1] for s in pts]);a[dist>=1]=0
   else:
    hard=max(.01,min(1,float(m.get('hfade','.5'))));a=np.clip((1-dist)/max(.01,1-hard),0,1)
    if m.get('id')=='gauss':a=np.where(dist<1,np.exp(-4*dist*dist)*a,0)
   frames=[Image.fromarray(np.uint8(a*255))]
  else:
   filename=b.get('filename');paths=[n for n in z.namelist() if n.startswith('brushes/') and n.split('/')[-1]==filename];assert paths,(name,filename)
   d=z.read(paths[0]);pos=0
   if filename.endswith('.gih'):
    pos=d.index(b'\n')+1;end=d.index(b'\n',pos);count=int(d[pos:end].split()[0]);pos=end+1
    for _ in range(count):
     if pos>=len(d):break
     f,pos=gbr(d,pos);frames.append(mask(f,f.mode=='L'))
   elif filename.endswith('.gbr'):
    f,pos=gbr(d,0);frames=[mask(f,f.mode=='L')]
   else:frames=[mask(Image.open(io.BytesIO(d)))]
  frames=frames[:32]
  tip=[]
  for f in frames:
   f.thumbnail((128,128),Image.Resampling.LANCZOS);square=Image.new('L',(128,128));square.paste(f,((128-f.width)//2,(128-f.height)//2));tip.append(square.tobytes())
  slug=re.sub('[^a-z0-9]+','_',re.sub(r'^[a-z]\)_','',name).lower()).strip('_');folder=out/cat/slug;folder.mkdir(parents=True,exist_ok=True)
  (folder/'tip.bin').write_bytes(b''.join(tip));(folder/'source.kpp').write_bytes(data)
  thumb=im.convert('RGBA');thumb.thumbnail((96,96));thumb.save(folder/'preview.png')
  tex=pattern(p);texdata=tex.tobytes() if tex else bytes([255])*16384;(folder/'texture.bin').write_bytes(texdata)
  params=[float(b.get('spacing','.1')),float(b.get('angle','0')),float(p.get('OpacityValue','1')),float(p.get('FlowValue','1')),1. if p.get('PressureSize')=='true' else 0.,1. if p.get('PressureOpacity')=='true' else 0.,1. if p.get('PressureFlow')=='true' else 0.,1. if r.get('paintopid')=='colorsmudge' else 0.,float(p.get('ColorRateValue','0')) if p.get('PressureColorRate')=='true' else 0.,float(p.get('SmudgeRateValue','1')),float(p.get('Texture/Pattern/Strength','1')) if tex else 0.,1. if cat=='aerografos' else 0.,float(b.get('density','1'))]
  # Preserve source pressure curves for size/opacity/flow; engine uses bounded linear interpolation.
  curves=[]
  for key in ['Size','Opacity','Flow']:
   curve='0,0;1,1;'
   if p.get(key+'UseCurve')=='true':
    try:
     c=E.fromstring(p.get(key+'Sensor',''));v=c.find('curve');curve=v.text if v is not None else curve
    except Exception:pass
   pts=[list(map(float,s.split(','))) for s in curve.strip(';').split(';') if s];curves.append(pts)
  preset={'id':nextid,'name':name,'category':label,'folder':cat+'/'+slug,'bundle':bun,'engine':r.get('paintopid'),'frames':len(frames),'params':params,'curves':curves,'sha256':hashlib.sha256(data).hexdigest()}
  (folder/'preset.json').write_text(json.dumps(preset,ensure_ascii=False,indent=2)+'\n')
  (folder/'README.md').write_text(f'# {name}\n\nRecurso importado ({bun}), CC0; procedencia registrada en provenance.json. `source.kpp` conserva el preset íntegro; `preset.json` contiene su adaptación, `tip.bin` las máscaras de hasta 32 puntas de 128×128, `texture.bin` la textura de papel y `preview.png` la miniatura original. Motor compartido en `runtime/`. No incluye código ni recursos de HiPaint.\n')
  catalog.append({k:preset[k] for k in ['id','name','category','folder']});nextid+=1
(out/'catalog.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n')
print('Imported',len(catalog),'presets',len(groups),'categories')
