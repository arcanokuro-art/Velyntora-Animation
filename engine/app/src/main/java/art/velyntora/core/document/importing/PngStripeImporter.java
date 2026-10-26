package art.velyntora.core;
import art.velyntora.core.document.CanvasLimits;
import java.io.*;
import java.util.zip.*;

/** Noninterlaced RGB/RGBA PNGs decode once, from top to bottom, in bounded stripes. */
final class PngStripeImporter {
  private static final int IDAT=0x49444154,IEND=0x49454e44;
  private static void skip(DataInputStream in,long count)throws IOException{
    byte[] buffer=new byte[8192];while(count>0){int n=in.read(buffer,0,(int)Math.min(count,buffer.length));if(n<0)throw new EOFException();count-=n;}
  }
  private static final class Idat extends InputStream {
    final DataInputStream in;int remaining;final CRC32 crc=new CRC32();boolean ended;
    Idat(DataInputStream input,int length)throws IOException{in=input;begin(length);}
    void begin(int length)throws IOException{
      if(length<0)throw new IOException("Chunk PNG inválido");remaining=length;crc.reset();crc.update(new byte[]{73,68,65,84});
    }
    public int read()throws IOException{byte[] one=new byte[1];return read(one,0,1)<0?-1:one[0]&255;}
    public int read(byte[] bytes,int offset,int length)throws IOException{
      if(length==0)return 0;
      while(!ended && remaining==0){
        if((int)crc.getValue()!=in.readInt())throw new IOException("CRC PNG inválido");
        int size=in.readInt(),type=in.readInt();
        if(type==IDAT)begin(size);
        else {if(size<0)throw new IOException("Chunk PNG inválido");PngStripeImporter.skip(in,size+4L);ended=true;}
      }
      if(ended)return -1;int n=in.read(bytes,offset,Math.min(length,remaining));if(n<0)throw new EOFException();
      crc.update(bytes,offset,n);remaining-=n;return n;
    }
    void finish()throws IOException{byte[] bytes=new byte[8192];while(read(bytes)>=0){} }
  }
  static boolean load(InputStream input,int orientation)throws IOException{return load(input,orientation,null);}
  static boolean load(InputStream input,int orientation,ImageImportSize plan)throws IOException{
    DataInputStream in=new DataInputStream(new BufferedInputStream(input));
    if(in.readLong()!=0x89504e470d0a1a0aL)return false;
    if(in.readInt()!=13 || in.readInt()!=0x49484452)return false;
    int width=in.readInt(),height=in.readInt(),depth=in.readUnsignedByte(),type=in.readUnsignedByte();
    int compression=in.readUnsignedByte(),filter=in.readUnsignedByte(),interlace=in.readUnsignedByte();in.readInt();
    if(depth!=8 || (type!=2 && type!=6) || compression!=0 || filter!=0 || interlace!=0)return false;
    if(width<=0 || height<=0 || width>1048576)throw new IOException("Dimensiones PNG no compatibles");
    if(plan==null && !CanvasLimits.accepts(width,height))throw new IOException("El límite es 6.000 × 6.000 px");
    if(plan!=null && (width!=plan.sourceWidth || height!=plan.sourceHeight))throw new IOException("La imagen cambió durante la importación");
    boolean reduced=plan!=null && plan.reduced();
    int outputWidth=reduced?plan.width:width,outputHeight=reduced?plan.height:height;
    int size;
    while(true){size=in.readInt();int kind=in.readInt();if(size<0)throw new IOException("Chunk PNG inválido");
      if(kind==IDAT)break;
      // RGB tRNS needs the generic decoder to preserve transparent-color metadata.
      if(kind==0x74524e53 || kind==0x69434350 || kind==0x6348524d)return false;
      if(kind==0x67414d41){if(size!=4)return false;int gamma=in.readInt();in.readInt();if(gamma!=45455)return false;continue;}
      if(kind==IEND)throw new IOException("PNG sin píxeles");skip(in,size+4L);
    }
    boolean rotated=orientation>=5;
    if(!DrawingView.nativeBeginImageImport(rotated?outputHeight:outputWidth,rotated?outputWidth:outputHeight))throw new IOException("No se pudo preparar el documento");
    Idat idat=new Idat(in,size);Inflater inflater=new Inflater();
    try {
      DataInputStream pixels=new DataInputStream(new InflaterInputStream(idat,inflater,65536));
      int bpp=type==2?3:4,stride=width*bpp,rows=Math.max(1,Math.min(256,1048576/width));
      byte[] previous=new byte[stride],current=new byte[stride];int[] stripe=new int[width*rows];
      PngReducedRows reducer=reduced?new PngReducedRows(plan):null;
      for(int top=0;top<height;top+=rows){int count=Math.min(rows,height-top);
        for(int y=0;y<count;y++){
          int mode=pixels.readUnsignedByte();if(mode>4)throw new IOException("Filtro PNG inválido");pixels.readFully(current);
          for(int at=0;at<stride;at++){
            int a=at>=bpp?current[at-bpp]&255:0,b=previous[at]&255,c=at>=bpp?previous[at-bpp]&255:0,value=0;
            if(mode==1)value=a;else if(mode==2)value=b;else if(mode==3)value=(a+b)/2;
            else if(mode==4){int p=a+b-c,pa=Math.abs(p-a),pb=Math.abs(p-b),pc=Math.abs(p-c);value=pa<=pb&&pa<=pc?a:pb<=pc?b:c;}
            current[at]=(byte)((current[at]&255)+value);
          }
          for(int x=0;x<width;x++){int at=x*bpp;stripe[y*width+x]=(type==6?(current[at+3]&255)<<24:0xff000000)|(current[at]&255)<<16|(current[at+1]&255)<<8|(current[at+2]&255);}
          if(reducer!=null)reducer.row(top+y,stripe,y*width);
          byte[] swap=previous;previous=current;current=swap;
        }
        if(reducer==null)ImageImportStripe.write(width,height,orientation,top,count,stripe);
      }
      if(reducer!=null)reducer.finish();
      if(pixels.read()!=-1)throw new IOException("PNG contiene datos adicionales");idat.finish();
      if(!DrawingView.nativeFinishImageImport())throw new IOException("No se pudo instalar el documento");
      return true;
    }finally{inflater.end();}
  }
  private PngStripeImporter(){}
}
