package app.velyntora.editor;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
/** Start screen -> editor contract. The Android library owns document operations. */
public final class EditorNavigator {
    private EditorNavigator(){}
    public static void create(Activity host){host.startActivity(new Intent(host,art.velyntora.core.MainActivity.class).setAction("app.velyntora.drawing.CREATE"));}
    public static void open(Activity host,Uri image){host.startActivity(new Intent(host,art.velyntora.core.MainActivity.class).setAction("app.velyntora.drawing.OPEN").setData(image).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}
}
