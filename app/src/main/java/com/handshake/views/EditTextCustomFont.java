package com.handshake.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Created by ankitgoyal on 6/11/15.
 */
public class EditTextCustomFont extends EditText {

    public EditTextCustomFont(Context context) {
        super(context);
    }

    public EditTextCustomFont(Context context, AttributeSet attrs) {
        super(context, attrs);
//        CustomFontHelper.setCustomFont(this, context, attrs);
        if (!isInEditMode()) {
            Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "helveticaneue.ttf");
            setTypeface(tf);
        }
    }

    public EditTextCustomFont(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
//        CustomFontHelper.setCustomFont(this, context, attrs);
        if (!isInEditMode()) {
            Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "helveticaneue.ttf");
            setTypeface(tf);
        }
    }
}