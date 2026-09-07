package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.TjLocale;
import org.telegram.messenger.tj.TjMessageArchive;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

/** Displays locally archived versions of one edited message. */
public class TjMessageHistoryActivity extends BaseFragment {
    private final MessageObject currentMessage;
    private LinearLayout container;

    public TjMessageHistoryActivity(MessageObject currentMessage) {
        this.currentMessage = currentMessage;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(TjLocale.getString(R.string.TjEditHistory));
        actionBar.setSubtitle("#" + currentMessage.getId());
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = root;
        ScrollView scroll = new ScrollView(context);
        container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12),
                AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        scroll.addView(container, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT, Gravity.TOP));
        root.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                LayoutHelper.MATCH_PARENT));
        loadHistory();
        return fragmentView;
    }

    private void loadHistory() {
        TjMessageArchive.getInstance().getRevisions(getCurrentAccount(), currentMessage.getDialogId(),
                currentMessage.getId(), this::showHistory);
    }

    private void showHistory(ArrayList<TjMessageArchive.Snapshot> revisions) {
        if (container == null || getContext() == null) {
            return;
        }
        container.removeAllViews();
        if (revisions.isEmpty()) {
            addEmpty();
            return;
        }
        for (int i = revisions.size() - 1; i >= 0; i--) {
            addRevision(revisions.get(i), i + 1);
        }
    }

    private void addEmpty() {
        TextView view = new TextView(getContext());
        view.setText(TjLocale.getString(R.string.TjNoEditHistory));
        view.setTextSize(15);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        container.addView(view, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 120));
    }

    private void addRevision(TjMessageArchive.Snapshot snapshot, int number) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12),
                AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        card.setBackground(Theme.getSelectorDrawable(true));
        card.setClickable(true);
        card.setFocusable(true);

        TextView title = new TextView(getContext());
        title.setText(TjLocale.formatString(R.string.TjEditRevision, number));
        title.setTextSize(13);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        card.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT));

        TextView body = new TextView(getContext());
        String text = snapshot.message.message;
        if (TextUtils.isEmpty(text)) {
            MessageObject object = new MessageObject(getCurrentAccount(), snapshot.message, false, true);
            text = mediaLabel(object);
        }
        body.setText(text);
        body.setTextSize(16);
        body.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        body.setMaxLines(8);
        body.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(body, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));

        TextView date = new TextView(getContext());
        date.setText(LocaleController.getInstance().getFormatterStats().format(snapshot.capturedAt));
        date.setTextSize(12);
        date.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        card.addView(date, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));

        card.setOnClickListener(v -> presentFragment(new MessageInfoActivity(
                new MessageObject(getCurrentAccount(), snapshot.message, false, true))));
        container.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
    }

    private String mediaLabel(MessageObject message) {
        if (message.isPhoto()) return LocaleController.getString(R.string.AttachPhoto);
        if (message.isVideo()) return LocaleController.getString(R.string.AttachVideo);
        if (message.isVoice()) return LocaleController.getString(R.string.AttachAudio);
        if (message.isMusic()) return LocaleController.getString(R.string.AttachMusic);
        if (message.isSticker() || message.isAnimatedSticker()) return LocaleController.getString(R.string.AttachSticker);
        if (message.getDocument() != null) return LocaleController.getString(R.string.AttachDocument);
        return LocaleController.getString(R.string.Message);
    }
}
