package app.velyntora.animation.export;
import android.graphics.Bitmap;
import java.io.*;
/** Worker-only frame access. The session restores the editable canvas after closing the export. */
public interface AnimationFrames {
 default void check(){}
 int width();int height();int fps();int count();int exposure(int index);int ticks();
 Bitmap render(int index,int width,int height,boolean opaque) throws IOException;
 void png(int index,OutputStream out) throws IOException;
}
