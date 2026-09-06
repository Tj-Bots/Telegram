package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.TjLocale;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

/**
 * Everything ghost mode in one place: the global switch, what it suppresses, and the
 * instant-read behaviour - reachable from TJ Settings, and by long-pressing the drawer's
 * own Ghost Mode row.
 */
public class TjGhostSettingsActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CHECK = 1;
    private static final int VIEW_TYPE_SHADOW = 2;

    private static final int ID_MASTER = 1;
    private static final int ID_TYPING = 2;
    private static final int ID_ONLINE = 3;
    private static final int ID_READ = 4;
    private static final int ID_INSTANT_READ = 5;

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

    private static boolean isChecked(int id) {
        switch (id) {
            case ID_MASTER: return TjSettingsActivity.isGhostModeEnabled();
            case ID_TYPING: return TjSettingsActivity.isGhostHideTypingSetting();
            case ID_ONLINE: return TjSettingsActivity.isGhostHideOnlineSetting();
            case ID_READ: return TjSettingsActivity.isGhostHideReadReceiptsSetting();
            case ID_INSTANT_READ: return TjSettingsActivity.isGhostInstantReadEnabled();
        }
        return false;
    }

    private void setChecked(int id, boolean value) {
        switch (id) {
            case ID_MASTER:
                TjSettingsActivity.setGhostModeEnabled(value);
                notifyGhostModeChanged();
                if (BulletinFactory.canShowBulletin(this)) {
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.info, TjLocale.getString(value ? R.string.TjGhostModeOnBulletin : R.string.TjGhostModeOffBulletin)).show();
                }
                break;
            case ID_TYPING: TjSettingsActivity.setGhostHideTypingSetting(value); break;
            case ID_ONLINE: TjSettingsActivity.setGhostHideOnlineSetting(value); break;
            case ID_READ: TjSettingsActivity.setGhostHideReadReceiptsSetting(value); break;
            case ID_INSTANT_READ: TjSettingsActivity.setGhostInstantReadEnabled(value); break;
        }
    }

    /** The chat list draws the ghost badge, so every account needs telling. */
    private void notifyGhostModeChanged() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.dialogFiltersUpdated);
            }
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(TjLocale.getString(R.string.TjGhostMode));
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
            if (item.viewType != VIEW_TYPE_CHECK) {
                return;
            }
            boolean value = !isChecked(item.id);
            setChecked(item.id, value);
            ((TextCheckCell) view).setChecked(value);
        });

        return fragmentView;
    }

    private void updateItems() {
        items.clear();
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjGhostMode)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_MASTER, TjLocale.getString(R.string.TjGhostMode)));
        items.add(new Item(VIEW_TYPE_SHADOW, 0, TjLocale.getString(R.string.TjGhostModeInfo)));
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjGhostSuppresses)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_TYPING, TjLocale.getString(R.string.TjGhostTyping)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_ONLINE, TjLocale.getString(R.string.TjGhostOnline)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_READ, TjLocale.getString(R.string.TjGhostRead)));
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjGhostInstantRead)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_INSTANT_READ, TjLocale.getString(R.string.TjGhostInstantRead)));
        items.add(new Item(VIEW_TYPE_SHADOW, 0, TjLocale.getString(R.string.TjGhostInstantReadInfo)));
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
            } else {
                boolean divider = position + 1 < items.size() && items.get(position + 1).viewType == VIEW_TYPE_CHECK;
                ((TextCheckCell) holder.itemView).setTextAndCheck(item.text, isChecked(item.id), divider);
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
            return holder.getItemViewType() == VIEW_TYPE_CHECK;
        }
    }
}
