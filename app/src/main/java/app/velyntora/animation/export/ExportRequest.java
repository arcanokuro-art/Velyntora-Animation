package app.velyntora.animation.export;
public final class ExportRequest {
 public final ExportKind kind;public final int width,height,bitrate;
 public ExportRequest(ExportKind kind,int width,int height,int bitrate){
  if(kind==null||width<1||height<1||width>6000||height>6000||bitrate<1)throw new IllegalArgumentException("Configuración de exportación inválida");
  this.kind=kind;this.width=width;this.height=height;this.bitrate=bitrate;
 }
}
