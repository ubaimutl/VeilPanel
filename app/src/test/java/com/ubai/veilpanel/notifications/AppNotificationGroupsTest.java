package com.ubai.veilpanel.notifications;
import android.app.Notification;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class AppNotificationGroupsTest {
    private NotificationEntry entry(String pkg,int id,int user,long time,boolean summary) {
        Notification n=new Notification.Builder(RuntimeEnvironment.getApplication(),"test").setContentTitle("Alert "+id).setGroup("conversation").setGroupSummary(summary).build();
        return NotificationEntry.from(new StatusBarNotification(pkg,pkg,id,null,10001,0,0,n,UserHandle.getUserHandleForUid(user * 100000 + 10001),time));
    }
    @Test public void stacksAcrossConversationsFromSameAppNewestFirst() {
        List<List<NotificationEntry>> groups=AppNotificationGroups.group(Arrays.asList(entry("chat",1,0,10,false),entry("mail",2,0,30,false),entry("chat",3,0,40,false)));
        assertEquals(2,groups.size());assertEquals("chat",groups.get(0).get(0).getPackageName());assertEquals(40,groups.get(0).get(0).getPostTime());assertEquals(2,groups.get(0).size());
    }
    @Test public void personalAndWorkProfilesStaySeparate() {
        assertEquals(2,AppNotificationGroups.group(Arrays.asList(entry("chat",1,0,10,false),entry("chat",2,10,20,false))).size());
    }
    @Test public void androidGroupSummaryDoesNotBecomeAnExtraCard() {
        List<List<NotificationEntry>> groups=AppNotificationGroups.group(Arrays.asList(entry("chat",1,0,10,false),entry("chat",2,0,20,true)));
        assertEquals(1,groups.size());assertEquals(1,groups.get(0).size());assertFalse(groups.get(0).get(0).isGroupSummary());
    }
    @Test public void noAlertsMeansNoStack() { assertTrue(AppNotificationGroups.group(Collections.emptyList()).isEmpty()); }
}
