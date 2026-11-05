package com.ubai.veilpanel.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import com.ubai.veilpanel.image.BackgroundImageStore;
import com.ubai.veilpanel.image.PowerSaverBackdrop;
import com.ubai.veilpanel.ui.CustomFont;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Portable user settings and selected assets. Never contains notifications or grants. */
public final class PanelSettingsArchive {
    private static final String[] PREFS={"veil_panel","panosp_preferences","panel_settings_ui"};
    private static final String[] ASSETS={"panel-wallpaper-source.png","power-saver-backdrop.png","panel-custom-font"};
    private static final String IMAGE_KEY="panel_background_image_uri";
    private static final long MAX_ARCHIVE=64L*1024*1024, MAX_ENTRY=32L*1024*1024;
    private PanelSettingsArchive() {}

    public static synchronized void write(Context context,OutputStream output)throws Exception {
        JSONObject manifest=new JSONObject().put("format","veil-panel-settings").put("version",1);
        JSONObject groups=new JSONObject();
        for(int i=0;i<PREFS.length;i++)groups.put("group"+i,encode(context.getSharedPreferences(PREFS[i],0).getAll()));
        JSONObject appearance=groups.getJSONObject("group1");
        String background=context.getSharedPreferences(PREFS[1],0).getString(IMAGE_KEY,null);
        // A path or provider grant from one sandbox is never portable.
        appearance.remove(IMAGE_KEY);
        manifest.put("preferences",groups);
        JSONArray assets=new JSONArray();
        for(String name:ASSETS)if(new File(context.getFilesDir(),name).isFile())assets.put(name);
        if(background!=null&&!background.isEmpty())assets.put("background.webp");
        manifest.put("assets",assets);
        try(ZipOutputStream zip=new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("settings.json"));zip.write(manifest.toString(2).getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            for(int i=0;i<assets.length();i++) {
                String name=assets.getString(i);zip.putNextEntry(new ZipEntry(name));
                try(InputStream input="background.webp".equals(name)?new BackgroundImageStore(context).open(Uri.parse(background)):new FileInputStream(new File(context.getFilesDir(),name))) {
                    BackgroundImageStore.copy(input,zip,MAX_ENTRY);
                }
                zip.closeEntry();
            }
        }
    }

    public static synchronized void restore(Context context,InputStream input)throws Exception {
        File stage=new File(context.getCacheDir(),"settings-restore-"+UUID.randomUUID());
        if(!stage.mkdirs())throw new IOException("Cannot prepare restore");
        String preparedBackground=null;
        BackgroundImageStore store=new BackgroundImageStore(context);
        try {
            File archive=new File(stage,"archive.zip");
            try(OutputStream out=new FileOutputStream(archive)){BackgroundImageStore.copy(input,out,MAX_ARCHIVE);}
            Map<String,File> entries=new HashMap<>();long total=0;
            try(ZipFile zip=new ZipFile(archive)) {
                Enumeration<? extends ZipEntry> enumeration=zip.entries();
                while(enumeration.hasMoreElements()) {
                    ZipEntry entry=enumeration.nextElement();String name=entry.getName();
                    if(entry.isDirectory()||!allowed(name)||entries.containsKey(name))throw new IOException("Invalid backup entry");
                    File file=new File(stage,name);
                    try(CheckedInputStream in=new CheckedInputStream(zip.getInputStream(entry),new CRC32());OutputStream out=new FileOutputStream(file)) {
                        BackgroundImageStore.copy(in,out,"settings.json".equals(name)?1024*1024:MAX_ENTRY);
                        if(in.getChecksum().getValue()!=entry.getCrc())throw new IOException("Invalid checksum");
                    }
                    total+=file.length();if(total>MAX_ARCHIVE)throw new IOException("Backup too large");entries.put(name,file);
                }
            }
            File json=entries.remove("settings.json");if(json==null)throw new IOException("Missing settings");
            JSONTokener parser=new JSONTokener(new String(java.nio.file.Files.readAllBytes(json.toPath()),StandardCharsets.UTF_8));
            Object parsed=parser.nextValue();if(!(parsed instanceof JSONObject)||parser.nextClean()!=0)throw new IOException("Invalid settings");
            JSONObject manifest=(JSONObject)parsed;
            if(!"veil-panel-settings".equals(manifest.getString("format"))||manifest.getDouble("version")!=1)throw new IOException("Unsupported backup");
            JSONArray declared=manifest.getJSONArray("assets");Set<String> names=new HashSet<>();
            for(int i=0;i<declared.length();i++)if(!names.add(declared.getString(i)))throw new IOException("Duplicate asset");
            if(!names.equals(entries.keySet()))throw new IOException("Missing or unexpected asset");
            List<Map<String,Object>> values=new ArrayList<>();JSONObject groups=manifest.getJSONObject("preferences");
            if(groups.length()!=PREFS.length)throw new IOException("Invalid preference groups");
            for(int i=0;i<PREFS.length;i++)values.add(decode(groups.getJSONObject("group"+i)));
            values.get(1).remove(IMAGE_KEY);
            for(String name:ASSETS)if(entries.containsKey(name))validateAsset(name,entries.get(name));
            if(entries.containsKey("background.webp")) {
                try(InputStream image=new FileInputStream(entries.get("background.webp"))){preparedBackground=store.prepareWebp(image);}
                values.get(1).put(IMAGE_KEY,preparedBackground);
            }
            publish(context,stage,entries,values);
            preparedBackground=null; // Now owned by the committed configuration.
            PowerSaverBackdrop.invalidateCache();CustomFont.invalidateCache();
        } finally {
            store.deleteManaged(preparedBackground);
            File[] files=stage.listFiles();if(files!=null)for(File file:files)file.delete();stage.delete();
        }
    }

