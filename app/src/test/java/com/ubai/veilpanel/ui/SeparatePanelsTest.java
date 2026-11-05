package com.ubai.veilpanel.ui;
import android.content.Context;
import android.view.*;
import android.widget.*;
import com.ubai.veilpanel.config.PanelRouting;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class SeparatePanelsTest {
    private Context context=RuntimeEnvironment.getApplication();
    private boolean contains(View view,Class<?> type) {
        if(type.isInstance(view))return true;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(contains(((ViewGroup)view).getChildAt(i),type))return true;
        return false;
    }
    @Test public void notificationsNeverContainQuickSettings() {
        ShadeView view=new ShadeView(context,()->{});
        assertFalse(contains(view,TileGridView.class));assertFalse(contains(view,BrightnessControlView.class));assertFalse(contains(view,ScrollView.class));view.onHidden();
    }

    @Test public void initialDownRoutesOnEachSideAndBoundary() {
        assertFalse(PanelRouting.isQuickSettings(249,500,50));assertTrue(PanelRouting.isQuickSettings(250,500,50));assertTrue(PanelRouting.isQuickSettings(499,500,50));
    }
    @Test public void splitPersistsAndClampsWithoutTouchingSystemSettings() {
        PanelRouting.setSplit(context,70);assertEquals(70,PanelRouting.split(context));assertFalse(PanelRouting.isQuickSettings(300,500,70));
        PanelRouting.setSplit(context,1000);assertEquals(80,PanelRouting.split(context));PanelRouting.setSplit(context,-1);assertEquals(20,PanelRouting.split(context));
    }
}
