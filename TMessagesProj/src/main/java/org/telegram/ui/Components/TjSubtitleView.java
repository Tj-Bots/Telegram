package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

import com.google.android.exoplayer2.text.Cue;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.TjSettingsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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

    /** The lines exactly as the file has them; cueTexts is these wrapped for direction. */
    private final ArrayList<String> rawLines = new ArrayList<>();
    /** The same lines before clean() touched them, kept only for describeCurrentCue. */
    private final ArrayList<String> rawSource = new ArrayList<>();
    private final ArrayList<CharSequence> cueTexts = new ArrayList<>();
    private final ArrayList<StaticLayout> layouts = new ArrayList<>();

    private int layoutWidth;
    private int layoutTextSize;
    private int layoutStyle = -1;
    /** How far above our bottom edge the block sits; the controls push this up when they show. */
    private int bottomOffset;
    /**
     * The video's own view. AspectRatioFrameLayout re-measures itself to the video's aspect ratio,
     * so its bottom edge is the bottom of the picture rather than of the screen - which is where
     * subtitles belong, letterboxing and fullscreen alike.
     */
    private View videoView;

    public TjSubtitleView(Context context) {
        super(context);
        setFocusable(false);
        setClickable(false);
        boxPaint.setColor(0xB0000000);
    }

    public void setCues(List<Cue> cues) {
        final ArrayList<String> next = new ArrayList<>();
        final ArrayList<String> nextSource = new ArrayList<>();
        if (cues != null) {
            for (int a = 0; a < cues.size(); a++) {
                final Cue cue = cues.get(a);
                // Bitmap cues (PGS, DVB) carry no text; there is nothing to lay out for them.
                if (cue == null || TextUtils.isEmpty(cue.text)) {
                    continue;
                }
                // One entry per line the subtitle file itself wrote, so the file's line breaks
                // are what gets drawn.
                final String source = cue.text.toString();
                for (String raw : SOURCE_LINE.split(source)) {
                    if (!raw.trim().isEmpty()) {
                        nextSource.add(raw.trim());
                    }
                }
                final String[] lines = SOURCE_LINE.split(clean(source));
                for (int b = 0; b < lines.length; b++) {
                    final String line = lines[b].trim();
                    if (!line.isEmpty()) {
                        next.add(line);
                    }
                }
            }
        }
        if (next.equals(rawLines)) {
            return;
        }
        rawLines.clear();
        rawLines.addAll(next);
        rawSource.clear();
        rawSource.addAll(nextSource);
        applyDirection();
    }

    /**
     * Wraps the lines on screen according to the direction setting.
     *
     * The raw lines are kept so the setting can be changed with a cue already showing and take
     * effect on it, rather than only on the next one.
     */
    private void applyDirection() {
        final int direction = TjSettingsActivity.getSubtitleDirection();
        // The line arrives already stripped of any bidi controls the file carried. All that is left
        // is to wrap it in one explicit embedding, so mixed content - Hebrew with numbers or Latin
        // names in it - is ordered by that embedding rather than by the algorithm's guess at the
        // line's own direction.
        final boolean rtl = direction == TjSettingsActivity.SUBTITLE_DIR_RTL || isRtl(TextUtils.join("\n", rawLines));
        cueIsRtl = rtl;

        cueTexts.clear();
        for (int a = 0; a < rawLines.size(); a++) {
            final String line = rawLines.get(a);
            // NONE leaves the stripped line alone and lets the layout order it, which is the escape
            // hatch if a file turns out not to want an embedding at all.
            cueTexts.add(direction == TjSettingsActivity.SUBTITLE_DIR_NONE ? line
                    : (rtl ? RLE : LRE) + line + PDF);
        }
        layouts.clear();
        layoutWidth = 0;
        setVisibility(cueTexts.isEmpty() ? GONE : VISIBLE);
        invalidate();
    }

    /**
     * Explicit bidi embedding: everything between these is laid out in one direction whatever it
     * contains, which is what keeps punctuation on the correct end of the line.
     */
    private static final String RLE = "\u202B";
    private static final String LRE = "\u202A";
    private static final String PDF = "\u202C";

    /** Direction of the cue currently on screen; drives the layout's own text direction. */
    private boolean cueIsRtl;

    /** True when the text carries any Hebrew or Arabic letter, including presentation forms. */
    private static boolean isRtl(String text) {
        for (int i = 0; i < text.length(); i++) {
            final byte dir = Character.getDirectionality(text.charAt(i));
            if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                    || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return true;
            }
        }
        return false;
    }

    /** A newline, or the \N and \n that SSA/ASS use for one. */
    private static final Pattern SOURCE_LINE = Pattern.compile("\r?\n|\\\\[Nn]");
    /** SSA/ASS override blocks such as {\an8} or {\i1}, and the HTML tags WebVTT allows. */
    private static final Pattern MARKUP = Pattern.compile("\\{[^}]*\\}|</?[a-zA-Z][^>]*>");
    /**
     * Bidi control characters already in the file: the marks (LRM, RLM), the embeddings and
     * overrides (LRE, RLE, LRO, RLO), PDF, and the isolates (LRI, RLI, FSI, PDI).
     *
     * These have to go before we add our own. Hebrew subtitle files usually arrive with a layer of
     * them already applied by whatever tool converted them, and wrapping a second embedding around
     * the first is what reverses the line instead of fixing it.
     */
    private static final Pattern BIDI_CONTROLS = Pattern.compile("[\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069]");

    private static String clean(String text) {
        return BIDI_CONTROLS.matcher(MARKUP.matcher(text).replaceAll("")).replaceAll("");
    }

    /**
     * The cue exactly as it arrived, before anything was stripped or wrapped, with every character
     * spelled out as a codepoint.
     *
     * Four rounds of reasoning about what these files contain have not settled it, so this reports
     * the bytes instead of me guessing at them again.
     */
    public String describeCurrentCue() {
        if (rawSource.isEmpty()) {
            return "(no subtitle on screen)";
        }
        final StringBuilder sb = new StringBuilder();
        for (int a = 0; a < rawSource.size(); a++) {
            final String line = rawSource.get(a);
            sb.append("line ").append(a + 1).append(": ").append(line).append('\n');
            for (int i = 0; i < line.length(); i++) {
                sb.append(String.format(java.util.Locale.US, "U+%04X ", (int) line.charAt(i)));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public void clear() {
        setCues(null);
    }

    /** The view the picture is drawn in; subtitles are anchored to its bottom edge. */
    public void setVideoView(View view) {
        if (videoView != view) {
            videoView = view;
            invalidate();
        }
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
        // Re-wraps as well as re-measures, so a change of direction shows on the cue already up.
        applyDirection();
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
            layouts.add(makeLayout(cueTexts.get(a), width));
        }
    }

    /**
     * Centred, and told which way the line runs rather than left to guess from its first strong
     * character. The embedding marks already force the glyph order; setting the layout's own
     * direction keeps the line box on the same side, which is what puts a trailing full stop
     * where a reader expects it.
     */
    private StaticLayout makeLayout(CharSequence text, int width) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, width)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setTextDirection(cueIsRtl ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR)
                    .setLineSpacing(0, 1f)
                    .setIncludePad(false)
                    .build();
        }
        return new StaticLayout(text, textPaint, width, Layout.Alignment.ALIGN_CENTER, 1.0f, 0, false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (cueTexts.isEmpty()) {
            return;
        }
        // Wrap to the picture's width, not the window's, so a line only ever breaks where it
        // would not fit over the video itself.
        int available = getMeasuredWidth();
        if (videoView != null && videoView.getVisibility() == VISIBLE && videoView.getWidth() > 0) {
            available = Math.min(available, videoView.getWidth());
        }
        final int width = available - dp(32);
        if (width <= 0) {
            return;
        }
        buildLayouts(width);

        int totalHeight = 0;
        for (int a = 0; a < layouts.size(); a++) {
            totalHeight += layouts.get(a).getHeight() + (a > 0 ? dp(2) : 0);
        }
        // Sit at the bottom of the picture, not the bottom of the screen - but never under the
        // control bar, which is what bottomOffset lifts us clear of.
        int baseline = getMeasuredHeight();
        if (videoView != null && videoView.getVisibility() == VISIBLE && videoView.getHeight() > 0) {
            baseline = Math.min(baseline, videoView.getBottom());
            baseline -= (int) (videoView.getHeight() * (TjSettingsActivity.getSubtitlePosition() / 100f));
        }
        baseline = Math.min(baseline, getMeasuredHeight() - bottomOffset);

        float y = baseline - dp(8) - totalHeight;
        if (y < 0) {
            y = 0;
        }
        for (int a = 0; a < layouts.size(); a++) {
            final StaticLayout layout = layouts.get(a);
            canvas.save();
            canvas.translate((getMeasuredWidth() - width) / 2f, y);
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
