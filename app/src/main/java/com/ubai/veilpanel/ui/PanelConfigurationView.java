package com.ubai.veilpanel.ui;

import fr.gibapps.ui.navigation.SectionPager;

import fr.gibapps.colorpicker.SvColorPicker;
import fr.gibapps.ui.background.BackgroundConfig;
import fr.gibapps.ui.background.BackgroundSelector;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.ubai.veilpanel.MainActivity;
import com.ubai.veilpanel.R;
import com.ubai.veilpanel.config.PanOSPPreferences;

/**
 * Launcher-side editor for PanOSP's quick-settings layout.
 *
 * The editor intentionally keeps the main screen simple: columns, visual
 * appearance and the ordered list of active tiles. The number of rows and
 * pages is derived automatically by TileGridView, like classic AOSP/custom
 * ROM Quick Settings.
 *
 * Available tiles live in a compact picker grid instead of an ever-growing
 * vertical list. This structure remains usable when PanOSP gains many more
 * tile backends later.
 */
public final class PanelConfigurationView extends LinearLayout {

    private static final int PICKER_COLUMNS = 4;

    // GibApps visual language for configuration menus.
    private static final int GIB_TEXT_PRIMARY = Color.rgb(255, 248, 240);
    private static final int GIB_TEXT_SECONDARY = Color.rgb(200, 207, 216);


    private final PanOSPPreferences preferences;

    private final java.util.Map<
            String,
            com.ubai.veilpanel.tiles.TileStatus
            > tilePreviewSnapshot;

    private LinearLayout selectedTilesContainer;

    private SeekBar columnsSeek;
    private SeekBar rowsSeek;
    private SeekBar sizeSeek;
    private SeekBar radiusSeek;
    private SeekBar opacitySeek;
    private SeekBar panelCornerRadiusSeek;
    private SeekBar notificationTextSeek;
    private SeekBar notificationBackgroundOpacitySeek;
    private SeekBar backgroundImageOpacitySeek;

    private boolean refreshing;

    // Temporary parent used while building a visual card. It changes only
    // presentation; controls keep the same listeners and preferences.
    private LinearLayout activeCard;

    // Four-page configuration pager.
    private SectionPager pager;
    private final List<TextView> pageTabs = new ArrayList<>();
    private final List<View> pageIndicators = new ArrayList<>();
    private int currentPage;
    private LinearLayout activePage;
    private LinearLayout headerTabs;


    // Lightweight previews for the two visual configuration pages.
    private PanelPreviewView dispositionPreview;
    private PanelPreviewView themePreview;
    private BackgroundSelector backgroundSelector;
    private View accentColorSwatch;

    public PanelConfigurationView(Context context) {
        this(
                context,
                java.util.Collections.emptyMap()
        );
    }

    public PanelConfigurationView(
            Context context,
            java.util.Map<
                    String,
                    com.ubai.veilpanel.tiles.TileStatus
                    > tilePreviewSnapshot) {

        super(context);

        preferences = new PanOSPPreferences(context);

        this.tilePreviewSnapshot =
                tilePreviewSnapshot == null
                        ? java.util.Collections.emptyMap()
                        : new java.util.HashMap<>(
                                tilePreviewSnapshot
                        );

        setOrientation(VERTICAL);
        build();
        refreshFromPreferences();
    }

    public void refreshFromPreferences() {
        refreshing = true;

        if (columnsSeek != null) {
            columnsSeek.setProgress(
                    preferences.getTileColumns()
            );
        }

        if (rowsSeek != null) {
            rowsSeek.setProgress(
                    preferences.getTileRows()
            );
        }

        if (sizeSeek != null) {
            sizeSeek.setProgress(
                    preferences.getTileSizeDp()
            );
        }

        if (radiusSeek != null) {
            int maxRadius = preferences.getTileSizeDp() / 2;

            radiusSeek.setMax(maxRadius);
            radiusSeek.setProgress(
                    Math.min(
                            preferences.getTileCornerRadiusDp(),
                            maxRadius
                    )
            );
        }

        if (opacitySeek != null) {
            opacitySeek.setProgress(
                    preferences.getPanelOpacityPercent()
            );
        }

        if (panelCornerRadiusSeek != null) {
            panelCornerRadiusSeek.setProgress(
                    preferences.getPanelCornerRadiusDp()
            );
        }

        if (notificationTextSeek != null) {
            notificationTextSeek.setProgress(
                    preferences.getNotificationTextSizeSp()
            );
        }

        if (notificationBackgroundOpacitySeek != null) {
            notificationBackgroundOpacitySeek.setProgress(
                    preferences.getNotificationBackgroundOpacityPercent()
            );
        }

        if (backgroundImageOpacitySeek != null) {
            backgroundImageOpacitySeek.setProgress(
                    preferences.getPanelBackgroundImageOpacityPercent()
            );
        }

        refreshing = false;

        refreshAppearanceRows();
        refreshPanelPreviews();
        renderSelectedTiles();
    }

