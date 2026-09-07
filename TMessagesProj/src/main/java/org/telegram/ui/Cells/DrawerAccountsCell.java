package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Collections;

/** A bounded, independently scrollable account card for the side drawer. */
public class DrawerAccountsCell extends FrameLayout {

    public interface Listener {
        void onAccountClick(int account);
        void onAccountPreview(int account);
        void onAddAccount();
        void onAccountsReordered(ArrayList<Integer> accounts);
    }

    private static final int MAX_VISIBLE_ROWS = 5;
    private final RecyclerListView listView;
    private final AccountsAdapter adapter = new AccountsAdapter();
    private final ItemTouchHelper touchHelper;
    private final ArrayList<Integer> accounts = new ArrayList<>();
    private Listener listener;
    private boolean accountDragActive;

    public DrawerAccountsCell(Context context) {
        super(context);
        setClipToOutline(true);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Theme.multAlpha(Theme.getColor(Theme.key_chats_menuItemText), 0.07f));
        background.setCornerRadius(AndroidUtilities.dp(14));
        setBackground(background);

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setAdapter(adapter);
        listView.setNestedScrollingEnabled(true);
        listView.setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);
        addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                if (position < 0 || position >= accounts.size()) {
                    return makeMovementFlags(0, 0);
                }
                int account = accounts.get(position);
                int dragFlags = accountDragActive || account == UserConfig.selectedAccount || AndroidUtilities.isTablet()
                        ? ItemTouchHelper.UP | ItemTouchHelper.DOWN
                        : 0;
                return makeMovementFlags(dragFlags, 0);
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                accountDragActive = actionState == ItemTouchHelper.ACTION_STATE_DRAG;
                super.onSelectedChanged(viewHolder, actionState);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder source,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = source.getAdapterPosition();
                int to = target.getAdapterPosition();
                if (from < 0 || to < 0 || from >= accounts.size() || to >= accounts.size()) {
                    return false;
                }
                Collections.swap(accounts, from, to);
                adapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                accountDragActive = false;
                if (listener != null) {
                    listener.onAccountsReordered(new ArrayList<>(accounts));
                }
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        });
        touchHelper.attachToRecyclerView(listView);
        listView.setOnItemClickListener((view, position) -> {
            if (listener == null) {
                return;
            }
            if (position >= 0 && position < accounts.size()) {
                listener.onAccountClick(accounts.get(position));
            } else if (position == accounts.size() && accounts.size() < UserConfig.MAX_ACCOUNT_COUNT) {
                listener.onAddAccount();
            }
        });
        listView.setOnItemLongClickListener((view, position) -> {
            if (position < 0 || position >= accounts.size()) {
                return false;
            }
            int account = accounts.get(position);
            if (account == UserConfig.selectedAccount || AndroidUtilities.isTablet()) {
                // ItemTouchHelper owns the complete gesture for draggable rows. Starting it again
                // from RecyclerListView's long-click callback cancels the subsequent move stream.
                return false;
            } else if (listener != null) {
                listener.onAccountPreview(account);
            }
            return true;
        });
    }

    public void setAccounts(ArrayList<Integer> value, Listener listener) {
        this.listener = listener;
        accounts.clear();
        accounts.addAll(value);
        adapter.notifyDataSetChanged();
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int rows = accounts.size() + (accounts.size() < UserConfig.MAX_ACCOUNT_COUNT ? 1 : 0);
        int height = AndroidUtilities.dp(48 * Math.max(1, Math.min(MAX_VISIBLE_ROWS, rows)));
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    private class AccountsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 1) {
                DrawerAddCell add = new DrawerAddCell(parent.getContext());
                return new RecyclerListView.Holder(add);
            }
            AccountRow row = new AccountRow(parent.getContext());
            return new RecyclerListView.Holder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.itemView instanceof AccountRow) {
                AccountRow row = (AccountRow) holder.itemView;
                int account = accounts.get(position);
                row.userCell.setAccount(account);
            }
        }

        @Override
        public int getItemCount() {
            return accounts.size() + (accounts.size() < UserConfig.MAX_ACCOUNT_COUNT ? 1 : 0);
        }

        @Override
        public int getItemViewType(int position) {
            return position < accounts.size() ? 0 : 1;
        }
    }

    private class AccountRow extends FrameLayout {
        final DrawerUserCell userCell;

        AccountRow(Context context) {
            super(context);
            userCell = new DrawerUserCell(context);
            userCell.setReorderHandleVisible(false);
            addView(userCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48));
            setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(48), MeasureSpec.EXACTLY));
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            info.addAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
        }
    }
}
