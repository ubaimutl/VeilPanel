package com.ubai.veilpanel.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ubai.veilpanel.config.PanOSPPreferences;
import com.ubai.veilpanel.tiles.PanTile;

/**
 * Responsive quick-tile container backed by AndroidX ViewPager.
 *
 * AndroidX owns horizontal drag, direction reversal, velocity and settling.
 * PanOSP owns only tile rendering and page content.
 */
public final class TileGridView extends LinearLayout {

    private static final long REFRESH_FAST_MS = 250L;
    private static final long REFRESH_MEDIUM_MS = 850L;
    private static final long REFRESH_SLOW_MS = 1700L;

    private final int columns;
    private final int rows;
    private final int requestedTileSizeDp;
    private final int tileCornerRadiusDp;
    private final boolean previewMode;

    private final List<PanTile> allTiles =
            new ArrayList<>();

    private final Map<PanTile, TileView> tileViews =
            new LinkedHashMap<>();

    private final List<View> pages =
            new ArrayList<>();

    private final TilePager pager;
    private final TextView pageIndicator;

    private int currentPage;
    private int pageCount = 1;

    private final PagerAdapter pageAdapter =
            new PagerAdapter() {

                @Override
                public int getCount() {
                    return pages.size();
                }

                @Override
                public boolean isViewFromObject(
                        @NonNull View view,
                        @NonNull Object object) {

                    return view == object;
                }

                @NonNull
                @Override
                public Object instantiateItem(
                        @NonNull ViewGroup container,
                        int position) {

                    View page = pages.get(position);

                    if (page.getParent() == null) {
                        container.addView(
                                page,
                                new ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                )
                        );
                    }

                    return page;
                }

                @Override
                public void destroyItem(
                        @NonNull ViewGroup container,
                        int position,
                        @NonNull Object object) {

                    container.removeView((View) object);
                }

