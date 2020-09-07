package org.codeandmagic.android.gauge;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Keep;

public class GaugeView extends View {

    private static final String TAG = "GaugeView";

    private static final long ANIMATION_DURATION_DEFAULT = 1000;
    private static final boolean USE_GRADIENT_DEFAULT = true;

    public static final float NEEDLE_WIDTH = 0.2f;
    public static final float NEEDLE_HEIGHT = 1.0f;

    public static final float SCALE_START_ANGLE = 30.0f;

    private static final int POSITIVE_DARK_COLOR = Color.rgb(0, 128, 0);
    private static final int POSITIVE_LIGHT_COLOR = Color.rgb(0, 255, 0);
    private static final int NEGATIVE_DARK_COLOR = Color.rgb(128, 0, 0);
    private static final int NEGATIVE_LIGHT_COLOR = Color.rgb(255, 0, 0);
    private static final int NEEDLE_COLOR = Color.GRAY;
    private static final int NEUTRAL_DARK_COLOR = Color.GRAY;
    private static final int NEUTRAL_LIGHT_COLOR = Color.LTGRAY;


    // *--------------------------------------------------------------------- *//
    // Customizable properties
    // *--------------------------------------------------------------------- *//

    private float mNeedleWidth;
    private float mNeedleHeight;

    private float mScaleStartAngle;

    private Paint mNeedleRightPaint;
    private Paint mNeedleLeftPaint;
    private Paint mNeedleScrewPaint;
    private Paint mNeedleScrewBorderPaint;

    private Path mNeedleRightPath;
    private Path mNeedleLeftPath;

    private boolean useGradient;

    // *--------------------------------------------------------------------- *//

    private float mCurrentValue = 50.0f;

    private boolean mNeedleInitialized;
    private int mPositiveDarkColor;
    private int mPositiveLightColor;
    private int mNegativeDarkColor;
    private int mNegativeLightColor;
    private int mNeutralDarkColor;
    private int mNeutralLightColor;
    private int mNeedleColor;
    private int mHideCentralZoneWithColor;
    private Paint mBackgroundPaintLight;
    private RectF backgroundRectF;
    private float needleAngle;
    private Paint mBackgroundPaintDark;
    private int mWidth;
    private int mHeight;
    private Rect mClipRect;
    private RectF backgroundHideRectF;
    private float mInnerRimWidth;
    private Paint mBackgroundHidePaint;

    public GaugeView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

//        mCurrentValue = 0.5f;
//        mTargetValue = 75;

