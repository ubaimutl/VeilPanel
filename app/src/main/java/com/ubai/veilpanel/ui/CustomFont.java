package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import java.io.*;

/** Private validated font copy; never depends on a document-provider grant. */
public final class CustomFont {
    private static final int MAX_BYTES=20*1024*1024;
    private static Typeface cached;
    private static String cachedPath;
    private CustomFont() {}
    private static File file(Context context){return new File(context.getFilesDir(),"panel-custom-font");}
    public static synchronized Typeface load(Context context){
        File font=file(context);
        if(!font.isFile())return null;
        if(cached!=null&&font.getAbsolutePath().equals(cachedPath))return cached;
        try{cached=Typeface.createFromFile(font);cachedPath=font.getAbsolutePath();return cached;}
        catch(RuntimeException invalid){return null;}
    }
    public static synchronized void importFont(Context context,Uri uri)throws IOException{
        File temp=File.createTempFile("font-import-",".tmp",context.getFilesDir());
        try{
            try(InputStream input=context.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(temp)){
                if(input==null)throw new IOException("No font stream");
                byte[] bytes=new byte[8192];int count,total=0;
                while((count=input.read(bytes))!=-1){total+=count;if(total>MAX_BYTES)throw new IOException("Font too large");out.write(bytes,0,count);}
            }
            try(RandomAccessFile input=new RandomAccessFile(temp,"r")){
                int signature=input.readInt();
                if(signature!=0x00010000&&signature!=0x4f54544f&&signature!=0x74727565)throw new IOException("Not TTF or OTF");
            }
            Typeface validated;
            try{validated=Typeface.createFromFile(temp);}catch(RuntimeException invalid){throw new IOException("Invalid font",invalid);}
            if(!temp.renameTo(file(context)))throw new IOException("Couldn't save font");
            cached=validated;cachedPath=file(context).getAbsolutePath();
            context.getSharedPreferences("veil_panel",0).edit().putString("font_family","custom").apply();
        }finally{if(temp.exists())temp.delete();}
    }
    public static synchronized void invalidateCache(){cached=null;cachedPath=null;}
    public static synchronized void clear(Context context){
        if(file(context).exists()&&!file(context).delete())return;
        cached=null;cachedPath=null;
        if("custom".equals(context.getSharedPreferences("veil_panel",0).getString("font_family","system")))
            context.getSharedPreferences("veil_panel",0).edit().putString("font_family","system").apply();
    }
}
