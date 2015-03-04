package com.cyngn.uicommon.view;

import android.content.res.Resources;
import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;

public class ViewUtil {

    private static final ViewOutlineProvider RECT_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
        }
    };

    public static void addRectangularOutlineProvider(View view) {
        view.setOutlineProvider(RECT_OUTLINE_PROVIDER);
    }
}
