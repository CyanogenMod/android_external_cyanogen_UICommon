package com.cyngn.uicommon.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;
import com.cyngn.uicommon.R;

import java.util.ArrayList;
import java.util.List;

/**
 * An informational view that has an auxiliary view that slides out from the bottom.
 * This auxiliary view is typically used to show actions that can be taken on the
 * main view.
 */
public class ExpandingCard extends FrameLayout {

    private static final int EXPAND_DURATION = 150;

    public static enum AnimationType {
        // the bottom of the aux view is anchored and the content view slides
        // up to reveal it
        ANCHOR_BOTTOM,

        // the top of the main view is anchored and the aux view slides out
        // from the bottom
        ANCHOR_TOP,

        // no animation, anchor to the top
        NONE,
    }

    private View mExpandingCard;
    private View mMainView;
    private View mAuxView;
    private View mShadowView;

    private int mAuxTop = -1;
    private int mAuxHeight;
    private int mMainBottom = -1;
    private int mColor;
    private int mColorSelected;
    private int mShadowMarginTop;
    private int mShadownMarginBottom;

    public ExpandingCard(Context context) {
        super(context);
    }

    public ExpandingCard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExpandingCard(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mExpandingCard = findViewById(R.id.expandingCard);
        mMainView = findViewById(R.id.mainView);
        mAuxView = findViewById(R.id.auxiliaryView);
        mShadowView = findViewById(R.id.shadowView);

        Resources res = getResources();
        mAuxHeight = res.getDimensionPixelSize(R.dimen.expanding_card_aux_height);
        mColor = res.getColor(R.color.expanding_card_color);
        mColorSelected = res.getColor(R.color.expanding_card_selected_color);

        MarginLayoutParams lp = (MarginLayoutParams)mShadowView.getLayoutParams();
        mShadowMarginTop = lp.topMargin;
        mShadownMarginBottom = lp.bottomMargin;
    }

    /**
     * Expand the card using the given type of animation
     *
     * @param type
     */
    public void expand(AnimationType type) {
        mAuxView.setVisibility(View.VISIBLE);

        int mid = getAuxTop();
        int bottom = getMainBottom();

        ValueAnimator anim = null;
        switch(type) {
            case ANCHOR_BOTTOM:
                anim = ValueAnimator.ofInt(0, bottom - mid);
                anim.addUpdateListener(
                        new MarginTwiddler(mMainView, new BottomMarginSetter()));
                break;
            case ANCHOR_TOP:
                anim = ValueAnimator.ofInt(mid, bottom);
                anim.addUpdateListener(
                        new MarginTwiddler(mAuxView, new TopMarginSetter()));
                break;
            case NONE:
                new TopMarginSetter().setMargin(mAuxView, bottom);
                mMainView.setBackgroundColor(mColorSelected);
                mAuxView.setBackgroundColor(mColorSelected);
                new TopMarginSetter().setMargin(mShadowView, 0);
                new BottomMarginSetter().setMargin(mShadowView, 0);
                break;
        }

        if (anim != null) {
            List<Animator> animations = getColorAnimations(mColor, mColorSelected);
            animations.add(anim);
            animations.addAll(getShadowAnimations(true));

            AnimatorSet set = new AnimatorSet();
            set.playTogether(animations);
            set.setDuration(EXPAND_DURATION);
            set.start();
        }
    }

