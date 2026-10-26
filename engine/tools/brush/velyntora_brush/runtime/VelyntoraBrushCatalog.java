package art.velyntora.core;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import org.json.*;
import java.io.*;
import java.util.*;

/** Read-only catalog. Original presets and each adaptation live in their own directory. */
final class VelyntoraBrushCatalog {
  static final class Preset {
    final int id;final String name,category,folder;
    Preset(JSONObject value)throws JSONException{id=value.getInt("id");name=value.getString("name");category=value.getString("category");folder=value.getString("folder");}
    String label(){return name.replaceFirst("^[a-z]\\)_", "").replace('_',' ');}
  }
  static List<Preset> load(Context context){
    try{
      JSONArray array=new JSONArray(new String(read(context,"catalog.json"),java.nio.charset.StandardCharsets.UTF_8));
      ArrayList<Preset> result=new ArrayList<>();
      for(int i=0;i<array.length();i++)result.add(new Preset(array.getJSONObject(i)));
      return Collections.unmodifiableList(result);
    }catch(IOException|JSONException error){throw new IllegalStateException("Catálogo de pinceles Velyntora",error);}
  }
  static byte[] read(Context context,String name)throws IOException{
    try(InputStream input=context.getAssets().open(name);ByteArrayOutputStream output=new ByteArrayOutputStream()){
      byte[] data=new byte[8192];int n;while((n=input.read(data))!=-1)output.write(data,0,n);return output.toByteArray();
    }
  }
  static BitmapDrawable icon(Context context,Preset preset){
    try{byte[] data=read(context,preset.folder+"/preview.png");return new BitmapDrawable(context.getResources(),BitmapFactory.decodeByteArray(data,0,data.length));}
    catch(IOException error){return null;}
  }
  private VelyntoraBrushCatalog(){}
}
