package com.ubai.veilpanel.ui;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewTreeObserver;
import com.ubai.veilpanel.image.PowerSaverBackdrop;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shares one software-blurred selected wallpaper across all cards. Never
 * captures the screen, renders notification text, or allocates images per frame. */
final class LiquidGlassBackdrop {
    private static final ExecutorService IMAGES=Executors.newSingleThreadExecutor();
    private final View owner;
    private final ArrayList<Watch> cards=new ArrayList<>();
    private final int[] location=new int[2],origin=new int[2];
    private Bitmap bitmap;
    private int generation;
    private boolean listening;
    private final ViewTreeObserver.OnPreDrawListener preDraw=this::beforeDraw;
    private static final class Watch {
        final WeakReference<View> view;
        final boolean persistent;
        int x=Integer.MIN_VALUE,y=Integer.MIN_VALUE,w,h;
        Watch(View view,boolean persistent){this.view=new WeakReference<>(view);this.persistent=persistent;}
    }
    LiquidGlassBackdrop(View owner){this.owner=owner;}
    Bitmap bitmap(){return bitmap;}
    void watch(View view){cards.add(new Watch(view,false));}
    void watchPersistent(View view){cards.add(new Watch(view,true));}
    void resetCards(){cards.removeIf(watch->!watch.persistent);}
    void open(){
        if(!LiquidGlassOptions.enabled(owner.getContext())&&!"liquid".equals(owner.getContext().getSharedPreferences("veil_panel",0).getString("header_clock_style","plain")))return;
        if(!listening){owner.getViewTreeObserver().addOnPreDrawListener(preDraw);listening=true;}
        int request=++generation,blur=LiquidGlassOptions.get(owner.getContext(),"liquid_blur");
        IMAGES.execute(()->{
            Bitmap loaded=null;
            try{loaded=PowerSaverBackdrop.loadAtBlur(owner.getContext(),blur);}catch(RuntimeException|OutOfMemoryError unavailable){}
            final Bitmap ready=loaded;
            owner.post(()->{if(request==generation&&listening){bitmap=ready;for(Watch card:cards){View view=card.view.get();if(view!=null)view.invalidate();}}});
        });
    }
    private boolean beforeDraw(){
        View root=owner.getRootView();root.getLocationOnScreen(origin);
        for(int i=cards.size()-1;i>=0;i--){
            Watch watch=cards.get(i);View card=watch.view.get();
            if(card==null){cards.remove(i);continue;}
            card.getLocationOnScreen(location);int x=location[0]-origin[0],y=location[1]-origin[1];
            if(x!=watch.x||y!=watch.y||root.getWidth()!=watch.w||root.getHeight()!=watch.h){
                watch.x=x;watch.y=y;watch.w=root.getWidth();watch.h=root.getHeight();card.invalidate();
            }
        }
        return true;
    }
    void close(){
        generation++;if(listening&&owner.getViewTreeObserver().isAlive())owner.getViewTreeObserver().removeOnPreDrawListener(preDraw);
        listening=false;cards.clear();bitmap=null;
    }
}
