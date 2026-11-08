package app.velyntora.animation.export.project;
import app.velyntora.animation.storage.AnimationProject;import java.io.*;
public final class EditableProjectExporter {
 public static void write(AnimationProject project,OutputStream out)throws IOException{project.exportArchive(out);}
 private EditableProjectExporter(){}
}
