package com.cyngn.uicommon.view;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.SensorManager;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.cyngn.uicommon.R;

/**
 * Container for a list view that intercepts touch events to translate the list view
 * when possible instead of scrolling.
 *
 * The list view is translated when:
 *      - it is in the "down" position and we scroll up, or
 *      - it is in the "up" position and we scroll down
 *
 * If the user continues the touch gesture after the list is translated, we pass
 * scroll operations down to the list so that it appears fluid.
 *
 * We do the same for the fling gesture, passing it on to the list view
 *
 * Known limitations:
 *      - Once the list view starts handling a scroll it calls requestDisallowInterceptTouchEvent.
 *        This can prevent us from stealing the event in certain cases, e.g. if the user
 *        scrolls down first and then changes directions in the same gesture.
 */
public class SlidingListLayout extends FrameLayout implements AbsListView.OnScrollListener {

    public static interface OnTranslateListener {
        /**
         * Reports a change in list translation
         *
         * @param yStart the initial translation offset
         * @param yEnd final translation offset
         * @param yValue the current translation amount
         */
        public void onTranslate(float yStart, float yEnd, float yValue);
    }

    private enum TranslateState {
        // list is pushed "down" to the initial translate position defined by initialOffset
        DOWN,

        // list is expanded "up", i.e. has zero translation
        UP,

        // in between up and down
        PARTIAL
    }

    /**
     * Amount of time (ms) it takes for list to move from DOWN to UP
     */
    private static final float DEFAULT_SETTLE_DURATION = 200f;
    private static final boolean DBG = false;
    private static final String TAG = "SlidingListLayout";

    private OnTranslateListener mTranslateListener;
    private ListView mList;
    private FlingCalculator mFlingCalculator;

    private boolean mFirstLayout = true;
    private float mInitialTranslateY;
    private float mInitialMotionY;
    private float mLastMotionY;
    private int mTouchSlop;
    private VelocityTracker mVelocityTracker;
    private float mDefaultVelocity;
    private float mMinVelocity;

    // need to define an inner frame to translate.  if we translate ourselves then our y
    // coordinates change which mess up the calculations.
    private FrameLayout mFrame;

    public SlidingListLayout(Context context) {
        this(context, null);
    }

