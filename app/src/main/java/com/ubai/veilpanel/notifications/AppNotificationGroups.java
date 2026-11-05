package com.ubai.veilpanel.notifications;
import java.util.*;
public final class AppNotificationGroups {
    public static String key(NotificationEntry entry) { return entry.getUserId()+":"+entry.getPackageName(); }
    public static List<List<NotificationEntry>> group(List<NotificationEntry> entries) {
        List<NotificationEntry> visible = new ArrayList<>(NotificationVisibilityFilter.filterForDisplay(entries));
        visible.sort(Comparator.comparingLong(NotificationEntry::getPostTime).reversed());
        Map<String,List<NotificationEntry>> groups = new LinkedHashMap<>();
        for(NotificationEntry entry:visible) groups.computeIfAbsent(key(entry),ignored->new ArrayList<>()).add(entry);
        return new ArrayList<>(groups.values());
    }
}
