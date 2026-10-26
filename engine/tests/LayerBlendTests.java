package art.velyntora.core;
import java.io.*;
public final class LayerBlendTests {
 public static void main(String[] args)throws Exception{
  try(BufferedReader in=new BufferedReader(new FileReader(args[0]))){String line;while((line=in.readLine())!=null){String[] v=line.split(" ");int mode=Integer.parseInt(v[0]),src=(int)Long.parseLong(v[1]),dst=(int)Long.parseLong(v[2]),expected=(int)Long.parseLong(v[3]);if(VlyProjectStream.composite(src,dst,.6f,mode)!=expected)throw new AssertionError(line);}}
  for(int mode=0;mode<16;mode++){ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);VlyProjectStream.header(out,1,1,1,0);VlyProjectStream.layer(out,new VlyProjectStream.Layer("Color",true,.6f,new int[]{0x80abcdef},mode),1);VlyProjectStream.Reader reader=new VlyProjectStream.Reader(new ByteArrayInputStream(bytes.toByteArray()));if(reader.next().blendMode!=mode)throw new AssertionError();reader.finish();String xml="<image w='1' h='1'><stack><layer src='data/l.png' composite-op='"+VlyProjectStream.ORA_MODES[mode]+"'/></stack></image>";if(OpenRasterArchive.metadata(xml.getBytes("UTF-8")).layers.get(0).blendMode!=mode)throw new AssertionError();}
  System.out.println("All 16 blends: native/Java alpha parity, project persistence and OpenRaster mappings passed");
 }
}
