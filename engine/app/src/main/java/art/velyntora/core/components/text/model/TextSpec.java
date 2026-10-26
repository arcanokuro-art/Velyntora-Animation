package art.velyntora.core;

import java.nio.charset.StandardCharsets;

/** Immutable text content and typography; placement belongs to the object transform. */
final class TextSpec {
  static final String[] FAMILIES={"sans-serif","serif","monospace"};
  final String content;final int size,color,family,style,wrapWidth;
  TextSpec(String content,int size,int color,int family,int style,int wrapWidth){
    if(content==null || content.trim().isEmpty() || content.length()>4096 || content.indexOf('\0')>=0 || size<4 || size>6000 || family<0 || family>2 || style<0 || style>3 || wrapWidth<8 || wrapWidth>6000)throw new IllegalArgumentException("Texto o formato inválido");
    this.content=content;this.size=size;this.color=color;this.family=family;this.style=style;this.wrapWidth=wrapWidth;
  }
  static TextSpec effective(TextSpec spec,PlacedImageTransform state){
    float sx=state.width/state.crop.width(),sy=state.height/state.crop.height();
    return new TextSpec(spec.content,Math.max(4,Math.min(6000,Math.round(spec.size*sy))),spec.color,spec.family,spec.style,Math.max(8,Math.min(6000,Math.round(spec.wrapWidth*sx))));
  }
  byte[] bytes(){return content.getBytes(StandardCharsets.UTF_8);}
  int[] values(){return new int[]{size,color,family,style,wrapWidth};}
  static TextSpec read(int index){byte[] text=DrawingView.nativeTextContent(index);if(text==null)return null;int[] v=DrawingView.nativeTextStyle(index);if(v==null || v.length!=5)throw new IllegalArgumentException("Formato de texto inválido");return new TextSpec(new String(text,StandardCharsets.UTF_8),v[0],v[1],v[2],v[3],v[4]);}
}
