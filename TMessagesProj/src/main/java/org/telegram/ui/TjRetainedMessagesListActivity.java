package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.TjLocale;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

/**
 * Flat, read-only list of everything TjDeletedMessagesActivity has kept - either the deleted
 * messages themselves, or the pre-edit text of messages that got edited. One screen, one query,
 * newest first; there's no per-chat drill-down because the volume this ever holds is small
 * enough (it's capped in settings) that a flat list is easier to scan than a folder tree.
 */
public class TjRetainedMessagesListActivity extends BaseFragment {

    private static final String ARG_MODE = "tj_mode";
    private static final int MODE_DELETED = 0;
    private static final int MODE_EDITS = 1;

    private int mode;
    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<MessageObject> messages = new ArrayList<>();
    private boolean loaded;

    public TjRetainedMessagesListActivity(Bundle args) {
        super(args);
    }

    public static TjRetainedMessagesListActivity forDeleted() {
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, MODE_DELETED);
        return new TjRetainedMessagesListActivity(args);
    }

    public static TjRetainedMessagesListActivity forEdits() {
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, MODE_EDITS);
        return new TjRetainedMessagesListActivity(args);
    }

    @Override
    public boolean onFragmentCreate() {
        mode = getArguments() != null ? getArguments().getInt(ARG_MODE, MODE_DELETED) : MODE_DELETED;
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(TjLocale.getString(mode == MODE_EDITS ? R.string.TjDeletedViewEdits : R.string.TjDeletedViewDeleted));
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

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        adapter = new ListAdapter();
        listView.setAdapter(adapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        loadMessages();

        return fragmentView;
    }

    private void loadMessages() {
        if (mode == MODE_EDITS) {
            getMessagesStorage().getTjAllMessageEdits(this::onMessagesLoaded);
        } else {
            getMessagesStorage().getTjAllDeletedMessages(this::onMessagesLoaded);
        }
    }

    private void onMessagesLoaded(ArrayList<MessageObject> loadedMessages) {
        messages.clear();
        messages.addAll(loadedMessages);
        loaded = true;
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private String previewText(MessageObject messageObject) {
        String text = messageObject.messageOwner != null ? messageObject.messageOwner.message : null;
        if (!TextUtils.isEmpty(text)) {
            return text;
        }
        return TjLocale.getString(R.string.TjDeletedMediaMessage);
    }

    private String subtitleText(MessageObject messageObject) {
        long dialogId = MessageObject.getDialogId(messageObject.messageOwner);
        String name = getMessagesController().getPeerName(dialogId);
        String date = LocaleController.formatDateChat(messageObject.messageOwner.date, false);
        if (TextUtils.isEmpty(name)) {
            return date;
        }
        return name + " · " + date;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == 1) {
                view = new TextInfoPrivacyCell(parent.getContext());
                view.setBackground(Theme.getThemedDrawableByKey(parent.getContext(), R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
            } else {
                TextDetailCell cell = new TextDetailCell(parent.getContext(), null, true, false);
                cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                view = cell;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == 1) {
                ((TextInfoPrivacyCell) holder.itemView).setText(TjLocale.getString(mode == MODE_EDITS ? R.string.TjDeletedViewEditsEmpty : R.string.TjDeletedViewDeletedEmpty));
                return;
            }
            MessageObject messageObject = messages.get(position);
            ((TextDetailCell) holder.itemView).setTextAndValue(previewText(messageObject), subtitleText(messageObject), position + 1 < messages.size());
        }

        @Override
        public int getItemCount() {
            return loaded && messages.isEmpty() ? 1 : messages.size();
        }

        @Override
        public int getItemViewType(int position) {
            return loaded && messages.isEmpty() ? 1 : 0;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
        }
    }
}
