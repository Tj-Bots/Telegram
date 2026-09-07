package org.telegram.messenger.tj;

import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.support.LongSparseIntArray;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/** Optional multi-device read-state synchronization for a user-supplied server. */
public final class TjSyncController {
    public enum State { DISABLED, MISSING_CONFIGURATION, CONNECTING, CONNECTED, ERROR }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final DispatchQueue queue = new DispatchQueue("TjSyncController");
    private static final Gson gson = new Gson();
    private static volatile TjSyncController instance;
    private static volatile State state = State.DISABLED;
    private static volatile long lastSent;
    private static volatile long lastReceived;
    private static volatile int registrationStatusCode;
    private static final long[] accountGenerations = new long[UserConfig.MAX_ACCOUNT_COUNT];

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build();
    private WebSocket socket;
    private volatile boolean stopped;

    private TjSyncController() {
    }

    /** Invalidates queued events whenever an account slot starts representing another user. */
    public static synchronized void onAccountIdentityChanged(int account) {
        if (account >= 0 && account < accountGenerations.length) {
            accountGenerations[account]++;
            TjGhostController.clearSession(account);
        }
    }

    private static synchronized long accountGeneration(int account) {
        return account >= 0 && account < accountGenerations.length ? accountGenerations[account] : -1;
    }

