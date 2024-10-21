package com.aiicon.market.workathome;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BackgroundWebView extends WebView {

    public BackgroundWebView(@NonNull Context context) {
        super(context);
    }

    public BackgroundWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BackgroundWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BackgroundWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility != View.GONE) {
            super.onWindowVisibilityChanged(View.VISIBLE);
        }
    }
}
