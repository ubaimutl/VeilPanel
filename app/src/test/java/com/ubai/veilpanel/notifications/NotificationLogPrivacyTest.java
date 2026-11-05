package com.ubai.veilpanel.notifications;

import android.app.Notification;
import android.service.notification.StatusBarNotification;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class NotificationLogPrivacyTest {
    @Test public void cardLaunchAndSwipeLogsDoNotExposeIdentifiers() {
        android.content.Context context=RuntimeEnvironment.getApplication();
        NotificationRepository repository=NotificationRepository.getInstance();
        com.ubai.veilpanel.ui.ShadeView shade=new com.ubai.veilpanel.ui.ShadeView(context,()->{});
        final String[] dismissed={null};
        NotificationRepository.ActionHandler handler=new NotificationRepository.ActionHandler(){
            @Override public void dismissNotification(String key){dismissed[0]=key;}
            @Override public void refreshNotifications(){}
        };
        try {
            ShadowLog.clear();
            for(int mode=0;mode<3;mode++){
                android.app.PendingIntent launch=mode==0?null:android.app.PendingIntent.getActivity(context,800+mode,
                    new android.content.Intent("private.intent.sentinel"),android.app.PendingIntent.FLAG_IMMUTABLE);
                Notification notification=new Notification.Builder(context,"private-channel-sentinel")
                    .setContentTitle("private-title-sentinel").setContentText("private-body-sentinel").setContentIntent(launch).build();
                if(mode==2)launch.cancel();
                org.robolectric.util.ReflectionHelpers.callInstanceMethod(shade,"openNotification",
                    org.robolectric.util.ReflectionHelpers.ClassParameter.from(NotificationEntry.class,NotificationEntry.from(privateNotification(notification),context)));
            }
            for(boolean ongoing:new boolean[]{false,true}){
                Notification notification=new Notification.Builder(context,"private-channel-sentinel").setContentTitle("private-title-sentinel").setOngoing(ongoing).build();
                StatusBarNotification sbn=privateNotification(notification);
                repository.replaceAll(new StatusBarNotification[]{sbn},context);repository.setActionHandler(handler);dismissed[0]=null;
                android.view.View card=new android.view.View(context);card.layout(0,0,500,300);
                org.robolectric.util.ReflectionHelpers.callInstanceMethod(shade,"attachNotificationGestures",
                    org.robolectric.util.ReflectionHelpers.ClassParameter.from(android.view.View.class,card),
                    org.robolectric.util.ReflectionHelpers.ClassParameter.from(NotificationEntry.class,NotificationEntry.from(sbn,context)));
                for(int action:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_MOVE,android.view.MotionEvent.ACTION_UP}){
                    android.view.MotionEvent event=android.view.MotionEvent.obtain(0,10,action,action==android.view.MotionEvent.ACTION_DOWN?0:400,100,0);
                    card.dispatchTouchEvent(event);event.recycle();
                }
                if(ongoing)assertNull(dismissed[0]);else assertEquals(sbn.getKey(),dismissed[0]);
            }
            java.util.List<ShadowLog.LogItem> logs=ShadowLog.getLogsForTag("PanOSP.Notification");
            assertTrue(logs.stream().anyMatch(item->"contentIntent sent".equals(item.msg)));
            assertTrue(logs.stream().anyMatch(item->"contentIntent canceled".equals(item.msg)));
            assertTrue(logs.stream().anyMatch(item->"swipe dismiss".equals(item.msg)));
            assertTrue(logs.stream().anyMatch(item->"swipe blocked, not clearable".equals(item.msg)));
            for(ShadowLog.LogItem item:logs){assertFalse(item.msg,item.msg.contains("sentinel"));assertNull("Card action logs omit potentially sensitive exception details",item.throwable);}
        }finally{repository.clearActionHandler(handler);repository.replaceAll(new StatusBarNotification[0]);}
    }
    private StatusBarNotification privateNotification(Notification notification){
        return new StatusBarNotification("private.package.sentinel","private.package.sentinel",42,"private-account-sentinel",10001,0,0,notification,android.os.Process.myUserHandle(),1L);
    }
    @Test public void identifiersStayUsableForDismissalButDoNotReachRoutineLogs() {
        NotificationRepository repository = NotificationRepository.getInstance();
        repository.replaceAll(new StatusBarNotification[0]);
        Notification notification = new Notification.Builder(RuntimeEnvironment.getApplication(), "private-channel-sentinel")
                .setContentTitle("private-title-sentinel")
                .setContentText("private-body-sentinel").build();
        StatusBarNotification sbn = new StatusBarNotification("private.package.sentinel", "private.package.sentinel",
                42, "private-account-sentinel", 10001, 0, 0, notification,
                android.os.Process.myUserHandle(), 1L);
        final String[] dismissed = new String[1];
        NotificationRepository.ActionHandler handler = new NotificationRepository.ActionHandler() {
            @Override public void dismissNotification(String key) { dismissed[0] = key; }
            @Override public void refreshNotifications() { }
        };
        PanOSPNotificationListener service = Robolectric.buildService(PanOSPNotificationListener.class).create().get();
        ShadowLog.clear();
        try {
            service.onNotificationPosted(sbn);
            assertEquals(sbn.getKey(), repository.snapshot().get(0).getKey());
            repository.setActionHandler(handler);
            assertTrue(repository.requestDismiss(sbn.getKey()));
            assertEquals(sbn.getKey(), dismissed[0]);
            service.onNotificationRemoved(sbn);
            assertTrue(repository.snapshot().isEmpty());
            assertFalse(repository.requestDismiss(sbn.getKey()));
            assertFalse(ShadowLog.getLogsForTag("PanOSP.Notification").isEmpty());
            for (ShadowLog.LogItem item : ShadowLog.getLogsForTag("PanOSP.Notification")) {
                assertFalse(item.msg, item.msg.contains("sentinel"));
                assertFalse(item.msg, item.msg.contains(sbn.getKey()));
            }
        } finally {
            repository.clearActionHandler(handler);
            repository.replaceAll(new StatusBarNotification[0]);
            service.onDestroy();
        }
    }
}
