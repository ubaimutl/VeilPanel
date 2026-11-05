package com.ubai.veilpanel.ui;

import android.app.ActivityOptions;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;

import com.ubai.veilpanel.MainActivity;
import com.ubai.veilpanel.R;
import com.ubai.veilpanel.config.PanOSPPreferences;
import com.ubai.veilpanel.notifications.NotificationEntry;
import com.ubai.veilpanel.notifications.NotificationRepository;
import com.ubai.veilpanel.notifications.NotificationVisibilityFilter;
import com.ubai.veilpanel.notifications.SystemFeedbackRepository;
import com.ubai.veilpanel.system.SettingsLauncher;
import com.ubai.veilpanel.system.NotificationActionExecutor;

/** Notification-only content. Native quick settings remain owned by Android. */
public final class ShadeView extends LinearLayout {

    private static final String TAG = "PanOSP.Notification";
    private static final int DISMISS_SWIPE_DP = 72;
    private static final long FEEDBACK_DURATION_MS = 2200L;

    private final Runnable requestClose;
    private final LiquidGlassBackdrop liquidBackdrop;
    private final SettingsLauncher settingsLauncher;
    private final NotificationRepository notificationRepository;
    private final SystemFeedbackRepository feedbackRepository;
    private final int touchSlop;
    private final NotificationActionExecutor actionExecutor;
    private final Set<String> expandedAppGroups = new HashSet<>();
    private final Set<String> expandedNotificationKeys = new HashSet<>();

    private boolean silentExpanded;
    private TextView clockView;
    private TextView dateView;
    private TextView systemFeedbackView;
    private LinearLayout notificationsContainer;
    private ScrollView notificationsScrollView;

    private float emptyAreaDownX;
    private float emptyAreaDownY;
    private boolean emptyAreaCloseCandidate;

    private int appearance(String key){return com.ubai.veilpanel.config.PanelAppearance.get(getContext(),key);}
    private int cardWidth(){return Math.max(1,Math.round((getResources().getDisplayMetrics().widthPixels-dp(2*appearance("side_margin")))*appearance("card_width")/100f));}
    private final Runnable headerTick=new Runnable(){public void run(){refreshHeader();postDelayed(this,60000-System.currentTimeMillis()%60000);}};
    private final Runnable hideFeedbackRunnable =
            this::hideSystemFeedback;


    private final NotificationRepository.Listener notificationListener =
            () -> post(this::renderNotifications);

    private final SystemFeedbackRepository.Listener feedbackListener =
            message -> post(() -> showSystemFeedback(message));

    public ShadeView(Context context, Runnable requestClose) {
        super(context);
        liquidBackdrop=new LiquidGlassBackdrop(this);

        this.requestClose = requestClose;
        settingsLauncher = new SettingsLauncher(context, requestClose);
        actionExecutor = new NotificationActionExecutor(context);
        notificationRepository = NotificationRepository.getInstance();
        feedbackRepository = SystemFeedbackRepository.getInstance();
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        build();
    }

    // =========================================================================
    // SHADE LIFECYCLE
    // =========================================================================


    public void onShown() {
        liquidBackdrop.open();
        com.ubai.veilpanel.notifications.PanOSPNotificationListener.reconnectIfNeeded(getContext());
        notificationRepository.addListener(notificationListener);
        notificationRepository.requestRefresh();
        feedbackRepository.addListener(feedbackListener);

        refreshHeader();removeCallbacks(headerTick);postDelayed(headerTick,60000-System.currentTimeMillis()%60000);
        renderNotifications();
    }

    public void onHidden() {
        liquidBackdrop.close();
        notificationRepository.removeListener(notificationListener);
        feedbackRepository.removeListener(feedbackListener);

        removeCallbacks(headerTick);
        removeCallbacks(hideFeedbackRunnable);
        hideSystemFeedback();

    }

    // =========================================================================
    // UI CONSTRUCTION
    // =========================================================================

    private void build() {
        setOrientation(VERTICAL);
        // Preserve resting margins without cutting moving cards at the padded edge.
        setClipToPadding(false);
        setClipChildren(false);
        setPadding(
                dp(appearance("side_margin")),
                dp(24),
                dp(appearance("side_margin")),
                dp(appearance("side_margin"))
        );
        PanOSPPreferences appearancePreferences =
                new PanOSPPreferences(getContext());

        int panelOpacity =
                appearancePreferences.getPanelOpacityPercent();

        int panelAlpha =
                Math.round(255f * panelOpacity / 100f);

        String panelBackgroundImageUri =
                appearancePreferences.getPanelBackgroundImageUri();

        int panelBackgroundImageOpacity =
                appearancePreferences
                        .getPanelBackgroundImageOpacityPercent();

        int panelCornerRadius =
                dp(appearancePreferences.getPanelCornerRadiusDp());

        // Draw the configured color immediately. The optional image is decoded
        // off the UI thread and applied afterwards to avoid blocking the shade.
        // The system wallpaper is behind the window; the panel surface stays clear.
        if (PanOSPTheme.followsWallpaper(getContext())) setBackgroundColor(Color.TRANSPARENT);
        else setBackground(new PanelBackgroundDrawable(
            appearancePreferences.getThemeBackgroundType(),appearancePreferences.getThemeBackgroundColor(),
            appearancePreferences.getThemeBackgroundColor2(),appearancePreferences.getThemeBackgroundDirection(),
            panelAlpha,null,panelBackgroundImageOpacity,panelCornerRadius,0f,0f,1f,0f,0f,1f));

        addHeader();
        addSystemFeedback();
        addNotifications();
        PanelStyle.apply(this);
        if(dateView!=null){
            int dateWeight=Math.max(100,Math.min(900,getContext().getSharedPreferences("veil_panel",0).getInt("header_date_weight",400)));
            dateView.setTypeface(Typeface.create(PanelStyle.font(getContext()),dateWeight,false));
            dateView.setFontVariationSettings("'wght' "+dateWeight);
        }
        if(clockView!=null)clockView.setFontVariationSettings("'wght' "+Math.max(100,Math.min(900,getContext().getSharedPreferences("veil_panel",0).getInt("header_clock_weight",500))));
    }

