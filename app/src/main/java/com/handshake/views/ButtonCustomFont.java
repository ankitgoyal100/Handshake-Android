package com.handshake.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Created by ankitgoyal on 6/11/15.
 */
public class ButtonCustomFont extends Button {

    public ButtonCustomFont(Context context) {
        super(context);
    }

    public ButtonCustomFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        CustomFontHelper.setCustomFont(this, context, attrs);
    }

    public ButtonCustomFont(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        CustomFontHelper.setCustomFont(this, context, attrs);
    }
}