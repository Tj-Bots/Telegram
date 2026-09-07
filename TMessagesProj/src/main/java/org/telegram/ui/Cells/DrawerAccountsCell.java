package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
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
        // The drawer's own RecyclerView (and the ItemTouchHelper it carries for account
        // reordering) otherwise claims the gesture before this nested, same-orientation list
        // ever sees it - this card would only ever show its first MAX_VISIBLE_ROWS rows with
        // no way to reach the rest.
        listView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        rv.getParent().requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        rv.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                return false;
            }
        });
        addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    private void startAccountDrag(AccountRow source, int account, float rawY) {
        listView.stopScroll();
        listView.requestDisallowInterceptTouchEvent(true);

        DrawerUserCell dragView = new DrawerUserCell(getContext());
        dragView.setAccount(account);
        dragView.setReorderHandleVisible(false);
        dragView.setBackground(Theme.createRoundRectDrawable(
                AndroidUtilities.dp(10),
                Theme.multAlpha(Theme.getColor(Theme.key_chats_menuItemText), 0.10f)));
        addView(dragView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48));

        int[] sourceLocation = new int[2];
        source.getLocationOnScreen(sourceLocation);
        int[] cardLocation = new int[2];
        getLocationOnScreen(cardLocation);
        float sourceTop = sourceLocation[1] - cardLocation[1];

        dragView.setY(sourceTop);
        dragView.setTranslationZ(AndroidUtilities.dp(8));
        dragView.setScaleX(1.02f);
        dragView.setScaleY(1.02f);
        source.setAlpha(0f);
        source.setHasTransientState(true);
        activeDrag = new AccountDragState(account, source, dragView,
                Math.max(0, Math.min(source.getHeight(), rawY - sourceLocation[1])));
    }

    private void moveDraggedAccount(AccountDragState state, float rawY) {
        if (activeDrag != state) {
            return;
        }
        int[] cardLocation = new int[2];
        getLocationOnScreen(cardLocation);
        float y = rawY - cardLocation[1] - state.touchOffsetY;
        y = Math.max(0, Math.min(getHeight() - state.dragView.getHeight(), y));
        state.dragView.setY(y);

        float listY = y - listView.getTop() + state.dragView.getHeight() / 2f;
        int edge = AndroidUtilities.dp(36);
        if (listY < edge) {
            listView.scrollBy(0, -AndroidUtilities.dp(12));
        } else if (listY > listView.getHeight() - edge) {
            listView.scrollBy(0, AndroidUtilities.dp(12));
        }

        float boundedY = Math.max(1, Math.min(listView.getHeight() - 1, listY));
        View targetView = listView.findChildViewUnder(listView.getWidth() / 2f, boundedY);
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
        listView.requestDisallowInterceptTouchEvent(false);
        if (state.changed && listener != null) {
            listener.onAccountsReordered(new ArrayList<>(accounts));
        }

        int finalPosition = accounts.indexOf(state.account);
        RecyclerView.ViewHolder finalHolder = finalPosition >= 0
                ? listView.findViewHolderForAdapterPosition(finalPosition) : null;
        View finalView = finalHolder != null ? finalHolder.itemView : state.source;
        float finalY = finalView.getTop() + listView.getTop();
        state.dragView.animate()
                .y(finalY)
                .alpha(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(120)
                .withEndAction(() -> {
                    removeView(state.dragView);
                    state.source.setHasTransientState(false);
                    state.source.setAlpha(1f);
                    RecyclerView.ViewHolder holder = finalPosition >= 0
                            ? listView.findViewHolderForAdapterPosition(finalPosition) : null;
                    if (holder != null) {
                        holder.itemView.setAlpha(1f);
                    }
                })
                .start();
    }

    private static class AccountDragState {
        final int account;
        final AccountRow source;
        final DrawerUserCell dragView;
        final float touchOffsetY;
        boolean changed;

        AccountDragState(int account, AccountRow source, DrawerUserCell dragView, float touchOffsetY) {
            this.account = account;
            this.source = source;
            this.dragView = dragView;
            this.touchOffsetY = touchOffsetY;
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
                row.bind(account);
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
        private float lastRawY;
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
                        lastRawY = event.getRawY();
                        longPressRunnable = this::handleLongPress;
                        userCell.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
                        break;
                    case MotionEvent.ACTION_MOVE:
                        lastRawY = event.getRawY();
                        if (activeDrag != null && activeDrag.source == this) {
                            moveDraggedAccount(activeDrag, lastRawY);
                            return true;
                        }
                        if (!longPressHandled && (Math.abs(event.getX() - downX) > touchSlop
                                || Math.abs(event.getY() - downY) > touchSlop)) {
                            cancelLongPressCheck();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (activeDrag != null && activeDrag.source == this) {
                            AccountDragState state = activeDrag;
                            cancelLongPressCheck();
                            finishAccountDrag(state);
                            longPressHandled = false;
                            return true;
                        }
                        cancelLongPressCheck();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        if (activeDrag != null && activeDrag.source == this) {
                            AccountDragState state = activeDrag;
                            cancelLongPressCheck();
                            finishAccountDrag(state);
                            longPressHandled = false;
                            return true;
                        }
                        cancelLongPressCheck();
                        break;
                }
                // DrawerUserCell remains the touch target, preserving its normal click behavior.
                return false;
            });
        }

        void bind(int account) {
            cancelLongPressCheck();
            boundAccount = account;
            longPressHandled = false;
            setAlpha(activeDrag != null && activeDrag.account == account ? 0f : 1f);
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
                startAccountDrag(this, account, lastRawY);
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
