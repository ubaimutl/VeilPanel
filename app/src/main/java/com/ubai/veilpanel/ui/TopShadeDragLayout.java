package com.ubai.veilpanel.ui;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.customview.widget.ViewDragHelper;

public final class TopShadeDragLayout extends FrameLayout {

    public interface Listener {
        void onOpened();
        void onClosed();
    }

    private final ViewDragHelper dragHelper;
    private final Listener listener;
    private final int openTop;
    private final int minVelocity;

    private View panel;
    private boolean geometryPrepared;

    public TopShadeDragLayout(
            Context context,
            int statusBarHeight,
            Listener listener) {

        super(context);

        this.openTop = statusBarHeight;
        this.listener = listener;

        setClipChildren(false);
        setClipToPadding(false);

        minVelocity =
                ViewConfiguration.get(context)
                        .getScaledMinimumFlingVelocity();

        dragHelper =
                ViewDragHelper.create(
                        this,
                        1.0f,
                        new ViewDragHelper.Callback() {

                            @Override
                            public boolean tryCaptureView(
                                    @NonNull View child,
                                    int pointerId) {
                                return child == panel;
                            }

                            @Override
                            public int clampViewPositionHorizontal(
                                    @NonNull View child,
                                    int left,
                                    int dx) {
                                return 0;
                            }

                            @Override
                            public int clampViewPositionVertical(
                                    @NonNull View child,
                                    int top,
                                    int dy) {

                                return Math.max(
                                        closedTop(),
                                        Math.min(openTop, top)
                                );
                            }

                            @Override
                            public int getViewVerticalDragRange(
                                    @NonNull View child) {

                                return child == panel
                                        ? Math.max(1, panel.getHeight())
                                        : 0;
                            }

                            @Override
                            public void onViewReleased(
                                    @NonNull View child,
                                    float xvel,
                                    float yvel) {

                                if (Math.abs(yvel) > minVelocity) {

                                    dragHelper.flingCapturedView(
                                            0,
                                            closedTop(),
                                            0,
                                            openTop
                                    );

                                } else {

                                    boolean open =
                                            getOpenFraction() >= 0.5f;

                                    dragHelper.settleCapturedViewAt(
                                            0,
                                            open ? openTop : closedTop()
                                    );
                                }

                                postInvalidateOnAnimation();
                            }

                            @Override
                            public void onViewDragStateChanged(int state) {

                                if (state != ViewDragHelper.STATE_IDLE
                                        || panel == null) {
                                    return;
                                }

                                if (panel.getTop() == openTop) {
                                    listener.onOpened();
                                } else if (panel.getTop() == closedTop()) {
                                    listener.onClosed();
                                }
                            }
                        }
                );

        dragHelper.setMinVelocity(minVelocity);
    }

    public void setPanel(View view) {

        removeAllViews();

        panel = view;
        geometryPrepared = false;

        if (panel == null) {
            return;
        }

        addView(
                panel,
                new LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                )
        );
    }

    /*
     * Critical for the accessibility-overlay case:
     * WindowManager.updateViewLayout() schedules a traversal asynchronously.
     * We therefore measure/layout this hierarchy synchronously before feeding
     * ACTION_DOWN to ViewDragHelper.
     */
    public void prepareGeometry(
            int width,
            int height) {

        if (panel == null || width <= 0 || height <= 0) {
            return;
        }

        int widthSpec =
                MeasureSpec.makeMeasureSpec(
                        width,
                        MeasureSpec.EXACTLY
                );

        int heightSpec =
                MeasureSpec.makeMeasureSpec(
                        height,
                        MeasureSpec.EXACTLY
                );

        measure(widthSpec, heightSpec);
        layout(0, 0, width, height);

        geometryPrepared = true;
    }

    public void beginTouchSequence(MotionEvent event) {

        if (panel == null
                || !geometryPrepared
                || event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return;
        }

        /*
         * PanOSP's accessibility overlay is already the top-edge trigger.
         * Unlike DrawerLayout we do not need ViewDragHelper to discover the
         * edge again. Feed DOWN first, then capture the panel immediately.
         */
        dragHelper.processTouchEvent(event);

        dragHelper.captureChildView(
                panel,
                event.getPointerId(event.getActionIndex())
        );
    }

    public void processTouchEventForShade(MotionEvent event) {

        if (panel != null && geometryPrepared) {
            dragHelper.processTouchEvent(event);
        }
    }

    public float getOpenFraction() {

        if (panel == null || panel.getHeight() <= 0) {
            return 0f;
        }

        float fraction =
                (panel.getTop() - closedTop())
                        / (float) panel.getHeight();

        return Math.max(
                0f,
                Math.min(1f, fraction)
        );
    }

    public void settleOpen() {

        if (panel == null) {
            return;
        }

        dragHelper.cancel();

        if (dragHelper.smoothSlideViewTo(
                panel,
                0,
                openTop)) {

            postInvalidateOnAnimation();

        } else {
            snapOpen();
        }
    }

    public void settleClosed() {

        if (panel == null) {
            return;
        }

        dragHelper.cancel();

        if (dragHelper.smoothSlideViewTo(
                panel,
                0,
                closedTop())) {

            postInvalidateOnAnimation();

        } else {
            offsetPanelTo(closedTop());
            listener.onClosed();
        }
    }

    public void snapOpen() {

        if (panel == null) {
            return;
        }

        dragHelper.abort();

        offsetPanelTo(openTop);
        listener.onOpened();
    }

    private int closedTop() {

        if (panel == null) {
            return openTop;
        }

        return openTop - panel.getMeasuredHeight();
    }

    private void offsetPanelTo(int targetTop) {

        if (panel == null) {
            return;
        }

        int delta =
                targetTop - panel.getTop();

        if (delta != 0) {
            panel.offsetTopAndBottom(delta);
        }

        invalidate();
    }

    @Override
    protected void onLayout(
            boolean changed,
            int left,
            int top,
            int right,
            int bottom) {

        if (panel == null) {
            return;
        }

        int width = right - left;
        int panelHeight = panel.getMeasuredHeight();

        int panelTop;

        if (!geometryPrepared) {
            panelTop = openTop - panelHeight;
        } else {
            panelTop = panel.getTop();
        }

        panel.layout(
                0,
                panelTop,
                width,
                panelTop + panelHeight
        );
    }

    @Override
    public void computeScroll() {

        if (dragHelper.continueSettling(true)) {
            postInvalidateOnAnimation();
        }
    }
}
