/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.ActionBar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;

public class DrawerLayoutContainer extends FrameLayout {

    private INavigationLayout parentActionBarLayout;
    private ActionBarLayout actionBarLayout;
    private boolean inLayout;

    public DrawerLayoutContainer(Context context) {
        super(context);

        ViewCompat.setOnApplyWindowInsetsListener(this, this::onApplyWindowInsets);
        setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    public void setParentActionBarLayout(INavigationLayout layout) {
        parentActionBarLayout = layout;
    }

    public void setActionBarLayout(ActionBarLayout actionBarLayout) {
        this.actionBarLayout = actionBarLayout;
    }

    public boolean isDrawCurrentPreviewFragmentAbove() {
        return false;
    }

    private View drawerContentView;
    private View drawerScrimView;
    private boolean drawerOpened;
    private static final int DRAWER_WIDTH_DP = 280;

    private boolean allowDrawerSwipe;
    private boolean draggingDrawer;
    private boolean maybeStartDragging;
    private float startedTrackingX;
    private float startedTrackingY;

    public void setDrawerLayout(View view) {
        if (drawerContentView == view) {
            return;
        }
        if (drawerContentView != null && drawerContentView.getParent() == this) {
            removeView(drawerContentView);
        }
        drawerContentView = view;
        if (drawerScrimView == null) {
            drawerScrimView = new View(getContext());
            drawerScrimView.setBackgroundColor(0x99000000);
            drawerScrimView.setVisibility(GONE);
            drawerScrimView.setAlpha(0f);
            drawerScrimView.setOnClickListener(v -> closeDrawer(true));
            addView(drawerScrimView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }
        drawerContentView.setVisibility(GONE);
        drawerContentView.setTranslationX(-AndroidUtilities.dp(DRAWER_WIDTH_DP));
        addView(drawerContentView, new LayoutParams(AndroidUtilities.dp(DRAWER_WIDTH_DP), LayoutParams.MATCH_PARENT));
        drawerContentView.bringToFront();
    }

    public boolean isDrawerOpened() {
        return drawerOpened;
    }

    /**
     * Enabled only where an edge swipe has nothing else to do - i.e. on the first
     * ("All chats") folder tab, where there is no previous tab to swipe to.
     */
    public void setAllowDrawerSwipe(boolean allow) {
        allowDrawerSwipe = allow;
    }

    public void openDrawer(boolean fast) {
        if (drawerContentView == null) {
            return;
        }
        drawerOpened = true;
        drawerScrimView.bringToFront();
        drawerContentView.bringToFront();
        drawerScrimView.setVisibility(VISIBLE);
        drawerScrimView.animate().setListener(null).cancel();
        drawerScrimView.animate().alpha(1f).setDuration(fast ? 150 : 250).setListener(null).start();
        drawerContentView.setVisibility(VISIBLE);
        drawerContentView.animate().setListener(null).cancel();
        drawerContentView.animate().translationX(0).setDuration(fast ? 150 : 250).setListener(null).start();
    }

    public void closeDrawer(boolean fast) {
        if (drawerContentView == null) {
            return;
        }
        drawerOpened = false;
        final View scrim = drawerScrimView;
        scrim.animate().setListener(null).cancel();
        scrim.animate().alpha(0f).setDuration(fast ? 150 : 250).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                scrim.animate().setListener(null);
                if (!drawerOpened) {
                    scrim.setVisibility(GONE);
                }
            }
        }).start();
        final View content = drawerContentView;
        content.animate().setListener(null).cancel();
        content.animate().translationX(-AndroidUtilities.dp(DRAWER_WIDTH_DP)).setDuration(fast ? 150 : 250).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                content.animate().setListener(null);
                if (!drawerOpened) {
                    content.setVisibility(GONE);
                }
            }
        }).start();
    }

    private void setDrawerProgress(float progress) {
        if (drawerContentView == null) {
            return;
        }
        final int width = AndroidUtilities.dp(DRAWER_WIDTH_DP);
        drawerContentView.setVisibility(VISIBLE);
        drawerScrimView.setVisibility(VISIBLE);
        drawerContentView.setTranslationX(-width + width * progress);
        drawerScrimView.setAlpha(progress);
    }

    private boolean canStartDrawerSwipe(MotionEvent ev) {
        if (drawerContentView == null || drawerOpened) {
            return false;
        }
        if (!allowDrawerSwipe) {
            return false;
        }
        final float edge = AndroidUtilities.dp(20);
        if (LocaleController.isRTL) {
            return ev.getX() > getMeasuredWidth() - edge;
        }
        return ev.getX() < edge;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (drawerContentView == null) {
            return false;
        }
        final int width = AndroidUtilities.dp(DRAWER_WIDTH_DP);
        final int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            if (canStartDrawerSwipe(ev)) {
                maybeStartDragging = true;
                startedTrackingX = ev.getX();
                startedTrackingY = ev.getY();
                return true;
            }
            return false;
        } else if (action == MotionEvent.ACTION_MOVE && (maybeStartDragging || draggingDrawer)) {
            float dx = ev.getX() - startedTrackingX;
            float dy = ev.getY() - startedTrackingY;
            if (LocaleController.isRTL) {
                dx = -dx;
            }
            if (maybeStartDragging && dx > AndroidUtilities.dp(10) && Math.abs(dx) > Math.abs(dy)) {
                maybeStartDragging = false;
                draggingDrawer = true;
            }
            if (draggingDrawer) {
                setDrawerProgress(Math.max(0, Math.min(1, dx / width)));
                return true;
            }
            return true;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (draggingDrawer) {
                float dx = ev.getX() - startedTrackingX;
                if (LocaleController.isRTL) {
                    dx = -dx;
                }
                draggingDrawer = false;
                maybeStartDragging = false;
                if (action == MotionEvent.ACTION_UP && dx > width / 3f) {
                    openDrawer(true);
                } else {
                    drawerOpened = true; // force closeDrawer to run its animation back
                    closeDrawer(true);
                }
                return true;
            }
            maybeStartDragging = false;
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (parentActionBarLayout != null && parentActionBarLayout.checkTransitionAnimation()) {
            return true;
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN && canStartDrawerSwipe(ev)) {
            maybeStartDragging = true;
            startedTrackingX = ev.getX();
            startedTrackingY = ev.getY();
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE && maybeStartDragging) {
            float dx = ev.getX() - startedTrackingX;
            float dy = ev.getY() - startedTrackingY;
            if (LocaleController.isRTL) {
                dx = -dx;
            }
            if (dx > AndroidUtilities.dp(10) && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                maybeStartDragging = false;
                draggingDrawer = true;
                return true;
            }
        } else if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            maybeStartDragging = false;
        }
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        inLayout = true;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            try {
                child.layout(lp.leftMargin, lp.topMargin + getPaddingTop(), lp.leftMargin + child.getMeasuredWidth(), lp.topMargin + child.getMeasuredHeight() + getPaddingTop());
            } catch (Exception e) {
                FileLog.e(e);
                if (BuildVars.DEBUG_VERSION) {
                    throw e;
                }
            }
        }
        inLayout = false;
    }

    @Override
    public void requestLayout() {
        if (!inLayout) {
            super.requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(widthSize, heightSize);

        final int newDisplayWidth = widthSize
            - systemAndCutoutInsets.left
            - systemAndCutoutInsets.right;

        final int newDisplayHeight = heightSize
            - systemAndCutoutInsets.top
            - systemAndCutoutInsets.bottom;

        AndroidUtilities.displaySize.x = newDisplayWidth;
        AndroidUtilities.displaySize.y = newDisplayHeight;

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            final int contentWidthSpec;
            if (lp.width > 0) {
                contentWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            } else {
                contentWidthSpec = MeasureSpec.makeMeasureSpec(widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
            }
            final int contentHeightSpec;
            if (lp.height > 0) {
                contentHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            } else {
                contentHeightSpec = MeasureSpec.makeMeasureSpec(heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
            }
            if (child instanceof ActionBarLayout) {
                ActionBarLayout actionBarLayout = (ActionBarLayout) child;
                //fix keyboard measuring
                if (actionBarLayout.storyViewerAttached()) {
                    child.forceLayout();
                }
            }
            child.measure(contentWidthSpec, contentHeightSpec);
        }
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (actionBarLayout != null && actionBarLayout.getParent() == this) {
            actionBarLayout.parentDraw(this, canvas);
        }

        super.dispatchDraw(canvas);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private final Paint internalNavbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Paint getInternalNavbarPaint() {
        return internalNavbarPaint;
    }

    public void setInternalNavigationBarColor(int color) {
        if (internalNavbarPaint.getColor() != color) {
            internalNavbarPaint.setColor(color);
            invalidate();

            for (int a = 0, N = getChildCount(); a < N; a++) {
                getChildAt(a).invalidate();
            }
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (lastWindowInsetsCompat != null) {
            dispatchApplyWindowInsetsInternal(child, lastWindowInsetsCompat);
        }
    }

    private @Nullable WindowInsetsCompat lastWindowInsetsCompat;
    private @NonNull Insets systemAndCutoutInsets = Insets.NONE;
    private @NonNull Insets systemAndCutoutAndImeInsets = Insets.NONE;

    private void dispatchApplyWindowInsetsInternal(View child, WindowInsetsCompat insets) {
        boolean canApplyInsets = child instanceof ActionBarLayout || child.getTag() == null;
        if (canApplyInsets) {
            ViewCompat.dispatchApplyWindowInsets(child, insets);
        }
    }

    @NonNull
    private WindowInsetsCompat onApplyWindowInsets(@NonNull View ignoredV, @NonNull WindowInsetsCompat insets) {
        lastWindowInsetsCompat = insets;

        final Insets systemInsets = AndroidUtilities.getDefaultWindowInsets(insets, false);
        final Insets systemAndImeInsets = AndroidUtilities.getDefaultWindowInsets(insets, true);

        if (!systemAndCutoutInsets.equals(systemInsets) || !systemAndCutoutAndImeInsets.equals(systemAndImeInsets)) {
            AndroidUtilities.statusBarHeight = systemInsets.top;
            AndroidUtilities.navigationBarHeight = systemInsets.bottom;

            systemAndCutoutInsets = systemInsets;
            systemAndCutoutAndImeInsets = systemAndImeInsets;
            requestLayout();
        }

        for (int a = 0, N = getChildCount(); a < N; a++) {
            final View child = getChildAt(a);
            dispatchApplyWindowInsetsInternal(child, insets);
        }

        invalidate();
        return WindowInsetsCompat.CONSUMED;
    }
}