    private void loadPanelBackgroundBitmapAsync(
            String uriString,
            int imageOpacityPercent,
            int cornerRadius) {

        if (uriString == null || uriString.trim().isEmpty()) {
            return;
        }

        Thread decoderThread =
                new Thread(
                        () -> {
                            android.graphics.Bitmap bitmap =
                                    loadPanelBackgroundBitmap(uriString);

                            if (bitmap == null) {
                                return;
                            }

                            post(
                                    () -> {
                                    PanOSPPreferences currentPreferences =
                                            new PanOSPPreferences(getContext());

                                    String currentUri =
                                            currentPreferences.getPanelBackgroundImageUri();

                                    if (!uriString.equals(currentUri)) {
                                        return;
                                    }

                                    setBackground(
                                            new PanelBackgroundDrawable(
                                                    currentPreferences.getThemeBackgroundType(),
                                                    currentPreferences.getThemeBackgroundColor(),
                                                    currentPreferences.getThemeBackgroundColor2(),
                                                    currentPreferences.getThemeBackgroundDirection(),
                                                    Math.round(255f * currentPreferences.getPanelOpacityPercent() / 100f),
                                                    bitmap,
                                                    imageOpacityPercent,
                                                    cornerRadius,
                                                    currentPreferences.getPanelBackgroundImageUvTopLeftX(),
                                                    currentPreferences.getPanelBackgroundImageUvTopLeftY(),
                                                    currentPreferences.getPanelBackgroundImageUvTopRightX(),
                                                    currentPreferences.getPanelBackgroundImageUvTopRightY(),
                                                    currentPreferences.getPanelBackgroundImageUvBottomLeftX(),
                                                    currentPreferences.getPanelBackgroundImageUvBottomLeftY()
                                            )
                                    );
                                    }
                            );
                        },
                        "PanOSP-background-decoder"
                );

        decoderThread.start();
    }

