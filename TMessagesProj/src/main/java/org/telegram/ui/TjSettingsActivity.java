package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.TjLocale;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class TjSettingsActivity extends BaseFragment {

    private static final String PREFS_NAME = "tjsettings";

    private static final String KEY_SHOW_CALL_BUTTON = "show_call_button";
    private static final String KEY_HIDE_PHONE_NUMBER = "hide_phone_number";
    private static final String KEY_BOT_API_IDS = "bot_api_ids";
    private static final String KEY_ACCOUNT_ORDER_PREFIX = "account_order_";

    private static final String KEY_MENU_MESSAGE_INFO = "menu_message_info";
    private static final String KEY_MENU_COPY_LINK = "menu_copy_message_link";
    private static final String KEY_MENU_COPY_IMAGE = "menu_copy_image";
    private static final String KEY_MENU_COPY_THUMB = "menu_copy_thumbnail";
    private static final String KEY_MENU_SAVE_TO_SAVED = "menu_save_to_saved";
    private static final String KEY_MENU_FORWARD_NO_TAG = "menu_forward_without_tag";

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isShowCallButtonEnabled() {
        return getPrefs().getBoolean(KEY_SHOW_CALL_BUTTON, false);
    }

    public static void setShowCallButtonEnabled(boolean value) {
        getPrefs().edit().putBoolean(KEY_SHOW_CALL_BUTTON, value).apply();
    }

    public static boolean isHidePhoneNumberEnabled() {
        return getPrefs().getBoolean(KEY_HIDE_PHONE_NUMBER, false);
    }

    public static void setHidePhoneNumberEnabled(boolean value) {
        getPrefs().edit().putBoolean(KEY_HIDE_PHONE_NUMBER, value).apply();
    }

    /**
     * When enabled (the default) chat ids are shown the way the Bot API reports them:
     * -100... for supergroups and channels, -... for legacy groups.
     */
    public static boolean isBotApiIdsEnabled() {
        return getPrefs().getBoolean(KEY_BOT_API_IDS, true);
    }

    public static void setBotApiIdsEnabled(boolean value) {
        getPrefs().edit().putBoolean(KEY_BOT_API_IDS, value).apply();
    }

    public static boolean isMessageInfoEnabled() {
        return getPrefs().getBoolean(KEY_MENU_MESSAGE_INFO, true);
    }

    public static boolean isCopyMessageLinkEnabled() {
        return getPrefs().getBoolean(KEY_MENU_COPY_LINK, true);
    }

    public static boolean isCopyImageEnabled() {
        return getPrefs().getBoolean(KEY_MENU_COPY_IMAGE, true);
    }

    public static boolean isCopyThumbnailEnabled() {
        return getPrefs().getBoolean(KEY_MENU_COPY_THUMB, true);
    }

    public static boolean isSaveToSavedEnabled() {
        return getPrefs().getBoolean(KEY_MENU_SAVE_TO_SAVED, true);
    }

    public static boolean isForwardWithoutTagEnabled() {
        return getPrefs().getBoolean(KEY_MENU_FORWARD_NO_TAG, true);
    }

    /**
     * Position of an account in the side menu. Accounts that were never reordered keep
     * a large order so they stay after the ones the user moved around.
     */
    public static int getAccountOrder(int account) {
        return getPrefs().getInt(KEY_ACCOUNT_ORDER_PREFIX + account, 1000 + account);
    }

    public static void setAccountOrder(int account, int order) {
        getPrefs().edit().putInt(KEY_ACCOUNT_ORDER_PREFIX + account, order).apply();
    }

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<Item> items = new ArrayList<>();

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CHECK = 1;
    private static final int VIEW_TYPE_SHADOW = 2;

    private static final int ID_HIDE_PHONE = 1;
    private static final int ID_BOT_API_IDS = 2;
    private static final int ID_SHOW_CALL_BUTTON = 3;
    private static final int ID_MENU_MESSAGE_INFO = 4;
    private static final int ID_MENU_COPY_LINK = 5;
    private static final int ID_MENU_COPY_IMAGE = 6;
    private static final int ID_MENU_COPY_THUMB = 7;
    private static final int ID_MENU_SAVE_TO_SAVED = 8;
    private static final int ID_MENU_FORWARD_NO_TAG = 9;

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
            case ID_HIDE_PHONE: return isHidePhoneNumberEnabled();
            case ID_BOT_API_IDS: return isBotApiIdsEnabled();
            case ID_SHOW_CALL_BUTTON: return isShowCallButtonEnabled();
            case ID_MENU_MESSAGE_INFO: return isMessageInfoEnabled();
            case ID_MENU_COPY_LINK: return isCopyMessageLinkEnabled();
            case ID_MENU_COPY_IMAGE: return isCopyImageEnabled();
            case ID_MENU_COPY_THUMB: return isCopyThumbnailEnabled();
            case ID_MENU_SAVE_TO_SAVED: return isSaveToSavedEnabled();
            case ID_MENU_FORWARD_NO_TAG: return isForwardWithoutTagEnabled();
        }
        return false;
    }

    private static void setChecked(int id, boolean value) {
        String key = null;
        switch (id) {
            case ID_HIDE_PHONE: key = KEY_HIDE_PHONE_NUMBER; break;
            case ID_BOT_API_IDS: key = KEY_BOT_API_IDS; break;
            case ID_SHOW_CALL_BUTTON: key = KEY_SHOW_CALL_BUTTON; break;
            case ID_MENU_MESSAGE_INFO: key = KEY_MENU_MESSAGE_INFO; break;
            case ID_MENU_COPY_LINK: key = KEY_MENU_COPY_LINK; break;
            case ID_MENU_COPY_IMAGE: key = KEY_MENU_COPY_IMAGE; break;
            case ID_MENU_COPY_THUMB: key = KEY_MENU_COPY_THUMB; break;
            case ID_MENU_SAVE_TO_SAVED: key = KEY_MENU_SAVE_TO_SAVED; break;
            case ID_MENU_FORWARD_NO_TAG: key = KEY_MENU_FORWARD_NO_TAG; break;
        }
        if (key != null) {
            getPrefs().edit().putBoolean(key, value).apply();
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(TjLocale.getString(R.string.TjSettings));
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
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjGeneralHeader)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_HIDE_PHONE, TjLocale.getString(R.string.TjHidePhoneNumber)));
        items.add(new Item(VIEW_TYPE_SHADOW, 0, TjLocale.getString(R.string.TjHidePhoneNumberInfo)));
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjChatIdHeader)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_BOT_API_IDS, TjLocale.getString(R.string.TjBotApiIds)));
        items.add(new Item(VIEW_TYPE_SHADOW, 0, TjLocale.getString(R.string.TjBotApiIdsInfo)));
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjChatsHeader)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_SHOW_CALL_BUTTON, TjLocale.getString(R.string.TjShowCallButton)));
        items.add(new Item(VIEW_TYPE_SHADOW, 0, TjLocale.getString(R.string.TjShowCallButtonInfo)));
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjMessageMenuHeader)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_MENU_MESSAGE_INFO, TjLocale.getString(R.string.TjMessageInfo)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_MENU_SAVE_TO_SAVED, TjLocale.getString(R.string.TjSaveToSaved)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_MENU_COPY_LINK, TjLocale.getString(R.string.TjCopyMessageLink)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_MENU_COPY_IMAGE, TjLocale.getString(R.string.TjCopyImage)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_MENU_COPY_THUMB, TjLocale.getString(R.string.TjCopyThumbnail)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_MENU_FORWARD_NO_TAG, TjLocale.getString(R.string.TjForwardWithoutTag)));
        items.add(new Item(VIEW_TYPE_SHADOW, 0, TjLocale.getString(R.string.TjMessageMenuInfo)));
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
