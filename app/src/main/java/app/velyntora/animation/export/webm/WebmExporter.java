package app.velyntora.animation.export.webm;
import app.velyntora.animation.export.*;import app.velyntora.animation.export.video.PlatformVideoEncoder;import java.io.*;
public final class WebmExporter{
 public static void write(AnimationFrames frames,ExportRequest request,File file)throws IOException{
  PlatformVideoEncoder.write(frames,request,file,"video/x-vnd.on2.vp8",android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM);
 }
 private WebmExporter(){}
}