    private static int resolveAccount(long userId) {
        if (userId == 0) return -1;
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            UserConfig config = UserConfig.getInstance(account);
            if (config.isClientActivated() && config.getClientUserId() == userId) {
                return account;
            }
        }
        return -1;
    }

    private static boolean matchesAccount(int account, long userId, long generation) {
        if (accountGeneration(account) != generation) return false;
        UserConfig config = UserConfig.getInstance(account);
        return config.isClientActivated() && config.getClientUserId() == userId;
    }

    public static synchronized void restart() {
        stop();
        if (!TjConfig.syncEnabled()) {
            setState(State.DISABLED);
            return;
        }
        if (TextUtils.isEmpty(TjConfig.syncServer()) || TextUtils.isEmpty(TjConfig.syncToken())) {
            setState(State.MISSING_CONFIGURATION);
            return;
        }
        instance = new TjSyncController();
        queue.postRunnable(instance::connect);
    }

    public static synchronized void stop() {
        if (instance != null) {
            instance.stopped = true;
            if (instance.socket != null) {
                instance.socket.close(1000, "disabled");
            }
            instance.client.dispatcher().cancelAll();
            instance = null;
        }
    }

    public static State getState() { return state; }
    public static long getLastSent() { return lastSent; }
    public static long getLastReceived() { return lastReceived; }
    public static int getRegistrationStatusCode() { return registrationStatusCode; }
    public static String getDeviceId() { return deviceId(); }

    private static void notifyStateChanged() {
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance()
                .postNotificationName(NotificationCenter.updateInterfaces, 0));
    }

    private static void setState(State value) {
        state = value;
        notifyStateChanged();
    }

    public static void onReadRequest(int account, TLObject request) {
        TjSyncController current = instance;
        if (current == null || state != State.CONNECTED) return;
        long dialogId = 0;
        int untilId = 0;
        if (request instanceof TLRPC.TL_messages_readHistory) {
            TLRPC.TL_messages_readHistory read = (TLRPC.TL_messages_readHistory) request;
            dialogId = TjGhostController.getDialogId(read.peer);
            untilId = read.max_id;
        } else if (request instanceof TLRPC.TL_channels_readHistory) {
            TLRPC.TL_channels_readHistory read = (TLRPC.TL_channels_readHistory) request;
            dialogId = -read.channel.channel_id;
            untilId = read.max_id;
        }
        if (dialogId != 0 && untilId > 0) current.sendRead(account, dialogId, untilId);
    }

    private String httpBase() {
        return "https://" + trimServer();
    }

    private String wsBase() {
        return "wss://" + trimServer();
    }

    private String trimServer() {
        String server = TjConfig.syncServer().trim();
        server = server.replaceFirst("^https?://", "").replaceFirst("^wss?://", "");
        while (server.endsWith("/")) server = server.substring(0, server.length() - 1);
        return server;
    }

    private Request.Builder request(String url) {
        return new Request.Builder().url(url)
                .header("Authorization", TjConfig.syncToken())
                .header("X-APP-PACKAGE", ApplicationLoader.applicationContext.getPackageName())
                .header("X-DEVICE-IDENTIFIER", deviceId());
    }

    private void connect() {
        if (!isCurrent()) {
            return;
        }
        setState(State.CONNECTING);
        try {
            Request selfRequest = request(httpBase() + "/user/v1").get().build();
            try (Response response = client.newCall(selfRequest).execute()) {
                if (!response.isSuccessful()) throw new IllegalStateException("user status " + response.code());
            }
            if (!isCurrent()) return;
            JsonObject registration = new JsonObject();
            registration.addProperty("name", Build.MANUFACTURER + " " + Build.MODEL);
            registration.addProperty("identifier", deviceId());
            Request register = request(httpBase() + "/sync/register/v1")
                    .post(RequestBody.create(registration.toString(), JSON)).build();
            try (Response response = client.newCall(register).execute()) {
                registrationStatusCode = response.code();
                notifyStateChanged();
                if (!response.isSuccessful()) throw new IllegalStateException("registration status " + response.code());
            }
            if (!isCurrent()) return;
            WebSocket newSocket = client.newWebSocket(request(wsBase() + "/sync/ws/v1").build(), new Listener());
            if (!isCurrent()) {
                newSocket.close(1000, "stale");
                return;
            }
            socket = newSocket;
        } catch (Throwable error) {
            FileLog.e("Tj sync connection failed", error);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!isCurrent()) return;
        setState(State.ERROR);
        if (isCurrent()) {
            queue.postRunnable(this::connect, 5000);
        }
    }

    private boolean isCurrent() {
        return !stopped && TjConfig.syncEnabled() && instance == this;
    }

    private void sendRead(int account, long dialogId, int untilId) {
        UserConfig config = UserConfig.getInstance(account);
        if (!config.isClientActivated() || config.getClientUserId() == 0) return;
        MessagesController controller = MessagesController.getInstance(account);
        TLRPC.Dialog dialog = controller.getDialog(dialogId);
        JsonObject args = new JsonObject();
        args.addProperty("dialogId", dialogId);
        args.addProperty("untilId", untilId);
        args.addProperty("unread", dialog == null ? 0 : controller.getDialogUnreadCount(dialog));
        JsonObject event = event("sync_read", config.getClientUserId());
        event.add("args", args);
        send(event);
    }

    public static void forceSync() {
        TjSyncController current = instance;
        if (current == null) return;
        int account = UserConfig.selectedAccount;
        long userId = UserConfig.getInstance(account).getClientUserId();
        long generation = accountGeneration(account);
        if (!matchesAccount(account, userId, generation)) return;
        queue.postRunnable(() -> {
            try {
                if (!current.isCurrent() || !matchesAccount(account, userId, generation)) return;
                JsonObject body = new JsonObject();
                body.addProperty("userId", userId);
                body.addProperty("fromDate", 0);
                Request request = current.request(current.httpBase() + "/sync/force/v1")
                        .post(RequestBody.create(body.toString(), JSON)).build();
                try (Response response = current.client.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IllegalStateException("force status " + response.code());
                }
            } catch (Throwable error) {
                FileLog.e("Tj force sync failed", error);
            }
        });
    }

    private void send(JsonObject event) {
        WebSocket current = socket;
        if (current != null && current.send(gson.toJson(event))) {
            lastSent = System.currentTimeMillis();
            notifyStateChanged();
        }
    }

    private static JsonObject event(String type, long userId) {
        JsonObject event = new JsonObject();
        event.addProperty("type", type);
        event.addProperty("userId", userId);
        return event;
    }

    private void handle(String message) {
        if (!isCurrent() || message == null || message.length() > 1024 * 1024) {
            return;
        }
        try {
            JsonObject event = JsonParser.parseString(message).getAsJsonObject();
            lastReceived = System.currentTimeMillis();
            notifyStateChanged();
            handle(event, 0, new int[]{0});
        } catch (Throwable error) {
            FileLog.e("Tj sync message failed", error);
        }
    }

    private void handle(JsonObject event, int depth, int[] processed) {
        if (depth > 4 || ++processed[0] > 1000) return;
        String type = event.has("type") ? event.get("type").getAsString() : "";
        long userId = event.has("userId") ? event.get("userId").getAsLong() : 0;
        if (userId != 0 && resolveAccount(userId) < 0) return;
        if ("sync_batch".equals(type)) {
            JsonArray events = event.getAsJsonObject("args").getAsJsonArray("events");
            for (int i = 0; i < events.size() && processed[0] <= 1000; i++) {
                handle(events.get(i).getAsJsonObject(), depth + 1, processed);
            }
        } else if ("sync_read".equals(type)) {
            applyRead(userId, event.getAsJsonObject("args"));
        } else if ("sync_force".equals(type)) {
            sendAllReads(userId);
            send(event("sync_force_finish", userId));
        }
    }

    private void sendAllReads(long userId) {
        int account = resolveAccount(userId);
        if (account < 0) return;
        long generation = accountGeneration(account);
        AndroidUtilities.runOnUIThread(() -> {
            if (!isCurrent() || !matchesAccount(account, userId, generation)) return;
            JsonArray events = new JsonArray();
            MessagesController controller = MessagesController.getInstance(account);
            for (TLRPC.Dialog dialog : new java.util.ArrayList<>(controller.getAllDialogs())) {
                JsonObject args = new JsonObject();
                args.addProperty("dialogId", dialog.id);
                args.addProperty("untilId", dialog.read_inbox_max_id);
                args.addProperty("unread", controller.getDialogUnreadCount(dialog));
                JsonObject read = event("sync_read", userId);
                read.add("args", args);
                events.add(read);
            }
            JsonObject args = new JsonObject();
            args.add("events", events);
            JsonObject batch = event("sync_batch", userId);
            batch.add("args", args);
            queue.postRunnable(() -> send(batch));
        });
    }

    private void applyRead(long userId, JsonObject args) {
        if (args == null || !args.has("dialogId") || !args.has("untilId") || !args.has("unread")) return;
        int account = resolveAccount(userId);
        if (account < 0) return;
        long generation = accountGeneration(account);
        long dialogId = args.get("dialogId").getAsLong();
        int untilId = args.get("untilId").getAsInt();
        int unread = args.get("unread").getAsInt();
        if (dialogId == 0 || untilId <= 0 || unread < 0) return;
        AndroidUtilities.runOnUIThread(() -> {
            if (!isCurrent() || !matchesAccount(account, userId, generation)) return;
            MessagesController controller = MessagesController.getInstance(account);
            TLRPC.Dialog dialog = controller.getDialog(dialogId);
            if (dialog == null || untilId <= dialog.read_inbox_max_id || dialog.unread_count <= unread) return;
            int safeUnread = Math.min(unread, dialog.unread_count);
            LongSparseIntArray inbox = new LongSparseIntArray();
            LongSparseIntArray remaining = new LongSparseIntArray();
            inbox.put(dialogId, untilId);
            remaining.put(dialogId, safeUnread);
            controller.dialogs_read_inbox_max.put(dialogId, untilId);
            MessagesStorage storage = MessagesStorage.getInstance(account);
            storage.updateDialogsWithReadMessages(inbox, null, null, remaining, true);
            storage.markMessagesAsRead(inbox, null, null, true);
            NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces,
                    MessagesController.UPDATE_MASK_READ_DIALOG_MESSAGE);
        });
    }

    private static String deviceId() {
        String id = Settings.Secure.getString(ApplicationLoader.applicationContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return TextUtils.isEmpty(id) ? Build.FINGERPRINT : id;
    }

    private final class Listener extends WebSocketListener {
        @Override public void onOpen(WebSocket webSocket, Response response) {
            if (isCurrent() && webSocket == socket) setState(State.CONNECTED);
            else webSocket.close(1000, "stale");
        }
        @Override public void onMessage(WebSocket webSocket, String text) {
            if (isCurrent() && webSocket == socket) queue.postRunnable(() -> handle(text));
        }
        @Override public void onClosing(WebSocket webSocket, int code, String reason) { webSocket.close(code, reason); }
        @Override public void onClosed(WebSocket webSocket, int code, String reason) {
            if (isCurrent() && webSocket == socket) scheduleReconnect();
        }
        @Override public void onFailure(WebSocket webSocket, Throwable error, Response response) {
            FileLog.e("Tj sync socket failed", error);
            if (isCurrent() && webSocket == socket) scheduleReconnect();
        }
    }
}
