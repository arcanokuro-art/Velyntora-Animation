package art.velyntora.core;

import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.widget.PopupMenu;
import java.util.LinkedHashMap;

/** Same two-stage dropdown as figures: category first, preset second. */
final class BrushSelector {
  static Menu populate(Menu menu,DrawingView drawing){
    Menu basic=menu;
    basic.add(1,BrushModule.CLASSIC,0,"Pincel clásico").setCheckable(true).setChecked(drawing.brushes().selected()==BrushModule.CLASSIC);
    basic.add(1,BrushModule.ANTIALIASED,1,"Pincel con bordes suavizados").setCheckable(true).setChecked(drawing.brushes().selected()==BrushModule.ANTIALIASED);
    basic.setGroupCheckable(1,true,true);
    LinkedHashMap<String,SubMenu> categories=new LinkedHashMap<>();
    for(VelyntoraBrushCatalog.Preset preset:drawing.brushes().presets){
      SubMenu category=categories.get(preset.category);
      if(category==null){category=menu.addSubMenu(preset.category);categories.put(preset.category,category);}
      category.add(2,preset.id,category.size(),preset.label()).setIcon(VelyntoraBrushCatalog.icon(drawing.getContext(),preset)).setCheckable(true).setChecked(preset.id==drawing.brushes().selected());
      category.setGroupCheckable(2,true,true);
    }
    return menu;
  }
  static void show(View anchor,DrawingView drawing,Runnable changed) {
    PopupMenu popup=new PopupMenu(anchor.getContext(),anchor);populate(popup.getMenu(),drawing);
    if(android.os.Build.VERSION.SDK_INT>=29)popup.setForceShowIcon(true);
    popup.setOnMenuItemClickListener(item->{
      if(item.hasSubMenu())return false;
      drawing.brushes().select(item.getItemId());changed.run();return true;
    });
    popup.show();
  }
}