    private void build() {
        // PanOSP keeps its own validated navigation UI.
        addView(createPagerHeader());

        // GibUI supplies only the ViewPager paging engine here.
        pager = new SectionPager(getContext());
        pager.setPrimaryNavigation(true);
        pager.setNavigationVisible(false);
        pager.setBackgroundColor(Color.TRANSPARENT);
        pager.setSwipeEnabled(true);
        pager.setOnSectionSelectedListener(index -> {
            currentPage = index;
            updatePagerHeader();
        });

        LayoutParams pagerParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        addView(pager, pagerParams);

        // -------------------------------------------------------------
        // PAGE 1 — GÉNÉRAL
        // -------------------------------------------------------------
        LinearLayout backupPage = createPage();
        backupPage.setTag("backup_page");
        activePage = backupPage;

        backupPage.addView(sectionTitle(R.string.app_language));

        LinearLayout languageCard = createCard();

        Button languageButton = new Button(getContext());
        languageButton.setAllCaps(false);
        styleActionButton(languageButton);
if (getContext() instanceof MainActivity) {
            MainActivity activity =
                    (MainActivity) getContext();

            languageButton.setText(
                    getResources().getString(
                            R.string.app_language_value,
                            activity.getCurrentLanguageLabel()
                    )
            );

            languageButton.setOnClickListener(
                    v -> activity.showLanguageDialog()
            );
        }

        LayoutParams languageParams =
                new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        languageParams.topMargin = dp(2);
        languageParams.bottomMargin = dp(4);

        languageCard.addView(languageButton, languageParams);
        addCard(languageCard);

        backupPage.addView(
                sectionTitle(R.string.panel_config_configuration_heading)
        );

        LinearLayout backupCard = createCard();

        Button exportButton = new Button(getContext());
        exportButton.setAllCaps(false);
        styleActionButton(exportButton);
        exportButton.setText(R.string.panel_config_export);
        exportButton.setOnClickListener(v -> {
            if (getContext() instanceof MainActivity) {
                ((MainActivity) getContext()).exportPanelConfiguration();
            }
        });

        LayoutParams exportParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        exportParams.topMargin = dp(4);
        exportParams.bottomMargin = dp(4);
        backupCard.addView(exportButton, exportParams);

        Button importButton = new Button(getContext());
        importButton.setAllCaps(false);
        styleActionButton(importButton);
        importButton.setText(R.string.panel_config_import);
        importButton.setOnClickListener(v -> {
            if (getContext() instanceof MainActivity) {
                ((MainActivity) getContext()).importPanelConfiguration();
            }
        });

        LayoutParams importParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        importParams.topMargin = dp(4);
        importParams.bottomMargin = dp(4);
        backupCard.addView(importButton, importParams);

        Button resetButton = new Button(getContext());
        resetButton.setAllCaps(false);
        styleActionButton(resetButton);
        resetButton.setText(R.string.panel_config_reset);
        resetButton.setOnClickListener(v -> {
            if (getContext() instanceof MainActivity) {
                ((MainActivity) getContext()).resetPanelConfiguration();
            }
        });

        LayoutParams resetParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        resetParams.topMargin = dp(4);
        resetParams.bottomMargin = dp(4);
        backupCard.addView(resetButton, resetParams);

        addCard(backupCard);
        registerPage(backupPage);

        // -------------------------------------------------------------
        // PAGE 2 — DISPOSITION
        // -------------------------------------------------------------
        LinearLayout layoutPage = createPage();
        activePage = layoutPage;
        LinearLayout layoutCard = createCard();
        activeCard = layoutCard;

        dispositionPreview =
                new PanelPreviewView(
                        getContext(),
                        tilePreviewSnapshot
                );
        LayoutParams dispositionPreviewParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dispositionPreviewParams.bottomMargin = dp(12);
        layoutPage.addView(dispositionPreview, dispositionPreviewParams);

        columnsSeek = addRangeControl(
                R.string.panel_config_columns,
                PanOSPPreferences.MIN_TILE_COLUMNS,
                PanOSPPreferences.MAX_TILE_COLUMNS,
                value -> {
                    preferences.setTileColumns(value);
                    refreshPanelPreviews();
                    return true;
                }
        );

        rowsSeek = addRangeControl(
                R.string.panel_config_rows,
                PanOSPPreferences.MIN_TILE_ROWS,
                PanOSPPreferences.MAX_TILE_ROWS,
                value -> {
                    preferences.setTileRows(value);
                    refreshPanelPreviews();
                    return true;
                }
        );

        sizeSeek = addRangeControl(
                R.string.panel_config_tile_size,
                PanOSPPreferences.MIN_TILE_SIZE_DP,
                PanOSPPreferences.MAX_TILE_SIZE_DP,
                value -> {
                    preferences.setTileSizeDp(value);

                    if (radiusSeek != null) {
                        int maxRadius = value / 2;
                        radiusSeek.setMax(maxRadius);

                        if (preferences.getTileCornerRadiusDp() > maxRadius) {
                            preferences.setTileCornerRadiusDp(maxRadius);
                            radiusSeek.setProgress(maxRadius);
                        }
                    }
                    refreshPanelPreviews();
                    return true;
                }
        );

        radiusSeek = addRangeControl(
                R.string.panel_config_corner_radius,
                PanOSPPreferences.MIN_TILE_CORNER_RADIUS_DP,
                preferences.getTileSizeDp() / 2,
                value -> {
                    preferences.setTileCornerRadiusDp(value);
                    refreshPanelPreviews();
                    return true;
                }
        );

        opacitySeek = addRangeControl(
                R.string.panel_config_panel_opacity,
                PanOSPPreferences.MIN_PANEL_OPACITY_PERCENT,
                PanOSPPreferences.MAX_PANEL_OPACITY_PERCENT,
                value -> {
                    preferences.setPanelOpacityPercent(value);
                    refreshPanelPreviews();
                    return true;
                }
        );

        panelCornerRadiusSeek = addRangeControl(
                R.string.panel_config_panel_corner_radius,
                PanOSPPreferences.MIN_PANEL_CORNER_RADIUS_DP,
                PanOSPPreferences.MAX_PANEL_CORNER_RADIUS_DP,
                value -> {
                    preferences.setPanelCornerRadiusDp(value);
                    refreshPanelPreviews();
                    return true;
                }
        );

        notificationTextSeek = addRangeControl(
                R.string.panel_config_notification_text_size,
                PanOSPPreferences.MIN_NOTIFICATION_TEXT_SIZE_SP,
                PanOSPPreferences.MAX_NOTIFICATION_TEXT_SIZE_SP,
                value -> {
                    preferences.setNotificationTextSizeSp(value);
                    refreshPanelPreviews();
                    return true;
                }
        );

        notificationBackgroundOpacitySeek = addRangeControl(
                R.string.panel_config_notification_background_opacity,
                PanOSPPreferences.MIN_NOTIFICATION_BACKGROUND_OPACITY_PERCENT,
                PanOSPPreferences.MAX_NOTIFICATION_BACKGROUND_OPACITY_PERCENT,
                value -> {
                    preferences.setNotificationBackgroundOpacityPercent(value);
                    refreshPanelPreviews();
                    return true;
                }
        );

        activeCard = null;
        addCard(layoutCard);
        registerPage(layoutPage);

        // -------------------------------------------------------------
        // PAGE 3 — THÈME ET FOND
        // -------------------------------------------------------------
        LinearLayout themePage = createPage();
        activePage = themePage;
        LinearLayout themeCard = createCard();

        themePreview =
                new PanelPreviewView(
                        getContext(),
                        tilePreviewSnapshot
                );
        LayoutParams themePreviewParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        themePreviewParams.bottomMargin = dp(12);
        themePage.addView(themePreview, themePreviewParams);

        backgroundSelector = new BackgroundSelector(getContext(), readBackgroundConfig(),
                preferences.getThemeAccentColor());
        backgroundSelector.setColor1Summary(getContext().getString(R.string.panel_config_background_color_1_summary));
        backgroundSelector.setColor2Summary(getContext().getString(R.string.panel_config_background_color_2_summary));
        backgroundSelector.setOnBackgroundChangedListener(config -> {
            // Includes live previews and rollback: persist every received value,
            // exactly as the former local selector did, not only confirmation.
            preferences.setThemeBackgroundType(config.type);
            preferences.setThemeBackgroundColor(config.color1);
            preferences.setThemeBackgroundColor2(config.color2);
            preferences.setThemeBackgroundDirection(config.direction);
            appearanceChanged();
        });
        themeCard.addView(backgroundSelector, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        accentColorSwatch = new View(getContext());
        addAppearanceRow(themeCard, R.string.panel_config_theme_accent,
                R.string.panel_config_theme_accent_summary, accentColorSwatch,
                v -> showAccentColorDialog());
        refreshAppearanceRows();

        Button backgroundImageButton = new Button(getContext());
        backgroundImageButton.setAllCaps(false);
        styleActionButton(backgroundImageButton);
        backgroundImageButton.setText(R.string.panel_config_background_image_choose);
        backgroundImageButton.setOnClickListener(v -> {
            if (getContext() instanceof MainActivity) {
                ((MainActivity) getContext()).selectPanelBackgroundImage();
            }
        });

        LayoutParams backgroundImageParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        backgroundImageParams.topMargin = dp(4);
        backgroundImageParams.bottomMargin = dp(4);
        themeCard.addView(backgroundImageButton, backgroundImageParams);

        Button removeBackgroundImageButton = new Button(getContext());
        removeBackgroundImageButton.setAllCaps(false);
        styleActionButton(removeBackgroundImageButton);
        removeBackgroundImageButton.setText(R.string.panel_config_background_image_remove);
        removeBackgroundImageButton.setOnClickListener(v -> {
            if (getContext() instanceof MainActivity) {
                ((MainActivity) getContext()).clearPanelBackgroundImage();
            }
        });

        LayoutParams removeBackgroundImageParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        removeBackgroundImageParams.topMargin = dp(4);
        removeBackgroundImageParams.bottomMargin = dp(4);
        themeCard.addView(removeBackgroundImageButton, removeBackgroundImageParams);

        activeCard = themeCard;
        backgroundImageOpacitySeek = addRangeControl(
                R.string.panel_config_background_image_opacity,
                PanOSPPreferences.MIN_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT,
                PanOSPPreferences.MAX_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT,
                value -> {
                    preferences.setPanelBackgroundImageOpacityPercent(value);
                    refreshPanelPreviews();
                    return true;
                }
        );
        centerRangeControlLabel(backgroundImageOpacitySeek);

        activeCard = null;
        addCard(themeCard);
        registerPage(themePage);

        // -------------------------------------------------------------
        // PAGE 4 — TUILES
        // -------------------------------------------------------------
        LinearLayout tilesPage = createPage();
        activePage = tilesPage;
        LinearLayout tilesCard = createCard();

        TextView reorderHint = new TextView(getContext());
        reorderHint.setText(R.string.panel_config_reorder_hint);
        reorderHint.setTextColor(GIB_TEXT_SECONDARY);
        reorderHint.setTextSize(13);
        reorderHint.setPadding(dp(10), dp(6), dp(10), dp(6));
        tilesCard.addView(reorderHint);

        selectedTilesContainer = new LinearLayout(getContext());
        selectedTilesContainer.setOrientation(VERTICAL);
        tilesCard.addView(selectedTilesContainer);

        Button addTilesButton = new Button(getContext());
        addTilesButton.setAllCaps(false);
        styleActionButton(addTilesButton);
        addTilesButton.setText(R.string.panel_config_open_tile_picker);
        addTilesButton.setOnClickListener(v -> showTilePicker());

        LayoutParams addParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        addParams.topMargin = 0;
        tilesCard.addView(addTilesButton, addParams);

        addCard(tilesCard);
        registerPage(tilesPage);


        activePage = null;

        // Initial visual state of the PanOSP header.
        currentPage = pager.getCurrentSection();
        updatePagerHeader();
    }

    private View createPagerHeader() {
        FrameLayout header = new FrameLayout(getContext());
        header.setPadding(0, dp(2), 0, dp(18));

        LinearLayout tabs = new LinearLayout(getContext());
        tabs.setOrientation(HORIZONTAL);
        tabs.setGravity(Gravity.TOP);
        headerTabs = tabs;

        int[] titles = {
                R.string.panel_config_backup_reset_tab,
                R.string.panel_config_layout,
                R.string.panel_config_theme_colors,
                R.string.panel_config_selected_tiles
        };

        for (int i = 0; i < titles.length; i++) {
            final int page = i;

            LinearLayout tab = new LinearLayout(getContext());
            tab.setOrientation(VERTICAL);
            tab.setGravity(Gravity.CENTER_HORIZONTAL);
            tab.setPadding(dp(4), dp(2), dp(4), 0);
            tab.setClickable(true);
            tab.setFocusable(true);
            tab.setOnClickListener(v -> pager.selectSection(page, true));

            TextView label = new TextView(getContext());
            label.setText(
                    getResources()
                            .getString(titles[i])
                            .toUpperCase(getResources().getConfiguration().getLocales().get(0))
            );
            label.setTextSize(11);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setMinLines(1);
            label.setTextColor(GIB_TEXT_SECONDARY);
            label.setPadding(0, 0, 0, dp(5));
            pageTabs.add(label);

            tab.addView(
                    label,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(36)
                    )
            );

            View indicator = new View(getContext());
            indicator.setBackgroundColor(Color.TRANSPARENT);
            pageIndicators.add(indicator);

            LinearLayout.LayoutParams indicatorParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(2)
                    );
            indicatorParams.topMargin = dp(3);
            tab.addView(indicator, indicatorParams);

            tabs.addView(
                    tab,
                    new LinearLayout.LayoutParams(
                            0,
                            dp(52),
                            1f
                    )
            );
        }