        if (!isInEditMode()) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GaugeView, defStyle, 0);
            readAndSetCustomAttrs(a);
            readAttrs(a);
            a.recycle();
        } else {

            mPositiveDarkColor = POSITIVE_DARK_COLOR;
            mPositiveLightColor = POSITIVE_LIGHT_COLOR;
            mNegativeDarkColor = NEGATIVE_DARK_COLOR;
            mNegativeLightColor = NEGATIVE_LIGHT_COLOR;
            mNeutralDarkColor = NEUTRAL_DARK_COLOR;
            mNeutralLightColor = NEUTRAL_LIGHT_COLOR;
        }
        init();
    }


    public GaugeView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GaugeView(final Context context) {
        this(context, null, 0);
    }

    private void readAttrs(TypedArray a) {

        mNeedleWidth = a.getFloat(R.styleable.GaugeView_needleWidth, NEEDLE_WIDTH);
        mNeedleHeight = a.getFloat(R.styleable.GaugeView_needleHeight, NEEDLE_HEIGHT);

        mScaleStartAngle = a.getFloat(R.styleable.GaugeView_scaleStartAngle, SCALE_START_ANGLE);

        mInnerRimWidth = a.getFloat(R.styleable.GaugeView_innerRimWidth, 0);
        mHideCentralZoneWithColor = a.getColor(R.styleable.GaugeView_hideCentralZoneWithColor, -1);

        useGradient = a.getBoolean(R.styleable.GaugeView_useGradient, USE_GRADIENT_DEFAULT);


    }


    private void readAndSetCustomAttrs(TypedArray a) {

//        mNeedleInitialized = true;

//        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GaugeView, defStyle, 0);
        mPositiveDarkColor = a.getColor(R.styleable.GaugeView_positiveDarkColor, POSITIVE_DARK_COLOR);
        mPositiveLightColor = a.getColor(R.styleable.GaugeView_positiveLightColor, POSITIVE_LIGHT_COLOR);
        mNegativeDarkColor = a.getColor(R.styleable.GaugeView_negativeDarkColor, NEGATIVE_DARK_COLOR);
        mNegativeLightColor = a.getColor(R.styleable.GaugeView_negativeLightColor, NEGATIVE_LIGHT_COLOR);
        mNeutralDarkColor = a.getColor(R.styleable.GaugeView_neutralDarkColor, NEUTRAL_DARK_COLOR);
        mNeutralLightColor = a.getColor(R.styleable.GaugeView_neutralLightColor, NEUTRAL_LIGHT_COLOR);


        mNeedleColor = a.getColor(R.styleable.GaugeView_needleColor, NEEDLE_COLOR);

//        a.recycle();
    }

    @TargetApi(11)
    private void init() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        setNeedleAngle();

        initDrawingTools();

    }

    private void setNeedleAngle() {
        needleAngle = getAngleForValue(mCurrentValue);
    }

    private void initDrawingTools() {

        mClipRect = new Rect();

        mBackgroundPaintLight = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPaintDark = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPaintLight.setFilterBitmap(true);
        mBackgroundPaintDark.setFilterBitmap(true);

        mBackgroundHidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundHidePaint.setColor(mHideCentralZoneWithColor);
        mBackgroundHidePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));

        backgroundRectF = new RectF();
        backgroundHideRectF = new RectF();

        setDefaultNeedlePaths();
        mNeedleLeftPaint = getDefaultNeedleLeftPaint();
        mNeedleRightPaint = getDefaultNeedleRightPaint();
        mNeedleScrewPaint = getDefaultNeedleScrewPaint();
        mNeedleScrewBorderPaint = getDefaultNeedleScrewBorderPaint();

        computeBackgrounds();
    }

    public void setDefaultNeedlePaths() {

        float halfNeedleWidth = mNeedleWidth * 0.5f;
        float centerY = 1.0f - halfNeedleWidth;

        final float x = 0.5f;

        mNeedleLeftPath = new Path();
        mNeedleLeftPath.moveTo(x, centerY);
        mNeedleLeftPath.lineTo(x - halfNeedleWidth, centerY);
        mNeedleLeftPath.lineTo(x, 0);
        mNeedleLeftPath.lineTo(x, centerY);

        mNeedleRightPath = new Path();
        mNeedleRightPath.moveTo(x, centerY);
        mNeedleRightPath.lineTo(x + halfNeedleWidth, centerY);
        mNeedleRightPath.lineTo(x, 0);
        mNeedleRightPath.lineTo(x, centerY);

    }

    public Paint getDefaultNeedleLeftPaint() {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // MODIFIED
//        paint.setColor(Color.rgb(176, 10, 19));
//        paint.setColor(Color.parseColor("#A8A8A8"));
        paint.setColor(mNeedleColor);
        return paint;
    }

    public Paint getDefaultNeedleRightPaint() {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // MODIFIED
//        paint.setColor(Color.rgb(252, 18, 30));
//        paint.setColor(Color.parseColor("#A8A8A8"));
        paint.setColor(mNeedleColor);
        return paint;
    }

    public Paint getDefaultNeedleScrewPaint() {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setShader(new RadialGradient(0.5f, 0.5f, 0.07f, new int[]{Color.rgb(171, 171, 171), Color.WHITE}, new float[]{0.05f,
//                0.9f}, TileMode.MIRROR));
        paint.setColor(mNeedleColor);
        return paint;
    }

    public Paint getDefaultNeedleScrewBorderPaint() {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(100, 81, 84, 89));
        paint.setStrokeWidth(0.005f);
        return paint;
    }

    @Override
    protected void onRestoreInstanceState(final Parcelable state) {
        final Bundle bundle = (Bundle) state;
        final Parcelable superState = bundle.getParcelable("superState");
        super.onRestoreInstanceState(superState);

        mNeedleInitialized = bundle.getBoolean("needleInitialized");
//        mNeedleVelocity = bundle.getFloat("needleVelocity");
//        mNeedleAcceleration = bundle.getFloat("needleAcceleration");
//        mNeedleLastMoved = bundle.getLong("needleLastMoved");
        mCurrentValue = bundle.getFloat("currentValue");
//        mTargetValue = bundle.getFloat("targetValue");
        computeBackgrounds();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        final Bundle state = new Bundle();
        state.putParcelable("superState", superState);
        state.putBoolean("needleInitialized", mNeedleInitialized);
//        state.putFloat("needleVelocity", mNeedleVelocity);
//        state.putFloat("needleAcceleration", mNeedleAcceleration);
//        state.putLong("needleLastMoved", mNeedleLastMoved);
        state.putFloat("currentValue", mCurrentValue);
//        state.putFloat("targetValue", mTargetValue);
        return state;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        float rat = 0.5f;

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthSize == 0) {
            if (heightSize > 0) {
                widthSize = (int) (heightSize / rat);
            }
        }

        int newHeight = (int) (widthSize * rat);

        if (heightSize > 0 && newHeight > heightSize) {
            widthSize = (int) (heightSize / rat);
            newHeight = heightSize;
        }

        float halfNeedleWidthPercent = mNeedleWidth * 0.5f;
        float handleNeedleWidthPx = halfNeedleWidthPercent * newHeight;

        float backgroundRadius = newHeight - handleNeedleWidthPx;

//        float left = widthSize * 0.5f - backgroundRadius;
        float left = 0;
//        float right = widthSize * 0.5f + backgroundRadius;
        float right = backgroundRadius * 2;
        float bottom = backgroundRadius * 2;

        backgroundRectF.set(left, 0, right, bottom);
        backgroundHideRectF.set(backgroundRadius * (1 - mInnerRimWidth), backgroundRadius * (1 - mInnerRimWidth), right - backgroundRadius * (1 - mInnerRimWidth), bottom - backgroundRadius * (1 - mInnerRimWidth));

//        mWidth = widthSize;
        mWidth = (int) (backgroundRadius * 2);
        mHeight = newHeight;

        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(final Canvas canvas) {

//        mCurrentValue = mTargetValue;

//        if (isInEditMode()) {
//            setTargetValue((float) 50.0);
////            mCurrentValue = mTargetValue;
//            drawBackground(canvas);
//            return;
//        }
        drawBackground(canvas);

        final float scale = Math.min(mWidth, mHeight);
        canvas.scale(scale, scale);
        canvas.translate((scale == mHeight) ? ((mWidth - scale) / 2) / scale : 0
                , (scale == mWidth) ? ((mHeight - scale) / 2) / scale : 0);

//        if (mShowNeedle) {
        drawNeedle(canvas);
//        }

//        if (mShowText) {
//            drawText(canvas);
//        }

//        computeCurrentValue();

    }

    private void drawBackground(final Canvas canvas) {

        float needleAngle = this.needleAngle;
        if (needleAngle < 270) {
            needleAngle = (90 - needleAngle);
        } else {
            needleAngle = (90 + (360 - needleAngle));
        }

        int start1 = (int) mScaleStartAngle;
        int end = 180 - start1;

        int sweep1 = (int) (needleAngle - start1);

        int start2 = (int) needleAngle;
        int sweep2 = end - start2;

        computeBackgrounds();

        canvas.drawArc(backgroundRectF, -start1, -sweep1, true, mBackgroundPaintLight);
        canvas.drawArc(backgroundRectF, -start2, -sweep2, true, mBackgroundPaintDark);
        if (mInnerRimWidth > 0) {
            canvas.drawArc(backgroundHideRectF, -start1 + 1, -sweep1 - sweep2 - 2, true, mBackgroundHidePaint);
        }

    }

    private void computeBackgrounds() {

        if (!useGradient) {

            if (mCurrentValue > 50.1)
                mBackgroundPaintLight.setColor(mPositiveLightColor);
            else if (mCurrentValue < 49.9) {

                mBackgroundPaintLight.setColor(mNegativeLightColor);
            } else {
                mBackgroundPaintLight.setColor(mNeutralLightColor);

            }
            if (mCurrentValue > 50.1)
                mBackgroundPaintDark.setColor(mPositiveDarkColor);
            else if (mCurrentValue < 49.9) {

                mBackgroundPaintDark.setColor(mNegativeDarkColor);
            } else {
                mBackgroundPaintDark.setColor(mNeutralDarkColor);
            }

        } else {

            int red0 = Color.red(mNegativeLightColor);
            int green0 = Color.green(mNegativeLightColor);
            int blue0 = Color.blue(mNegativeLightColor);

            int red1 = Color.red(mNeutralLightColor);
            int green1 = Color.green(mNeutralLightColor);
            int blue1 = Color.blue(mNeutralLightColor);

            int red2 = Color.red(mPositiveLightColor);
            int green2 = Color.green(mPositiveLightColor);
            int blue2 = Color.blue(mPositiveLightColor);

            if (mCurrentValue > 50.1)
//            mBackgroundPaintLight.setColor(mPositiveLightColor);
                mBackgroundPaintLight.setColor(Color.rgb(
                        (int) ((red1 * (100 - mCurrentValue) + red2 * mCurrentValue) / 100f),
                        (int) ((green1 * (100 - mCurrentValue) + green2 * mCurrentValue) / 100f),
                        (int) ((blue1 * (100 - mCurrentValue) + blue2 * mCurrentValue) / 100f)
                ));
            else if (mCurrentValue < 49.9) {

//            mBackgroundPaintLight.setColor(mNegativeLightColor);
                mBackgroundPaintLight.setColor(Color.rgb(
                        (int) ((red0 * (100 - mCurrentValue * 2) + red1 * mCurrentValue * 2) / 100f),
                        (int) ((green0 * (100 - mCurrentValue * 2) + green1 * mCurrentValue * 2) / 100f),
                        (int) ((blue0 * (100 - mCurrentValue * 2) + blue1 * mCurrentValue * 2) / 100f)
                ));
            } else {
                mBackgroundPaintLight.setColor(mNeutralLightColor);
//            mBackgroundPaintLight.setColor(Color.rgb(
//                    (int) ((red0 * (100 - mCurrentValue) + red1 * mCurrentValue) / 100f),
//                    (int) ((green0 * (100 - mCurrentValue) + green1 * mCurrentValue) / 100f),
//                    (int) ((blue0 * (100 - mCurrentValue) + blue1 * mCurrentValue) / 100f)
//            ));

            }


            red0 = Color.red(mNegativeDarkColor);
            green0 = Color.green(mNegativeDarkColor);
            blue0 = Color.blue(mNegativeDarkColor);

            red1 = Color.red(mNeutralDarkColor);
            green1 = Color.green(mNeutralDarkColor);
            blue1 = Color.blue(mNeutralDarkColor);

            red2 = Color.red(mPositiveDarkColor);
            green2 = Color.green(mPositiveDarkColor);
            blue2 = Color.blue(mPositiveDarkColor);

            if (mCurrentValue > 50.1) {
//            mBackgroundPaintDark.setColor(mPositiveDarkColor);
                mBackgroundPaintDark.setColor(Color.rgb(
                        (int) ((red1 * (100 - mCurrentValue) + red2 * mCurrentValue) / 100f),
                        (int) ((green1 * (100 - mCurrentValue) + green2 * mCurrentValue) / 100f),
                        (int) ((blue1 * (100 - mCurrentValue) + blue2 * mCurrentValue) / 100f)
                ));
            } else if (mCurrentValue < 49.9) {

//            mBackgroundPaintDark.setColor(mNegativeDarkColor);
                mBackgroundPaintDark.setColor(Color.rgb(
                        (int) ((red0 * (100 - mCurrentValue * 2) + red1 * mCurrentValue * 2) / 100f),
                        (int) ((green0 * (100 - mCurrentValue * 2) + green1 * mCurrentValue * 2) / 100f),
                        (int) ((blue0 * (100 - mCurrentValue * 2) + blue1 * mCurrentValue * 2) / 100f)
                ));
            } else {
                mBackgroundPaintDark.setColor(mNeutralDarkColor);
//            mBackgroundPaintDark.setColor(Color.rgb(
//                    (int) ((red0 * (100 - mCurrentValue) + red1 * mCurrentValue) / 100f),
//                    (int) ((green0 * (100 - mCurrentValue) + green1 * mCurrentValue) / 100f),
//                    (int) ((blue0 * (100 - mCurrentValue) + blue1 * mCurrentValue) / 100f)
//            ));
            }

        }
    }

    private void drawNeedle(final Canvas canvas) {
        if (mNeedleInitialized) {

//            canvas.getClipBounds(mClipRect);
//            Log.i(TAG, String.format("mClipRect: %s", mClipRect));

            canvas.save(Canvas.MATRIX_SAVE_FLAG);

            float centerY = 1.0f - mNeedleWidth * 0.5f;
//            Log.i(TAG, String.format("needle centerY: %f, %f, %d", centerY, mNeedleWidth, mHeight));

            canvas.rotate(needleAngle, 0.5f, centerY);

            canvas.drawPath(mNeedleLeftPath, mNeedleLeftPaint);
            canvas.drawPath(mNeedleRightPath, mNeedleRightPaint);

            canvas.restore();

            canvas.drawCircle(0.5f, centerY, mNeedleWidth * 0.5f, mNeedleScrewPaint);
//            canvas.drawCircle(0.5f, centerY, 0.04f, mNeedleScrewBorderPaint);
        }
    }

    private float getAngleForValue(final float value) {
        return (270 + mScaleStartAngle + value / 100 * (180 - 2 * mScaleStartAngle)) % 360;
    }

    @Keep
    public void setTargetValue(final float value) {
//        if (mShowScale || mShowRanges) {
//            if (value < mScaleStartValue) {
//                mTargetValue = mScaleStartValue;
//            } else if (value > mScaleEndValue) {
//                mTargetValue = mScaleEndValue;
//            } else {
//                mTargetValue = value;
//            }
//        } else {
//        mTargetValue = value;
        mCurrentValue = value;
        computeBackgrounds();
//        }
        mNeedleInitialized = true;
        setNeedleAngle();
        invalidate();
    }

    public float getTargetValue() {
        return mCurrentValue;
    }

    public void animateTargetValue(double v) {
        ObjectAnimator animation = ObjectAnimator.ofFloat(this, "targetValue", mCurrentValue, (float) v); // see this max value coming back here, we animale towards that value

        animation.setDuration(ANIMATION_DURATION_DEFAULT); //in milliseconds
        animation.setInterpolator(new DecelerateInterpolator());

        post(animation::start);

    }

    public void setUseGradient(boolean useGradient) {
        this.useGradient = useGradient;
    }
}
