package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PopupSwipeBackLayout;

/**
 * A page inside the video player's own settings menu: a Back row, a divider, then whatever the
 * caller puts in {@link #buttonsLayout}.
 *
 * This is the shape ChooseQualityLayout and ChooseDownloadQualityLayout already use, factored out
 * so the audio, subtitle and subtitle-settings pages are all the same thing. The parent attaches
 * one with ActionBarMenuItem.addSwipeBackItem, which adds a row with a chevron that slides this
 * page in - the sub-menu behaviour rather than a floating dialog.
 */
public class TjPlayerSubmenu {

    public final ActionBarPopupWindow.ActionBarPopupWindowLayout layout;
    public final LinearLayout buttonsLayout;

    public TjPlayerSubmenu(Context context, PopupSwipeBackLayout swipeBackLayout) {
        layout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, 0, null);
        layout.setFitItems(true);

        ActionBarMenuSubItem backItem = ActionBarMenuItem.addItem(layout, R.drawable.msg_arrow_back, getString(R.string.Back), false, null);
        backItem.setOnClickListener(view -> {
            if (swipeBackLayout != null) {
                swipeBackLayout.closeForeground();
            }
        });
        backItem.setColors(0xfffafafa, 0xfffafafa);
        backItem.setSelectorColor(0x0fffffff);

        FrameLayout gap = new FrameLayout(context);
        gap.setMinimumWidth(dp(196));
        gap.setBackgroundColor(0xff181818);
        layout.addView(gap);
        LinearLayout.LayoutParams gapParams = (LinearLayout.LayoutParams) gap.getLayoutParams();
        if (LocaleController.isRTL) {
            gapParams.gravity = Gravity.RIGHT;
        }
        gapParams.width = LayoutHelper.MATCH_PARENT;
        gapParams.height = dp(8);
        gap.setLayoutParams(gapParams);

        buttonsLayout = new LinearLayout(context);
        buttonsLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(buttonsLayout);
    }

    /** A checkable row, coloured the way every other row in this menu is. */
    public ActionBarMenuSubItem addRow(CharSequence text, boolean checked, Runnable onClick) {
        ActionBarMenuSubItem item = ActionBarMenuItem.addItem(buttonsLayout, 0, text, true, null);
        item.setChecked(checked);
        item.setColors(0xfffafafa, 0xfffafafa);
        item.setSelectorColor(0x0fffffff);
        item.setOnClickListener(v -> onClick.run());
        return item;
    }

    public void clear() {
        buttonsLayout.removeAllViews();
    }
}
