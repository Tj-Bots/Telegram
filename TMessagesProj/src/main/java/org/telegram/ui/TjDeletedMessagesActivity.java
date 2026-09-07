package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.TjLocale;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

/**
 * Local retention of messages the server reports deleted, and of prior versions of edited
 * messages - nothing here ever leaves the device. Off by default.
 */
public class TjDeletedMessagesActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CHECK = 1;
    private static final int VIEW_TYPE_SHADOW = 2;
    private static final int VIEW_TYPE_SETTING = 3;
    private static final int VIEW_TYPE_PREVIEW = 4;

    private static final int ID_ENABLED = 1;
    private static final int ID_ICON = 2;
    private static final int ID_COLOR = 3;
    private static final int ID_DIM = 4;
    private static final int ID_STORAGE_CAP = 5;
    private static final int ID_VIEW_DELETED = 6;
    private static final int ID_VIEW_EDITS = 7;
    private static final int ID_CLEAR_ALL = 8;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<Item> items = new ArrayList<>();

    private static class Item {
        final int viewType;
        final int id;
        final CharSequence text;

        Item(int viewType, int id, CharSequence text) {
            this.viewType = viewType;
            this.id = id;
            this.text = text;
        }
    }

    private static String iconName(int icon) {
        return TjLocale.getString(icon == TjSettingsActivity.DELETED_ICON_CROSS ? R.string.TjDeletedIconCross : R.string.TjDeletedIconTrash);
    }

    private static String colorName(int color) {
        switch (color) {
            case TjSettingsActivity.DELETED_COLOR_RED: return TjLocale.getString(R.string.TjDeletedColorRed);
            case TjSettingsActivity.DELETED_COLOR_BLACK: return TjLocale.getString(R.string.TjDeletedColorBlack);
            default: return TjLocale.getString(R.string.TjDeletedColorGrey);
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(TjLocale.getString(R.string.TjDeletedMessages));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        updateItems();

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        adapter = new ListAdapter();
        listView.setAdapter(adapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= items.size()) {
                return;
            }
            Item item = items.get(position);
            switch (item.id) {
                case ID_ENABLED: {
                    boolean value = !TjSettingsActivity.isDeletedMessagesEnabled();
                    TjSettingsActivity.setDeletedMessagesEnabled(value);
                    ((TextCheckCell) view).setChecked(value);
                    break;
                }
                case ID_DIM: {
                    boolean value = !TjSettingsActivity.isDeletedMessagesDimmed();
                    TjSettingsActivity.setDeletedMessagesDimmed(value);
                    ((TextCheckCell) view).setChecked(value);
                    break;
                }
                case ID_ICON:
                    showIconPicker();
                    break;
                case ID_COLOR:
                    showColorPicker();
                    break;
                case ID_STORAGE_CAP:
                    showStorageCapPicker();
                    break;
                case ID_VIEW_DELETED:
                    presentFragment(TjRetainedMessagesListActivity.forDeleted());
                    break;
                case ID_VIEW_EDITS:
                    presentFragment(TjRetainedMessagesListActivity.forEdits());
                    break;
                case ID_CLEAR_ALL:
                    confirmClearAll();
                    break;
            }
        });

        return fragmentView;
    }

    /** Small grid of the actual icon choices, tap to pick - mirrors FilterCreateActivity's folder icon picker. */
    private void showIconPicker() {
        if (getParentActivity() == null) {
            return;
        }
        int[] icons = new int[]{R.drawable.msg_delete, R.drawable.msg_close};
        int current = TjSettingsActivity.getDeletedMessagesIcon();

        LinearLayout content = new LinearLayout(getParentActivity());
        content.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(getParentActivity());
        titleView.setText(TjLocale.getString(R.string.TjDeletedIcon));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setPadding(dp(22), dp(16), dp(22), dp(8));
        content.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        final BottomSheet[] sheet = new BottomSheet[1];
        GridLayout grid = new GridLayout(getParentActivity());
        grid.setColumnCount(8);
        grid.setPadding(dp(12), dp(4), dp(12), dp(16));
        for (int i = 0; i < icons.length; i++) {
            int iconValue = i;
            ImageView icon = new ImageView(getParentActivity());
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            icon.setImageResource(icons[i]);
            boolean selected = iconValue == current;
            icon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(selected ? Theme.key_dialogTextBlue : Theme.key_dialogIcon), PorterDuff.Mode.MULTIPLY));
            icon.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_CIRCLE_20DP));
            icon.setPadding(dp(8), dp(8), dp(8), dp(8));
            icon.setOnClickListener(v -> {
                TjSettingsActivity.setDeletedMessagesIcon(iconValue);
                if (sheet[0] != null) {
                    sheet[0].dismiss();
                }
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(40);
            lp.height = dp(40);
            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            grid.addView(icon, lp);
        }

        ScrollView scroll = new ScrollView(getParentActivity());
        scroll.addView(grid);
        content.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setCustomView(content);
        sheet[0] = builder.create();
        showDialog(sheet[0]);
    }

    /** Small grid of actual colored circles, tap to pick. */
    private void showColorPicker() {
        if (getParentActivity() == null) {
            return;
        }
        int[] colors = new int[]{0xFF9E9E9E, 0xFFE53935, 0xFF000000};
        int current = TjSettingsActivity.getDeletedMessagesColor();

        LinearLayout content = new LinearLayout(getParentActivity());
        content.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(getParentActivity());
        titleView.setText(TjLocale.getString(R.string.TjDeletedColor));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setPadding(dp(22), dp(16), dp(22), dp(8));
        content.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        final BottomSheet[] sheet = new BottomSheet[1];
        GridLayout grid = new GridLayout(getParentActivity());
        grid.setColumnCount(8);
        grid.setPadding(dp(12), dp(4), dp(12), dp(16));
        for (int i = 0; i < colors.length; i++) {
            int colorValue = i;
            boolean selected = colorValue == current;
            FrameLayout swatch = new FrameLayout(getParentActivity());
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(colors[i]);
            if (selected) {
                circle.setStroke(dp(2), Theme.getColor(Theme.key_dialogTextBlue));
            }
            swatch.setBackground(circle);
            swatch.setOnClickListener(v -> {
                TjSettingsActivity.setDeletedMessagesColor(colorValue);
                if (sheet[0] != null) {
                    sheet[0].dismiss();
                }
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(32);
            lp.height = dp(32);
            lp.setMargins(dp(6), dp(6), dp(6), dp(6));
            grid.addView(swatch, lp);
        }

        ScrollView scroll = new ScrollView(getParentActivity());
        scroll.addView(grid);
        content.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setCustomView(content);
        sheet[0] = builder.create();
        showDialog(sheet[0]);
    }

    private void showStorageCapPicker() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] options = new CharSequence[10];
        for (int i = 0; i < 10; i++) {
            options[i] = (i + 1) + " GB";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(TjLocale.getString(R.string.TjDeletedStorageCap));
        builder.setItems(options, (dialog, which) -> {
            TjSettingsActivity.setDeletedMessagesStorageCapGb(which + 1);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void confirmClearAll() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(TjLocale.getString(R.string.TjDeletedClearAll));
        builder.setMessage(TjLocale.getString(R.string.TjDeletedClearAllConfirm));
        builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
            getMessagesStorage().clearTjRetainedMessages(freed -> {
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                BulletinFactory.of(this).createSimpleBulletin(R.raw.ic_delete, TjLocale.formatString(R.string.TjClearedFromCache, AndroidUtilities.formatFileSize(freed))).show();
            });
        });
        AlertDialog dialog = builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null).create();
        showDialog(dialog);
        TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(getThemedColor(Theme.key_text_RedBold));
        }
    }

    private void updateItems() {
        items.clear();
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjDeletedMessages)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_ENABLED, TjLocale.getString(R.string.TjDeletedEnable)));
        items.add(new Item(VIEW_TYPE_SHADOW, 0, TjLocale.getString(R.string.TjDeletedEnableInfo)));
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjDeletedAppearance)));
        items.add(new Item(VIEW_TYPE_PREVIEW, 0, null));
        items.add(new Item(VIEW_TYPE_SETTING, ID_ICON, TjLocale.getString(R.string.TjDeletedIcon)));
        items.add(new Item(VIEW_TYPE_SETTING, ID_COLOR, TjLocale.getString(R.string.TjDeletedColor)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_DIM, TjLocale.getString(R.string.TjDeletedDim)));
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjDeletedStorageHeader)));
        items.add(new Item(VIEW_TYPE_SETTING, ID_STORAGE_CAP, TjLocale.getString(R.string.TjDeletedStorageCap)));
        items.add(new Item(VIEW_TYPE_SETTING, ID_CLEAR_ALL, TjLocale.getString(R.string.TjDeletedClearAll)));
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjDeletedBrowseHeader)));
        items.add(new Item(VIEW_TYPE_SETTING, ID_VIEW_DELETED, TjLocale.getString(R.string.TjDeletedViewDeleted)));
        items.add(new Item(VIEW_TYPE_SETTING, ID_VIEW_EDITS, TjLocale.getString(R.string.TjDeletedViewEdits)));
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_HEADER) {
                view = new HeaderCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (viewType == VIEW_TYPE_CHECK) {
                view = new TextCheckCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (viewType == VIEW_TYPE_SETTING) {
                view = new TextSettingsCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (viewType == VIEW_TYPE_PREVIEW) {
                view = new PreviewCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else {
                view = new TextInfoPrivacyCell(parent.getContext());
                view.setBackground(Theme.getThemedDrawableByKey(parent.getContext(), R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position < 0 || position >= items.size()) {
                return;
            }
            Item item = items.get(position);
            if (item.viewType == VIEW_TYPE_HEADER) {
                ((HeaderCell) holder.itemView).setText(item.text);
            } else if (item.viewType == VIEW_TYPE_SHADOW) {
                ((TextInfoPrivacyCell) holder.itemView).setText(item.text);
            } else if (item.viewType == VIEW_TYPE_SETTING) {
                String value;
                if (item.id == ID_ICON) {
                    value = iconName(TjSettingsActivity.getDeletedMessagesIcon());
                } else if (item.id == ID_COLOR) {
                    value = colorName(TjSettingsActivity.getDeletedMessagesColor());
                } else if (item.id == ID_STORAGE_CAP) {
                    value = TjSettingsActivity.getDeletedMessagesStorageCapGb() + " GB";
                } else {
                    value = "";
                }
                ((TextSettingsCell) holder.itemView).setTextAndValue(item.text, value, false);
            } else if (item.viewType == VIEW_TYPE_PREVIEW) {
                ((PreviewCell) holder.itemView).refresh();
            } else {
                boolean divider = position + 1 < items.size() && items.get(position + 1).viewType == VIEW_TYPE_CHECK;
                boolean checked = item.id == ID_ENABLED ? TjSettingsActivity.isDeletedMessagesEnabled() : TjSettingsActivity.isDeletedMessagesDimmed();
                ((TextCheckCell) holder.itemView).setTextAndCheck(item.text, checked, divider);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (position < 0 || position >= items.size()) {
                return VIEW_TYPE_SHADOW;
            }
            return items.get(position).viewType;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int t = holder.getItemViewType();
            return t == VIEW_TYPE_CHECK || t == VIEW_TYPE_SETTING;
        }
    }

    /** A tiny sample bubble showing the icon/color/dim settings applied live, as they're picked. */
    private static class PreviewCell extends FrameLayout {
        private final TextView bubbleText;
        private final ImageView badge;

        PreviewCell(Context context) {
            super(context);
            setPadding(dp(16), dp(14), dp(16), dp(14));

            FrameLayout bubble = new FrameLayout(context);
            GradientDrawable bubbleBg = new GradientDrawable();
            bubbleBg.setShape(GradientDrawable.RECTANGLE);
            bubbleBg.setCornerRadius(dp(14));
            bubbleBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
            bubble.setBackground(bubbleBg);
            bubble.setPadding(dp(14), dp(10), dp(14), dp(10));

            bubbleText = new TextView(context);
            bubbleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            bubbleText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            bubbleText.setText(TjLocale.getString(R.string.TjDeletedPreviewSample));
            bubble.addView(bubbleText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL, LocaleController.isRTL ? 22 : 0, 0, LocaleController.isRTL ? 0 : 22, 0));

            badge = new ImageView(context);
            badge.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            bubble.addView(badge, LayoutHelper.createFrame(18, 18,
                (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

            addView(bubble, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        }

        void refresh() {
            int color = TjSettingsActivity.getDeletedMessagesColorArgb();
            badge.setImageResource(TjSettingsActivity.getDeletedMessagesIconDrawable());
            badge.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            float alpha = TjSettingsActivity.isDeletedMessagesDimmed() ? 0.5f : 1f;
            bubbleText.setAlpha(alpha);
            badge.setAlpha(alpha);
        }
    }
}
