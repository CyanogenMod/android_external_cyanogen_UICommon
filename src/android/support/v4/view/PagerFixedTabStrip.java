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
package android.support.v4.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

/**
 * Tab strip similar to {@link PagerTabStrip} except using fixed tabs instead of scrolling
 * tabs.  Use as a child view of a ViewPager widget in XML.
 *
 * Note: this class must be in the android.support.v4.view package in order to have access
 * to protected interfaces.
 *
 * TODO: include attributes to customize the style of selected/unselected tabs
 */
public class PagerFixedTabStrip extends LinearLayout {

    private PageListener mPageListener = new PageListener();
    private ViewPager mPager;

    public PagerFixedTabStrip(Context context) {
        super(context);
    }

    public PagerFixedTabStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PagerFixedTabStrip(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        final ViewParent parent = getParent();
        if (!(parent instanceof ViewPager)) {
            throw new IllegalStateException(
                    "PagerTitleStrip must be a direct child of a ViewPager.");
        }

        final ViewPager pager = (ViewPager) parent;
        pager.setOnPageChangeListener(mPageListener);
        mPager = pager;
        setupTabs();
    }

    private void setupTabs() {
        PagerAdapter adapter = mPager.getAdapter();
        for (int i=0; i < adapter.getCount(); ++i) {
            CharSequence label = adapter.getPageTitle(i);
            Button button = new Button(getContext());
            button.setText(label);
            button.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
            final int buttonIdx = i;
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPager.setCurrentItem(buttonIdx);
                }
            });
            button.setGravity(Gravity.CENTER);
            addView(button);
        }
    }

    // TODO: when a new page is selected, update styles
    private class PageListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }
}

