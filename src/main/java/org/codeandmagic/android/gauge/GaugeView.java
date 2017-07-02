/*******************************************************************************
 * Copyright (c) 2012 Evelina Vrabie
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package org.codeandmagic.android.gauge;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

public class GaugeView extends View {

    public static final float CENTER = 0.5f;
    public static final boolean SHOW_NEEDLE = true;
    public static final boolean SHOW_SCALE = false;
    public static final boolean SHOW_RANGES = true;
    public static final boolean SHOW_TEXT = false;

    //    public static final float NEEDLE_WIDTH = 0.035f;
    public static final float NEEDLE_WIDTH = 0.1f;
    //    public static final float NEEDLE_HEIGHT = 0.28f;
    public static final float NEEDLE_HEIGHT = 1.0f;

    public static final float SCALE_START_VALUE = 0.0f;
    public static final float SCALE_END_VALUE = 100.0f;
    public static final float SCALE_START_ANGLE = 30.0f;
    public static final int SCALE_DIVISIONS = 10;
    public static final int SCALE_SUBDIVISIONS = 5;

    public static final float[] RANGE_VALUES = {16.0f, 25.0f, 40.0f, 100.0f};
    public static final int[] RANGE_COLORS = {Color.rgb(231, 32, 43), Color.rgb(232, 111, 33),
            Color.rgb(232, 231, 33), Color.rgb(27, 202, 33)};

    public static final int TEXT_SHADOW_COLOR = Color.argb(100, 0, 0, 0);
    public static final int TEXT_VALUE_COLOR = Color.WHITE;
    public static final int TEXT_UNIT_COLOR = Color.WHITE;
    public static final float TEXT_VALUE_SIZE = 0.3f;
    public static final float TEXT_UNIT_SIZE = 0.1f;
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

    private boolean mShowScale;
    private boolean mShowRanges;
    private boolean mShowNeedle;
    private boolean mShowText;

    private float mNeedleWidth;
    private float mNeedleHeight;

    private float mScaleStartValue;
    private float mScaleEndValue;
    private float mScaleStartAngle;
    private float mScaleEndAngle;
    private float[] mRangeValues;

    private int[] mRangeColors;
    private int mDivisions;
    private int mSubdivisions;

    private Paint mNeedleRightPaint;
    private Paint mNeedleLeftPaint;
    private Paint mNeedleScrewPaint;
    private Paint mNeedleScrewBorderPaint;
    private Paint mTextValuePaint;
    private Paint mTextUnitPaint;

    private String mTextValue;
    private String mTextUnit;
    private int mTextValueColor;
    private int mTextUnitColor;
    private int mTextShadowColor;
    private float mTextValueSize;
    private float mTextUnitSize;

    private Path mNeedleRightPath;
    private Path mNeedleLeftPath;

    // *--------------------------------------------------------------------- *//

    private float mScaleRotation;
    private float mSubdivisionValue;
    private float mSubdivisionAngle;

    private float mTargetValue = 50.0f;
    private float mCurrentValue = 50.0f;

    private float mNeedleVelocity;
    private float mNeedleAcceleration;
    private long mNeedleLastMoved = -1;
    private boolean mNeedleInitialized;
    private int mPositiveDarkColor;
    private int mPositiveLightColor;
    private int mNegativeDarkColor;
    private int mNegativeLightColor;
    private int mNeutralDarkColor;
    private int mNeutralLightColor;
    private int mNeedleColor;

    public GaugeView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