        FrameLayout.LayoutParams tabsParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tabsParams.leftMargin = 0;
        tabsParams.rightMargin = 0;
        header.addView(tabs, tabsParams);

        return header;
    }


    private LinearLayout createPage() {
        LinearLayout page = new LinearLayout(getContext());
        page.setOrientation(VERTICAL);
        page.setPadding(0, dp(4), 0, 0);
        return page;
    }

    private void registerPage(View page) {
        int index = pager.getSectionCount();

        int[] titles = {
                R.string.panel_config_backup_reset_tab,
                R.string.panel_config_layout,
                R.string.panel_config_theme_colors,
                R.string.panel_config_selected_tiles
        };

        if (index >= titles.length) {
            throw new IllegalStateException("Unexpected PanOSP configuration page");
        }


        pager.addSection(
                getResources()
                        .getString(titles[index])
                        .toUpperCase(
                                getResources()
                                        .getConfiguration()
                                        .getLocales()
                                        .get(0)
                        ),
                page
        );
    }

    private void updatePagerHeader() {
        for (int i = 0; i < pageTabs.size(); i++) {
            TextView tab = pageTabs.get(i);
            View indicator = pageIndicators.get(i);
            boolean selected = i == currentPage;

            int color = selected
                    ? GIB_TEXT_PRIMARY
                    : GIB_TEXT_SECONDARY;

            tab.setTextColor(color);
            float alpha = selected ? 1f : 0.72f;
            tab.setAlpha(alpha);

            indicator.setBackgroundColor(
                    selected
                            ? GIB_TEXT_PRIMARY
                            : Color.TRANSPARENT
            );
        }
    }


    private void centerRangeControlLabel(SeekBar seekBar) {
        if (seekBar == null) {
            return;
        }

        android.view.ViewParent parent = seekBar.getParent();
        if (!(parent instanceof LinearLayout)) {
            return;
        }

        LinearLayout row = (LinearLayout) parent;

        if (row.getChildCount() > 0
                && row.getChildAt(0) instanceof TextView) {
            TextView label = (TextView) row.getChildAt(0);
            label.setGravity(Gravity.CENTER);
            label.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        }
    }

    private SeekBar addRangeControl(
            int labelRes,
            int min,
            int max,
            ValueChangeHandler handler) {

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(VERTICAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(6));

        TextView label = new TextView(getContext());
        label.setTextColor(GIB_TEXT_PRIMARY);
        label.setTextSize(14);
        row.addView(label);

        SeekBar seekBar = createComfortableSeekBar(min, max);
        row.addView(
                seekBar,
                new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(48)
                )
        );

        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    private int acceptedValue = min;

                    @Override
                    public void onProgressChanged(
                            SeekBar bar,
                            int value,
                            boolean fromUser) {

                        label.setText(
                                getResources().getString(
                                        R.string.panel_config_value,
                                        getResources().getString(labelRes),
                                        value
                                )
                        );

                        if (refreshing || !fromUser) {
                            acceptedValue = value;
                            return;
                        }

                        if (handler.onValueChanged(value)) {
                            acceptedValue = value;
                        } else {
                            refreshing = true;
                            bar.setProgress(acceptedValue);
                            refreshing = false;
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        acceptedValue = seekBar.getProgress();
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                }
        );

        if (activeCard != null) {
            activeCard.addView(row);
        } else {
            addView(row);
        }
        return seekBar;
    }

    // =========================================================================
    // ACTIVE TILES
    // =========================================================================

    private void renderSelectedTiles() {
        if (selectedTilesContainer == null) {
            return;
        }

        selectedTilesContainer.removeAllViews();

        List<String> selected = preferences.getTileSpecs();

        for (String spec : selected) {
            View row = createSelectedTileRow(spec);
            LayoutParams rowParams = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowParams.topMargin = dp(2);
            rowParams.bottomMargin = dp(2);
            selectedTilesContainer.addView(row, rowParams);
        }
    }

    private View createSelectedTileRow(String spec) {
        LinearLayout row = tileRow();

        TextView label = tileLabel(spec);
        row.addView(
                label,
                new LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        Button dragHandle = compactButton("≡");
        dragHandle.setContentDescription(
                getResources().getString(
                        R.string.panel_config_drag,
                        tileName(spec)
                )
        );
        dragHandle.setOnLongClickListener(v -> {
            row.setAlpha(0.55f);
            row.startDragAndDrop(
                    null,
                    new View.DragShadowBuilder(row),
                    spec,
                    0
            );
            return true;
        });
        row.addView(dragHandle);

        row.setOnDragListener((targetView, event) ->
                handleTileDrag(row, spec, event));

        Button remove = compactButton("−");
        remove.setContentDescription(
                getResources().getString(
                        R.string.panel_config_remove,
                        tileName(spec)
                )
        );
        remove.setOnClickListener(v -> removeTile(spec));
        row.addView(remove);

        return row;
    }

    private LinearLayout tileRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(3), dp(6), dp(3));
        row.setBackground(createSubtleRowBackground());
        return row;
    }

    private TextView tileLabel(String spec) {
        TextView label = new TextView(getContext());
        label.setText(tileName(spec));
        label.setTextColor(GIB_TEXT_PRIMARY);
        label.setTextSize(15);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setMinHeight(dp(48));
        return label;
    }

    private Button compactButton(String text) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setTextColor(resolveAccentColor());
        button.setTextSize(20);
        button.setMinWidth(dp(48));
        button.setMinimumWidth(dp(48));
        button.setMinHeight(dp(48));
        button.setMinimumHeight(dp(48));
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setStateListAnimator(null);
        button.setBackground(createCompactButtonBackground());
        return button;
    }

    // =========================================================================
    // TILE PICKER
    // =========================================================================

    private void showTilePicker() {
        Dialog dialog = new Dialog(getContext());

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(16));
        root.setBackground(PanOSPDialogStyler.roundedBackground(getContext()));
        root.setClipToOutline(true);

        TextView title = sectionTitle(R.string.panel_config_tile_picker_title);
        title.setPadding(0, 0, 0, dp(4));
        root.addView(title);

        TextView hint = new TextView(getContext());
        hint.setText(R.string.panel_config_tile_picker_explanation);
        hint.setTextColor(Color.LTGRAY);
        hint.setTextSize(13);
        hint.setPadding(0, 0, 0, dp(10));
        root.addView(hint);

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setFillViewport(false);

        LinearLayout gridContainer = new LinearLayout(getContext());
        gridContainer.setOrientation(VERTICAL);
        scrollView.addView(
                gridContainer,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                scrollView,
                new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Math.min(
                                dp(520),
                                Math.round(
                                        getResources()
                                                .getDisplayMetrics()
                                                .heightPixels * 0.62f
                                )
                        )
                )
        );

        Button close = new Button(getContext());
        close.setAllCaps(false);
        close.setText(android.R.string.ok);
        close.setTextColor(resolveAccentColor());
        close.setOnClickListener(v -> dialog.dismiss());
        root.addView(
                close,
                new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        dialog.setContentView(root);
        renderTilePickerGrid(gridContainer, dialog);
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void renderTilePickerGrid(
            LinearLayout gridContainer,
            Dialog dialog) {

        gridContainer.removeAllViews();

        List<String> selected = preferences.getTileSpecs();
        List<String> available = new ArrayList<>();

        for (String spec : preferences.getAvailableTileSpecs()) {
            if (!selected.contains(spec)) {
                available.add(spec);
            }
        }

        if (available.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText(R.string.panel_config_all_tiles_selected);
            empty.setTextColor(Color.GRAY);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(24), 0, dp(24));
            gridContainer.addView(empty);
            return;
        }

        TextView groupTitle = new TextView(getContext());
        groupTitle.setText(R.string.panel_config_tile_picker_panosp);
        groupTitle.setTextColor(Color.WHITE);
        groupTitle.setTextSize(14);
        groupTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        groupTitle.setPadding(0, dp(4), 0, dp(8));
        gridContainer.addView(groupTitle);

        for (int start = 0;
             start < available.size();
             start += PICKER_COLUMNS) {

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.TOP);

            for (int column = 0;
                 column < PICKER_COLUMNS;
                 column++) {

                int index = start + column;

                if (index < available.size()) {
                    String spec = available.get(index);
                    row.addView(
                            createTilePickerCell(
                                    spec,
                                    gridContainer,
                                    dialog
                            ),
                            new LayoutParams(
                                    0,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    1f
                            )
                    );
                } else {
                    View spacer = new View(getContext());
                    row.addView(
                            spacer,
                            new LayoutParams(
                                    0,
                                    1,
                                    1f
                            )
                    );
                }
            }

            gridContainer.addView(row);
        }
    }

    private View createTilePickerCell(
            String spec,
            LinearLayout gridContainer,
            Dialog dialog) {

        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setPadding(dp(3), dp(6), dp(3), dp(10));

        ImageButton icon = new ImageButton(getContext());
        icon.setImageResource(tileIconRes(spec));
        icon.setColorFilter(Color.WHITE);
        icon.setPadding(dp(12), dp(12), dp(12), dp(12));
        icon.setBackground(createPickerTileBackground());
        icon.setContentDescription(
                getResources().getString(
                        R.string.panel_config_add,
                        tileName(spec)
                )
        );
        icon.setOnClickListener(v -> {
            addTile(spec);
            renderTilePickerGrid(gridContainer, dialog);
        });

        cell.addView(
                icon,
                new LayoutParams(dp(52), dp(52))
        );

        TextView label = new TextView(getContext());
        label.setText(tileName(spec));
        label.setTextColor(Color.LTGRAY);
        label.setTextSize(11);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setPadding(0, dp(4), 0, 0);
        label.setOnClickListener(v -> {
            addTile(spec);
            renderTilePickerGrid(gridContainer, dialog);
        });

        cell.addView(
                label,
                new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        return cell;
    }

    private GradientDrawable createPickerTileBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(Color.rgb(55, 55, 55));
        background.setCornerRadius(dp(26));
        return background;
    }

    // =========================================================================
    // SLIDERS
    // =========================================================================

    /**
     * Large touch surface with a deliberately slim visual track.
     */
    private SeekBar createComfortableSeekBar(int min, int max) {
        SeekBar seekBar = new SeekBar(getContext());

        seekBar.setMin(min);
        seekBar.setMax(max);
        seekBar.setSplitTrack(false);

        android.content.res.ColorStateList accentTint =
                android.content.res.ColorStateList.valueOf(
                        resolveAccentColor()
                );

        seekBar.setProgressTintList(accentTint);
        seekBar.setThumbTintList(accentTint);

        return seekBar;
    }


    private int resolveAccentColor() {
        return preferences.getThemeAccentColor();
    }

    // =========================================================================
    // REORDER / ADD / REMOVE
    // =========================================================================

    private boolean handleTileDrag(
            LinearLayout targetRow,
            String targetSpec,
            DragEvent event) {

        Object state = event.getLocalState();

        if (!(state instanceof String)) {
            return false;
        }

        String draggedSpec = (String) state;

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return preferences.isKnownTileSpec(draggedSpec);

            case DragEvent.ACTION_DRAG_ENTERED:
                if (!draggedSpec.equals(targetSpec)) {
                    targetRow.setAlpha(0.65f);
                }
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                targetRow.setAlpha(1f);
                return true;

            case DragEvent.ACTION_DROP:
                targetRow.setAlpha(1f);
                moveTileToSpec(draggedSpec, targetSpec);
                selectedTilesContainer.post(this::renderSelectedTiles);
                return true;

            case DragEvent.ACTION_DRAG_ENDED:
                targetRow.setAlpha(1f);
                return true;

            default:
                return true;
        }
    }

    private void moveTileToSpec(
            String draggedSpec,
            String targetSpec) {

        List<String> specs =
                new ArrayList<>(preferences.getTileSpecs());

        int from = specs.indexOf(draggedSpec);
        int to = specs.indexOf(targetSpec);

        if (from < 0 || to < 0 || from == to) {
            return;
        }

        String moved = specs.remove(from);

        if (from < to) {
            to--;
        }

        specs.add(to, moved);
        preferences.setTileSpecs(specs);
    }

    private void addTile(String spec) {
        List<String> specs =
                new ArrayList<>(preferences.getTileSpecs());

        if (specs.contains(spec)) {
            return;
        }

        specs.add(spec);
        preferences.setTileSpecs(specs);
        renderSelectedTiles();
    }

    private void removeTile(String spec) {
        List<String> specs =
                new ArrayList<>(preferences.getTileSpecs());

        if (specs.size() <= 1) {
            Toast.makeText(
                    getContext(),
                    R.string.panel_config_keep_one_tile,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        specs.remove(spec);
        preferences.setTileSpecs(specs);
        renderSelectedTiles();
    }


    // =========================================================================
    // GLOBAL THEME
    // =========================================================================

    private TextView addAppearanceRow(
            LinearLayout parent,
            int labelRes,
            int summaryRes,
            View swatch,
            View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), 0, 0, 0);
        row.setMinimumHeight(dp(64));
        row.setOnClickListener(listener);
        row.setClickable(true);
        row.setFocusable(true);

        LinearLayout labels = new LinearLayout(getContext());
        labels.setOrientation(VERTICAL);
        labels.setPadding(0, dp(16), 0, dp(16));
        TextView title = new TextView(getContext());
        title.setText(labelRes);
        title.setTextColor(GIB_TEXT_PRIMARY);
        title.setTextSize(16);
        labels.addView(title);
        TextView value = new TextView(getContext());
        if (summaryRes != 0) value.setText(summaryRes);
        value.setTextColor(GIB_TEXT_SECONDARY);
        value.setTextSize(14);
        value.setMaxLines(10);
        labels.addView(value);
        row.addView(labels, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (swatch != null) {
            FrameLayout widgetFrame = new FrameLayout(getContext());
            widgetFrame.setForegroundGravity(Gravity.CENTER);
            FrameLayout.LayoutParams swatchParams = new FrameLayout.LayoutParams(
                    dp(26), dp(26), Gravity.CENTER);
            widgetFrame.addView(swatch, swatchParams);
            LayoutParams widgetParams = new LayoutParams(dp(48), dp(48));
            widgetParams.leftMargin = dp(16);
            row.addView(widgetFrame, widgetParams);
        }
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        parent.addView(row, params);
        return value;
    }

    private BackgroundConfig readBackgroundConfig() {
        return new BackgroundConfig(preferences.getThemeBackgroundType(),
                preferences.getThemeBackgroundColor(), preferences.getThemeBackgroundColor2(),
                preferences.getThemeBackgroundDirection());
    }

    private void refreshAppearanceRows() {
        if (backgroundSelector != null) {
            backgroundSelector.setConfig(readBackgroundConfig());
            backgroundSelector.setAccentColor(preferences.getThemeAccentColor());
            backgroundSelector.setColor1Summary(getContext().getString(R.string.panel_config_background_color_1_summary));
            backgroundSelector.setColor2Summary(getContext().getString(R.string.panel_config_background_color_2_summary));
        }
        setColorSwatch(accentColorSwatch, preferences.getThemeAccentColor());
    }

    private void setColorSwatch(View swatch, int color) {
        if (swatch == null) return;
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(color);
        disc.setStroke(dp(1), Color.rgb(136, 136, 136));
        swatch.setBackground(disc);
    }

    private void showAccentColorDialog() {
        showSingleColorDialog(
                R.string.panel_config_theme_accent,
                preferences.getThemeAccentColor(),
                preferences::setThemeAccentColor);
    }

    private interface ColorConsumer {
        void accept(int color);
    }

    private void showSingleColorDialog(int titleRes, int initialColor, ColorConsumer consumer) {
        Dialog dialog = new Dialog(getContext());
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(12));
        root.setBackground(PanOSPDialogStyler.roundedBackground(getContext()));
        root.setClipToOutline(true);

        TextView title = new TextView(getContext());
        title.setText(titleRes);
        title.setTextColor(GIB_TEXT_PRIMARY);
        title.setTextSize(20);
        root.addView(title);

        SvColorPicker picker = new SvColorPicker(getContext());
        picker.setColor(initialColor);
        LayoutParams pickerParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(240));
        pickerParams.topMargin = dp(14);
        root.addView(picker, pickerParams);

        SeekBar hue = createComfortableSeekBar(0, 360);
        float[] hsv = new float[3];
        Color.colorToHSV(initialColor, hsv);
        hue.setMax(360);
        hue.setProgress(Math.round(hsv[0]));
        root.addView(hue, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        final int[] selected = {initialColor | 0xFF000000};
        picker.setOnColorChangeListener((color, fromUser) -> {
            if (fromUser) selected[0] = color | 0xFF000000;
        });
        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                picker.setHue(progress);
                selected[0] = picker.getColor() | 0xFF000000;
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        LinearLayout actions = new LinearLayout(getContext());
        actions.setGravity(Gravity.END);
        Button cancel = new Button(getContext());
        cancel.setText(android.R.string.cancel);
        PanOSPDialogStyler.styleDialogAction(cancel);
        Button apply = new Button(getContext());
        apply.setText(android.R.string.ok);
        PanOSPDialogStyler.styleDialogAction(apply);
        actions.addView(cancel);
        actions.addView(apply);
        root.addView(actions);
        cancel.setOnClickListener(v -> dialog.dismiss());
        apply.setOnClickListener(v -> {
            consumer.accept(selected[0]);
            appearanceChanged();
            dialog.dismiss();
        });

        dialog.setContentView(root);
        dialog.show();
        PanOSPDialogStyler.apply(dialog);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );

            window.clearFlags(
                    android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
            );

            window.setGravity(
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
            );

            int dialogWidth = Math.round(
                    getResources()
                            .getDisplayMetrics()
                            .widthPixels * 0.88f
            );

            window.setLayout(
                    dialogWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            android.view.WindowManager.LayoutParams attributes =
                    window.getAttributes();

            attributes.y = dp(16);
            attributes.dimAmount = 0f;

            window.setAttributes(attributes);
        }
    }

    private void appearanceChanged() {
        refreshAppearanceRows();
        refreshPanelPreviews();
        if (getContext() instanceof MainActivity) {
            ((MainActivity) getContext()).refreshGlobalAppearance();
        }
    }

    // =========================================================================
    // MINI PREVIEWS
    // =========================================================================

    private void refreshPanelPreviews() {
        if (dispositionPreview != null) {
            dispositionPreview.refresh();
        }

        if (themePreview != null) {
            themePreview.refresh();
        }
    }

    // =========================================================================
    // LABELS / SUMMARY
    // =========================================================================

    private String tileName(String spec) {
        return getResources().getString(
                tileLabelRes(spec)
        );
    }

    private int tileLabelRes(String spec) {
        switch (spec) {
            case "wifi":
                return R.string.tile_wifi;
            case "data":
                return R.string.tile_mobile_data;
            case "bluetooth":
                return R.string.tile_bluetooth;
            case "flashlight":
                return R.string.tile_flashlight;
            case "rotation":
                return R.string.tile_rotation;
            case "location":
                return R.string.tile_location;
            case "hotspot":
                return R.string.tile_hotspot;
            case "airplane":
                return R.string.tile_airplane;
            case "dnd":
                return R.string.tile_dnd;
            case "battery":
                return R.string.tile_battery_saver;

            // NEW TILES - TEMPORARY TEST GROUP
            case "nfc":
                return R.string.tile_nfc;
            case "dark":
                return R.string.tile_dark_mode;
            case "silent":
                return R.string.tile_silent;
            case "vibrate":
                return R.string.tile_vibrate;
            case "reading":
                return R.string.tile_reading_mode;
            case "screenshot":
                return R.string.tile_screenshot;
            case "camera":
                return R.string.tile_camera;

            default:
                return R.string.app_name;
        }
    }

    private int tileIconRes(String spec) {
        switch (spec) {
            case "wifi":
                return R.drawable.ic_qs_wifi;
            case "data":
                return R.drawable.ic_qs_mobile_data;
            case "bluetooth":
                return R.drawable.ic_qs_bluetooth;
            case "flashlight":
                return R.drawable.ic_qs_flashlight;
            case "rotation":
                return R.drawable.ic_qs_rotation;
            case "location":
                return R.drawable.ic_qs_location;
            case "hotspot":
                return R.drawable.ic_qs_hotspot;
            case "airplane":
                return R.drawable.ic_qs_airplane;
            case "dnd":
                return R.drawable.ic_qs_dnd;
            case "battery":
                return R.drawable.ic_qs_battery_saver;

            // NEW TILES - TEMPORARY TEST GROUP
            case "nfc":
                return R.drawable.ic_qs_nfc;
            case "dark":
                return R.drawable.ic_qs_dark;
            case "silent":
                return R.drawable.ic_qs_silent;
            case "vibrate":
                return R.drawable.ic_qs_vibrate;
            case "reading":
                return R.drawable.ic_qs_reading;
            case "screenshot":
                return R.drawable.ic_qs_screenshot;
            case "camera":
                return R.drawable.ic_qs_camera;

            default:
                return R.drawable.ic_qs_unknown;
        }
    }

    private GradientDrawable createGibClockSectionBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(14));
        background.setColor(Color.argb(0x14, 255, 255, 255));
        background.setStroke(
                dp(1),
                Color.argb(0x28, 255, 255, 255)
        );
        return background;
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setPadding(dp(12), dp(4), dp(12), dp(6));
        card.setBackground(createGibClockSectionBackground());
        return card;
    }


    private void addCard(LinearLayout card) {
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(4);
        params.bottomMargin = dp(10);

        if (activePage != null) {
            ScrollView cardScroll = new ScrollView(getContext());
            cardScroll.setFillViewport(false);
            cardScroll.setClipToPadding(false);
            cardScroll.setVerticalScrollBarEnabled(false);

            // Fixed rounded viewport: corners remain rounded while the
            // card content scrolls underneath the upper/lower edges.
            cardScroll.setBackground(createGibClockSectionBackground());
            cardScroll.setClipToOutline(true);

            // The visual card background now belongs to the fixed viewport,
            // avoiding a moving/double border while scrolling.
            card.setBackgroundColor(Color.TRANSPARENT);

            cardScroll.addView(
                    card,
                    new ScrollView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            boolean compactPage =
                    "backup_page".equals(activePage.getTag());

            LayoutParams scrollParams = compactPage
                    ? new LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    : new LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            0,
                            1f
                    );

            scrollParams.topMargin = dp(2);
            scrollParams.bottomMargin = dp(2);

            activePage.addView(cardScroll, scrollParams);
        } else {
            addView(card, params);
        }
    }

    private GradientDrawable createSubtleRowBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.argb(10, 255, 255, 255));
        drawable.setCornerRadius(dp(10));
        return drawable;
    }

    private GradientDrawable createCompactButtonBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.argb(14, 255, 255, 255));
        drawable.setStroke(dp(1), Color.argb(35, 255, 255, 255));
        drawable.setCornerRadius(dp(12));
        return drawable;
    }

    private void styleActionButton(Button button) {
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTextColor(GIB_TEXT_PRIMARY);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setMinHeight(dp(52));
        button.setMinimumHeight(dp(52));
        button.setPadding(dp(4), dp(8), dp(4), dp(8));

        android.content.res.TypedArray attributes =
                getContext().obtainStyledAttributes(
                        new int[]{android.R.attr.selectableItemBackground}
                );

        int selectableBackground =
                attributes.getResourceId(0, 0);

        attributes.recycle();

        if (selectableBackground != 0) {
            button.setBackgroundResource(selectableBackground);
        } else {
            button.setBackgroundColor(Color.TRANSPARENT);
        }

        button.setStateListAnimator(null);
        button.setElevation(0f);
    }


    private TextView sectionTitle(int labelRes) {
        TextView title = new TextView(getContext());
        title.setText(labelRes);
        title.setTextColor(resolveAccentColor());
        title.setTextSize(16);
        title.setPadding(dp(2), dp(14), 0, dp(6));
        return title;
    }

    private int dp(int value) {
        return Math.round(
                value * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private interface ValueChangeHandler {
        boolean onValueChanged(int value);
    }
}
