package com.handshake.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.handshake.Handshake.CustomFontHelper;

/**
 * Created by ankitgoyal on 6/11/15.
 */
public class TextViewCustomFont extends TextView {

    public TextViewCustomFont(Context context) {
        super(context);
    }

    public TextViewCustomFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        CustomFontHelper.setCustomFont(this, context, attrs);
    }

    public TextViewCustomFont(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        CustomFontHelper.setCustomFont(this, context, attrs);
    }
}