    /**
     * Collapse the card using the same anchor that was used to expand it.
     */
    public void collapse() {
        MarginLayoutParams mlp = (MarginLayoutParams)mMainView.getLayoutParams();
        MarginLayoutParams alp = (MarginLayoutParams)mAuxView.getLayoutParams();

        // whichever margin is out of alignment due to an expand, animate that margin back
        // to its original position
        ValueAnimator anim = null;
        if (mlp.bottomMargin > 0) {
            anim = ValueAnimator.ofInt(mlp.bottomMargin, 0);
            anim.addUpdateListener(
                    new MarginTwiddler(mMainView, new BottomMarginSetter()));
        } else if (alp.topMargin > 0) {
            anim = ValueAnimator.ofInt(alp.topMargin, 0);
            anim.addUpdateListener(
                    new MarginTwiddler(mAuxView, new TopMarginSetter()));
        }

        if (anim != null) {
            anim.setDuration(EXPAND_DURATION);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}

                @Override
                public void onAnimationCancel(Animator animation) {}

                @Override
                public void onAnimationRepeat(Animator animation) {}

                @Override
                public void onAnimationEnd(Animator animation) {
                    mAuxView.setVisibility(View.GONE);
                }
            });

            List<Animator> animations = getColorAnimations(mColorSelected, mColor);
            animations.addAll(getShadowAnimations(false));
            animations.add(anim);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(animations);
            set.setDuration(EXPAND_DURATION);
            set.start();
        } else {
            // layouts are already collapsed.  reset colors/visibility for completeness
            mAuxView.setVisibility(View.GONE);
            resetColors();
        }
    }

    /**
     * Interpolate color change over time
     *
     * @param fromColor
     * @param toColor
     * @return
     */
    private List<Animator> getColorAnimations(int fromColor, int toColor) {
        ObjectAnimator colorAnim1 =
                ObjectAnimator.ofInt(mMainView, "backgroundColor", fromColor, toColor);
        colorAnim1.setEvaluator(new ArgbEvaluator());

        ObjectAnimator colorAnim2 =
                ObjectAnimator.ofInt(mAuxView, "backgroundColor", fromColor, toColor);
        colorAnim2.setEvaluator(new ArgbEvaluator());

        List<Animator> result = new ArrayList<Animator>();
        result.add(colorAnim1);
        result.add(colorAnim2);
        return result;
    }

    /**
     * Gradually decrease the margins of the shadow view so that the shadow grows over
     * time, creating the illusion that the card is lifting up out of the view.
     *
     * @param isExpand
     * @return
     */
    private List<Animator> getShadowAnimations(boolean isExpand) {
        int fromTop, toTop, fromBottom, toBottom;
        if (isExpand) {
            fromTop = mShadowMarginTop;
            toTop = 0;
            fromBottom = mShadownMarginBottom;
            toBottom = 0;
        } else {
            fromTop = 0;
            toTop = mShadowMarginTop;
            fromBottom = 0;
            toBottom = mShadownMarginBottom;
        }

        List<Animator> result = new ArrayList<Animator>();

        ValueAnimator topAnim = ValueAnimator.ofInt(fromTop, toTop);
        topAnim.addUpdateListener(new MarginTwiddler(mShadowView, new TopMarginSetter()));
        result.add(topAnim);

        ValueAnimator bottomAnim = ValueAnimator.ofInt(fromBottom, toBottom);
        bottomAnim.addUpdateListener(new MarginTwiddler(mShadowView, new BottomMarginSetter()));
        result.add(bottomAnim);

        return result;
    }

    /**
     * Puts card back in original collapsed state
     */
    public void reset() {
        resetColors();
        mAuxView.setVisibility(View.GONE);
        new BottomMarginSetter().setMargin(mMainView, 0);
        new TopMarginSetter().setMargin(mAuxView, 0);
        new BottomMarginSetter().setMargin(mShadowView, mShadownMarginBottom);
        new TopMarginSetter().setMargin(mShadowView, mShadowMarginTop);
        mAuxTop = -1;
        mMainBottom = -1;
    }

    private void resetColors() {
        mMainView.setBackgroundColor(mColor);
        mAuxView.setBackgroundColor(mColor);
    }

    private class MarginTwiddler implements ValueAnimator.AnimatorUpdateListener {

        private View mView;
        private MarginSetter mMarginSetter;

        public MarginTwiddler(View view, MarginSetter m) {
            mView = view;
            mMarginSetter = m;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int val = (Integer)animation.getAnimatedValue();
            mMarginSetter.setMargin(mView, val);
            mExpandingCard.requestLayout();
        }
    }

    private int getAuxTop() {
        if (mAuxTop < 0) {
            mAuxTop = getMainBottom() - mAuxHeight;
        }
        return mAuxTop;
    }

    private int getMainBottom() {
        if (mMainBottom < 0) {
            // assumption: main view is bigger than aux view.  We could use getBottom here,
            // but getHeight seems more robust since the height never changes
            mMainBottom = mMainView.getHeight();
        }
        return mMainBottom;
    }

    private static interface MarginSetter {
        public void setMargin(View v, int margin);
    }

    private static class TopMarginSetter implements MarginSetter {
        @Override
        public void setMargin(View v, int margin) {
            FrameLayout.LayoutParams lp =
                    (FrameLayout.LayoutParams)v.getLayoutParams();
            lp.topMargin = margin;
            v.setLayoutParams(lp);
        }
    }

    private static class BottomMarginSetter implements MarginSetter {
        @Override
        public void setMargin(View v, int margin) {
            FrameLayout.LayoutParams lp =
                    (FrameLayout.LayoutParams)v.getLayoutParams();
            lp.bottomMargin = margin;
            v.setLayoutParams(lp);
        }
    }

    /**
     * Helper class that can be used by the adapter to manage the state of expanding
     * cards.  It tracks the position of the currently selected card, and adds a click
     * handler to manage expanding/collpasing.
     */
    public static class ExpandingCardManager {
        private int mSelectedPosition = -1;
        private ExpandingCard mSelectedCard;
        private ListView mList;

        public ExpandingCardManager(ListView list) {
            mList = list;
        }

        /**
         * Invoke this method whenever an expanding card view is bound to a certain position
         * in the list
         *
         * @param card
         * @param position
         */
        public void onBindExpandingCard(final ExpandingCard card, final int position) {
            card.reset();
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (position == mSelectedPosition) {
                        card.collapse();
                        mSelectedPosition = -1;
                        mSelectedCard = null;
                    } else {
                        // when the selection is moved from one card to another, we want the
                        // newly selected card to expand into the space left by the collpasing
                        // one.
                        if (mSelectedPosition >= 0 && position > mSelectedPosition) {
                            card.expand(AnimationType.ANCHOR_BOTTOM);
                        } else {
                            card.expand(AnimationType.ANCHOR_TOP);
                        }

                        // If the currently selected card is in view, animate it closing.
                        // We're assuming that our reference to the selected card view is still
                        // valid is long as it is visible.
                        if (mSelectedPosition >= mList.getFirstVisiblePosition() &&
                                mSelectedPosition <= mList.getLastVisiblePosition() &&
                                mSelectedCard != null) {
                            mSelectedCard.collapse();
                        }

                        mSelectedPosition = position;
                        mSelectedCard = card;
                    }
                }
            });
            if (position == mSelectedPosition) {
                // selected card is coming back into view on a scroll, show selected
                // state without animation
                mSelectedCard = card;
                card.expand(AnimationType.NONE);
            }
        }
    }
}
