#!/usr/bin/env python3
"""Validate protected code and ownership without depending on Android/Gradle."""
from pathlib import Path
import hashlib,json,re,sys
root=Path(__file__).resolve().parents[1]
manifest=json.loads((root/'docs/modules/protected-code.json').read_text())
errors=[]
actual={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in (root/'tools').rglob('*') if p.is_file()}
if actual!=manifest['tools']:errors.append('Protected tools changed: '+', '.join(sorted(set(actual)^set(manifest['tools'])|{p for p in set(actual)&set(manifest['tools']) if actual[p]!=manifest['tools'][p]})))
if hashlib.sha256((root/"app/src/main/cpp/session/EngineSession.inc").read_bytes()).hexdigest()!=manifest["session"]:errors.append("Native session implementation changed")
exports={}
for p in (root/'app/src/main/cpp').rglob('*Bridge.inc'):
 text=p.read_text();starts=[m.start() for m in re.finditer(r'extern "C" JNIEXPORT',text)]
 for i,a in enumerate(starts):
  chunk=text[a:starts[i+1] if i+1<len(starts) else len(text)];name=re.search(r'DrawingView_(native\w+)',chunk).group(1)
  if name in exports:errors.append('Duplicate JNI export: '+name)
  exports[name]=hashlib.sha256(chunk.encode()).hexdigest()
if exports!=manifest['jni']:errors.append('JNI signatures or implementation bodies changed')
base=root/'app/src/main/java/art/velyntora/core'
for p in base.rglob('*.java'):
 if p.name in ('MainActivity.java','DrawingView.java'):continue
 for m in re.finditer(r'\bhost\.([A-Za-z_][A-Za-z_0-9]*)',p.read_text()):
  rest=p.read_text()[m.end():].lstrip()
  if not rest.startswith('('):errors.append(f'{p.relative_to(root)}: direct host field access {m.group(1)}')
for p in (base/'components').rglob('*.java'):
 if not (p.parent/'README.md').exists():errors.append('Missing component documentation: '+str(p.parent.relative_to(root)))
if errors:print('\n'.join(errors),file=sys.stderr);sys.exit(1)
print(f'Modular boundaries OK: {len(actual)} protected tool files, {len(exports)} validated JNI exports.')