//        mCurrentValue = 0.5f;
//        mTargetValue = 75;

        if (!isInEditMode()) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GaugeView, defStyle, 0);
            readAndSetCustomAttrs(a);
            readAttrs(context, a);
            a.recycle();
            init();
        }
    }


    public GaugeView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GaugeView(final Context context) {
        this(context, null, 0);
    }

    private void readAttrs(final Context context, TypedArray a) {
//        if (isInEditMode()) return;
        mShowNeedle = a.getBoolean(R.styleable.GaugeView_showNeedle, SHOW_NEEDLE);
        mShowScale = a.getBoolean(R.styleable.GaugeView_showScale, SHOW_SCALE);
        mShowRanges = a.getBoolean(R.styleable.GaugeView_showRanges, SHOW_RANGES);
        mShowText = a.getBoolean(R.styleable.GaugeView_showText, SHOW_TEXT);

        mNeedleWidth = a.getFloat(R.styleable.GaugeView_needleWidth, NEEDLE_WIDTH);
        mNeedleHeight = a.getFloat(R.styleable.GaugeView_needleHeight, NEEDLE_HEIGHT);

        mScaleStartValue = a.getFloat(R.styleable.GaugeView_scaleStartValue, SCALE_START_VALUE);
        mScaleEndValue = a.getFloat(R.styleable.GaugeView_scaleEndValue, SCALE_END_VALUE);
        mScaleStartAngle = a.getFloat(R.styleable.GaugeView_scaleStartAngle, SCALE_START_ANGLE);
        mScaleEndAngle = a.getFloat(R.styleable.GaugeView_scaleEndAngle, 360.0f - mScaleStartAngle);

        mDivisions = a.getInteger(R.styleable.GaugeView_divisions, SCALE_DIVISIONS);
        mSubdivisions = a.getInteger(R.styleable.GaugeView_subdivisions, SCALE_SUBDIVISIONS);

        if (mShowRanges) {
            mTextShadowColor = a.getColor(R.styleable.GaugeView_textShadowColor, TEXT_SHADOW_COLOR);

            final CharSequence[] rangeValues = a.getTextArray(R.styleable.GaugeView_rangeValues);
            final CharSequence[] rangeColors = a.getTextArray(R.styleable.GaugeView_rangeColors);
            readRanges(rangeValues, rangeColors);
        }

        if (mShowText) {
            final int textValueId = a.getResourceId(R.styleable.GaugeView_textValue, 0);
            final String textValue = a.getString(R.styleable.GaugeView_textValue);
            mTextValue = (0 < textValueId) ? context.getString(textValueId) : (null != textValue) ? textValue : "";

            final int textUnitId = a.getResourceId(R.styleable.GaugeView_textUnit, 0);
            final String textUnit = a.getString(R.styleable.GaugeView_textUnit);
            boolean textUnitIdLt = 0 < textUnitId;
            if (textUnitIdLt)
                mTextUnit = context.getString(textUnitId);
            else {
                boolean textUnitNotNull = null != textUnit;
                if (textUnitNotNull)
                    mTextUnit = textUnit;
                else
                    mTextUnit = "";
            }
            mTextValueColor = a.getColor(R.styleable.GaugeView_textValueColor, TEXT_VALUE_COLOR);
            mTextUnitColor = a.getColor(R.styleable.GaugeView_textUnitColor, TEXT_UNIT_COLOR);
            mTextShadowColor = a.getColor(R.styleable.GaugeView_textShadowColor, TEXT_SHADOW_COLOR);

            mTextValueSize = a.getFloat(R.styleable.GaugeView_textValueSize, TEXT_VALUE_SIZE);
            mTextUnitSize = a.getFloat(R.styleable.GaugeView_textUnitSize, TEXT_UNIT_SIZE);
        }


//        a.recycle();
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

    private void readRanges(final CharSequence[] rangeValues, final CharSequence[] rangeColors) {

        int rangeValuesLength;
        if (rangeValues == null) {
            rangeValuesLength = RANGE_VALUES.length;
        } else {
            rangeValuesLength = rangeValues.length;
        }

        int rangeColorsLength;
        if (rangeColors == null) {
            rangeColorsLength = RANGE_COLORS.length;
        } else {
            rangeColorsLength = rangeColors.length;
        }

        if (rangeValuesLength != rangeColorsLength) {
            throw new IllegalArgumentException(
                    "The ranges and colors arrays must have the same length.");
        }

        final int length = rangeValuesLength;
        if (rangeValues != null) {
            mRangeValues = new float[length];
            for (int i = 0; i < length; i++) {
                mRangeValues[i] = Float.parseFloat(rangeValues[i].toString());
            }
        } else {
            mRangeValues = RANGE_VALUES;
        }

        if (rangeColors != null) {
            mRangeColors = new int[length];
            for (int i = 0; i < length; i++) {
                mRangeColors[i] = Color.parseColor(rangeColors[i].toString());
            }
        } else {
            mRangeColors = RANGE_COLORS;
        }
    }

    @TargetApi(11)
    private void init() {
        if (isInEditMode()) return;
        // TODO Why isn't this working with HA layer?
        // The needle is not displayed although the onDraw() is being triggered by invalidate()
        // calls.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

//        initDrawingRects();
        initDrawingTools();

        // Compute the scale properties
        if (mShowRanges) {
            initScale();
        }
    }

    private void initDrawingTools() {
        Paint mBackgroundPaint = new Paint();
        mBackgroundPaint.setFilterBitmap(true);

        if (mShowRanges) {
            setDefaultScaleRangePaints();
        }
        if (mShowNeedle) {
            setDefaultNeedlePaths();
            mNeedleLeftPaint = getDefaultNeedleLeftPaint();
            mNeedleRightPaint = getDefaultNeedleRightPaint();
            mNeedleScrewPaint = getDefaultNeedleScrewPaint();
            mNeedleScrewBorderPaint = getDefaultNeedleScrewBorderPaint();
        }
        if (mShowText) {
            mTextValuePaint = getDefaultTextValuePaint();
            mTextUnitPaint = getDefaultTextUnitPaint();
        }

    }

    public void setDefaultNeedlePaths() {
//        final float x = 0.5f, y = 0.5f;
        final float x = 0.5f, y = 1.0f;
        mNeedleLeftPath = new Path();
        mNeedleLeftPath.moveTo(x, y);
        mNeedleLeftPath.lineTo(x - mNeedleWidth, y);
        mNeedleLeftPath.lineTo(x, y - mNeedleHeight);
        mNeedleLeftPath.lineTo(x, y);
        mNeedleLeftPath.lineTo(x - mNeedleWidth, y);

        mNeedleRightPath = new Path();
        mNeedleRightPath.moveTo(x, y);
        mNeedleRightPath.lineTo(x + mNeedleWidth, y);
        mNeedleRightPath.lineTo(x, y - mNeedleHeight);
        mNeedleRightPath.lineTo(x, y);
        mNeedleRightPath.lineTo(x + mNeedleWidth, y);
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
        if (!isInEditMode())
            paint.setShadowLayer(0.01f, 0.005f, -0.005f, Color.argb(127, 0, 0, 0));
        return paint;
    }

    public Paint getDefaultNeedleScrewPaint() {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new RadialGradient(0.5f, 0.5f, 0.07f, new int[]{Color.rgb(171, 171, 171), Color.WHITE}, new float[]{0.05f,
                0.9f}, TileMode.MIRROR));
        return paint;
    }

    public Paint getDefaultNeedleScrewBorderPaint() {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(100, 81, 84, 89));
        paint.setStrokeWidth(0.005f);
        return paint;
    }

    public void setDefaultScaleRangePaints() {
        final int length = mRangeValues.length;
        Paint[] mRangePaints = new Paint[length];
        for (int i = 0; i < length; i++) {
            mRangePaints[i] = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            mRangePaints[i].setColor(mRangeColors[i]);
            mRangePaints[i].setStyle(Paint.Style.STROKE);
            mRangePaints[i].setStrokeWidth(0.005f);
            mRangePaints[i].setTextSize(0.05f);
            mRangePaints[i].setTypeface(Typeface.SANS_SERIF);
            mRangePaints[i].setTextAlign(Align.CENTER);
            // MODIFIED
            if (isInEditMode()) continue;
            mRangePaints[i].setShadowLayer(0.005f, 0.002f, 0.002f, mTextShadowColor);
        }
    }

    public Paint getDefaultTextValuePaint() {
        final Paint paint = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        paint.setColor(mTextValueColor);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(0.005f);
        paint.setTextSize(mTextValueSize);
        paint.setTextAlign(Align.CENTER);
        paint.setTypeface(Typeface.SANS_SERIF);
        // MODIFIED
        if (!isInEditMode())
            paint.setShadowLayer(0.01f, 0.002f, 0.002f, mTextShadowColor);
        return paint;
    }

    public Paint getDefaultTextUnitPaint() {
        final Paint paint = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        paint.setColor(mTextUnitColor);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(0.005f);
        paint.setTextSize(mTextUnitSize);
        paint.setTextAlign(Align.CENTER);
        // MODIFIED
        if (!isInEditMode())
            paint.setShadowLayer(0.01f, 0.002f, 0.002f, mTextShadowColor);
        return paint;
    }

    @Override
    protected void onRestoreInstanceState(final Parcelable state) {
        final Bundle bundle = (Bundle) state;
        final Parcelable superState = bundle.getParcelable("superState");
        super.onRestoreInstanceState(superState);

        mNeedleInitialized = bundle.getBoolean("needleInitialized");
        mNeedleVelocity = bundle.getFloat("needleVelocity");
        mNeedleAcceleration = bundle.getFloat("needleAcceleration");
        mNeedleLastMoved = bundle.getLong("needleLastMoved");
        mCurrentValue = bundle.getFloat("currentValue");
        mTargetValue = bundle.getFloat("targetValue");
    }

    private void initScale() {
        mScaleRotation = (mScaleStartAngle + 180) % 360;
        float mDivisionValue = (mScaleEndValue - mScaleStartValue) / mDivisions;
        mSubdivisionValue = mDivisionValue / mSubdivisions;
        mSubdivisionAngle = (mScaleEndAngle - mScaleStartAngle) / (mDivisions * mSubdivisions);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        final Bundle state = new Bundle();
        state.putParcelable("superState", superState);
        state.putBoolean("needleInitialized", mNeedleInitialized);
        state.putFloat("needleVelocity", mNeedleVelocity);
        state.putFloat("needleAcceleration", mNeedleAcceleration);
        state.putLong("needleLastMoved", mNeedleLastMoved);
        state.putFloat("currentValue", mCurrentValue);
        state.putFloat("targetValue", mTargetValue);
        return state;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        // Loggable.log.debug(String.format("widthMeasureSpec=%s, heightMeasureSpec=%s",
        // View.MeasureSpec.toString(widthMeasureSpec),
        // View.MeasureSpec.toString(heightMeasureSpec)));

        float rat = 0.5f;

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

//        if (widthMode != MeasureSpec.AT_MOST &&
//                (widthMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.AT_MOST)) {
////            if (heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST) {
//            widthSize = (int) (heightSize / rat);
////            }
//        }

//        if (widthMode != MeasureSpec.EXACTLY || widthSize == 0) {
        if (widthSize == 0) {
//            if (heightMode == MeasureSpec.EXACTLY) {
            if (heightSize > 0) {
                widthSize = (int) (heightSize / rat);
            }
        }

//        heightSize = (int) (widthSize * rat);
        int newHeight = (int) (widthSize * rat);

        if (heightSize > 0 && newHeight > heightSize) {
            widthSize = (int) (heightSize / rat);
            newHeight = heightSize;
        }
//        final int chosenWidth = chooseDimension(widthMode, widthSize);
//        final int chosenHeight = chooseDimension(heightMode, heightSize);
//        setMeasuredDimension(chosenWidth, chosenHeight);
        setMeasuredDimension(widthSize, newHeight);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
//        drawGauge();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        if (isInEditMode()) {
            setTargetValue((float) 50.0);
            mCurrentValue = mTargetValue;
            drawBackground(canvas);
            return;
        }
        drawBackground(canvas);

        final float scale = Math.min(getWidth(), getHeight());
        canvas.scale(scale, scale);
        canvas.translate((scale == getHeight()) ? ((getWidth() - scale) / 2) / scale : 0
                , (scale == getWidth()) ? ((getHeight() - scale) / 2) / scale : 0);

        if (mShowNeedle) {
            drawNeedle(canvas);
        }

        if (mShowText) {
            drawText(canvas);
        }

        computeCurrentValue();

    }

    private void drawBackground(final Canvas canvas) {

        float needleAngle = getAngleForValue(mCurrentValue);
        if (needleAngle < 270) {
            needleAngle = (90 - needleAngle);
        } else {
            needleAngle = (90 + (360 - needleAngle));
        }

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF rectF = new RectF(0, 0, getWidth(), getHeight() * 2);
        int mPositiveDarkColor;
        int mPositiveLightColor;
        int mNegativeLightColor;
        int mNegativeDarkColor;
        int mNeutralDarkColor;
        int mNeutralLightColor;
        if (isInEditMode()) {
            mPositiveDarkColor = POSITIVE_DARK_COLOR;
            mPositiveLightColor = POSITIVE_LIGHT_COLOR;
            mNegativeDarkColor = NEGATIVE_DARK_COLOR;
            mNegativeLightColor = NEGATIVE_LIGHT_COLOR;
            mNeutralDarkColor = NEUTRAL_DARK_COLOR;
            mNeutralLightColor = NEUTRAL_LIGHT_COLOR;
        } else {
            mPositiveDarkColor = this.mPositiveDarkColor;
            mPositiveLightColor = this.mPositiveLightColor;
            mNegativeDarkColor = this.mNegativeDarkColor;
            mNegativeLightColor = this.mNegativeLightColor;
            mNeutralDarkColor = this.mNeutralDarkColor;
            mNeutralLightColor = this.mNeutralLightColor;
        }

        int start1 = (int) (mScaleStartAngle - 90);
        int end = 180 - start1;

        int sweep1 = (int) (needleAngle - start1);

        int start2 = (int) needleAngle;
        int sweep2 = end - start2;

        if (mCurrentValue > 50.1)
            p.setColor(mPositiveLightColor);
        else if (mCurrentValue < 49.9) {

            p.setColor(mNegativeLightColor);
        } else {
            p.setColor(mNeutralLightColor);

        }
        canvas.drawArc(rectF, -start1, -sweep1, true, p);
        if (mCurrentValue > 50.1)
            p.setColor(mPositiveDarkColor);
        else if (mCurrentValue < 49.9) {

            p.setColor(mNegativeDarkColor);
        } else {
            p.setColor(mNeutralDarkColor);

        }
        canvas.drawArc(rectF, -start2, -sweep2, true, p);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);

    }

    private void drawText(final Canvas canvas) {
        final String textValue = !TextUtils.isEmpty(mTextValue) ? mTextValue : valueString(mCurrentValue);
        final float textValueWidth = mTextValuePaint.measureText(textValue);
        final float textUnitWidth = !TextUtils.isEmpty(mTextUnit) ? mTextUnitPaint.measureText(mTextUnit) : 0;

        final float startX = CENTER - textUnitWidth / 2;
        final float startY = CENTER + 0.1f;

        canvas.drawText(textValue, startX, startY, mTextValuePaint);

        if (!TextUtils.isEmpty(mTextUnit)) {
            canvas.drawText(mTextUnit, CENTER + textValueWidth / 2 + 0.03f, CENTER, mTextUnitPaint);
        }
    }

    private String valueString(final float value) {
        return String.format(Locale.getDefault(), "%d", (int) value);
    }

    private void drawNeedle(final Canvas canvas) {
        if (mNeedleInitialized) {
            final float angle = getAngleForValue(mCurrentValue);
            // Logger.log.info(String.format("value=%f -> angle=%f", mCurrentValue, angle));

            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.rotate(angle, 0.5f, 1.0f);

            setNeedleShadowPosition(angle);
            canvas.drawPath(mNeedleLeftPath, mNeedleLeftPaint);
            canvas.drawPath(mNeedleRightPath, mNeedleRightPaint);

            canvas.restore();

            // Draw the needle screw and its border
//            canvas.drawCircle(0.5f, 1.0f, 0.04f, mNeedleScrewPaint);
            canvas.drawCircle(0.5f, 1.0f, 0.1f, mNeedleScrewPaint);
            canvas.drawCircle(0.5f, 1.0f, 0.04f, mNeedleScrewBorderPaint);
        }
    }

    private void setNeedleShadowPosition(final float angle) {
        // MODIFIED
        if (isInEditMode()) return;
        if (angle > 180 && angle < 360) {
            // Move shadow from right to left
            mNeedleRightPaint.setShadowLayer(0, 0, 0, Color.BLACK);
            mNeedleLeftPaint.setShadowLayer(0.01f, -0.005f, 0.005f, Color.argb(127, 0, 0, 0));
        } else {
            // Move shadow from left to right
            mNeedleLeftPaint.setShadowLayer(0, 0, 0, Color.BLACK);
            mNeedleRightPaint.setShadowLayer(0.01f, 0.005f, -0.005f, Color.argb(127, 0, 0, 0));
        }
    }

    private float getAngleForValue(final float value) {
//        return (float) ((mScaleRotation + ((value - mScaleStartValue) / mSubdivisionValue) * mSubdivisionAngle / 2.0) % 360);
        return (mScaleRotation + ((value - mScaleStartValue) / mSubdivisionValue) * mSubdivisionAngle) % 360;
//        return (mScaleRotation + ((value - mScaleStartValue) / mSubdivisionValue) * mSubdivisionAngle) % 180;
//        return (float) 25.0;
    }

    private void computeCurrentValue() {
        // Logger.log.warn(String.format("velocity=%f, acceleration=%f", mNeedleVelocity,
        // mNeedleAcceleration));

        if (!(Math.abs(mCurrentValue - mTargetValue) > 0.01f)) {
            return;
        }

        if (-1 != mNeedleLastMoved) {
            final float time = (System.currentTimeMillis() - mNeedleLastMoved) / 1000.0f;
            final float direction = Math.signum(mNeedleVelocity);
            if (Math.abs(mNeedleVelocity) < 90.0f) {
                mNeedleAcceleration = 5.0f * (mTargetValue - mCurrentValue);
            } else {
                mNeedleAcceleration = 0.0f;
            }

            mNeedleAcceleration = 5.0f * (mTargetValue - mCurrentValue);
            mCurrentValue += mNeedleVelocity * time;
            mNeedleVelocity += mNeedleAcceleration * time;

            if ((mTargetValue - mCurrentValue) * direction < 0.01f * direction) {
                mCurrentValue = mTargetValue;
                mNeedleVelocity = 0.0f;
                mNeedleAcceleration = 0.0f;
                mNeedleLastMoved = -1L;
            } else {
                mNeedleLastMoved = System.currentTimeMillis();
            }

            invalidate();

        } else {
            mNeedleLastMoved = System.currentTimeMillis();
            computeCurrentValue();
        }
    }

    public void setTargetValue(final float value) {
        if (mShowScale || mShowRanges) {
            if (value < mScaleStartValue) {
                mTargetValue = mScaleStartValue;
            } else if (value > mScaleEndValue) {
                mTargetValue = mScaleEndValue;
            } else {
                mTargetValue = value;
            }
        } else {
            mTargetValue = value;
        }
        mNeedleInitialized = true;
        invalidate();
    }

}
