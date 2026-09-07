package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.TjLocale;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

/**
 * Read-only chat-bubble timeline of one message's retained edit history, oldest version first,
 * ending with the message as it is now. Local-only, backed by the tj_message_edits table.
 */
public class TjEditHistoryActivity extends BaseFragment {

    private final long dialogId;
    private final int messageId;
    private final MessageObject currentMessage;
    private final ArrayList<MessageObject> historyMessages = new ArrayList<>();

    private RecyclerListView listView;
    private ListAdapter adapter;

    public TjEditHistoryActivity(long dialogId, int messageId, MessageObject currentMessage) {
        super();
        this.dialogId = dialogId;
        this.messageId = messageId;
        this.currentMessage = currentMessage;
        if (currentMessage != null) {
            historyMessages.add(currentMessage);
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(TjLocale.getString(R.string.TjDeletedViewEdits));
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
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        listView.setLayoutManager(layoutManager);
        adapter = new ListAdapter(context);
        listView.setAdapter(adapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        getMessagesStorage().getTjMessageEdits(dialogId, messageId, olderVersions -> {
            if (olderVersions != null && !olderVersions.isEmpty()) {
                historyMessages.addAll(0, olderVersions);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        });

        return fragmentView;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ChatMessageCell cell = new ChatMessageCell(context, currentAccount, true, null, getResourceProvider());
            cell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {
                @Override
                public boolean canPerformActions() {
                    return false;
                }
            });
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position < 0 || position >= historyMessages.size()) {
                return;
            }
            MessageObject messageObject = historyMessages.get(position);
            ChatMessageCell cell = (ChatMessageCell) holder.itemView;
            cell.setParentViewSize(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y);
            cell.setMessageObject(messageObject, null, true, true, position == 0);
        }

        @Override
        public int getItemCount() {
            return historyMessages.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
        }
    }
}