    private static boolean allowed(String name){return "settings.json".equals(name)||"background.webp".equals(name)||Arrays.asList(ASSETS).contains(name);}
    private static void validateAsset(String name,File file)throws IOException {
        if("panel-custom-font".equals(name)) {
            if(file.length()>20L*1024*1024)throw new IOException("Font too large");
            try(RandomAccessFile input=new RandomAccessFile(file,"r")) {
                int magic=input.readInt();if(magic!=0x00010000&&magic!=0x4f54544f&&magic!=0x74727565)throw new IOException("Invalid font");
            }
            try{Typeface.createFromFile(file);}catch(RuntimeException invalid){throw new IOException("Invalid font",invalid);}
        }else {
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),options);
            if(!"image/png".equals(options.outMimeType)||options.outWidth<1||options.outHeight<1||options.outWidth>4096||options.outHeight>4096)throw new IOException("Invalid wallpaper");
        }
    }
    private static void publish(Context context,File stage,Map<String,File> entries,List<Map<String,Object>> values)throws IOException {
        List<Map<String,?>> previous=new ArrayList<>();Set<String> replaced=new HashSet<>(),saved=new HashSet<>();
        for(String name:PREFS)previous.add(new HashMap<>(context.getSharedPreferences(name,0).getAll()));
        try {
            for(String name:ASSETS) {
                File current=new File(context.getFilesDir(),name), rollback=new File(stage,"previous-"+name);
                if(current.exists()){if(!current.renameTo(rollback))throw new IOException("Cannot preserve current asset");saved.add(name);}
                replaced.add(name);
                File next=entries.get(name);if(next!=null&&!next.renameTo(current))throw new IOException("Cannot save asset");
            }
            for(int i=0;i<PREFS.length;i++)if(!replace(context.getSharedPreferences(PREFS[i],0),values.get(i)))throw new IOException("Cannot save settings");
        }catch(IOException|RuntimeException error) {
            for(int i=0;i<PREFS.length;i++)replace(context.getSharedPreferences(PREFS[i],0),previous.get(i));
            for(String name:replaced){new File(context.getFilesDir(),name).delete();if(saved.contains(name))new File(stage,"previous-"+name).renameTo(new File(context.getFilesDir(),name));}
            throw error;
        }
    }
    private static boolean replace(SharedPreferences preferences,Map<String,?> values){
        SharedPreferences.Editor edit=preferences.edit().clear();
        for(Map.Entry<String,?> entry:values.entrySet()) {
            String key=entry.getKey();Object value=entry.getValue();
            if(value instanceof Integer)edit.putInt(key,(Integer)value);else if(value instanceof Long)edit.putLong(key,(Long)value);
            else if(value instanceof Float)edit.putFloat(key,(Float)value);else if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);
            else if(value instanceof String)edit.putString(key,(String)value);else if(value instanceof Set){Set<String> strings=new HashSet<>();for(Object item:(Set<?>)value)strings.add((String)item);edit.putStringSet(key,strings);}
            else throw new IllegalArgumentException("Unsupported preference");
        }
        return edit.commit();
    }
    private static JSONObject encode(Map<String,?> preferences)throws JSONException {
        JSONObject result=new JSONObject();
        for(Map.Entry<String,?> entry:preferences.entrySet()) {
            Object value=entry.getValue();String type;
            if(value instanceof Integer)type="int";else if(value instanceof Long)type="long";else if(value instanceof Float)type="float";
            else if(value instanceof Boolean)type="boolean";else if(value instanceof String)type="string";
            else if(value instanceof Set){type="strings";value=new JSONArray((Set<?>)value);}else throw new JSONException("Unsupported preference");
            result.put(entry.getKey(),new JSONObject().put("type",type).put("value",value));
        }
        return result;
    }
    private static Map<String,Object> decode(JSONObject object)throws JSONException {
        if(object.length()>2000)throw new JSONException("Too many preferences");Map<String,Object> result=new HashMap<>();
        Iterator<String> keys=object.keys();
        while(keys.hasNext()) {
            String key=keys.next();if(key.length()>200)throw new JSONException("Invalid key");
            JSONObject entry=object.getJSONObject(key);Object raw=entry.get("value"), value;
            switch(entry.getString("type")) {
                case "int": if(!(raw instanceof Number)||((Number)raw).doubleValue()!=((Number)raw).intValue())throw new JSONException("Invalid integer");value=((Number)raw).intValue();break;
                case "long": if(!(raw instanceof Number)||raw instanceof Double||raw instanceof Float)throw new JSONException("Invalid long");value=((Number)raw).longValue();break;
                case "float": if(!(raw instanceof Number)||!Float.isFinite(((Number)raw).floatValue()))throw new JSONException("Invalid float");value=((Number)raw).floatValue();break;
                case "boolean": if(!(raw instanceof Boolean))throw new JSONException("Invalid boolean");value=raw;break;
                case "string": if(!(raw instanceof String))throw new JSONException("Invalid string");value=raw;break;
                case "strings": if(!(raw instanceof JSONArray))throw new JSONException("Invalid strings");Set<String> strings=new HashSet<>();JSONArray array=(JSONArray)raw;for(int i=0;i<array.length();i++){Object item=array.get(i);if(!(item instanceof String))throw new JSONException("Invalid string");strings.add((String)item);}value=strings;break;
                default:throw new JSONException("Unknown preference type");
            }
            result.put(key,value);
        }
        return result;
    }
}
