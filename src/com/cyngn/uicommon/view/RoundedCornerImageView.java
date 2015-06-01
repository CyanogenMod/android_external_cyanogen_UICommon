/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.cyngn.uicommon.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.cyngn.uicommon.R;

/**
 * RoundedCornerImageView
 * <pre>
 *
 *     Image view that allows you to manipulate the corners by rounding them using a given radius.
 *     This image view has limitations as defined below:
 *     - [roundTopLeft] + [roundTopRight] + [roundBottomLeft] = all 4 corners rounded
 *     - Rounding 2 diagonally rounds all 4 ([roundTopLeft] + [roundBottomRight] = all 4 corners rounded)
 *
 *     Example usage:
 *      roundBottomLeft="true"
 *      roundBottomRight="true"
 *
 *     This will give us the result of rounding 2 bottom corners.
 *
 * </pre>
 *
 * @since 9/18/14
 */
public class RoundedCornerImageView extends ImageView {

    // Shape stuffs
    public static final int SHAPE_ID_RECTANGLE = 1;
    public static final int SHAPE_ID_CIRCLE = 2;

    public static enum Shape {
        RECTANGLE,
        CIRCLE
    }

    // Constants
    public static final String TAG = RoundedCornerImageView.class.getSimpleName();
    public static final float DEFAULT_RECTANGLE_RADIUS = 5.0f;
    public static final float DEFAULT_CIRCLE_RADIUS = 75.0f;
    public static final float ASPECT_RATIO_1_1 = 1.0f;
    public static final float ASPECT_RATIO_16_9 = 1.7f;
    public static final float ASPECT_RATIO_4_3 = 1.3f;
    public static final float DEFAULT_ASPECT_RATIO = ASPECT_RATIO_1_1;

    private static final Paint sCanvasPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint sMaskXferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint sRestorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Setup paints
    static {
        sCanvasPaint.setAntiAlias(true);
        sCanvasPaint.setColor(Color.argb(255, 255, 255, 255));
        sMaskXferPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
        sRestorePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
    }

    // Members
    private final Rect mBounds = new Rect();
    private final RectF mBoundsF = new RectF();
    private float mRadius = DEFAULT_RECTANGLE_RADIUS;
    private Shape mShape = Shape.RECTANGLE;

    // Flags
    private boolean mRoundTopLeft = false;
    private boolean mRoundTopRight = false;
    private boolean mRoundBottomLeft = false;
    private boolean mRoundBottomRight = false;
    private boolean mClampLayoutToAspectRatio = false;

    /**
     * Constructor
     *
     * @param context {@link android.content.Context}
     */
    public RoundedCornerImageView(Context context) {
        this(context, null);
    }

