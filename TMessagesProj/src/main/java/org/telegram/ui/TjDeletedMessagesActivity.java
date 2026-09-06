package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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

    private void showIconPicker() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] options = new CharSequence[]{
            TjLocale.getString(R.string.TjDeletedIconTrash),
            TjLocale.getString(R.string.TjDeletedIconCross)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(TjLocale.getString(R.string.TjDeletedIcon));
        builder.setItems(options, (dialog, which) -> {
            TjSettingsActivity.setDeletedMessagesIcon(which);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showColorPicker() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] options = new CharSequence[]{
            TjLocale.getString(R.string.TjDeletedColorGrey),
            TjLocale.getString(R.string.TjDeletedColorRed),
            TjLocale.getString(R.string.TjDeletedColorBlack)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(TjLocale.getString(R.string.TjDeletedColor));
        builder.setItems(options, (dialog, which) -> {
            TjSettingsActivity.setDeletedMessagesColor(which);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
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
}
