package dngsoftware.elgrfid;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

public class SpoolTagLogoView extends View {

    private static final String LOGO_TEXT = "SpoolTag";
    private static final float TEXT_SIZE_SP = 58f;
    private static final float LETTER_SPACING = -0.01f;
    private static final float HORIZONTAL_SCALE = 0.78f;
    private static final float ICON_SIZE_SCALE = 1.5f;
    private static final float ICON_OUTER = 2.35f * ICON_SIZE_SCALE;
    private static final float ICON_MID = 1.65f * ICON_SIZE_SCALE;
    private static final float ICON_CORE = 1.28f * ICON_SIZE_SCALE;
    private static final float CONTENT_LIFT_DP = 18f;
    private static final float G_TAIL_RESERVE_DP = 20f;

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect textBounds = new Rect();
    private final Rect gBounds = new Rect();

    private Drawable nfcIcon;
    private Typeface typeface;
    private float textSizePx;
    private float textWidth;
    private float textHeight;
    private float textBaselineY;
    private float squishedWidth;
    private float contentLiftPx;

    public SpoolTagLogoView(Context context) {
        super(context);
        init(context);
    }

    public SpoolTagLogoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SpoolTagLogoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        contentLiftPx = dp(CONTENT_LIFT_DP);
        typeface = ResourcesCompat.getFont(context, R.font.syne_extrabold);
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP, getResources().getDisplayMetrics()));
        textPaint.setLetterSpacing(LETTER_SPACING);
        textPaint.setStyle(Paint.Style.FILL);

        glowPaint.setTypeface(typeface);
        glowPaint.setLetterSpacing(LETTER_SPACING);
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setColor(0x55BBDEFB);
        glowPaint.setMaskFilter(new BlurMaskFilter(dp(10f), BlurMaskFilter.Blur.NORMAL));

        haloPaint.setTypeface(typeface);
        haloPaint.setLetterSpacing(LETTER_SPACING);
        haloPaint.setColor(0x88FFFFFF);
        haloPaint.setStyle(Paint.Style.STROKE);
        haloPaint.setStrokeWidth(dp(1.6f));
        haloPaint.setStrokeJoin(Paint.Join.ROUND);

        nfcIcon = ContextCompat.getDrawable(context, R.drawable.contactless_logo_mark);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        applyLayoutPadding();
    }

    private void applyLayoutPadding() {
        measureTextMetrics();
        float iconHalf = textSizePx * ICON_OUTER * 0.5f;
        float iconTopAboveBaseline = textHeight * 0.34f + iconHalf;
        float textTopAboveBaseline = -textPaint.getFontMetrics().ascent;
        float extraTop = iconTopAboveBaseline - textTopAboveBaseline + dp(10f);
        int padX = Math.round(dp(16));
        int padTop = Math.max(Math.round(extraTop), Math.round(dp(10f)));
        int padBottom = Math.round(dp(8f));
        setPadding(padX, padTop, padX, padBottom);
        textBaselineY = getPaddingTop() - textPaint.getFontMetrics().ascent;
    }

    private float textBottomFromBaseline() {
        textPaint.getTextBounds(LOGO_TEXT, 0, LOGO_TEXT.length(), textBounds);
        textPaint.getTextBounds("Tag", 0, 3, gBounds);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float glyphBottom = Math.max(metrics.descent, Math.max(textBounds.bottom, gBounds.bottom));
        return glyphBottom
                + haloPaint.getStrokeWidth()
                + dp(10f)
                + dp(6f)
                + dp(2f)
                + dp(G_TAIL_RESERVE_DP);
    }

    private float iconBottomFromBaseline() {
        float ooCenterY = -textHeight * 0.34f;
        return ooCenterY + textSizePx * ICON_OUTER * 0.5f;
    }

    private void measureTextMetrics() {
        textSizePx = textPaint.getTextSize();
        textPaint.getTextBounds(LOGO_TEXT, 0, LOGO_TEXT.length(), textBounds);
        textWidth = Math.max(textPaint.measureText(LOGO_TEXT), textBounds.width());
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        textHeight = metrics.descent - metrics.ascent;
        squishedWidth = textWidth * HORIZONTAL_SCALE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        applyLayoutPadding();
        int width = (int) (squishedWidth + getPaddingLeft() + getPaddingRight()
                + haloPaint.getStrokeWidth() + dp(12f));
        float drawBaseline = textBaselineY - contentLiftPx;
        float textBottom = drawBaseline + textBottomFromBaseline();
        float iconBottom = drawBaseline + iconBottomFromBaseline();
        int height = (int) Math.ceil(Math.max(textBottom, iconBottom) + getPaddingBottom());
        setMeasuredDimension(
                resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float pivotX = getWidth() * 0.5f;
        float textY = textBaselineY - contentLiftPx;
        float textX = pivotX - textWidth * 0.5f;

        float spoEnd = textX + textPaint.measureText("Spo");
        float spoolEnd = textX + textPaint.measureText("Spool");
        float ooCenterUnscaled = (spoEnd + spoolEnd) * 0.5f;
        float ooCenterX = pivotX + (ooCenterUnscaled - pivotX) * HORIZONTAL_SCALE;
        float ooCenterY = textY - textHeight * 0.34f;

        drawIntegratedIcon(canvas, ooCenterX, ooCenterY);

        canvas.save();
        canvas.scale(HORIZONTAL_SCALE, 1f, pivotX, textY);

        glowPaint.setTextSize(textPaint.getTextSize());
        canvas.drawText(LOGO_TEXT, textX, textY, glowPaint);

        textPaint.setShadowLayer(dp(6f), 0f, dp(2f), 0xAA000000);
        textPaint.setShader(buildTextGradient(textX));
        canvas.drawText(LOGO_TEXT, textX, textY, textPaint);
        textPaint.setShader(null);
        textPaint.clearShadowLayer();

        haloPaint.setTextSize(textPaint.getTextSize());
        canvas.drawText(LOGO_TEXT, textX, textY, haloPaint);

        canvas.restore();
    }

    private void drawIntegratedIcon(Canvas canvas, float centerX, float centerY) {
        if (nfcIcon == null) {
            return;
        }

        int outerGlow = (int) (textSizePx * ICON_OUTER);
        nfcIcon.setTint(ContextCompat.getColor(getContext(), R.color.primary_variant));
        nfcIcon.setAlpha(55);
        placeIcon(canvas, centerX, centerY, outerGlow);

        int midGlow = (int) (textSizePx * ICON_MID);
        nfcIcon.setTint(ContextCompat.getColor(getContext(), R.color.brand_title_icon));
        nfcIcon.setAlpha(120);
        placeIcon(canvas, centerX, centerY, midGlow);

        int core = (int) (textSizePx * ICON_CORE);
        nfcIcon.setTint(0xFFFFFFFF);
        nfcIcon.setAlpha(235);
        placeIcon(canvas, centerX, centerY, core);
    }

    private void placeIcon(Canvas canvas, float centerX, float centerY, int size) {
        int half = size / 2;
        nfcIcon.setBounds(
                (int) centerX - half,
                (int) centerY - half,
                (int) centerX + half,
                (int) centerY + half);
        nfcIcon.draw(canvas);
    }

    private Shader buildTextGradient(float textX) {
        int white = ContextCompat.getColor(getContext(), R.color.brand_title_icon);
        int softBlue = 0xFFE3F2FD;
        int accentBlue = ContextCompat.getColor(getContext(), R.color.primary_variant);
        return new LinearGradient(
                textX, 0f, textX + textWidth, 0f,
                new int[]{white, softBlue, accentBlue},
                new float[]{0f, 0.42f, 1f},
                Shader.TileMode.CLAMP);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}