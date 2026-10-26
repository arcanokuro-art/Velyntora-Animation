package art.velyntora.core;

import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.widget.PopupMenu;

/** Two-stage figure dropdown: finish first, then a specific figure. */
final class FigurasSelector {
  static Menu populate(Menu menu,DrawingView drawing){
    add(menu.addSubMenu("Sin relleno"),1,FigurasModule.SIN_RELLENO,drawing);
    add(menu.addSubMenu("Con relleno"),2,FigurasModule.CON_RELLENO,drawing);
    return menu;
  }
  private static void add(SubMenu menu,int group,int[] ids,DrawingView drawing){
    int selected=FigurasModule.selected(drawing);
    for(int i=0;i<ids.length;i++)menu.add(group,ids[i],i,FigurasModule.label(ids[i])).setIcon(Tools.icon(ids[i],drawing.getContext())).setCheckable(true).setChecked(ids[i]==selected);
    menu.setGroupCheckable(group,true,true);
  }
  static void show(View anchor,DrawingView drawing,Runnable changed){
    PopupMenu popup=new PopupMenu(anchor.getContext(),anchor);
    populate(popup.getMenu(),drawing);
    if(android.os.Build.VERSION.SDK_INT>=29)popup.setForceShowIcon(true);
    popup.setOnMenuItemClickListener(item->{
      if(item.hasSubMenu() || !FigurasModule.contains(item.getItemId()))return false;
      FigurasModule.select(drawing,item.getItemId());changed.run();return true;
    });
    popup.show();
  }
  private FigurasSelector(){}
}
