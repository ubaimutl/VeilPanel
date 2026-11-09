package com.ubai.veilpanel.accessibility;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import java.util.*;

/** Dismissal only: no notification data, settings, opening or permissions are exposed. */
public final class PanelNavigationProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    @Override public Bundle call(String method,String arg,Bundle extras){
        Bundle result=new Bundle();
        result.putBoolean("handled","dismissIfOpen".equals(method)&&authorizedCaller()
                && PanOSPAccessibilityServiceCore.requestPanelDismiss());
        return result;
    }
    private boolean authorizedCaller(){
        if(getContext()==null)return false;
        String[] packages=getContext().getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if(packages==null)return false;
        String enabled=Settings.Secure.getString(getContext().getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if(enabled==null)return false;
        Set<String> callers=new HashSet<>(Arrays.asList(packages));
        for(String entry:enabled.split(":")){
            ComponentName service=ComponentName.unflattenFromString(entry);
            if(service!=null&&callers.contains(service.getPackageName()))return true;
        }
        return false;
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder){return null;}
    @Override public String getType(Uri uri){return null;}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] selectionArgs){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] selectionArgs){throw new UnsupportedOperationException();}
}
