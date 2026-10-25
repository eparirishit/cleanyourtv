package com.gathin.tvhome;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

final class GlassMenu {
    private GlassMenu() { }

    static void addAction(LinearLayout rows, PopupWindow popup, int label, int icon, Runnable action) {
        TextView row = (TextView) LayoutInflater.from(rows.getContext())
                .inflate(R.layout.item_context_menu, rows, false);
        row.setText(label);
        Drawable symbol = rows.getContext().getDrawable(icon);
        int size = Math.round(20 * rows.getResources().getDisplayMetrics().density);
        symbol.setBounds(0, 0, size, size);
        row.setCompoundDrawablesRelative(symbol, null, null, null);
        row.setOnClickListener(view -> {
            popup.dismiss();
            action.run();
        });
        final boolean[] pressed = {false};
        row.setOnKeyListener((view, code, event) -> {
            if (code != KeyEvent.KEYCODE_DPAD_CENTER && code != KeyEvent.KEYCODE_ENTER) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0) pressed[0] = true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                boolean activate = pressed[0] && !event.isCanceled();
                pressed[0] = false;
                if (activate) row.performClick();
            }
            return true;
        });
        rows.addView(row);
    }

    static void show(PopupWindow popup, View anchor, int gravity, boolean animations, boolean reducedMotion) {
        ScrollView panel = (ScrollView) popup.getContentView();
        float density = anchor.getResources().getDisplayMetrics().density;
        int gap = Math.round(10 * density);
        Rect frame = new Rect();
        anchor.getWindowVisibleDisplayFrame(frame);
        int maxWidth = Math.max(1, frame.width() - gap * 2);
        panel.measure(View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int width = Math.min(maxWidth, Math.max(popup.getWidth(), panel.getMeasuredWidth()));
        panel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        popup.setWidth(width);
        popup.setHeight(Math.max(1, Math.min(panel.getMeasuredHeight(), popup.getMaxAvailableHeight(anchor, gap))));
        popup.setAnimationStyle(0);
        popup.setElevation(0);
        int duration = UiMotion.duration(animations, reducedMotion, 160);
        panel.setSmoothScrollingEnabled(duration > 0);
        if (duration > 0) panel.setAlpha(0);
        popup.showAsDropDown(anchor, 0, gap, gravity);
        if (duration > 0) {
            panel.setPivotY(popup.isAboveAnchor() ? popup.getHeight() : 0);
            panel.setScaleY(0.97f);
            panel.setTranslationY((popup.isAboveAnchor() ? 4 : -4) * density);
            panel.animate().alpha(1).scaleY(1).translationY(0).setDuration(duration)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f)).start();
        }
        LinearLayout rows = panel.findViewById(R.id.rows);
        if (rows.getChildCount() > 0) rows.getChildAt(0).requestFocus();
    }
}
