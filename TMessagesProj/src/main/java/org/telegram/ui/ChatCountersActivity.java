package org.telegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

/**
 * Shows how many chats of every kind the current account has. The numbers are counted off the
 * main thread, so the screen opens with a spinner and swaps to the list once the count is done.
 */
public class ChatCountersActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_VALUE = 1;
    private static final int VIEW_TYPE_SHADOW = 2;

    private FrameLayout contentView;
    private RecyclerListView listView;
    private RadialProgressView progressView;

    private final ArrayList<Item> items = new ArrayList<>();
    private boolean loaded;

    private static class Item {
        final int viewType;
        final CharSequence text;
        final CharSequence value;

        Item(int viewType, CharSequence text, CharSequence value) {
            this.viewType = viewType;
            this.text = text;
            this.value = value;
        }
    }

    private static class Counters {
        int total;
        int privateChats;
        int groups;
        int channels;
        int bots;
        int unread;
        int muted;
        int archived;
        int folders;
        int contacts;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.TjChatCounters));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        contentView = new FrameLayout(context);
        fragmentView = contentView;
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(new ListAdapter());
        listView.setVisibility(View.GONE);
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        progressView = new RadialProgressView(context);
        progressView.setProgressColor(Theme.getColor(Theme.key_progressCircle));
        contentView.addView(progressView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        loadCounters();

        return fragmentView;
    }

    private void loadCounters() {
        final int account = currentAccount;
        Utilities.globalQueue.postRunnable(() -> {
            final Counters counters = count(account);
            AndroidUtilities.runOnUIThread(() -> {
                if (isFinishing() || listView == null) {
                    return;
                }
                buildItems(counters);
                loaded = true;
                progressView.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
                listView.getAdapter().notifyDataSetChanged();
            });
        }, 250);
    }

    private Counters count(int account) {
        final Counters counters = new Counters();
        final MessagesController controller = MessagesController.getInstance(account);
        for (int folderId = 0; folderId <= 1; folderId++) {
            ArrayList<TLRPC.Dialog> dialogs;
            try {
                dialogs = new ArrayList<>(controller.getDialogs(folderId));
            } catch (Exception e) {
                continue;
            }
            for (int a = 0; a < dialogs.size(); a++) {
                TLRPC.Dialog dialog = dialogs.get(a);
                if (dialog == null || dialog instanceof TLRPC.TL_dialogFolder) {
                    continue;
                }
                final long dialogId = dialog.id;
                counters.total++;
                if (folderId == 1) {
                    counters.archived++;
                }
                if (dialog.unread_count > 0 || dialog.unread_mark) {
                    counters.unread++;
                }
                try {
                    if (controller.isDialogMuted(dialogId, 0)) {
                        counters.muted++;
                    }
                } catch (Exception ignore) {
                }
                if (DialogObject.isUserDialog(dialogId) || DialogObject.isEncryptedDialog(dialogId)) {
                    TLRPC.User user = DialogObject.isUserDialog(dialogId) ? controller.getUser(dialogId) : null;
                    if (user != null && user.bot) {
                        counters.bots++;
                    } else {
                        counters.privateChats++;
                    }
                } else {
                    TLRPC.Chat chat = controller.getChat(-dialogId);
                    if (chat != null && ChatObject.isChannel(chat) && !chat.megagroup) {
                        counters.channels++;
                    } else {
                        counters.groups++;
                    }
                }
            }
        }
        ArrayList<MessagesController.DialogFilter> filters = controller.getDialogFilters();
        if (filters != null) {
            for (int a = 0; a < filters.size(); a++) {
                MessagesController.DialogFilter filter = filters.get(a);
                if (filter != null && !filter.isDefault()) {
                    counters.folders++;
                }
            }
        }
        try {
            counters.contacts = org.telegram.messenger.ContactsController.getInstance(account).contacts.size();
        } catch (Exception ignore) {
        }
        return counters;
    }

    private void buildItems(Counters counters) {
        items.clear();
        items.add(new Item(VIEW_TYPE_HEADER, LocaleController.getString(R.string.TjChatCounters), null));
        items.add(new Item(VIEW_TYPE_VALUE, LocaleController.getString(R.string.TjTotalChats), format(counters.total)));
        items.add(new Item(VIEW_TYPE_VALUE, LocaleController.getString(R.string.TjPrivateChats), format(counters.privateChats)));
        items.add(new Item(VIEW_TYPE_VALUE, LocaleController.getString(R.string.TjGroups), format(counters.groups)));
        items.add(new Item(VIEW_TYPE_VALUE, LocaleController.getString(R.string.TjChannels), format(counters.channels)));
        items.add(new Item(VIEW_TYPE_VALUE, LocaleController.getString(R.string.TjBots), format(counters.bots)));
        items.add(new Item(VIEW_TYPE_SHADOW, null, null));
        items.add(new Item(VIEW_TYPE_VALUE, LocaleController.getString(R.string.TjUnreadChats), format(counters.unread)));
        items.add(new Item(VIEW_TYPE_VALUE, LocaleController.getString(R.string.TjMutedChats), format(counters.muted)));
        items.add(new Item(VIEW_TYPE_VALUE, LocaleController.getString(R.string.TjArchivedChats), format(counters.archived)));
        items.add(new Item(VIEW_TYPE_SHADOW, null, null));
        items.add(new Item(VIEW_TYPE_VALUE, LocaleController.getString(R.string.TjFoldersCount), format(counters.folders)));
        items.add(new Item(VIEW_TYPE_VALUE, LocaleController.getString(R.string.TjContactsCount), format(counters.contacts)));
        items.add(new Item(VIEW_TYPE_SHADOW, LocaleController.getString(R.string.TjChatCountersInfo), null));
    }

    private static String format(int value) {
        return LocaleController.formatNumber(value, ',');
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_HEADER) {
                view = new HeaderCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (viewType == VIEW_TYPE_VALUE) {
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
            } else {
                boolean divider = position + 1 < items.size() && items.get(position + 1).viewType == VIEW_TYPE_VALUE;
                ((TextSettingsCell) holder.itemView).setTextAndValue(item.text, item.value, divider);
            }
        }

        @Override
        public int getItemCount() {
            return loaded ? items.size() : 0;
        }

        @Override
        public int getItemViewType(int position) {
            if (position < 0 || position >= items.size()) {
                return VIEW_TYPE_SHADOW;
            }
            return items.get(position).viewType;
        }
    }
}
