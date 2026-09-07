package org.telegram.ui.Cells;

import android.content.ClipData;
import android.content.Context;
import android.os.Build;
import android.graphics.drawable.GradientDrawable;
import android.view.DragEvent;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

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
    private final ArrayList<Integer> accounts = new ArrayList<>();
    private Listener listener;
    private AccountDragState activeDrag;

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
        listView.setOnDragListener((view, event) -> handleAccountDrag(event));
        addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    private boolean handleAccountDrag(DragEvent event) {
        Object stateObject = event.getLocalState();
        if (!(stateObject instanceof AccountDragState)) {
            return false;
        }
        AccountDragState state = (AccountDragState) stateObject;
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return true;
            case DragEvent.ACTION_DRAG_LOCATION:
                if (activeDrag != state) {
                    return false;
                }
                moveDraggedAccount(state, event.getX(), event.getY());
                return true;
            case DragEvent.ACTION_DROP:
                finishAccountDrag(state);
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                finishAccountDrag(state);
                return true;
            default:
                return true;
        }
    }

    private void moveDraggedAccount(AccountDragState state, float x, float y) {
        int edge = AndroidUtilities.dp(36);
        if (y < edge) {
            listView.scrollBy(0, -AndroidUtilities.dp(12));
        } else if (y > listView.getHeight() - edge) {
            listView.scrollBy(0, AndroidUtilities.dp(12));
        }

        float boundedY = Math.max(1, Math.min(listView.getHeight() - 1, y));
        View targetView = listView.findChildViewUnder(x, boundedY);
        if (targetView == null) {
            return;
        }
        int from = accounts.indexOf(state.account);
        int to = listView.getChildAdapterPosition(targetView);
        if (to >= accounts.size()) {
            to = accounts.size() - 1;
        }
        if (from < 0 || to < 0 || from == to) {
            return;
        }
        accounts.remove(from);
        accounts.add(to, state.account);
        state.changed = true;
        adapter.notifyItemMoved(from, to);
    }

    private void finishAccountDrag(AccountDragState state) {
        if (activeDrag != state) {
            return;
        }
        activeDrag = null;
        state.source.setAlpha(1f);
        listView.requestDisallowInterceptTouchEvent(false);
        if (state.changed && listener != null) {
            listener.onAccountsReordered(new ArrayList<>(accounts));
        }
    }

    private static class AccountDragState {
        final int account;
        final View source;
        boolean changed;

        AccountDragState(int account, View source) {
            this.account = account;
            this.source = source;
        }
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
                add.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAddAccount();
                    }
                });
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
                row.bind(holder, account);
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
        private final int touchSlop;
        private int boundAccount = -1;
        private float downX;
        private float downY;
        private boolean longPressHandled;
        private Runnable longPressRunnable;

        AccountRow(Context context) {
            super(context);
            touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            userCell = new DrawerUserCell(context);
            userCell.setReorderHandleVisible(false);
            addView(userCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48));
            setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
            userCell.setOnClickListener(v -> {
                if (longPressHandled) {
                    longPressHandled = false;
                    return;
                }
                if (listener != null && boundAccount >= 0) {
                    listener.onAccountClick(boundAccount);
                }
            });
            userCell.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        cancelLongPressCheck();
                        longPressHandled = false;
                        downX = event.getX();
                        downY = event.getY();
                        longPressRunnable = this::handleLongPress;
                        userCell.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (!longPressHandled && (Math.abs(event.getX() - downX) > touchSlop
                                || Math.abs(event.getY() - downY) > touchSlop)) {
                            cancelLongPressCheck();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        cancelLongPressCheck();
                        break;
                }
                // DrawerUserCell remains the touch target, preserving its normal click behavior.
                return false;
            });
        }

        void bind(RecyclerView.ViewHolder holder, int account) {
            cancelLongPressCheck();
            boundAccount = account;
            longPressHandled = false;
        }

        private void handleLongPress() {
            longPressRunnable = null;
            int account = boundAccount;
            if (account < 0 || accounts.indexOf(account) < 0) {
                return;
            }
            longPressHandled = true;
            try {
                userCell.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            } catch (Exception ignore) {
            }
            if (account == UserConfig.selectedAccount || AndroidUtilities.isTablet()) {
                listView.stopScroll();
                listView.requestDisallowInterceptTouchEvent(true);
                AccountDragState state = new AccountDragState(account, userCell);
                activeDrag = state;
                ClipData data = ClipData.newPlainText("tjgram-account-order", Integer.toString(account));
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(userCell);
                boolean started;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    started = userCell.startDragAndDrop(data, shadow, state, 0);
                } else {
                    started = userCell.startDrag(data, shadow, state, 0);
                }
                if (started) {
                    userCell.setAlpha(0.35f);
                } else {
                    activeDrag = null;
                    listView.requestDisallowInterceptTouchEvent(false);
                    longPressHandled = false;
                }
            } else if (listener != null) {
                listener.onAccountPreview(account);
            }
        }

        private void cancelLongPressCheck() {
            if (longPressRunnable != null) {
                userCell.removeCallbacks(longPressRunnable);
                longPressRunnable = null;
            }
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
