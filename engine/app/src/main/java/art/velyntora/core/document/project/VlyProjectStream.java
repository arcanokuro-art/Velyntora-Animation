package art.velyntora.core;

import art.velyntora.core.document.CanvasLimits;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Portable streamed adapter for the native VLYCORE v2 project format (reads v1/v2 and the raster result of v3 image elements). */
final class VlyProjectStream {
  static final byte[] MAGIC = {'V', 'L', 'Y', 'C', 'O', 'R', 'E', 2};

  static int integer(DataInputStream in) throws IOException {
    return Integer.reverseBytes(in.readInt());
  }

  static void integer(DataOutputStream out, int value) throws IOException {
    out.writeInt(Integer.reverseBytes(value));
  }

  static void limits(int w, int h, int count, int active) throws IOException {
    if (w < 1
        || h < 1
        || w > CanvasLimits.MAX_SIDE
        || h > CanvasLimits.MAX_SIDE
        || (long) w * h > CanvasLimits.MAX_PIXELS
        || count < 1
        || count > 32
        || active < 0
        || active >= count) throw new IOException("Proyecto demasiado grande o inválido");
  }

  static final class Layer {
    String name;
    boolean visible;
    float opacity;
    int[] pixels;
    int blendMode;

    Layer(String n, boolean v, float o, int[] p) {this(n,v,o,p,0);}
    Layer(String n, boolean v, float o, int[] p,int mode) {
      blendMode=mode;
      name = n;
      visible = v;
      opacity = o;
      pixels = p;
    }
  }

  static final class Reader {
    final DataInputStream in;
    final int width, height, count, active;
    int index;
    final int version;

    Reader(InputStream input) throws IOException {
      in = new DataInputStream(new BufferedInputStream(input));
      byte[] magic = new byte[8];
      in.readFully(magic);
      version=magic[7];magic[7]=2;
      if ((version!=1&&version!=2&&version!=3&&version!=4)||!java.util.Arrays.equals(magic, MAGIC)) throw new IOException("Proyecto incompatible");
      width = integer(in);
      height = integer(in);
      count = integer(in);
      active = integer(in);
      limits(width, height, count, active);
    }

    Layer next() throws IOException {
      if (index >= count) throw new EOFException();
      int size = integer(in);
      if (size < 0 || size > 4096) throw new IOException("Nombre inválido");
      byte[] name = new byte[size];
      in.readFully(name);
      int visibility = integer(in);
      float opacity = Float.intBitsToFloat(integer(in));
      if ((visibility != 0 && visibility != 1)
          || (Float.isNaN(opacity) || Float.isInfinite(opacity))
          || opacity < 0
          || opacity > 1) throw new IOException("Propiedades inválidas");
      int mode=version>=2?integer(in):0;
      if(mode<0||mode>15)throw new IOException("Mezcla inválida");
      int[] pixels = new int[width * height];
      for (int i = 0; i < pixels.length; i++) pixels[i] = integer(in);
      if(version>=3){
        int has=integer(in);if(has<0 || has>1)throw new IOException("Elemento inválido");
        if(has==1){
          float[] t=new float[9];for(int i=0;i<9;i++){t[i]=Float.intBitsToFloat(integer(in));if(Float.isNaN(t[i]) || Float.isInfinite(t[i]) || Math.abs(t[i])>1000000)throw new IOException("Transformación inválida");}
          if(t[0]<0 || t[1]<0 || t[2]<=t[0] || t[3]<=t[1] || t[2]>6000 || t[3]>6000 || t[6]<1 || t[7]<1 || t[6]>24000 || t[7]>24000)throw new IOException("Transformación inválida");
          int bytes=integer(in);if(bytes<1 || bytes>256*1024*1024)throw new IOException("Fuente inválida");
          byte[] buffer=new byte[65536];while(bytes>0){int n=Math.min(bytes,buffer.length);in.readFully(buffer,0,n);bytes-=n;}
        }
      }
      if(version>=4){int has=integer(in);if(has<0 || has>1)throw new IOException("Texto inválido");if(has==1){int n=integer(in);if(n<1 || n>16384)throw new IOException("Texto inválido");byte[] text=new byte[n];in.readFully(text);int sizePx=integer(in);integer(in);int family=integer(in),style=integer(in),wrap=integer(in);if(sizePx<4 || sizePx>6000 || family<0 || family>2 || style<0 || style>3 || wrap<8 || wrap>6000)throw new IOException("Formato de texto inválido");}}
      index++;
      return new Layer(new String(name, StandardCharsets.UTF_8), visibility == 1, opacity, pixels,mode);
    }

    void finish() throws IOException {
      if (index != count || in.read() != -1)
        throw new IOException("Proyecto incompleto o con datos sobrantes");
    }
  }

  static void header(DataOutputStream out, int w, int h, int count, int active) throws IOException {
    limits(w, h, count, active);
    out.write(MAGIC);
    integer(out, w);
    integer(out, h);
    integer(out, count);
    integer(out, active);
  }

  static void layer(DataOutputStream out, Layer layer, int count) throws IOException {
    byte[] name = layer.name.getBytes(StandardCharsets.UTF_8);
    if (name.length > 4096
        || layer.pixels.length != count
        || (Float.isNaN(layer.opacity) || Float.isInfinite(layer.opacity))
        || layer.opacity < 0
        || layer.opacity > 1) throw new IOException("Capa inválida");
    integer(out, name.length);
    out.write(name);
    integer(out, layer.visible ? 1 : 0);
    integer(out, Float.floatToIntBits(layer.opacity));
    if(layer.blendMode<0||layer.blendMode>15)throw new IOException("Mezcla inválida");
    integer(out,layer.blendMode);
    for (int pixel : layer.pixels) integer(out, pixel);
  }

 static int composite(int src,int dst,float opacity,int mode){return BlendCompositor.composite(src,dst,opacity,mode);}
 static final String[] ORA_MODES={"svg:src-over","svg:multiply","svg:color-burn","svg:color-dodge","svg:overlay","svg:difference","svg:lighten","svg:darken","svg:screen","velyntora:xor","svg:hard-light","svg:soft-light","svg:color","svg:luminosity","svg:hue","svg:saturation"};
}
