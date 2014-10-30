/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cyngn.uicommon.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.HeaderViewListAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.SeekBar;
import android.widget.TextView;
import com.cyngn.uicommon.R;

/**
 * Widget to fast scroll through a list view with a horizontal scrubber.
 * Usage:
 *     scrubber = new ListScrubber(context);
 *     scrubber.setFadeInOnMotion(true); // optional
 *     scrubber.setSource(listView); // call when adapter is ready
 */
public class ListScrubber extends LinearLayout implements OnClickListener {

    private SectionIndexer mSectionIndexer;
    private RelativeLayout mScrubberWidget;
    private ListView mListView;
    private RecyclerView mRecyclerView;
    private TextView mFirstIndicator, mLastIndicator;
    private TextView mScrubberIndicator;
    private SeekBar mSeekBar;
    private String[] mSections;
    private int mHeaderCount = 0;
    private boolean mFadeInOnMotion;
    private ListScrubberFadeHelper mFadeHelper;
    private ListScrubberListener mListener;

    public static interface ListScrubberListener {

        /**
         * Called when list scrubber is beginning to fade in
         */
        public void onAppearing();

        /**
         * List scrubber is fully visible
         */
        public void onAppeared();

        /**
         * Called when list scrubber is beginning to fade out
         */
        public void onDisappearing();

        /**
         * List scrubber is fully gone
         */
        public void onDisappeared();
    }

    /**
     * Convenience implementation of list scrubber listener that does nothing
     */
    public static class DefaultListScrubberListener implements ListScrubberListener {
        @Override
        public void onDisappeared() {}

        @Override
        public void onDisappearing() {}

        @Override
        public void onAppeared() {}

        @Override
        public void onAppearing() {}
    }

    public ListScrubber(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.scrub_layout, this);
        mScrubberWidget = (RelativeLayout)findViewById(R.id.scrubberWidget);
        mFirstIndicator = ((TextView) findViewById(R.id.firstSection));
        mFirstIndicator.setOnClickListener(this);
        mLastIndicator = ((TextView) findViewById(R.id.lastSection));
        mLastIndicator.setOnClickListener(this);
        mScrubberIndicator = (TextView) findViewById(R.id.scrubberIndicator);
        mSeekBar = (SeekBar) findViewById(R.id.scrubber);
        init();
    }

    public void updateSections() {
        mSections = (String[]) mSectionIndexer.getSections();
        if (mSections.length > 0) {
            mSeekBar.setMax(mSections.length - 1);
            mFirstIndicator.setText(mSections[0]);
            mLastIndicator.setText(mSections[mSections.length - 1]);
        }
    }

    public void setSource(ListView listView) {
        resetSource();
        mListView = listView;

        ListAdapter adapter = listView.getAdapter();
        if (adapter instanceof HeaderViewListAdapter) {
            mHeaderCount = ((HeaderViewListAdapter) adapter).getHeadersCount();
            adapter = ((HeaderViewListAdapter) adapter).getWrappedAdapter();
        }

        if (adapter instanceof SectionIndexer) {
            mSectionIndexer = (SectionIndexer)adapter;
        } else {
            throw new IllegalArgumentException("ListView adapter must implement SectionIndexer");
        }
    }

    public void setSource(RecyclerView recyclerView) {
        resetSource();
        mRecyclerView = recyclerView;
        mSectionIndexer = (SectionIndexer)mRecyclerView.getAdapter();

    }

    private void resetSource() {
        mRecyclerView = null;
        mListView = null;
    }

    private boolean isReady() {
        return (mListView != null || mRecyclerView != null) &&
                mSectionIndexer != null &&
                mSections != null;
    }

    private void init() {
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                if (!isReady()) {
                    return;
                }
                resetScrubber();
                mScrubberIndicator.setTranslationX((progress * seekBar.getWidth()) / mSections.length);
                String section = String.valueOf(mSections[progress]);
                scrollToPositionWithOffset(mSectionIndexer.getPositionForSection(progress)
                        + mHeaderCount, 0);
                mScrubberIndicator.setText(section);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (!isReady()) {
                    return;
                }
                resetScrubber();
                mScrubberIndicator.setAlpha(1f);
                mScrubberIndicator.setVisibility(View.VISIBLE);
                if (mFadeInOnMotion) {
                    mFadeHelper.onEvent(ListScrubberFadeHelper.Event.SCRUBBER_TOUCH);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!isReady()) {
                    return;
                }
                resetScrubber();
                mScrubberIndicator.animate().alpha(0f).translationYBy(20f)
                    .setDuration(200).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mScrubberIndicator.setVisibility(View.INVISIBLE);
                    }
                });
                if (mFadeInOnMotion) {
                    mFadeHelper.onEvent(ListScrubberFadeHelper.Event.SCRUBBER_RELEASE);
                }
            }

            private void resetScrubber() {
                mScrubberIndicator.animate().cancel();
                mScrubberIndicator.setTranslationY(0f);
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v == mFirstIndicator) {
            int positionForFirstSection = mSectionIndexer.getPositionForSection(0);
            scrollToPositionWithOffset(positionForFirstSection, 0);
        } else if (v == mLastIndicator) {
            int positionForLastSection = mSectionIndexer.getPositionForSection(mSections.length - 1);
            scrollToPositionWithOffset(positionForLastSection, 0);
        }
    }

    /**
     * If set to true, the scrubber appears once the list is scrolled and disappears when
     * the list is idle for some predetermined amount of time.
     *
     * Defaults to false
     *
     * @param fadeInOnMotion true to enable fading behavior
     */
    public void setFadeInOnMotion(boolean fadeInOnMotion) {
        if (fadeInOnMotion) {
            mFadeHelper = new ListScrubberFadeHelper(mScrubberWidget);
            mFadeHelper.setListScrubberListener(mListener);
            mFadeHelper.updateState(ListScrubberFadeHelper.State.HIDDEN);
        }
        else {
            mFadeHelper = null;
        }
        mFadeInOnMotion = fadeInOnMotion;
    }

    public boolean isFadeInOnMotion() {
        return mFadeInOnMotion;
    }

    private void scrollToPositionWithOffset(int position, int y) {
        if (mListView != null) {
            mListView.setSelectionFromTop(position, y);
        }
        else if (mRecyclerView != null) {
            LinearLayoutManager layoutManager =
                    (LinearLayoutManager)mRecyclerView.getLayoutManager();
            layoutManager.scrollToPositionWithOffset(position, y);
        }
    }

    public void handleScrollStateChanged(int scrollState) {
        if (!mFadeInOnMotion)
            return;

        switch (scrollState) {
            case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                mFadeHelper.onEvent(ListScrubberFadeHelper.Event.LIST_SCROLL);
                break;
            case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
                mFadeHelper.onEvent(ListScrubberFadeHelper.Event.LIST_FLING);
                break;
            case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                mFadeHelper.onEvent(ListScrubberFadeHelper.Event.LIST_IDLE);
                break;
        }
    }

    public void setHeaderCount(int count) {
        mHeaderCount = count;
    }

    public void setListScrubberListener(ListScrubberListener listener) {
        mListener = listener;
        if (mFadeHelper != null) {
            mFadeHelper.setListScrubberListener(listener);
        }
    }
}