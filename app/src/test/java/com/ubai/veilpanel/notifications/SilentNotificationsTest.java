package com.ubai.veilpanel.notifications;
import android.app.Notification;
import android.app.NotificationManager;
import android.service.notification.StatusBarNotification;
import android.view.*;
import android.widget.TextView;
import com.ubai.veilpanel.ui.ShadeView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class SilentNotificationsTest {
    private final android.content.Context context=RuntimeEnvironment.getApplication();
    private StatusBarNotification alert(int id,String title) {
        Notification n=new Notification.Builder(context,"test").setContentTitle(title).setContentText("Detail").build();
        return new StatusBarNotification(context.getPackageName(),context.getPackageName(),id,null,10001,0,0,n,android.os.Process.myUserHandle(),id);
    }
    private TextView find(View view,String text) {
        if(view instanceof TextView && text.contentEquals(((TextView)view).getText()))return (TextView)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){TextView child=find(((ViewGroup)view).getChildAt(i),text);if(child!=null)return child;}
        return null;
    }
    @After public void clear(){NotificationRepository.getInstance().replaceAll(new StatusBarNotification[0]);}
    @Test public void rankingUsesChannelImportanceAndAmbientState() {
        assertTrue(NotificationRepository.isSilentRanking(NotificationManager.IMPORTANCE_LOW,false));
        assertTrue(NotificationRepository.isSilentRanking(NotificationManager.IMPORTANCE_MIN,false));
        assertFalse(NotificationRepository.isSilentRanking(NotificationManager.IMPORTANCE_DEFAULT,false));
        assertFalse(NotificationRepository.isSilentRanking(NotificationManager.IMPORTANCE_HIGH,false));
        assertFalse(NotificationRepository.isSilentRanking(NotificationManager.IMPORTANCE_UNSPECIFIED,false));
        assertTrue(NotificationRepository.isSilentRanking(NotificationManager.IMPORTANCE_DEFAULT,true));
    }
    @Test public void quietNotificationsStartCollapsedBelowAlertingAndExpandOnTap() {
        NotificationRepository repo=NotificationRepository.getInstance();repo.replaceAll(new StatusBarNotification[]{alert(1,"Alerting item"),alert(2,"Quiet item")},context);
        for(NotificationEntry e:repo.snapshot())e.updateSilent("Quiet item".contentEquals(e.getTitle()));
        ShadeView view=new ShadeView(context,()->{});view.onShown();
        try {
            assertNotNull(find(view,"Alerting item"));assertNull(find(view,"Quiet item"));
            TextView toggle=find(view,"Silent · 1");assertNotNull(toggle);((View)toggle.getParent()).performClick();
            assertNotNull(find(view,"Quiet item"));((View)find(view,"Silent · 1").getParent()).performClick();assertNull(find(view,"Quiet item"));
        } finally {view.onHidden();}
    }
    @Test public void aNewPanelCollapsesSilentNotificationsAgain() {
        NotificationRepository repo=NotificationRepository.getInstance();repo.replaceAll(new StatusBarNotification[]{alert(1,"Quiet item")},context);repo.snapshot().get(0).updateSilent(true);
        ShadeView first=new ShadeView(context,()->{});first.onShown();((View)find(first,"Silent · 1").getParent()).performClick();assertNotNull(find(first,"Quiet item"));first.onHidden();
        ShadeView second=new ShadeView(context,()->{});second.onShown();try{assertNull(find(second,"Quiet item"));assertNotNull(find(second,"Silent · 1"));}finally{second.onHidden();}
    }
}
