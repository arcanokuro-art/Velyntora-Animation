package app.velyntora.animation.export.three_gp;
import app.velyntora.animation.export.*;import app.velyntora.animation.export.video.PlatformVideoEncoder;import java.io.*;
public final class ThreeGpExporter{
 public static void write(AnimationFrames frames,ExportRequest request,File file)throws IOException{
  PlatformVideoEncoder.write(frames,request,file,"video/avc",android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_3GPP);
 }
 private ThreeGpExporter(){}
}