                @Override
                public int getItemPosition(
                        @NonNull Object object) {

                    return POSITION_NONE;
                }
            };

    /**
     * Interactive constructor used by the real PanOSP panel.
     */
    public TileGridView(
            Context context,
            int columns,
            int rows,
            int requestedTileSizeDp,
            int tileCornerRadiusDp) {

        this(
                context,
                columns,
                rows,
                requestedTileSizeDp,
                tileCornerRadiusDp,
                false
        );
    }

    /**
     * Non-interactive constructor used by PanelPreviewView.
     */
    public TileGridView(
            Context context,
            int columns,
            int rows,
            int requestedTileSizeDp,
            int tileCornerRadiusDp,
            boolean previewMode) {

        super(context);

        this.columns = Math.max(1, columns);
        this.rows = Math.max(1, rows);
        this.requestedTileSizeDp = requestedTileSizeDp;
        this.tileCornerRadiusDp = tileCornerRadiusDp;
        this.previewMode = previewMode;

        setOrientation(VERTICAL);

        pager = new TilePager(context);
        pager.setPagingEnabled(!previewMode);
        pager.setAdapter(pageAdapter);

        pager.addOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {

                    @Override
                    public void onPageSelected(int position) {
                        currentPage = position;
                        updatePageIndicator();
                    }
                }
        );

        addView(
                pager,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        pageIndicator = new TextView(context);
        pageIndicator.setTextColor(
                PanOSPTheme.TEXT_SECONDARY
        );
        pageIndicator.setTextSize(11f);
        pageIndicator.setGravity(Gravity.CENTER);
        pageIndicator.setPadding(
                0,
                0,
                0,
                dp(2)
        );

        addView(
                pageIndicator,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        updatePageIndicator();
    }

    public void setTiles(Iterable<PanTile> tiles) {

        allTiles.clear();

        if (tiles != null) {
            for (PanTile tile : tiles) {
                if (tile != null) {
                    allTiles.add(tile);
                }
            }
        }

        int capacity = getPageCapacity();

        pageCount =
                Math.max(
                        1,
                        (allTiles.size() + capacity - 1)
                                / capacity
                );

        currentPage =
                Math.min(
                        currentPage,
                        pageCount - 1
                );

        rebuildPages();
    }

    public void refresh() {

        for (TileView tileView : tileViews.values()) {
            tileView.refresh();
        }
    }

    @Override
    protected void onSizeChanged(
            int width,
            int height,
            int oldWidth,
            int oldHeight) {

        super.onSizeChanged(
                width,
                height,
                oldWidth,
                oldHeight
        );

        if (width != oldWidth) {
            applyResponsiveTileSize(width);
            pager.requestLayout();
        }
    }

    // =========================================================================
    // PAGE CONTENT
    // =========================================================================

    private void rebuildPages() {

        /*
         * Detach old ViewPager pages cleanly before replacing
         * the backing page collection.
         */
        pager.setAdapter(null);

        pages.clear();
        tileViews.clear();

        int capacity = getPageCapacity();

        for (int pageIndex = 0;
             pageIndex < pageCount;
             pageIndex++) {

            LinearLayout page =
                    new LinearLayout(getContext());

            page.setOrientation(VERTICAL);

            int start =
                    pageIndex * capacity;

            int end =
                    Math.min(
                            start + capacity,
                            allTiles.size()
                    );

            int tileCount =
                    Math.max(
                            0,
                            end - start
                    );

            int rowsOnPage =
                    getRowsForTileCount(tileCount);

            for (int rowIndex = 0;
                 rowIndex < rowsOnPage;
                 rowIndex++) {

                LinearLayout row = createRow();

                for (int columnIndex = 0;
                     columnIndex < columns;
                     columnIndex++) {

                    int tileIndex =
                            start
                                    + rowIndex * columns
                                    + columnIndex;

                    if (tileIndex < end) {

                        PanTile tile =
                                allTiles.get(tileIndex);

                        TileView tileView =
                                createTileView(tile);

                        row.addView(tileView);
                        tileViews.put(
                                tile,
                                tileView
                        );

                    } else {

                        LinearLayout spacer =
                                new LinearLayout(
                                        getContext()
                                );

                        spacer.setVisibility(
                                INVISIBLE
                        );

                        row.addView(
                                spacer,
                                createCellLayoutParams()
                        );
                    }
                }

                page.addView(row);
            }

            pages.add(page);
        }

        /*
         * There is always at least one page, even with no tiles.
         */
        if (pages.isEmpty()) {

            LinearLayout empty =
                    new LinearLayout(getContext());

            empty.setOrientation(VERTICAL);

            pages.add(empty);
        }

        pager.setAdapter(pageAdapter);

        /*
         * Keep every small tile page warm, as SectionPager already does.
         * No page construction occurs in the gesture path.
         */
        pager.setOffscreenPageLimit(
                Math.max(
                        1,
                        pages.size() - 1
                )
        );

        pager.setCurrentItem(
                Math.min(
                        currentPage,
                        pages.size() - 1
                ),
                false
        );

        updatePageIndicator();

        applyResponsiveTileSize(
                getWidth()
        );

        pager.requestLayout();
    }

    private void updatePageIndicator() {

        if (pageCount <= 1) {
            pageIndicator.setVisibility(GONE);
            return;
        }

        pageIndicator.setVisibility(VISIBLE);

        pageIndicator.setText(
                getContext().getString(
                        com.ubai.veilpanel.R.string.tile_page_indicator,
                        currentPage + 1,
                        pageCount
                )
        );
    }

    // =========================================================================
    // TILE RENDERING
    // =========================================================================

    private int getPageCapacity() {

        return Math.max(
                1,
                columns * rows
        );
    }

    private int getRowsForTileCount(int tileCount) {

        if (tileCount <= 0) {
            return 0;
        }

        int needed =
                (tileCount + columns - 1)
                        / columns;

        return Math.max(
                1,
                Math.min(
                        rows,
                        needed
                )
        );
    }

    private LinearLayout createRow() {

        LinearLayout row =
                new LinearLayout(getContext());

        row.setOrientation(HORIZONTAL);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        rowParams.bottomMargin =
                dp(
                        PanOSPTheme
                                .TILE_VERTICAL_GAP_DP
                );

        row.setLayoutParams(rowParams);

        return row;
    }

    private TileView createTileView(PanTile tile) {

        TileView tileView =
                new TileView(getContext());

        tileView.setLayoutParams(
                createCellLayoutParams()
        );

        tileView.applyVisualStyle(
                requestedTileSizeDp,
                tileCornerRadiusDp
        );

        tileView.bind(tile);

        if (previewMode) {

            tileView.setClickable(false);
            tileView.setFocusable(false);
            tileView.setLongClickable(false);

        } else {

            tileView.setOnClickListener(
                    view -> {
                        tile.handleClick();
                        scheduleStateRefreshes();
                    }
            );

            tileView.setOnLongClickListener(
                    view -> {

                        boolean handled =
                                tile.handleLongClick();

                        if (handled) {
                            scheduleStateRefreshes();
                        }

                        return handled;
                    }
            );
        }

        return tileView;
    }

    private void applyResponsiveTileSize(
            int widthPx) {

        int actualSizeDp =
                requestedTileSizeDp;

        if (widthPx > 0) {

            int cellWidthPx =
                    widthPx / columns;

            int maxVisualPx =
                    Math.max(
                            dp(
                                    PanOSPPreferences
                                            .MIN_TILE_SIZE_DP
                            ),
                            cellWidthPx
                                    - dp(
                                    PanOSPTheme
                                            .TILE_HORIZONTAL_GAP_DP
                                            * 2
                            )
                    );

            int maxVisualDp =
                    Math.round(
                            maxVisualPx
                                    / getResources()
                                    .getDisplayMetrics()
                                    .density
                    );

            actualSizeDp =
                    Math.min(
                            requestedTileSizeDp,
                            maxVisualDp
                    );
        }

        for (TileView tileView :
                tileViews.values()) {

            tileView.applyVisualStyle(
                    actualSizeDp,
                    tileCornerRadiusDp
            );
        }
    }

    /**
     * Hardware radios and OEM services do not all publish state
     * at the same speed. Multiple cheap redraws keep PanOSP
     * visually in sync.
     */
    private void scheduleStateRefreshes() {

        postDelayed(
                this::refresh,
                REFRESH_FAST_MS
        );

        postDelayed(
                this::refresh,
                REFRESH_MEDIUM_MS
        );

        postDelayed(
                this::refresh,
                REFRESH_SLOW_MS
        );
    }

    private LinearLayout.LayoutParams
    createCellLayoutParams() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );

        int halfGap =
                dp(
                        PanOSPTheme
                                .TILE_HORIZONTAL_GAP_DP
                ) / 2;

        params.setMargins(
                halfGap,
                0,
                halfGap,
                0
        );

        return params;
    }

    private int dp(int value) {

        return Math.round(
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    // =========================================================================
    // ANDROIDX PAGER
    // =========================================================================

    /**
     * ViewPager normally expects a fixed height. PanOSP's quick-tile
     * block is WRAP_CONTENT, so measure the tile pages first and give
     * AndroidX exactly the height required by the tallest page.
     *
     * Touch physics remain entirely native ViewPager.
     */
    private final class TilePager extends ViewPager {

        private boolean pagingEnabled = true;

        TilePager(Context context) {
            super(context);
            setOverScrollMode(
                    OVER_SCROLL_NEVER
            );
        }

        void setPagingEnabled(boolean enabled) {
            pagingEnabled = enabled;
        }

        @Override
        public boolean onInterceptTouchEvent(
                MotionEvent event) {

            if (!pagingEnabled) {
                return false;
            }

            try {
                return super
                        .onInterceptTouchEvent(
                                event
                        );
            } catch (IllegalArgumentException ignored) {

                /*
                 * Same defensive protection as GibUI SectionPager
                 * for malformed OEM pointer streams.
                 */
                return false;
            }
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent event) {

            if (!pagingEnabled) {
                return false;
            }

            try {
                return super.onTouchEvent(event);
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }

        @Override
        protected void onMeasure(
                int widthMeasureSpec,
                int heightMeasureSpec) {

            int width =
                    MeasureSpec.getSize(
                            widthMeasureSpec
                    );

            int pageWidthSpec =
                    MeasureSpec.makeMeasureSpec(
                            Math.max(0, width),
                            MeasureSpec.EXACTLY
                    );

            int pageHeightSpec =
                    MeasureSpec.makeMeasureSpec(
                            0,
                            MeasureSpec.UNSPECIFIED
                    );

            int maxHeight = 0;

            for (View page : pages) {

                page.measure(
                        pageWidthSpec,
                        pageHeightSpec
                );

                maxHeight =
                        Math.max(
                                maxHeight,
                                page.getMeasuredHeight()
                        );
            }

            maxHeight =
                    Math.max(
                            1,
                            maxHeight
                    );

            int exactHeight =
                    MeasureSpec.makeMeasureSpec(
                            maxHeight,
                            MeasureSpec.EXACTLY
                    );

            super.onMeasure(
                    widthMeasureSpec,
                    exactHeight
            );
        }
    }
}
