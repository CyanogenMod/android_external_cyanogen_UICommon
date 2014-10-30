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
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import com.cyngn.uicommon.animation.FadeUtil;
import com.cyngn.uicommon.view.ListScrubber.ListScrubberListener;

/**
 * State machine to manage fading the list scrubber in and out when the user interacts
 * with the list view or the scrubber itself.
 */
public class ListScrubberFadeHelper {

    private static final int FADE_OUT_DELAY = 2000;

    public static enum State {
        VISIBLE,
        HIDDEN,
        FADING_IN,
        FADING_OUT,
        FADE_OUT_SCHEDULED,
    };

    public static enum Event {
        LIST_FLING,
        LIST_SCROLL,
        LIST_IDLE,
        LIST_IDLE_TIMEOUT,
        SCRUBBER_TOUCH,
        SCRUBBER_RELEASE,
    }

    private ViewGroup mScrubber;
    private Handler mHandler;
    private Runnable mFadeOutRunnable;
    private State mState;
    private ListScrubberListener mListener;

    class FadeOutRunnable implements Runnable {
        @Override
        public void run() {
            onEvent(Event.LIST_IDLE_TIMEOUT);
        }
    }

    public ListScrubberFadeHelper(ViewGroup scrubber) {
        mScrubber = scrubber;
        mHandler = new Handler();
    }

    public void onEvent(Event event) {
        switch (event) {
            case LIST_FLING:
            case LIST_SCROLL:
                if (mState == State.FADING_OUT || mState == State.FADE_OUT_SCHEDULED) {
                    cancelFade();
                    updateState(State.VISIBLE);
                } else if (mState == State.HIDDEN) {
                    updateState(State.FADING_IN);
                }
                break;

            case LIST_IDLE:
                updateState(State.FADE_OUT_SCHEDULED);
                break;

            case LIST_IDLE_TIMEOUT:
                updateState(State.FADING_OUT);
                break;

            case SCRUBBER_TOUCH:
                if (mState == State.FADING_OUT || mState == State.FADE_OUT_SCHEDULED) {
                    cancelFade();
                }
                break;

            case SCRUBBER_RELEASE:
                updateState(State.FADE_OUT_SCHEDULED);
                break;
        }
    }

    public void updateState(State state) {
        mState = state;
        switch (state) {
            case VISIBLE:
                mScrubber.setAlpha(1f);
                mScrubber.setVisibility(View.VISIBLE);
                if (mListener != null) {
                    mListener.onAppeared();
                }
                break;
            case HIDDEN:
                mScrubber.setAlpha(0f);
                mScrubber.setVisibility(View.INVISIBLE);
                if (mListener != null) {
                    mListener.onDisappeared();
                }
                break;
            case FADING_IN:
                if (mListener != null) {
                    mListener.onAppearing();
                }
                mScrubber.animate().alpha(1f).setDuration(FadeUtil.DEFAULT_FADE_DURATION)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                // strange that this is required...
                                updateState(State.VISIBLE);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                updateState(State.VISIBLE);
                            }
                        });
                break;
            case FADING_OUT:
                if (mListener != null) {
                    mListener.onDisappearing();
                }
                mScrubber.animate().alpha(0f).setDuration(FadeUtil.DEFAULT_FADE_DURATION)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                // strange that this is required...
                                updateState(State.HIDDEN);
                            }
                            @Override
                            public void onAnimationCancel(Animator animation) {
                                updateState(State.HIDDEN);
                            }
                        });
                break;
            case FADE_OUT_SCHEDULED:
                mFadeOutRunnable = new FadeOutRunnable();
                mHandler.postDelayed(mFadeOutRunnable, FADE_OUT_DELAY);
                break;
        }
    }

    private void cancelFade() {
        if (mFadeOutRunnable != null) {
            mHandler.removeCallbacks(mFadeOutRunnable);
            mFadeOutRunnable = null;
        }
        mScrubber.animate().cancel();
    }

    public void setListScrubberListener(ListScrubberListener listener) {
        mListener = listener;
    }
}