    public SlidingListLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingListLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SlidingListLayout);
            if (ta != null) {
                int initialOffset =
                        ta.getDimensionPixelSize(R.styleable.SlidingListLayout_initialOffset, 0);
                mInitialTranslateY = initialOffset;
                ta.recycle();
            }
        }

        ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mFlingCalculator = new FlingCalculator(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mFrame = new FrameLayout(mContext);
        mFrame.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        // reparent our current children
        for (int i=0; i < getChildCount(); ++i) {
            View child = getChildAt(i);
            removeView(child);
            mFrame.addView(child);
        }

        addView(mFrame);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mFirstLayout) {
            mFrame.setTranslationY(mInitialTranslateY);
            mDefaultVelocity = mInitialTranslateY / DEFAULT_SETTLE_DURATION;
            mMinVelocity = mDefaultVelocity / 2;
            mFirstLayout = false;
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    /**
     * Steals the motion event if the user attempts to scroll up when the list is
     * in the DOWN position so that we can replace the upward scroll with a list
     * translation.
     *
     * @param ev The motion event being dispatched down the hierarchy.
     * @return true to steal
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = MotionEventCompat.getActionMasked(ev);
        float y = ev.getY();
        TranslateState state = getTranslateState();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mInitialMotionY = y;
                mLastMotionY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mInitialMotionY;
                if (Math.abs(dy) < mTouchSlop) {
                    // "unreasonable" hack
                    // when we pass a downward smooth scroll to the list to simulate a fling,
                    // if ends up in a state where mScrollY is non-zero even after the list is
                    // at rest.  this causes the list to think it is in overscroll mode, which
                    // causes it to steal the move event before the touch slop threshold is reached.
                    // We can't properly intercept here when that happens.  This hack fixes the
                    // issue... probably needs more investigation to fully understand though.
                    if (mList.getScrollY() != 0) {
                        mList.setScrollY(0);
                    }
                    return false;
                }

                if (dy < 0) {
                    return state != TranslateState.UP;
                } else if (dy > 0) {
                    return state != TranslateState.DOWN;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = MotionEventCompat.getActionMasked(ev);
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // getting a down on the container itself indicates an attempt to tap on
                // the area outside the list view.  return false here to pass this on to
                // any views drawn underneath us.
                return false;

            case MotionEvent.ACTION_MOVE:
                handleMove(ev);
                break;

            case MotionEvent.ACTION_UP:
                handleUp(ev);
                recycleVelocityTracker();
                break;

            case MotionEvent.ACTION_CANCEL:
                recycleVelocityTracker();
                break;
        }
        return true;
    }

    private void handleMove(MotionEvent ev) {
        TranslateState state = getTranslateState();
        float y = ev.getY();
        float dy = y - mLastMotionY;
        if (dy < 0) {
            // moving up
            // translate list if there is room, otherwise scroll
            if (state != TranslateState.UP) {
                updateTranslation(dy);
            } else {
                scrollListByOffset(dy);
            }
        } else if (dy > 0) {
            // moving down
            if (state != TranslateState.DOWN) {
                updateTranslation(dy);
            } else {
                scrollListByOffset(dy);
            }
        }

        mLastMotionY = y;
    }

    private void handleUp(MotionEvent ev) {
        TranslateState state = getTranslateState();
        mVelocityTracker.computeCurrentVelocity(1);
        float yvel = mVelocityTracker.getYVelocity();

        Float target = null;
        if (Math.abs(yvel) > mMinVelocity) {
            target = yvel > 0 ? mInitialTranslateY : 0;
        } else if (state == TranslateState.PARTIAL) {
            // if we have no velocity but the list is not settled, snap it to
            // the nearest endpoint
            target = (mFrame.getTranslationY() > mInitialTranslateY / 2) ?
                            mInitialTranslateY : 0;
        }

        if (target != null) {
            float vel = Math.max(mMinVelocity, Math.abs(yvel));
            settleListAt(target, vel);
        }

        if (yvel != 0) {
            fling(yvel);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(
            AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    /**
     * Move translation clamped to 0, mInitialTranslateY
     *
     * @param dy relative amount to move translation
     */
    private void updateTranslation(float dy) {
        float txY = mFrame.getTranslationY() + dy;
        float clampedY = Math.min(Math.max(txY, 0), mInitialTranslateY);
        mFrame.setTranslationY(clampedY);
        if (mTranslateListener != null) {
            mTranslateListener.onTranslate(mInitialTranslateY, 0, clampedY);
        }
    }

    /**
     * @return translate state based on actual position of list
     */
    private TranslateState getTranslateState() {
        float txY = mFrame.getTranslationY();
        if (txY <= 0) {
            return TranslateState.UP;
        } else if (txY >= mInitialTranslateY) {
            return TranslateState.DOWN;
        } else {
            return TranslateState.PARTIAL;
        }
    }

    /**
     * Scroll the underlying list by dy pixels
     *
     * @param dy
     */
    private void scrollListByOffset(float dy) {
        if (mList.getChildCount() > 0) {
            int topPos = mList.getFirstVisiblePosition();
            int topOffset = mList.getChildAt(0).getTop();
            int newOffset = topOffset + (int) dy;
            mList.setSelectionFromTop(topPos, newOffset);
        }
    }

    /**
     * Animate the list translation so that it settles at the given translation position
     *
     * @param destTx translation value to settle at.  should be in the range (0, initialOffset)
     */
    private void settleListAt(float destTx, float yvel) {
        if (yvel != 0) {
            float dist = destTx - mFrame.getTranslationY();
            float dt = Math.abs(dist / yvel);

            ObjectAnimator anim =
                    ObjectAnimator.ofFloat(mFrame, "translationY",
                            mFrame.getTranslationY(), destTx);
            anim.setDuration((long)dt);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (mTranslateListener != null) {
                        Float txY = (Float)animation.getAnimatedValue();
                        mTranslateListener.onTranslate(mInitialTranslateY, 0, txY);
                    }
                }
            });
            anim.start();
        }
    }

    private void fling(float yvel) {
        int dist = (int)mFlingCalculator.getSplineFlingDistance((int)yvel);
        int time = mFlingCalculator.getSplineFlingDuration((int)yvel);

        // when flinging down the list scrolls up
        if (yvel > 0) {
            dist *= -1;
        }

        if (DBG) {
            Log.v(TAG, "Flinging yvel=" + yvel + ", dist=" + dist + ", time=" + time);
        }
        mList.smoothScrollBy(dist, time);
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    public void setListView(ListView listView) {
        mList = listView;
    }

    public ListView getListView() {
        return mList;
    }

    public void setOnTranslateListener(OnTranslateListener listener) {
        mTranslateListener = listener;
    }

    public OnTranslateListener getOnTranslateListener() {
        return mTranslateListener;
    }

    /**
     * Helper class: given a velocity, calculate fling duration and distance.
     *
     * Ported from OverScroller.java
     */
    private static class FlingCalculator {

        private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
        private float mFlingFriction = ViewConfiguration.getScrollFriction();
        private float mPhysicalCoeff;
        private static float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));


        public FlingCalculator(Context context) {
            final float ppi = context.getResources().getDisplayMetrics().density * 160.0f;
            mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
                    * 39.37f // inch/meter
                    * ppi
                    * 0.84f; // look and feel tuning
        }

        private double getSplineDeceleration(int velocity) {
            // multiply velocity by 1000 to get pixels per second
            return Math.log(INFLEXION * Math.abs(velocity*1000) /
                    (mFlingFriction * mPhysicalCoeff));
        }

        private double getSplineFlingDistance(int velocity) {
            final double l = getSplineDeceleration(velocity);
            final double decelMinusOne = DECELERATION_RATE - 1.0;
            return mFlingFriction * mPhysicalCoeff
                    * Math.exp(DECELERATION_RATE / decelMinusOne * l);
        }

        private int getSplineFlingDuration(int velocity) {
            final double l = getSplineDeceleration(velocity);
            final double decelMinusOne = DECELERATION_RATE - 1.0;
            return (int) (1000.0 * Math.exp(l / decelMinusOne));
        }
    }
}