    @android.annotation.SuppressLint("WrongThread")
    private android.graphics.Bitmap loadPanelBackgroundBitmap(
            String uriString) {

        if (uriString == null || uriString.trim().isEmpty()) {
            return null;
        }

        try {
            android.net.Uri uri = android.net.Uri.parse(uriString);

            android.graphics.ImageDecoder.Source source =
                    android.graphics.ImageDecoder.createSource(
                            getContext().getContentResolver(),
                            uri
                    );

            final int maxDimension =
                    Math.max(
                            getResources().getDisplayMetrics().widthPixels,
                            getResources().getDisplayMetrics().heightPixels
                    );

            return android.graphics.ImageDecoder.decodeBitmap(
                    source,
                    (decoder, info, sourceInfo) -> {
                        android.util.Size size = info.getSize();
                        int width = size.getWidth();
                        int height = size.getHeight();
                        int largest = Math.max(width, height);

                        if (largest > maxDimension && largest > 0) {
                            float scale =
                                    (float) maxDimension / largest;

                            decoder.setTargetSize(
                                    Math.max(1, Math.round(width * scale)),
                                    Math.max(1, Math.round(height * scale))
                            );
                        }

                        decoder.setAllocator(
                                android.graphics.ImageDecoder
                                        .ALLOCATOR_SOFTWARE
                        );
                    }
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private void addHeader() {
        android.content.SharedPreferences prefs=getContext().getSharedPreferences("veil_panel",0);
        boolean showDate=prefs.getBoolean("header_date_visible",true),showSettings=prefs.getBoolean("header_settings_visible",true),showClose=prefs.getBoolean("header_close_visible",true);
        FrameLayout top=new FrameLayout(getContext());top.setTag("panel_header");
        dateView=new TextView(getContext());dateView.setTag("panel_date");dateView.setTextSize(Math.max(12,Math.min(28,prefs.getInt("header_date_size",17))));dateView.setTextColor(PanOSPTheme.primaryText(getContext()));
        dateView.setGravity(headerGravity("header_date_alignment")|Gravity.CENTER_VERTICAL);dateView.setVisibility(showDate?VISIBLE:GONE);
        dateView.setMaxLines(1);dateView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        int controls=(showSettings?44:0)+(showClose?44:0);
        boolean centred="center".equals(prefs.getString("header_date_alignment","start"));
        dateView.setPadding(centred?dp(controls):0,0,dp(controls),0);
        top.addView(dateView,new FrameLayout.LayoutParams(-1,dp(48)));
        LinearLayout buttons=new LinearLayout(getContext());
        ImageButton settings=iconButton(R.drawable.ic_panel_more,"Panel settings",()->{
            Intent intent=new Intent(getContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);getContext().startActivity(intent);requestClose.run();
        });settings.setVisibility(showSettings?VISIBLE:GONE);buttons.addView(settings,new LinearLayout.LayoutParams(dp(44),dp(44)));
        ImageButton close=iconButton(R.drawable.ic_panel_close,"Close notifications",requestClose);close.setVisibility(showClose?VISIBLE:GONE);buttons.addView(close,new LinearLayout.LayoutParams(dp(44),dp(44)));
        top.addView(buttons,new FrameLayout.LayoutParams(-2,dp(48),Gravity.END|Gravity.CENTER_VERTICAL));
        top.setVisibility(showDate||showSettings||showClose?VISIBLE:GONE);addView(top,new LinearLayout.LayoutParams(-1,-2));
        clockView=new SculptedClockView(getContext(),prefs.getString("header_clock_style","plain"),liquidBackdrop);
        clockView.setTag("panel_clock");clockView.setTextSize(appearance("clock_size"));clockView.setTextColor(PanOSPTheme.primaryText(getContext()));
        clockView.setTypeface(Typeface.create(PanelStyle.font(getContext()),Math.max(100,Math.min(900,prefs.getInt("header_clock_weight",500))),false));
        clockView.setGravity(headerGravity("header_clock_alignment"));clockView.setMaxLines(1);
        clockView.setLetterSpacing((Math.max(0,Math.min(100,prefs.getInt("header_clock_spacing",25)))-50)/1000f);
        clockView.setAutoSizeTextTypeUniformWithConfiguration(12,appearance("clock_size"),1,android.util.TypedValue.COMPLEX_UNIT_SP);
        clockView.setVisibility(prefs.getBoolean("header_clock_visible",true)?VISIBLE:GONE);
        int above=Math.max(-80,Math.min(120,prefs.getInt("header_clock_top_space",4)));
        int below=Math.max(-80,Math.min(120,prefs.getInt("header_clock_bottom_space",appearance("header_gap"))));
        clockView.setPadding(0,dp(Math.max(0,above)),0,dp(Math.max(0,below)));
        LinearLayout.LayoutParams clockPlacement=new LinearLayout.LayoutParams(-1,-2);
        clockPlacement.topMargin=dp(Math.min(0,above));clockPlacement.bottomMargin=dp(Math.min(0,below));
        addView(clockView,clockPlacement);refreshHeader();
    }
    private int headerGravity(String key){
        String value=getContext().getSharedPreferences("veil_panel",0).getString(key,"start");
        return "center".equals(value)?Gravity.CENTER_HORIZONTAL:"end".equals(value)?Gravity.END:Gravity.START;
    }






    /**
     * Short-lived PanOSP-owned feedback. It is not a notification and it is
     * visible only while the shade is already open.
     */
    private void addSystemFeedback() {
        systemFeedbackView = new TextView(getContext());
        systemFeedbackView.setTextColor(Color.WHITE);
        systemFeedbackView.setTextSize(14);
        systemFeedbackView.setTypeface(null, Typeface.BOLD);
        systemFeedbackView.setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(10)
        );
        systemFeedbackView.setBackgroundColor(
                Color.rgb(45, 45, 45)
        );
        systemFeedbackView.setVisibility(GONE);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        params.bottomMargin = dp(6);

        addView(systemFeedbackView, params);
    }

    private void addNotifications() {
        notificationsContainer=new LinearLayout(getContext());notificationsContainer.setOrientation(VERTICAL);
        notificationsContainer.setClipChildren(false);notificationsContainer.setClipToPadding(false);
        addView(notificationsContainer,new LinearLayout.LayoutParams(-1,-2));
    }

    // =========================================================================
    // HEADER
    // =========================================================================

    private void refreshHeader() {
        if (dateView == null) {
            return;
        }

        Locale locale = Locale.getDefault();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "EEE d MMM",
                        locale
                );

        if (clockView != null) clockView.setText(new java.text.SimpleDateFormat(getContext().getSharedPreferences("veil_panel",0).getBoolean("header_clock_leading_zero",false)?"HH:mm":"H:mm",Locale.getDefault()).format(new java.util.Date()));
        String text = LocalDate.now().format(formatter);

        if (!text.isEmpty()) {
            text =
                    text.substring(0, 1).toUpperCase(locale)
                            + text.substring(1);
        }

        android.content.Intent battery=getContext().registerReceiver(null,new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
        int level=battery!=null ? battery.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL,-1) : -1;
        int scale=battery!=null ? battery.getIntExtra(android.os.BatteryManager.EXTRA_SCALE,100) : 100;
        dateView.setText(text+(getContext().getSharedPreferences("veil_panel",0).getBoolean("header_battery_visible",true) && level>=0 && scale>0 ? "  "+Math.round(level*100f/scale)+"%" : ""));
    }



    private void openAndroidSettings() {
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            getContext().startActivity(intent);
            postDelayed(requestClose, 80L);

        } catch (RuntimeException exception) {
            Log.e(
                    TAG,
                    "failed to open Android settings",
                    exception
            );
        }
    }

    // =========================================================================
    // TRANSIENT SYSTEM FEEDBACK
    // =========================================================================

    private void showSystemFeedback(String message) {
        if (systemFeedbackView == null
                || message == null
                || message.isEmpty()) {
            return;
        }

        removeCallbacks(headerTick);
        removeCallbacks(hideFeedbackRunnable);

        systemFeedbackView.setText(message);
        systemFeedbackView.setAlpha(1f);
        systemFeedbackView.setVisibility(VISIBLE);

        postDelayed(
                hideFeedbackRunnable,
                FEEDBACK_DURATION_MS
        );
    }

    private void hideSystemFeedback() {
        if (systemFeedbackView == null) {
            return;
        }

        systemFeedbackView.setVisibility(GONE);
        systemFeedbackView.setText(null);
    }

    // =========================================================================
    // EMPTY NOTIFICATION AREA -> CLOSE
    // =========================================================================

