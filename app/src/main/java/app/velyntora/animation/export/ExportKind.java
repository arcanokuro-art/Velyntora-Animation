package app.velyntora.animation.export;
public enum ExportKind {
 PROJECT("Guardar proyecto editable (.vlyanim)","vlyanim","application/zip"),
 MP4("Exportar video MP4 (.mp4)","mp4","video/mp4"),
 WEBM("Exportar video WebM (.webm)","webm","video/webm"),
 THREE_GP("Exportar video 3GP (.3gp)","3gp","video/3gpp"),
 GIF("Exportar GIF animado (.gif)","gif","image/gif"),
 APNG("Exportar PNG animado (.apng)","apng","image/apng"),
 PNG_SEQUENCE("Exportar secuencia PNG (.zip)","zip","application/zip"),
 PNG_FRAME("Exportar fotograma actual (.png)","png","image/png");
 public final String label,extension,mime;
 ExportKind(String label,String extension,String mime){this.label=label;this.extension=extension;this.mime=mime;}
 public boolean video(){return this==MP4||this==WEBM||this==THREE_GP;}
 public boolean animation(){return video()||this==GIF||this==APNG;}
}