    /**
     * Constructor
     *
     * @param context {@link android.content.Context}
     * @param attrs   {@link android.util.AttributeSet}
     */
    public RoundedCornerImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor
     *
     * @param context  {@link android.content.Context}
     * @param attrs    {@link android.util.AttributeSet}
     * @param defStyle {@link java.lang.Integer}
     */
    public RoundedCornerImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.RoundedCornerImageView, defStyle, 0);
            mRoundTopLeft = a.getBoolean(R.styleable.RoundedCornerImageView_roundTopLeft, false);
            mRoundTopRight = a.getBoolean(R.styleable.RoundedCornerImageView_roundTopRight, false);
            mRoundBottomLeft = a.getBoolean(R.styleable.RoundedCornerImageView_roundBottomLeft,
                    false);
            mRoundBottomRight = a.getBoolean(R.styleable.RoundedCornerImageView_roundBottomRight,
                    false);
            int shapeId = a.getInt(R.styleable.RoundedCornerImageView_shape, SHAPE_ID_RECTANGLE);
            mShape = (shapeId != 2) ? Shape.RECTANGLE : Shape.CIRCLE;
            float defaultRadius =
                    (mShape == Shape.CIRCLE) ? DEFAULT_CIRCLE_RADIUS : DEFAULT_RECTANGLE_RADIUS;
            mRadius = a.getDimensionPixelSize(R.styleable.RoundedCornerImageView_radius,
                    (int) defaultRadius);
            a.recycle();
        }
    }

    /**
     * Overridden to hijack the scale type from being set if we need to clamp
     *
     * @param scaleType The desired scaling mode.
     */
    @Override
    public void setScaleType(ScaleType scaleType) {
        if (mClampLayoutToAspectRatio) {
            super.setScaleType(ScaleType.FIT_XY);
        } else {
            super.setScaleType(scaleType);
        }
    }

    /**
     * Enable clamping of the layout to the image to keep same aspect ratio
     *
     * @param enabled {@link java.lang.Boolean}
     */
    public void setLayoutToRatioClampEnabled(boolean enabled) {
        mClampLayoutToAspectRatio = enabled;
        if (mClampLayoutToAspectRatio) {
            setScaleType(ScaleType.FIT_XY);
        }
    }

    /**
     * Set rounded flag
     *
     * @param rounded {@link java.lang.Boolean}
     */
    public void setRoundedTopLeft(boolean rounded) {
        mRoundTopLeft = rounded;
    }

    /**
     * Set rounded flag
     *
     * @param rounded {@link java.lang.Boolean}
     */
    public void setRoundedTopRight(boolean rounded) {
        mRoundTopRight = rounded;
    }

    /**
     * Set rounded flag
     *
     * @param rounded {@link java.lang.Boolean}
     */
    public void setRoundedBottomLeft(boolean rounded) {
        mRoundBottomLeft = rounded;
    }

    /**
     * Set rounded flag
     *
     * @param rounded {@link java.lang.Boolean}
     */
    public void setRoundedBottomRight(boolean rounded) {
        mRoundBottomRight = rounded;
    }

    /**
     * Set the radius
     *
     * @param radius {@link java.lang.Float}
     */
    public void setRadius(float radius) {
        mRadius = radius;
    }

    /**
     * Set the image view shape
     *
     * @param shape {@link com.cyngn.uicommon.view.RoundedCornerImageView.Shape}
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    public void setShape(Shape shape) throws IllegalArgumentException {
        if (shape == null) throw new IllegalArgumentException("'shape' cannot be null!");
        mShape = shape;
        mRadius = (mShape == Shape.CIRCLE) ? DEFAULT_CIRCLE_RADIUS : DEFAULT_RECTANGLE_RADIUS;
    }

    /**
     * Overridden to clamp aspect ratio of the drawable to view width
     *
     * @param widthMeasureSpec  {@link java.lang.Integer}
     * @param heightMeasureSpec {@link java.lang.Integer}
     */
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        // If we aren't clamping, then we don't need to do anything
        if (!mClampLayoutToAspectRatio) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        // Ensure scale type
        setScaleType(ScaleType.FIT_XY);

        // Check the drawable
        Drawable drawable = getDrawable();
        float aspectRatio = DEFAULT_ASPECT_RATIO;
        // If we have one, calculate the aspect ratio
        if (drawable != null) {
            // Set the drawable aspect ratio
            float dWidth = drawable.getIntrinsicWidth();
            float dHeight = drawable.getIntrinsicHeight();
            // If portrait
            if (dHeight > dWidth) {
                // Don't change aspect ratio
                // Force center cropping
                super.setScaleType(ScaleType.CENTER_CROP);
            } else {
                // Invert teh ratios
                aspectRatio = dHeight / dWidth;
            }
        }

        // Get new dimensions
        int originWidth = MeasureSpec.getSize(widthMeasureSpec);
        int calcHeight = (int) ((float) originWidth * aspectRatio);

        // Measure views
        super.onMeasure(MeasureSpec.makeMeasureSpec(originWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(calcHeight, MeasureSpec.EXACTLY));

    }

    /**
     * This is overridden in order to use porterduff to make anything outside
     * of the rounded rectangle disappear.
     *
     * @param canvas {@link android.graphics.Canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.getClipBounds(mBounds);
        if (mShape == Shape.CIRCLE) {
            mBoundsF.set(mBounds);
        } else {
            float topOffset = (mRoundTopLeft || mRoundTopRight) ? 0 : DEFAULT_RECTANGLE_RADIUS;
            float bottomOffset =
                    (mRoundBottomLeft || mRoundBottomRight) ? 0 : DEFAULT_RECTANGLE_RADIUS;
            float leftOffset = (mRoundBottomLeft) ? 0 : DEFAULT_RECTANGLE_RADIUS;
            float rightOffset = (mRoundBottomRight) ? 0 : DEFAULT_RECTANGLE_RADIUS;
            mBoundsF.set(mBounds.left - leftOffset, mBounds.top - topOffset, mBounds.right +
                    rightOffset, mBounds.bottom + bottomOffset);
        }
        canvas.saveLayer(mBoundsF, sRestorePaint, Canvas.ALL_SAVE_FLAG);
        super.onDraw(canvas);
        canvas.saveLayer(mBoundsF, sMaskXferPaint, Canvas.ALL_SAVE_FLAG);
        canvas.drawARGB(0, 0, 0, 0);
        if (mShape == Shape.CIRCLE) {
            canvas.drawCircle(mBounds.centerX(), mBounds.centerY(), mRadius, sCanvasPaint);
        } else {
            canvas.drawRoundRect(mBoundsF, mRadius, mRadius, sCanvasPaint);
        }
        canvas.restore();
        canvas.restore();
    }

}