    private boolean handleNotificationAreaTouch(
            View view,
            MotionEvent event) {

        float contentY =
                event.getY()
                        + notificationsScrollView.getScrollY();

        boolean belowContent =
                notificationsContainer != null
                        && contentY
                        > notificationsContainer.getBottom();

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                emptyAreaDownX = event.getRawX();
                emptyAreaDownY = event.getRawY();
                emptyAreaCloseCandidate = belowContent;
                return emptyAreaCloseCandidate;

            case MotionEvent.ACTION_MOVE:
                if (!emptyAreaCloseCandidate) {
                    return false;
                }

                float dx = event.getRawX() - emptyAreaDownX;
                float dy = event.getRawY() - emptyAreaDownY;

                if (Math.abs(dx) > touchSlop
                        || Math.abs(dy) > touchSlop) {
                    emptyAreaCloseCandidate = false;
                }

                return true;

            case MotionEvent.ACTION_UP:
                if (!emptyAreaCloseCandidate) {
                    return false;
                }

                emptyAreaCloseCandidate = false;
                requestClose.run();
                return true;

            case MotionEvent.ACTION_CANCEL:
                emptyAreaCloseCandidate = false;
                return false;

            default:
                return emptyAreaCloseCandidate;
        }
    }

    // =========================================================================
    // NOTIFICATIONS
    // =========================================================================

    private void renderNotifications() {
        if (notificationsContainer == null) {
            return;
        }

        notificationsContainer.removeAllViews();
        liquidBackdrop.resetCards();

        List<NotificationEntry> snapshot = notificationRepository.snapshot();
        Set<String> activeKeys = new HashSet<>();
        for (NotificationEntry entry : snapshot) {
            activeKeys.add(entry.getKey());
            if (!hasExpandedContent(entry)) expandedNotificationKeys.remove(entry.getKey());
        }
        expandedNotificationKeys.retainAll(activeKeys);
        List<NotificationEntry> notifications = NotificationVisibilityFilter.filterForDisplay(snapshot);

        if (notifications.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText(R.string.notifications_empty);
            empty.setTextColor(PanOSPTheme.secondaryText(getContext()));
            empty.setTextSize(15);
            empty.setPadding(
                    dp(8),
                    dp(18),
                    dp(8),
                    dp(12)
            );

            notificationsContainer.addView(empty);
            return;
        }

        List<NotificationEntry> alerting=new java.util.ArrayList<>(), silent=new java.util.ArrayList<>();
        for(NotificationEntry entry:notifications) (entry.isSilent()?silent:alerting).add(entry);
        Set<String> activeGroups=new HashSet<>();
        renderSection(alerting,"",activeGroups);
        if(!silent.isEmpty()) {
            LinearLayout toggle=new LinearLayout(getContext());toggle.setTransitionName("silent-toggle");toggle.setGravity(Gravity.CENTER_VERTICAL);toggle.setMinimumHeight(dp(48));
            TextView quietLabel=new TextView(getContext());quietLabel.setText("Silent · "+silent.size());quietLabel.setTextSize(appearance("body_size"));quietLabel.setTextColor(PanOSPTheme.secondaryText(getContext()));toggle.addView(quietLabel,new LinearLayout.LayoutParams(0,-2,1));
            ImageButton quietIcon=iconButton(silentExpanded?R.drawable.ic_panel_up:R.drawable.ic_panel_down,"Toggle silent notifications",()->{animateNotificationChange();silentExpanded=!silentExpanded;renderNotifications();});toggle.addView(quietIcon,new LinearLayout.LayoutParams(dp(44),dp(44)));
            toggle.setContentDescription("Silent notifications, "+silent.size()+", "+(silentExpanded?"expanded":"collapsed"));toggle.setOnClickListener(v->{animateNotificationChange();silentExpanded=!silentExpanded;renderNotifications();});
            LinearLayout.LayoutParams quietParams=new LinearLayout.LayoutParams(cardWidth(),-2);quietParams.gravity=Gravity.CENTER_HORIZONTAL;notificationsContainer.addView(toggle,quietParams);
            if(silentExpanded)renderSection(silent,"silent:",activeGroups);
        }
        expandedAppGroups.retainAll(activeGroups);
        Button clear=new Button(getContext());clear.setText(R.string.veil_clear_all);clear.setTextColor(PanOSPTheme.secondaryText(getContext()));clear.setBackgroundColor(Color.TRANSPARENT);clear.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        clear.setOnClickListener(v->{for(NotificationEntry entry:notificationRepository.snapshot()) if(entry.isClearable())notificationRepository.requestDismiss(entry.getKey());});
        LinearLayout.LayoutParams clearPlacement=new LinearLayout.LayoutParams(-2,dp(48));clearPlacement.gravity=Gravity.END;
        clear.setMinWidth(0);clear.setMinimumWidth(0);clear.setPadding(dp(8),0,dp(8),0);clear.setContentDescription("Clear all dismissible notifications");
        notificationsContainer.addView(clear,clearPlacement);PanelStyle.apply(notificationsContainer);
    }
    private void renderSection(List<NotificationEntry> entries,String prefix,Set<String> activeGroups) {
        for(List<NotificationEntry> group:com.ubai.veilpanel.notifications.AppNotificationGroups.group(entries)) {
            String key=prefix+com.ubai.veilpanel.notifications.AppNotificationGroups.key(group.get(0));activeGroups.add(key);
            if(group.size()==1)notificationsContainer.addView(buildNotificationView(group.get(0)));
            else if(expandedAppGroups.contains(key))addExpandedStack(key,group);
            else addCollapsedStack(key,group);
        }
    }

    private void animateNotificationChange() {
        if(notificationsContainer!=null && notificationsContainer.isLaidOut() && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            android.transition.TransitionManager.endTransitions(notificationsContainer);
            android.transition.TransitionSet transition=new android.transition.TransitionSet();
            transition.setOrdering(android.transition.TransitionSet.ORDERING_TOGETHER);
            transition.addTransition(new android.transition.ChangeBounds());
            boolean motion=getContext().getSharedPreferences("veil_panel",0).getBoolean("experimental_motion",false);transition.setDuration(motion?260:160);transition.setInterpolator(motion?new android.view.animation.PathInterpolator(.16f,1f,.3f,1f):new android.view.animation.DecelerateInterpolator());
            android.transition.TransitionManager.beginDelayedTransition(notificationsContainer,transition);
        }
    }
    private void toggleGroup(String key) {
        animateNotificationChange();
        if (!expandedAppGroups.remove(key)) expandedAppGroups.add(key);
        renderNotifications();
    }

    private void addCollapsedStack(String key, List<NotificationEntry> group) {
        FrameLayout stack = new FrameLayout(getContext());stack.setTransitionName("stack:"+key);
        stack.setContentDescription(group.get(0).getApplicationLabel()+", "+group.size()+" notifications. Expand group");
        for (int layer=2;layer>=1;layer--) {
            View back = new View(getContext());
            android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
            bg.setColor(layer==2 ? Color.rgb(42,42,42) : Color.rgb(49,49,49)); bg.setCornerRadius(dp(appearance("card_radius"))); back.setBackground(bg);
            if(getContext().getSharedPreferences("veil_panel",0).getBoolean("reference_glass",false))back.setBackground(new ReferenceGlassDrawable(getContext(),dp(appearance("card_radius")),getContext().getSharedPreferences("veil_panel",0).getInt("glass_opacity",66)));
            if(LiquidGlassOptions.enabled(getContext())){
                back.setBackground(new LiquidGlassDrawable(getContext(),back,liquidBackdrop,dp(appearance("card_radius"))));liquidBackdrop.watch(back);
            }
            FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,dp(appearance("card_height")));
            bp.setMargins(dp(layer*8),dp(layer*6),dp(layer*8),0); stack.addView(back,bp);
        }
        LinearLayout card=buildNotificationView(group.get(0),group.size(),()->toggleGroup(key));
        // Swipe dismissal belongs to individual notifications after expansion.
        // The collapsed stack opens as one tappable surface and cannot hide other alerts by accident.
        card.setOnTouchListener(null);
        ((NotificationCard)card).interceptChildSwipes=false;
        card.setOnClickListener(v->toggleGroup(key));
        redirectExpandButtons(card,()->toggleGroup(key));
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(12);stack.addView(card,cp);
        card.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            int height=b-t;
            for(int i=0;i<2;i++){View layerView=stack.getChildAt(i);android.view.ViewGroup.LayoutParams size=layerView.getLayoutParams();if(size.height!=height){size.height=height;layerView.setLayoutParams(size);}}
        });
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(cardWidth(),-2);sp.gravity=Gravity.CENTER_HORIZONTAL;sp.bottomMargin=dp(appearance("card_gap"));
        notificationsContainer.addView(stack,sp);
    }

    private void redirectExpandButtons(View v, Runnable expand) {
        if(v instanceof ImageButton) v.setOnClickListener(ignored->expand.run());
        if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) redirectExpandButtons(((ViewGroup)v).getChildAt(i),expand);
    }

    private void addExpandedStack(String key, List<NotificationEntry> group) {
        LinearLayout row=new LinearLayout(getContext()); row.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=new TextView(getContext());
        title.setText(group.get(0).getApplicationLabel()+" · "+group.size());title.setTextSize(15);title.setTextColor(PanOSPTheme.primaryText(getContext()));
        row.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));title.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(iconButton(R.drawable.ic_panel_up,"Collapse group",()->toggleGroup(key)),new LinearLayout.LayoutParams(dp(44),dp(44)));
        row.addView(iconButton(R.drawable.ic_panel_trash,"Clear group",()->{for(NotificationEntry e:group)if(e.isClearable())notificationRepository.requestDismiss(e.getKey());}),new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout.LayoutParams groupParams=new LinearLayout.LayoutParams(cardWidth(),-2);groupParams.gravity=Gravity.CENTER_HORIZONTAL;notificationsContainer.addView(row,groupParams);
        for(NotificationEntry e:group) notificationsContainer.addView(buildNotificationView(e));
    }

    private LinearLayout buildNotificationView(NotificationEntry entry) { return buildNotificationView(entry,1,null); }

    private ImageButton iconButton(int resource,String description,Runnable action) {
        ImageButton button=new ImageButton(getContext());button.setImageResource(resource);button.setColorFilter(PanOSPTheme.notificationSecondaryText(getContext()));button.setBackgroundColor(Color.TRANSPARENT);int inset=dp((44-ExperimentalGlass.iconSize(getContext()))/2);button.setPadding(inset,inset,inset,inset);button.setContentDescription(description);ExperimentalGlass.styleControl(button);button.setOnClickListener(v->action.run());return button;
    }
    private LinearLayout buildNotificationView(NotificationEntry entry,int count,Runnable openGroup) {
        NotificationCard card=new NotificationCard(getContext());card.setTransitionName("notification:"+entry.getKey());card.setOrientation(HORIZONTAL);card.setGravity(Gravity.TOP);
        int pad=dp(appearance("card_padding"));card.setPadding(pad,pad,pad,pad);card.setMinimumHeight(dp(appearance("card_height")));
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();int colour=PanOSPTheme.notificationBackground(getContext());
        bg.setColor((colour&0xffffff)|(Math.round(getContext().getSharedPreferences("veil_panel",0).getInt("glass_opacity",66)*2.55f)<<24));bg.setCornerRadius(dp(appearance("card_radius")));bg.setStroke(dp(1),0x20FFFFFF);card.setBackground(ExperimentalGlass.enabled(getContext())?ExperimentalGlass.surface(getContext(),dp(appearance("card_radius")),getContext().getSharedPreferences("veil_panel",0).getInt("glass_opacity",66),false):bg);
        if(getContext().getSharedPreferences("veil_panel",0).getBoolean("reference_glass",false))card.setBackground(new ReferenceGlassDrawable(getContext(),dp(appearance("card_radius")),getContext().getSharedPreferences("veil_panel",0).getInt("glass_opacity",66)));
        if(LiquidGlassOptions.enabled(getContext())){card.setBackground(new LiquidGlassDrawable(getContext(),card,liquidBackdrop,dp(appearance("card_radius"))));liquidBackdrop.watch(card);}
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(cardWidth(),-2);cp.gravity=Gravity.CENTER_HORIZONTAL;cp.bottomMargin=dp(appearance("card_gap"));card.setLayoutParams(cp);
        android.widget.ImageView icon=new android.widget.ImageView(getContext());icon.setImageDrawable(entry.icon(getContext()));icon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        icon.setClipToOutline(true);icon.setOutlineProvider(new ViewOutlineProvider(){public void getOutline(View v,android.graphics.Outline outline){outline.setRoundRect(0,0,v.getWidth(),v.getHeight(),dp(appearance("icon_corner")));}});
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(appearance("icon_size")),dp(appearance("icon_size")));ip.setMarginEnd(dp(appearance("icon_gap")));ip.topMargin=dp(2);card.addView(icon,ip);
        LinearLayout content=new LinearLayout(getContext());content.setOrientation(VERTICAL);card.addView(content,new LinearLayout.LayoutParams(0,-2,1));
        boolean expanded=expandedNotificationKeys.contains(entry.getKey());
        CharSequence appName=firstNonBlank(entry.getApplicationLabel(),entry.getPackageName());
        CharSequence title=expanded?firstNonBlank(entry.getTitleBig(),entry.getTitle()):firstNonBlank(entry.getTitle(),entry.getTitleBig());
        CharSequence headline=openGroup!=null?appName:firstNonBlank(title,appName);
        LinearLayout identity=new LinearLayout(getContext());identity.setGravity(Gravity.TOP);identity.setBaselineAligned(true);content.addView(identity);
        TextView main=new TextView(getContext());main.setText(headline);main.setIncludeFontPadding(false);main.setTextSize(appearance("headline_size"));main.setTypeface(Typeface.create(PanelStyle.font(getContext()),appearance("font_weight"),false));main.setTextColor(PanOSPTheme.notificationPrimaryText(getContext()));main.setMaxLines(1);main.setEllipsize(android.text.TextUtils.TruncateAt.END);identity.addView(main,new LinearLayout.LayoutParams(0,-2,1));
        TextView time=new TextView(getContext());time.setIncludeFontPadding(false);time.setTextSize(appearance("time_size"));time.setTextColor(PanOSPTheme.notificationSecondaryText(getContext()));time.setMaxLines(1);time.setMaxWidth(dp(100));time.setEllipsize(android.text.TextUtils.TruncateAt.END);time.setPadding(dp(8),0,0,0);time.setText(android.text.format.DateUtils.getRelativeTimeSpanString(entry.getPostTime(),System.currentTimeMillis(),60000,android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE));identity.addView(time);
        LinearLayout messageRow=new LinearLayout(getContext());messageRow.setOrientation(HORIZONTAL);content.addView(messageRow);
        LinearLayout preview=new LinearLayout(getContext());preview.setOrientation(VERTICAL);messageRow.addView(preview,new LinearLayout.LayoutParams(0,-2,1));
        if(openGroup!=null && title!=null && !sameText(title,headline))preview.addView(notificationText(title,appearance("body_size"),1));
        CharSequence body=expanded?firstNonBlank(entry.getBigText(),entry.getText(),entry.getTickerText()):firstNonBlank(entry.getText(),entry.getBigText(),entry.getTickerText());
        if(body!=null&&!sameText(body,headline))preview.addView(notificationText(body,appearance("body_size"),expanded?appearance("expanded_preview_lines"):appearance("preview_lines")));
        card.setContentDescription(appName+", "+headline);
        if(openGroup!=null||hasExpandedContent(entry)) {
            LinearLayout controls=new LinearLayout(getContext());controls.setGravity(Gravity.CENTER_VERTICAL);controls.setPadding(dp(4),0,0,0);
            if(openGroup!=null){TextView countView=new TextView(getContext());countView.setText(String.valueOf(count));countView.setTextSize(appearance("time_size"));countView.setTextColor(PanOSPTheme.notificationSecondaryText(getContext()));controls.addView(countView);}
            Runnable expand=()->{if(openGroup!=null)openGroup.run();else{if(!expandedNotificationKeys.remove(entry.getKey()))expandedNotificationKeys.add(entry.getKey());replaceNotificationCard(card,entry.getKey());}};
            controls.addView(iconButton(expanded?R.drawable.ic_panel_up:R.drawable.ic_panel_down,openGroup!=null?"Expand "+count+" notifications":expanded?"Collapse notification":"Expand notification",expand),new LinearLayout.LayoutParams(dp(44),dp(44)));
            LinearLayout.LayoutParams tools=new LinearLayout.LayoutParams(-2,-2);tools.gravity=Gravity.TOP;messageRow.addView(controls,tools);
        }
        if(expanded && entry.getCustomContent()!=null && entry.getCustomContent().hasBigContentView()) {
            FrameLayout remote=new CappedRemotePreview(getContext());try{remote.addView(entry.getCustomContent().copyBigContentView().apply(getContext(),remote));content.addView(remote);}catch(RuntimeException unavailable){remote.removeAllViews();}
        }
        if(expanded&&openGroup==null){
            NotificationActionRow actions=new NotificationActionRow(getContext());
            LinearLayout.LayoutParams actionParams=new LinearLayout.LayoutParams(-1,-2);actionParams.topMargin=dp(8);content.addView(actions,actionParams);
            for(Notification.Action action:entry.getActions()){
                if(!NotificationActionExecutor.isSupported(action)&&!NotificationActionExecutor.supportsReply(action))continue;
                actions.addAction(action.title,action.title.toString(),false,()->executeNotificationAction(entry,action));
            }
            if(entry.getContentIntent()!=null && body!=null && (body.length()>300 || body.toString().split("\n",-1).length>appearance("expanded_preview_lines")))actions.addAction("Open in app","Read full notification in app",false,()->openNotification(entry));
            actions.addAction("Settings","Notification settings",true,()->settingsLauncher.openNotificationSettings(entry.getPackageName(),entry.getChannelId(),entry.getUserId(),entry.getUser()));
        }
        card.setOnClickListener(v->{if(openGroup!=null)openGroup.run();else openNotification(entry);});card.setOnLongClickListener(v->{
            card.longPressConsumed=true;card.cancelLongPress();card.setPressed(false);
            card.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            if(openGroup!=null)openGroup.run();
            else {expandedNotificationKeys.add(entry.getKey());expandCardInPlace(card,entry);}
            return true;
        });
        if(openGroup==null){attachNotificationGestures(card,entry);card.interceptChildSwipes=true;}
        PanelStyle.apply(card);return card;
    }
    private void expandCardInPlace(NotificationCard card,NotificationEntry entry){
        // Keep the original touch target alive through UP; rebuilding the whole list
        // during a held gesture can let the release activate another card.
        animateNotificationChange();LinearLayout expanded=buildNotificationView(entry);
        card.removeAllViews();
        while(expanded.getChildCount()>0){View child=expanded.getChildAt(0);android.view.ViewGroup.LayoutParams params=child.getLayoutParams();expanded.removeViewAt(0);card.addView(child,params);}
        card.requestLayout();
    }
    private TextView notificationText(CharSequence value,int size,int maxLines){TextView text=new TextView(getContext());text.setText(value);text.setIncludeFontPadding(false);text.setTextSize(size);text.setTextColor(PanOSPTheme.notificationDescriptionText(getContext()));text.setMaxLines(maxLines);text.setEllipsize(android.text.TextUtils.TruncateAt.END);text.setPadding(0,dp(5),0,0);return text;}
    private void executeNotificationAction(NotificationEntry entry,Notification.Action action) {
        if(NotificationActionExecutor.supportsReply(action)) {
            Intent reply=new Intent(getContext(),com.ubai.veilpanel.ReplyActivity.class).putExtra("action",action).putExtra("app",entry.getApplicationLabel()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try{getContext().startActivity(reply);requestClose.run();}catch(RuntimeException unavailable){Toast.makeText(getContext(),R.string.veil_reply_failed,Toast.LENGTH_SHORT).show();}return;
        }
        NotificationActionExecutor.Result result=actionExecutor.execute(action);
        if(result==NotificationActionExecutor.Result.SENT_ACTIVITY)requestClose.run();
        else if(result!=NotificationActionExecutor.Result.SENT)Toast.makeText(getContext(),R.string.veil_reply_failed,Toast.LENGTH_SHORT).show();
    }

    // =========================================================================
    // NOTIFICATION INTERACTIONS
    // =========================================================================

    private void replaceNotificationCard(View oldCard,String key) {
        android.view.ViewParent parent=getParent();while(parent!=null && !(parent instanceof ScrollView))parent=parent.getParent();
        final ScrollView outer=parent instanceof ScrollView?(ScrollView)parent:null;
        int y=outer!=null?outer.getScrollY():0;animateNotificationChange();renderNotifications();if(outer!=null)outer.post(()->outer.scrollTo(0,y));
    }

    private boolean hasExpandedContent(NotificationEntry entry) {
        if (entry.getCustomContent() != null && entry.getCustomContent().hasBigContentView()) return true;
        CharSequence compactTitle = firstNonBlank(entry.getTitle(), entry.getTitleBig(), entry.getApplicationLabel(), entry.getPackageName());
        CharSequence compactBody = firstNonBlank(entry.getText(), entry.getBigText(), entry.getTickerText());
        if (firstNonBlank(entry.getTitleBig()) != null && !sameText(entry.getTitleBig(), compactTitle)) return true;
        CharSequence expandedTitle = firstNonBlank(entry.getTitleBig(), entry.getTitle(), entry.getApplicationLabel(), entry.getPackageName());
        if (firstNonBlank(entry.getBigText()) != null && !sameText(entry.getBigText(), compactBody)
                && !sameText(entry.getBigText(), expandedTitle)) return true;
        for (Notification.Action action : entry.getActions()) {
            if (NotificationActionExecutor.isSupported(action) || NotificationActionExecutor.supportsReply(action)) return true;
        }
        return false;
    }

    private static CharSequence firstNonBlank(CharSequence... values) {
        for (CharSequence value : values) {
            if (value == null) continue;
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (!Character.isWhitespace(c) && !Character.isSpaceChar(c)) return value;
            }
        }
        return null;
    }

    private static boolean sameText(CharSequence a, CharSequence b) {
        return a != null && b != null && a.toString().strip().equals(b.toString().strip());
    }

    private final class CappedRemotePreview extends FrameLayout {
        CappedRemotePreview(Context context){super(context);setClipChildren(true);}
        @Override protected void onMeasure(int width,int height){super.onMeasure(width,View.MeasureSpec.makeMeasureSpec(dp(240),View.MeasureSpec.AT_MOST));}
    }
    private final class NotificationCard extends LinearLayout {
        private boolean interceptChildSwipes;
        private boolean longPressConsumed;
        @Override public boolean dispatchTouchEvent(MotionEvent event){
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN)longPressConsumed=false;
            if(longPressConsumed&&event.getActionMasked()==MotionEvent.ACTION_UP){
                MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();return true;
            }
            return super.dispatchTouchEvent(event);
        }
        @Override public boolean performClick(){if(longPressConsumed)return false;return super.performClick();}
        private float startX;
        private float startY;

        NotificationCard(Context context) { super(context); }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            if (!interceptChildSwipes) return super.onInterceptTouchEvent(event);
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                startX = event.getRawX();
                startY = event.getRawY();
                // Also initialise the existing swipe handler when a child receives DOWN.
                if (swipeTouchListener != null) swipeTouchListener.onTouch(this, event);
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float dx = event.getRawX() - startX;
                float dy = event.getRawY() - startY;
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) return true;
            }
            return super.onInterceptTouchEvent(event);
        }

        private OnTouchListener swipeTouchListener;

        // Delegates to View's listener/click handling; no accessibility click is replaced here.
        @android.annotation.SuppressLint("ClickableViewAccessibility")
        @Override
        public void setOnTouchListener(OnTouchListener listener) {
            swipeTouchListener = listener;
            super.setOnTouchListener(listener);
        }
    }

    private void openNotification(NotificationEntry entry) {
        PendingIntent pendingIntent = entry.getContentIntent();

        if (pendingIntent == null) {
            Log.i(
                    TAG,
                    "open ignored, no contentIntent"
            );
            return;
        }

        Log.i(
                TAG,
                "open notification"
        );

        try {
            ActivityOptions options =
                    ActivityOptions.makeBasic();

            if (android.os.Build.VERSION.SDK_INT >= 36) {
                configurePendingIntentLaunchApi36(options);
            } else if (android.os.Build.VERSION.SDK_INT >= 34) {
                configurePendingIntentLaunchApi34(options);
            }
pendingIntent.send(
                    getContext(),
                    0,
                    null,
                    null,
                    null,
                    null,
                    options.toBundle()
            );

            Log.i(
                    TAG,
                    "contentIntent sent"
            );

            postDelayed(requestClose, 100L);

            if (entry.isAutoCancel()) {
                notificationRepository.requestDismiss(
                        entry.getKey()
                );
            }

        } catch (PendingIntent.CanceledException exception) {
            Log.w(
                    TAG,
                    "contentIntent canceled"
            );

        } catch (RuntimeException exception) {
            Log.e(
                    TAG,
                    "contentIntent send failed"
            );
        }
    }

    private void attachNotificationGestures(
            View card,
            NotificationEntry entry) {

        final float[] downX = new float[1];
        final float[] downY = new float[1];
        final boolean[] horizontalSwipe = new boolean[1];

        card.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:
                    view.animate().cancel();
                    view.setTranslationX(0f);
                    view.setAlpha(1f);

                    downX[0] = event.getRawX();
                    downY[0] = event.getRawY();
                    horizontalSwipe[0] = false;
                    return false;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - downX[0];
                    float dy = event.getRawY() - downY[0];

                    if (!horizontalSwipe[0]
                            && Math.abs(dx) > touchSlop
                            && Math.abs(dx) > Math.abs(dy)) {

                        horizontalSwipe[0] = true;
                        view.cancelLongPress();
                        view.setPressed(false);

                        if (view.getParent() != null) {
                            view.getParent()
                                    .requestDisallowInterceptTouchEvent(
                                            true
                                    );
                        }
                    }

                    if (!horizontalSwipe[0]) {
                        return false;
                    }

                    float translation =
                            entry.isClearable()
                                    ? dx
                                    : dx * 0.20f;

                    view.setTranslationX(translation);

                    float width =
                            Math.max(1f, view.getWidth());

                    float fade =
                            Math.min(
                                    0.65f,
                                    Math.abs(translation) / width
                            );

                    view.setAlpha(
                            Math.max(
                                    0.35f,
                                    1f - fade
                            )
                    );

                    return true;

                case MotionEvent.ACTION_UP:
                    if (!horizontalSwipe[0]) {
                        return false;
                    }

                    if (view.getParent() != null) {
                        view.getParent()
                                .requestDisallowInterceptTouchEvent(
                                        false
                                );
                    }

                    float totalDx =
                            event.getRawX() - downX[0];

                    float threshold =
                            Math.max(
                                    dp(DISMISS_SWIPE_DP),
                                    view.getWidth() * 0.25f
                            );

                    if (entry.isClearable()
                            && Math.abs(totalDx) >= threshold) {

                        Log.i(
                                TAG,
                                "swipe dismiss"
                        );

                        boolean requested =
                                notificationRepository.requestDismiss(
                                        entry.getKey()
                                );

                        if (!requested) {
                            resetNotificationCard(view);
                        }

                    } else {
                        if (!entry.isClearable()) {
                            Log.i(
                                    TAG,
                                    "swipe blocked, not clearable"
                            );
                        }

                        resetNotificationCard(view);
                    }

                    horizontalSwipe[0] = false;
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    if (view.getParent() != null) {
                        view.getParent()
                                .requestDisallowInterceptTouchEvent(
                                        false
                                );
                    }

                    horizontalSwipe[0] = false;
                    resetNotificationCard(view);
                    return false;

                default:
                    return false;
            }
        });
    }

    @android.annotation.SuppressLint("UseRequiresApi")
    @android.annotation.TargetApi(34)
    private static void configurePendingIntentLaunchApi34(
            ActivityOptions options) {
        options.setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        );
    }

    @android.annotation.SuppressLint("UseRequiresApi")
    @android.annotation.TargetApi(36)
    private static void configurePendingIntentLaunchApi36(
            ActivityOptions options) {
        options.setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
        );
    }



    private void resetNotificationCard(View view) {
        view.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(120L)
                .start();
    }

    /**
     * ScrollView that measures to its real content height when notifications
     * are few, while still respecting the maximum height offered by the shade
     * window. The platform ScrollView can otherwise greedily consume the
     * available height in this overlay hierarchy.
     *
     * This mirrors the classic AOSP idea: measure the content/page first, then
     * clamp only when the available display height is actually exceeded.
     */
    private static final class ContentHeightScrollView extends ScrollView {

        ContentHeightScrollView(Context context) {
            super(context);
            setFillViewport(false);
        }

        @Override
        protected void onMeasure(
                int widthMeasureSpec,
                int heightMeasureSpec) {

            if (getChildCount() == 0) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }

            int heightMode = MeasureSpec.getMode(heightMeasureSpec);

            if (heightMode == MeasureSpec.UNSPECIFIED) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }

            View child = getChildAt(0);
            ViewGroup.LayoutParams childParams = child.getLayoutParams();

            int childWidthSpec =
                    ViewGroup.getChildMeasureSpec(
                            widthMeasureSpec,
                            getPaddingLeft() + getPaddingRight(),
                            childParams.width
                    );

            child.measure(
                    childWidthSpec,
                    MeasureSpec.makeMeasureSpec(
                            0,
                            MeasureSpec.UNSPECIFIED
                    )
            );

            int desiredHeight =
                    child.getMeasuredHeight()
                            + getPaddingTop()
                            + getPaddingBottom();

            int offeredHeight =
                    MeasureSpec.getSize(heightMeasureSpec);

            int measuredHeight =
                    Math.min(desiredHeight, offeredHeight);

            super.onMeasure(
                    widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(
                            measuredHeight,
                            MeasureSpec.EXACTLY
                    )
            );
        }
    }

    private int dp(int value) {
        return Math.round(
                value * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
