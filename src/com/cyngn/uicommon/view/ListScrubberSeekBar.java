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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

/**
 * Override SeekBar for use in list scrubber
 */
public class ListScrubberSeekBar extends SeekBar {

    public ListScrubberSeekBar(Context context) {
        super(context);
    }

    public ListScrubberSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListScrubberSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean isInScrollingContainer() {
        // For the list scrubber seek bar we can simplify things by assuming this
        // is false.  This would only backfire if we attempt to put a list scrubber
        // inside a list item.
        return false;
    }
}
