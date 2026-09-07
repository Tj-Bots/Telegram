package org.telegram.messenger.tj;

import android.text.TextUtils;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Compiles and applies user-defined local message filters. */
public final class TjMessageFilter {
    private static final int MAX_PATTERNS = 64;
    private static final int MAX_PATTERN_LENGTH = 512;
    private static final int MAX_TEXT_LENGTH = 8192;
    private static final Pattern NESTED_QUANTIFIER = Pattern.compile("\\([^)]*[+*][^)]*\\)\\s*(?:[+*]|\\{)");
    private static String cachedSource;
    private static boolean cachedCaseInsensitive;
    private static ArrayList<Pattern> cachedPatterns = new ArrayList<>();

    private TjMessageFilter() {
    }

    public static boolean isFiltered(MessageObject message, MessageObject.GroupedMessages group) {
        if (!TjConfig.messageFilters() || message == null) {
            return false;
        }
        ArrayList<Pattern> patterns = patterns();
        if (patterns.isEmpty()) {
            return false;
        }
        if (group != null && group.messages != null && !group.messages.isEmpty()) {
            for (MessageObject item : group.messages) {
                if (matches(patterns, textOf(item))) {
                    return true;
                }
            }
            return false;
        }
        return matches(patterns, textOf(message));
    }

    public static void removeFiltered(ArrayList<MessageObject> messages) {
        if (!TjConfig.messageFilters() || messages == null || messages.isEmpty()) {
            return;
        }
        HashSet<Long> filteredGroups = new HashSet<>();
        for (MessageObject message : messages) {
            if (isFiltered(message, null) && message.getGroupId() != 0) {
                filteredGroups.add(message.getGroupId());
            }
        }
        Iterator<MessageObject> iterator = messages.iterator();
        while (iterator.hasNext()) {
            MessageObject message = iterator.next();
            if (isFiltered(message, null)
                    || message.getGroupId() != 0 && filteredGroups.contains(message.getGroupId())) {
                iterator.remove();
            }
        }
    }

    public static void blurPreview(MessageObject message) {
        if (message == null || message.messageOwner == null) {
            return;
        }
        String text = message.messageOwner.message;
        if (!TextUtils.isEmpty(text)) {
            boolean covered = false;
            for (TLRPC.MessageEntity entity : message.messageOwner.entities) {
                if (entity instanceof TLRPC.TL_messageEntitySpoiler
                        && entity.offset == 0 && entity.length >= text.length()) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                TLRPC.TL_messageEntitySpoiler spoiler = new TLRPC.TL_messageEntitySpoiler();
                spoiler.offset = 0;
                spoiler.length = text.length();
                message.messageOwner.entities.add(spoiler);
            }
        }
        if (message.messageOwner.media != null) {
            message.messageOwner.media.spoiler = true;
        }
    }

    public static void invalidate() {
        synchronized (TjMessageFilter.class) {
            cachedSource = null;
            cachedPatterns = new ArrayList<>();
        }
    }

    private static ArrayList<Pattern> patterns() {
        String source = TjConfig.filterExpressions();
        boolean caseInsensitive = TjConfig.filtersCaseInsensitive();
        synchronized (TjMessageFilter.class) {
            if (source.equals(cachedSource) && caseInsensitive == cachedCaseInsensitive) {
                return cachedPatterns;
            }
            int flags = Pattern.MULTILINE;
            if (caseInsensitive) {
                flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            }
            ArrayList<Pattern> compiled = new ArrayList<>();
            for (String expression : source.split("\\r?\\n")) {
                expression = expression.trim();
                if (expression.isEmpty()) {
                    continue;
                }
                if (compiled.size() >= MAX_PATTERNS) {
                    break;
                }
                if (expression.length() > MAX_PATTERN_LENGTH || NESTED_QUANTIFIER.matcher(expression).find()
                        || expression.contains(".*.*") || expression.contains(".+.+")) {
                    FileLog.e("Rejected unsafe Tj message filter");
                    continue;
                }
                try {
                    compiled.add(Pattern.compile(expression, flags));
                } catch (PatternSyntaxException error) {
                    FileLog.e("Invalid Tj message filter: " + expression, error);
                }
            }
            cachedSource = source;
            cachedCaseInsensitive = caseInsensitive;
            cachedPatterns = compiled;
            return cachedPatterns;
        }
    }

    private static boolean matches(ArrayList<Pattern> patterns, CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        CharSequence safeText = text.length() > MAX_TEXT_LENGTH ? text.subSequence(0, MAX_TEXT_LENGTH) : text;
        for (Pattern pattern : patterns) {
            if (pattern.matcher(safeText).find()) {
                return true;
            }
        }
        return false;
    }

    private static CharSequence textOf(MessageObject message) {
        if (message == null || message.messageOwner == null) {
            return null;
        }
        if (!TextUtils.isEmpty(message.messageOwner.message)) {
            return message.messageOwner.message;
        }
        return message.messageText;
    }
}
