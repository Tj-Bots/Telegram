package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

import com.google.android.exoplayer2.text.Cue;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.TjSettingsActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the cues the player hands us.
 *
 * ExoPlayer ships a SubtitleView, but it is not part of the copy vendored in this tree - only the
 * text package is - so this draws them itself. That turns out to be the better shape anyway: it
 * lets the appearance follow our own settings, and it puts bidi in our hands.
 *
 * On right-to-left: each cue goes through a StaticLayout, whose paragraph direction is decided by
 * the first strong character in the line. A Hebrew or Arabic subtitle therefore lays out
 * right-to-left on its own, with any Latin words inside it reordered correctly by the same Bidi
 * pass - which is exactly what a per-character draw would get wrong. Alignment stays centred, so
 * the direction only affects the order of the glyphs, not where the block sits.
 */
public class TjSubtitleView extends View {

    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private float strokeWidth;
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF boxRect = new RectF();

    private final ArrayList<CharSequence> cueTexts = new ArrayList<>();
    private final ArrayList<StaticLayout> layouts = new ArrayList<>();

    private int layoutWidth;
    private int layoutTextSize;
    private int layoutStyle = -1;
    /** How far above our bottom edge the block sits; the controls push this up when they show. */
    private int bottomOffset;

    public TjSubtitleView(Context context) {
        super(context);
        setFocusable(false);
        setClickable(false);
        boxPaint.setColor(0xB0000000);
    }

    public void setCues(List<Cue> cues) {
        final ArrayList<CharSequence> next = new ArrayList<>();
        if (cues != null) {
            for (int a = 0; a < cues.size(); a++) {
                final Cue cue = cues.get(a);
                // Bitmap cues (PGS, DVB) carry no text; there is nothing to lay out for them.
                if (cue == null || TextUtils.isEmpty(cue.text)) {
                    continue;
                }
                next.add(cue.text);
            }
        }
        if (next.equals(cueTexts)) {
            return;
        }
        cueTexts.clear();
        cueTexts.addAll(next);
        layouts.clear();
        layoutWidth = 0;
        setVisibility(cueTexts.isEmpty() ? GONE : VISIBLE);
        invalidate();
    }

    public void clear() {
        setCues(null);
    }

    /** Called when the player controls appear or go away, so subtitles are never hidden by them. */
    public void setBottomOffset(int offset) {
        if (bottomOffset != offset) {
            bottomOffset = offset;
            invalidate();
        }
    }

    /** Re-reads the appearance settings; call after the settings dialog changes anything. */
    public void updateAppearance() {
        layoutWidth = 0;
        layouts.clear();
        invalidate();
    }

    private void buildLayouts(int width) {
        final int textSize = TjSettingsActivity.getSubtitleFontSize();
        final int style = TjSettingsActivity.getSubtitleStyle();
        if (width == layoutWidth && textSize == layoutTextSize && style == layoutStyle && !layouts.isEmpty()) {
            return;
        }
        layoutWidth = width;
        layoutTextSize = textSize;
        layoutStyle = style;
        layouts.clear();

        textPaint.setTextSize(AndroidUtilities.dpf2(textSize));
        textPaint.setTypeface(AndroidUtilities.bold());
        textPaint.setColor(0xFFFFFFFF);
        textPaint.clearShadowLayer();
        if (style == TjSettingsActivity.SUBTITLE_STYLE_SHADOW) {
            textPaint.setShadowLayer(dp(2), 0, dp(1), 0xFF000000);
        }
        strokeWidth = Math.max(1f, AndroidUtilities.dpf2(textSize) / 9f);

        for (int a = 0; a < cueTexts.size(); a++) {
            layouts.add(new StaticLayout(cueTexts.get(a), textPaint, width, Layout.Alignment.ALIGN_CENTER, 1.0f, 0, false));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (cueTexts.isEmpty()) {
            return;
        }
        final int width = getMeasuredWidth() - dp(32);
        if (width <= 0) {
            return;
        }
        buildLayouts(width);

        int totalHeight = 0;
        for (int a = 0; a < layouts.size(); a++) {
            totalHeight += layouts.get(a).getHeight() + (a > 0 ? dp(2) : 0);
        }
        float y = getMeasuredHeight() - bottomOffset - dp(8) - totalHeight;
        if (y < 0) {
            y = 0;
        }
        for (int a = 0; a < layouts.size(); a++) {
            final StaticLayout layout = layouts.get(a);
            canvas.save();
            canvas.translate(dp(16), y);
            if (layoutStyle == TjSettingsActivity.SUBTITLE_STYLE_BOX) {
                for (int line = 0; line < layout.getLineCount(); line++) {
                    boxRect.set(layout.getLineLeft(line) - dp(6), layout.getLineTop(line),
                            layout.getLineRight(line) + dp(6), layout.getLineBottom(line));
                    canvas.drawRoundRect(boxRect, dp(4), dp(4), boxPaint);
                }
            } else if (layoutStyle == TjSettingsActivity.SUBTITLE_STYLE_OUTLINE) {
                // The stroke goes under the fill, so the layout is drawn twice with the paint it
                // already holds - a second TextPaint would not be the one the layout draws with.
                textPaint.setStyle(Paint.Style.STROKE);
                textPaint.setStrokeWidth(strokeWidth);
                textPaint.setColor(0xFF000000);
                layout.draw(canvas);
                textPaint.setStyle(Paint.Style.FILL);
                textPaint.setColor(0xFFFFFFFF);
            }
            layout.draw(canvas);
            canvas.restore();
            y += layout.getHeight() + dp(2);
        }
    }
}
