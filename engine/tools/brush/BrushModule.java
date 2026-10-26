package art.velyntora.core;

import java.util.List;

/** Owns the selected preset; all variants retain the stable BRUSH tool ID. */
final class BrushModule {
  static final int CLASSIC=0, ANTIALIASED=1;
  private final DrawingView view;
  private int selected;
  final List<VelyntoraBrushCatalog.Preset> presets;
  final VelyntoraBrushRuntime runtime;
  BrushModule(DrawingView view) {
    this.view=view;presets=VelyntoraBrushCatalog.load(view.getContext());runtime=new VelyntoraBrushRuntime(view);
    selected=view.getContext().getSharedPreferences("brush-variants",0).getInt("selected",CLASSIC);
    if(selected!=CLASSIC && selected!=ANTIALIASED){VelyntoraBrushCatalog.Preset p=find(selected);if(p==null || !runtime.load(p))selected=CLASSIC;}
  }
  VelyntoraBrushCatalog.Preset find(int id){for(VelyntoraBrushCatalog.Preset preset:presets)if(preset.id==id)return preset;return null;}
  int selected() { return selected; }
  boolean isAntialiased() { return selected==ANTIALIASED; }
  boolean isVelyntoraPreset(){return selected>=100;}
  String label() {VelyntoraBrushCatalog.Preset p=find(selected);return p!=null?p.label():isAntialiased()?"Pincel suavizado":"Pincel clásico";}
  void begin(){if(isVelyntoraPreset() && !runtime.begin())((MainActivity)view.getContext()).message("No se pudo preparar el pincel");}
  void stop(){runtime.stop();}
  void select(int id) {
    VelyntoraBrushCatalog.Preset preset=find(id);
    if(id!=CLASSIC && id!=ANTIALIASED && preset==null)return;
    view.finishOpacityStroke();runtime.stop();
    if(preset!=null && !runtime.load(preset)){((MainActivity)view.getContext()).message("No se pudo cargar el pincel");return;}
    selected=id;
    view.getContext().getSharedPreferences("brush-variants",0).edit().putInt("selected",id).apply();
    view.setTool(DrawingView.BRUSH);view.invalidate();
  }
}
