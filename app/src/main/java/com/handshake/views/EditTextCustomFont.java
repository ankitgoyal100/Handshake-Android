package com.handshake.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
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

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new EditTextInputConnection(super.onCreateInputConnection(outAttrs),
                true);
    }

    private class EditTextInputConnection extends InputConnectionWrapper {

        public EditTextInputConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                // Un-comment if you wish to cancel the backspace:
                // return false;
            }
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            // magic: in latest Android, deleteSurroundingText(1, 0) will be called for backspace
            if (beforeLength == 1 && afterLength == 0) {
                // backspace
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }

            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }
}