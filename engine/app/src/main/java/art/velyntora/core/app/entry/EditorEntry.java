package art.velyntora.core;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
/** Drawing start-screen entry; preserves Core's document protection and import pipeline. */
final class EditorEntry {
    private EditorEntry(){}
    static boolean hasRequest(Intent intent){return intent!=null && ("app.velyntora.drawing.CREATE".equals(intent.getAction()) || "app.velyntora.drawing.OPEN".equals(intent.getAction()));}
    static void enter(MainActivity host,Bundle state){
        Intent intent=host.getIntent();
        if(state!=null || !hasRequest(intent))return;
        if("app.velyntora.drawing.CREATE".equals(intent.getAction()))host.protectDocument(()->host.configureDimensions(0));
        else if(intent.getData()!=null)host.protectDocument(()->host.openDetected(intent.getData()));
    }
    static void immersive(Window window){
        if(Build.VERSION.SDK_INT>=30){
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller=window.getInsetsController();
            if(controller!=null){controller.hide(WindowInsets.Type.systemBars());controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}
        }else window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
}
