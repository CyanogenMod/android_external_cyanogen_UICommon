package com.cyngn.uicommon.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
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

    private static final int EXPAND_DURATION = 200;
    private ListView mList;
    private ViewGroup mRowContainer;
    private boolean mRowContainerInitialized;

    public void setListView(ListView listView) {
        mList = listView;
    }

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
    private View mContainerView;
    private View mMainView;
    private View mAuxView;

    private int mAuxTop = -1;
    private int mMainBottom = -1;
    private ColorDrawable mColor;
    private GradientDrawable mColorSelected;
    private int mCardElevation;

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
        mContainerView = findViewById(R.id.containerView);

        mMainView = findViewById(R.id.mainView);
        mAuxView = findViewById(R.id.auxiliaryView);

        Resources res = getResources();
        mCardElevation = res.getDimensionPixelSize(R.dimen.expanding_card_elevation);
        mColor = new ColorDrawable(res.getColor(R.color.expanding_card_color));
        mColorSelected = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{res.getColor(R.color.expanding_card_start_gradient), res.getColor(R.color.expanding_card_selected_color)});
    }

    /**
     * Expand the card using the given type of animation
     *
     * @param type
     */
    public void expand(AnimationType type) {
        mAuxView.setVisibility(View.VISIBLE);

        int mid = mMainView.getHeight() - mAuxView.getHeight();
        int bottom = mMainView.getHeight();

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
                mContainerView.setBackground(mColorSelected);
                if (mRowContainer != null) {
                    mRowContainer.setTranslationZ(mCardElevation);
                }
                break;
        }

        if (anim != null) {
            mAuxView.setAlpha(0f);
            List<Animator> animations = new ArrayList<Animator>();
            animations.add(anim);
            animations.add(getShadowAnimation(true));

            if (mList != null && mRowContainer != null) {
                // Set up the animator to animate the expansion and shadow depth.
                ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
                int scrollingNeeded = 0;

                if (mRowContainer.getTop() < 0) {
                    scrollingNeeded = mRowContainer.getTop(); // view at top/partially visible
                } else {
                    int listViewHeight = mList.getHeight();
                    int offset = mRowContainer.getTop() + mRowContainer.getHeight()
                            + mAuxView.getHeight() - listViewHeight;
                    if (offset > 0) {
                        scrollingNeeded = offset;
                    }
                }
                final int finalScrollingNeeded = scrollingNeeded;
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    private int mCurrentScroll = 0;

                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {
                        Float value = (Float) animator.getAnimatedValue();
                        if (mList != null) {
                            int scrollBy = (int) (value * finalScrollingNeeded) - mCurrentScroll;
                            mList.smoothScrollBy(scrollBy, /* duration = */ 0);
                            mCurrentScroll += scrollBy;
                        }
                    }
                });
                animations.add(animator);
            }

            AnimatorSet set = new AnimatorSet();
            set.playTogether(animations);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    TransitionDrawable transitionDrawable =
                            new TransitionDrawable(new Drawable[]{mColor, mColorSelected});
                    mContainerView.setBackground(transitionDrawable);
                    transitionDrawable.startTransition(EXPAND_DURATION);
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    getAlphaAnimation(true).start();
                }
            });
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
            List<Animator> animations = new ArrayList<Animator>();
            animations.add(getShadowAnimation(false));
            animations.add(anim);
            Animator alphaAnimator = getAlphaAnimation(false);
            animations.add(alphaAnimator);
            final AnimatorSet set = new AnimatorSet();
            set.playTogether(animations);
            set.setDuration(EXPAND_DURATION);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{mColorSelected, mColor});
                    mContainerView.setBackground(transitionDrawable);
                    transitionDrawable.startTransition(EXPAND_DURATION);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mAuxView.setVisibility(View.INVISIBLE);
                }
            });
            set.start();
        } else {
            // layouts are already collapsed.  reset colors/visibility for completeness
            mAuxView.setVisibility(View.INVISIBLE);
            resetColors();
        }
    }

    private Animator getAlphaAnimation(boolean isExpand) {
        float start = isExpand ? 0f : 1f;
        float end = isExpand ? 1f : 0f;
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(
                mAuxView, View.ALPHA, start, end);
        alphaAnimator.setDuration(100);
        if (!isExpand) {
            alphaAnimator.setInterpolator(new Interpolator() {
                @Override
                public float getInterpolation(float input) {
                    return Math.min(1, input * 4);
                }
            });
        } else {
            alphaAnimator.setInterpolator(new AccelerateInterpolator());
        }
        return alphaAnimator;
    }

    /**
     * Gradually decrease the margins of the shadow view so that the shadow grows over
     * time, creating the illusion that the card is lifting up out of the view.
     *
     * @param isExpand
     * @return
     */
    private Animator getShadowAnimation(boolean isExpand) {
        float fromElevation = isExpand ? 0 : mCardElevation;
        float toElevation = mCardElevation - fromElevation;

        ValueAnimator anim = ValueAnimator.ofFloat(fromElevation, toElevation);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                if (mRowContainer != null) {
                    mRowContainer.setTranslationZ((Float) animator.getAnimatedValue());
                    mRowContainer.requestLayout();
                }
            }
        });
        return anim;
    }

    /**
     * Puts card back in original collapsed state
     */
    public void reset() {
        resetColors();
        if (mRowContainer != null) {
            mRowContainer.setTranslationZ(0);
        }
        mAuxView.setVisibility(View.INVISIBLE);
        new BottomMarginSetter().setMargin(mMainView, 0);
        new TopMarginSetter().setMargin(mAuxView, 0);
        mAuxTop = -1;
        mMainBottom = -1;
    }

    private void resetColors() {
        mContainerView.setBackground(mColor);
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
        private long mSelectedCardId = -1;
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
         * @param cardId
         */
        public void onBindExpandingCard(final ExpandingCard card, final long cardId, final int position) {
            card.reset();
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (cardId == mSelectedCardId) {
                        card.collapse();
                        mSelectedCardId = -1;
                        mSelectedCard = null;
                    } else {
                        int selectedCardPosition = -1;
                        // when the selection is moved from one card to another, we want the
                        // newly selected card to expand into the space left by the collpasing
                        // one.
                        if (mSelectedCardId >= 0) {
                            for (int i = mList.getFirstVisiblePosition(); i <= mList.getLastVisiblePosition(); i++) {
                                long id = mList.getAdapter().getItemId(i);
                                if (id == mSelectedCardId) {
                                    selectedCardPosition = i;
                                    break;
                                }
                            }
                        }
                        if (selectedCardPosition != -1 && position > selectedCardPosition) {
                            card.expand(AnimationType.ANCHOR_BOTTOM);
                        } else {
                            card.expand(AnimationType.ANCHOR_TOP);
                        }

                        // If the currently selected card is in view, animate it closing.
                        // We're assuming that our reference to the selected card view is still
                        // valid is long as it is visible.
                        if (mSelectedCard != null && selectedCardPosition >= 0) {
                            mSelectedCard.collapse();
                        }

                        mSelectedCardId = cardId;
                        mSelectedCard = card;
                    }
                }
            });
            if (cardId == mSelectedCardId) {
                // selected card is coming back into view on a scroll, show selected
                // state without animation
                mSelectedCard = card;
                card.expand(AnimationType.NONE);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initializeRowContainer();
    }

    private void initializeRowContainer() {
        if (!mRowContainerInitialized) {
            ViewGroup lastView = (ViewGroup) getParent();
            while (lastView != null) {
                if (lastView.getParent() instanceof ListView) {
                    mRowContainer = lastView;
                    ViewUtil.addRectangularOutlineProvider(mRowContainer);
                    break;
                }
                lastView = (ViewGroup) lastView.getParent();
            }
            mList.setClipChildren(false);
            mList.setClipToPadding(false);
        }
        mRowContainerInitialized = true;
    }
}
