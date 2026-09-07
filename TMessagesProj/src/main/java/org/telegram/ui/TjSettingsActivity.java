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
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Components.TjFolderIcons;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.messenger.NotificationCenter;
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
    private static final String KEY_GHOST_MODE = "ghost_mode";
    private static final String KEY_GHOST_TYPING = "ghost_hide_typing";
    private static final String KEY_GHOST_ONLINE = "ghost_hide_online";
    private static final String KEY_GHOST_READ = "ghost_hide_read";
    private static final String KEY_GHOST_WARNED = "ghost_warning_dismissed";

    private static final String KEY_SUBTITLE_FONT_SIZE = "subtitle_font_size";
    private static final String KEY_SUBTITLE_STYLE = "subtitle_style";
    private static final String KEY_SUBTITLE_POSITION = "subtitle_position";
    private static final String KEY_SUBTITLE_AUTO = "subtitle_auto_enable";

    private static final String KEY_FOLDER_TAB_STYLE = "folder_tab_style";
    private static final String KEY_FOLDER_EMOTICON_PREFIX = "folder_emoticon_";

    private static final String KEY_MENU_MESSAGE_INFO = "menu_message_info";
    private static final String KEY_MENU_COPY_LINK = "menu_copy_message_link";
    private static final String KEY_MENU_COPY_IMAGE = "menu_copy_image";
    private static final String KEY_MENU_COPY_THUMB = "menu_copy_thumbnail";
    private static final String KEY_MENU_SAVE_TO_SAVED = "menu_save_to_saved";
    private static final String KEY_MENU_FORWARD_NO_TAG = "menu_forward_without_tag";
    private static final String KEY_MENU_REPLY_PRIVATELY = "menu_reply_privately";
    private static final String KEY_DELETE_FOR_BOTH = "delete_for_both_default";

    private static final String KEY_DELETED_MESSAGES_ENABLED = "deleted_messages_enabled";
    private static final String KEY_DELETED_MESSAGES_ICON = "deleted_messages_icon";
    private static final String KEY_DELETED_MESSAGES_COLOR = "deleted_messages_color";
    private static final String KEY_DELETED_MESSAGES_DIM = "deleted_messages_dim";
    private static final String KEY_DELETED_MESSAGES_CAP_GB = "deleted_messages_cap_gb";

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isShowCallButtonEnabled() {
        return getPrefs().getBoolean(KEY_SHOW_CALL_BUTTON, false);
    }

    /**
     * Local retention of messages the server reports deleted, and of prior versions of edited
     * messages. Off by default - this is a deliberate opt-in, not a default behaviour change.
     */
    public static final int DELETED_ICON_TRASH = 0;
    public static final int DELETED_ICON_CROSS = 1;

    public static final int DELETED_COLOR_GREY = 0;
    public static final int DELETED_COLOR_RED = 1;
    public static final int DELETED_COLOR_BLACK = 2;

    public static boolean isDeletedMessagesEnabled() {
        return getPrefs().getBoolean(KEY_DELETED_MESSAGES_ENABLED, false);
    }

    public static void setDeletedMessagesEnabled(boolean value) {
        getPrefs().edit().putBoolean(KEY_DELETED_MESSAGES_ENABLED, value).apply();
    }

    public static int getDeletedMessagesIcon() {
        return getPrefs().getInt(KEY_DELETED_MESSAGES_ICON, DELETED_ICON_TRASH);
    }

    public static void setDeletedMessagesIcon(int icon) {
        getPrefs().edit().putInt(KEY_DELETED_MESSAGES_ICON, icon).apply();
    }

    public static int getDeletedMessagesColor() {
        return getPrefs().getInt(KEY_DELETED_MESSAGES_COLOR, DELETED_COLOR_GREY);
    }

    public static void setDeletedMessagesColor(int color) {
        getPrefs().edit().putInt(KEY_DELETED_MESSAGES_COLOR, color).apply();
    }

    /** The badge drawable for a retained/deleted message, matching the chosen icon. */
    public static int getDeletedMessagesIconDrawable() {
        return getDeletedMessagesIcon() == DELETED_ICON_CROSS ? R.drawable.msg_close : R.drawable.msg_delete;
    }

    /** The resolved ARGB color for a retained/deleted message's badge and dim tint. */
    public static int getDeletedMessagesColorArgb() {
        switch (getDeletedMessagesColor()) {
            case DELETED_COLOR_RED:
                return 0xFFE53935;
            case DELETED_COLOR_BLACK:
                return 0xFF000000;
            default:
                return 0xFF9E9E9E;
        }
    }

    /** Whether a retained message renders dimmer than a normal one. On by default. */
    public static boolean isDeletedMessagesDimmed() {
        return getPrefs().getBoolean(KEY_DELETED_MESSAGES_DIM, true);
    }

    public static void setDeletedMessagesDimmed(boolean value) {
        getPrefs().edit().putBoolean(KEY_DELETED_MESSAGES_DIM, value).apply();
    }

    /** Local storage cap for retained messages, in GB, 1-10. */
    public static int getDeletedMessagesStorageCapGb() {
        return Math.max(1, Math.min(10, getPrefs().getInt(KEY_DELETED_MESSAGES_CAP_GB, 2)));
    }

    public static void setDeletedMessagesStorageCapGb(int gb) {
        getPrefs().edit().putInt(KEY_DELETED_MESSAGES_CAP_GB, Math.max(1, Math.min(10, gb))).apply();
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

    /** One of TjFolderIcons.STYLE_*. Icon plus name by default. */
    /**
     * Ghost mode. The master switch is what the drawer toggles; the three sub-switches say what it
     * actually suppresses, and all three are on by default so the master switch alone does the
     * expected thing.
     */
    public static boolean isGhostModeEnabled() {
        return getPrefs().getBoolean(KEY_GHOST_MODE, false);
    }

    public static void setGhostModeEnabled(boolean value) {
        getPrefs().edit().putBoolean(KEY_GHOST_MODE, value).apply();
    }

    public static boolean isGhostHideOnline() {
        return isGhostModeEnabled() && getPrefs().getBoolean(KEY_GHOST_ONLINE, true);
    }

    public static boolean isGhostHideTypingSetting() {
        return getPrefs().getBoolean(KEY_GHOST_TYPING, true);
    }

    public static void setGhostHideTypingSetting(boolean value) {
        getPrefs().edit().putBoolean(KEY_GHOST_TYPING, value).apply();
    }

    public static boolean isGhostHideOnlineSetting() {
        return getPrefs().getBoolean(KEY_GHOST_ONLINE, true);
    }

    public static void setGhostHideOnlineSetting(boolean value) {
        getPrefs().edit().putBoolean(KEY_GHOST_ONLINE, value).apply();
    }

    public static boolean isGhostHideReadReceiptsSetting() {
        return getPrefs().getBoolean(KEY_GHOST_READ, true);
    }

    public static void setGhostHideReadReceiptsSetting(boolean value) {
        getPrefs().edit().putBoolean(KEY_GHOST_READ, value).apply();
    }

    public static boolean isGhostWarningDismissed() {
        return getPrefs().getBoolean(KEY_GHOST_WARNED, false);
    }

    public static void setGhostWarningDismissed(boolean value) {
        getPrefs().edit().putBoolean(KEY_GHOST_WARNED, value).apply();
    }

    /**
     * A per-chat override lets one conversation keep ghost behaviour independent of, and past,
     * the global toggle - the override always wins over the global switch when it's set.
     */
    public static final int GHOST_OVERRIDE_INHERIT = 0;
    public static final int GHOST_OVERRIDE_ON = 1;
    public static final int GHOST_OVERRIDE_OFF = 2;

    private static final String KEY_GHOST_CHAT_OVERRIDE_PREFIX = "ghost_chat_override_";
    private static final String KEY_GHOST_INSTANT_READ = "ghost_instant_read";

    public static int getGhostChatOverride(int account, long dialogId) {
        return getPrefs().getInt(KEY_GHOST_CHAT_OVERRIDE_PREFIX + account + "_" + dialogId, GHOST_OVERRIDE_INHERIT);
    }

    public static void setGhostChatOverride(int account, long dialogId, int value) {
        if (value == GHOST_OVERRIDE_INHERIT) {
            getPrefs().edit().remove(KEY_GHOST_CHAT_OVERRIDE_PREFIX + account + "_" + dialogId).apply();
        } else {
            getPrefs().edit().putInt(KEY_GHOST_CHAT_OVERRIDE_PREFIX + account + "_" + dialogId, value).apply();
        }
    }

    /** Whether ghost mode is in effect for this chat right now - the override if set, else the global switch. */
    public static boolean isGhostActiveForChat(int account, long dialogId) {
        int override = getGhostChatOverride(account, dialogId);
        if (override == GHOST_OVERRIDE_ON) {
            return true;
        }
        if (override == GHOST_OVERRIDE_OFF) {
            return false;
        }
        return isGhostModeEnabled();
    }

    public static boolean isGhostHideTyping(int account, long dialogId) {
        return isGhostActiveForChat(account, dialogId) && getPrefs().getBoolean(KEY_GHOST_TYPING, true);
    }

    public static boolean isGhostHideReadReceipts(int account, long dialogId) {
        return isGhostActiveForChat(account, dialogId) && getPrefs().getBoolean(KEY_GHOST_READ, true);
    }

    /**
     * While ghost mode is active in a chat, mark it as read the instant a message is sent into
     * it - so the other side never sees an unread badge lingering on a reply that was seen.
     */
    public static boolean isGhostInstantReadEnabled() {
        return getPrefs().getBoolean(KEY_GHOST_INSTANT_READ, false);
    }

    public static void setGhostInstantReadEnabled(boolean value) {
        getPrefs().edit().putBoolean(KEY_GHOST_INSTANT_READ, value).apply();
    }

    public static final int SUBTITLE_STYLE_OUTLINE = 0;
    public static final int SUBTITLE_STYLE_SHADOW = 1;
    public static final int SUBTITLE_STYLE_BOX = 2;

    /** Sizes offered in the player's subtitle settings, in dp. */
    public static final int[] SUBTITLE_FONT_SIZES = {14, 18, 22, 26};

    public static int getSubtitleFontSize() {
        return getPrefs().getInt(KEY_SUBTITLE_FONT_SIZE, 18);
    }

    public static void setSubtitleFontSize(int size) {
        getPrefs().edit().putInt(KEY_SUBTITLE_FONT_SIZE, size).apply();
    }

    /**
     * Turn subtitles on by themselves, preferring a track in the app's own language and falling
     * back to English. Off by default; a track you pick by hand always wins.
     */
    public static boolean isSubtitleAutoEnabled() {
        return getPrefs().getBoolean(KEY_SUBTITLE_AUTO, false);
    }

    public static void setSubtitleAutoEnabled(boolean value) {
        getPrefs().edit().putBoolean(KEY_SUBTITLE_AUTO, value).apply();
    }

    /** How far up from the bottom of the picture subtitles sit, as a percentage of its height. */
    public static final int SUBTITLE_POSITION_MAX = 50;

    public static int getSubtitlePosition() {
        return Math.max(0, Math.min(SUBTITLE_POSITION_MAX, getPrefs().getInt(KEY_SUBTITLE_POSITION, 0)));
    }

    public static void setSubtitlePosition(int percent) {
        getPrefs().edit().putInt(KEY_SUBTITLE_POSITION, Math.max(0, Math.min(SUBTITLE_POSITION_MAX, percent))).apply();
    }

    public static int getSubtitleStyle() {
        return getPrefs().getInt(KEY_SUBTITLE_STYLE, SUBTITLE_STYLE_OUTLINE);
    }

    public static void setSubtitleStyle(int style) {
        getPrefs().edit().putInt(KEY_SUBTITLE_STYLE, style).apply();
    }

    public static int getFolderTabStyle() {
        return getPrefs().getInt(KEY_FOLDER_TAB_STYLE, 0);
    }

    public static void setFolderTabStyle(int style) {
        getPrefs().edit().putInt(KEY_FOLDER_TAB_STYLE, style).apply();
    }

    /**
     * Folder ids are only unique within one account - two logged-in accounts routinely each
     * have their own folder #2 - so the key must carry the account too, or setting an icon on
     * one account's folder silently overwrites a different folder of the same id on another.
     */
    public static String getFolderEmoticon(int account, int filterId) {
        return getPrefs().getString(KEY_FOLDER_EMOTICON_PREFIX + account + "_" + filterId, null);
    }

    public static void setFolderEmoticon(int account, int filterId, String emoticon) {
        if (emoticon == null) {
            getPrefs().edit().remove(KEY_FOLDER_EMOTICON_PREFIX + account + "_" + filterId).apply();
        } else {
            getPrefs().edit().putString(KEY_FOLDER_EMOTICON_PREFIX + account + "_" + filterId, emoticon).apply();
        }
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

    /**
     * Tick "delete also for X" by default when deleting a message in a private chat. On by
     * default - deleting only your own copy is rarely what anyone means.
     */
    public static boolean isDeleteForBothDefault() {
        return getPrefs().getBoolean(KEY_DELETE_FOR_BOTH, true);
    }

    /** "Reply privately" in the message menu of a group. On by default. */
    public static boolean isReplyPrivatelyEnabled() {
        return getPrefs().getBoolean(KEY_MENU_REPLY_PRIVATELY, true);
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
    private static final int VIEW_TYPE_SETTING = 3;

    private static final int ID_HIDE_PHONE = 1;
    private static final int ID_BOT_API_IDS = 2;
    private static final int ID_SHOW_CALL_BUTTON = 3;
    private static final int ID_MENU_MESSAGE_INFO = 4;
    private static final int ID_MENU_COPY_LINK = 5;
    private static final int ID_MENU_COPY_IMAGE = 6;
    private static final int ID_MENU_COPY_THUMB = 7;
    private static final int ID_MENU_SAVE_TO_SAVED = 8;
    private static final int ID_MENU_FORWARD_NO_TAG = 9;
    private static final int ID_FOLDER_TAB_STYLE = 10;
    private static final int ID_GHOST_SETTINGS = 11;
    private static final int ID_DELETED_MESSAGES = 18;
    private static final int ID_SUBTITLE_AUTO = 15;
    private static final int ID_MENU_REPLY_PRIVATELY = 16;
    private static final int ID_DELETE_FOR_BOTH = 17;

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
            case ID_SUBTITLE_AUTO: return isSubtitleAutoEnabled();
            case ID_MENU_REPLY_PRIVATELY: return isReplyPrivatelyEnabled();
            case ID_DELETE_FOR_BOTH: return isDeleteForBothDefault();
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
            case ID_SUBTITLE_AUTO: key = KEY_SUBTITLE_AUTO; break;
            case ID_MENU_REPLY_PRIVATELY: key = KEY_MENU_REPLY_PRIVATELY; break;
            case ID_DELETE_FOR_BOTH: key = KEY_DELETE_FOR_BOTH; break;
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
            if (item.id == ID_FOLDER_TAB_STYLE) {
                showFolderTabStyleAlert();
                return;
            }
            if (item.id == ID_GHOST_SETTINGS) {
                presentFragment(new TjGhostSettingsActivity());
                return;
            }
            if (item.id == ID_DELETED_MESSAGES) {
                presentFragment(new TjDeletedMessagesActivity());
                return;
            }
            if (item.viewType != VIEW_TYPE_CHECK) {
                return;
            }
            boolean value = !isChecked(item.id);
            setChecked(item.id, value);
            ((TextCheckCell) view).setChecked(value);
        });

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // The Ghost row's value column needs to reflect changes made in the sub-screen.
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
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
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjGhostMode)));
        items.add(new Item(VIEW_TYPE_SETTING, ID_GHOST_SETTINGS, TjLocale.getString(R.string.TjGhostMode)));
        items.add(new Item(VIEW_TYPE_SHADOW, 0, TjLocale.getString(R.string.TjGhostModeInfo)));
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjDeletedMessages)));
        items.add(new Item(VIEW_TYPE_SETTING, ID_DELETED_MESSAGES, TjLocale.getString(R.string.TjDeletedMessages)));
        items.add(new Item(VIEW_TYPE_SHADOW, 0, TjLocale.getString(R.string.TjDeletedEnableInfo)));
        items.add(new Item(VIEW_TYPE_HEADER, 0, LocaleController.getString(R.string.Filters)));
        items.add(new Item(VIEW_TYPE_SETTING, ID_FOLDER_TAB_STYLE, TjLocale.getString(R.string.TjFolderTabStyle)));
        items.add(new Item(VIEW_TYPE_SHADOW, 0, TjLocale.getString(R.string.TjFolderTabStyleInfo)));
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjSubtitles)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_SUBTITLE_AUTO, TjLocale.getString(R.string.TjSubtitleAuto)));
        items.add(new Item(VIEW_TYPE_SHADOW, 0, TjLocale.getString(R.string.TjSubtitleAutoInfo)));
        items.add(new Item(VIEW_TYPE_HEADER, 0, TjLocale.getString(R.string.TjMessageMenuHeader)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_MENU_MESSAGE_INFO, TjLocale.getString(R.string.TjMessageInfo)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_MENU_SAVE_TO_SAVED, TjLocale.getString(R.string.TjSaveToSaved)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_MENU_COPY_LINK, TjLocale.getString(R.string.TjCopyMessageLink)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_MENU_FORWARD_NO_TAG, TjLocale.getString(R.string.TjForwardWithoutTag)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_MENU_REPLY_PRIVATELY, TjLocale.getString(R.string.TjReplyPrivately)));
        items.add(new Item(VIEW_TYPE_CHECK, ID_DELETE_FOR_BOTH, TjLocale.getString(R.string.TjDeleteForBoth)));
        items.add(new Item(VIEW_TYPE_SHADOW, 0, TjLocale.getString(R.string.TjMessageMenuInfo)));
    }

    private static String folderTabStyleName() {
        switch (getFolderTabStyle()) {
            case TjFolderIcons.STYLE_ICON_ONLY: return TjLocale.getString(R.string.TjFolderTabIconOnly);
            case TjFolderIcons.STYLE_NAME_ONLY: return TjLocale.getString(R.string.TjFolderTabNameOnly);
            default: return TjLocale.getString(R.string.TjFolderTabIconAndName);
        }
    }

    private void showFolderTabStyleAlert() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] options = new CharSequence[]{
                TjLocale.getString(R.string.TjFolderTabIconAndName),
                TjLocale.getString(R.string.TjFolderTabIconOnly),
                TjLocale.getString(R.string.TjFolderTabNameOnly)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(TjLocale.getString(R.string.TjFolderTabStyle));
        builder.setItems(options, (dialog, which) -> {
            setFolderTabStyle(which);
            if (listView != null && listView.getAdapter() != null) {
                listView.getAdapter().notifyDataSetChanged();
            }
            // dialogFiltersUpdated is account-scoped - posting it on the global instance reaches
            // nobody, which is why the style only took effect after an app restart.
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                if (UserConfig.getInstance(a).isClientActivated()) {
                    NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.dialogFiltersUpdated);
                }
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
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
                if (item.id == ID_GHOST_SETTINGS) {
                    value = LocaleController.getString(isGhostModeEnabled() ? R.string.NotificationsOn : R.string.NotificationsOff);
                } else if (item.id == ID_DELETED_MESSAGES) {
                    value = LocaleController.getString(isDeletedMessagesEnabled() ? R.string.NotificationsOn : R.string.NotificationsOff);
                } else {
                    value = folderTabStyleName();
                }
                ((TextSettingsCell) holder.itemView).setTextAndValue(item.text, value, false);
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
            int t = holder.getItemViewType();
            return t == VIEW_TYPE_CHECK || t == VIEW_TYPE_SETTING;
        }
    }
}
