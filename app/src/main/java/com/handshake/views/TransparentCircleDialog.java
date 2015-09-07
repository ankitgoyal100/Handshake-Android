package com.handshake.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

import com.handshake.Handshake.R;

/**
 * Created by ankitgoyal on 8/24/15.
 */
public class TransparentCircleDialog extends View {

    Bitmap bm;
    Canvas cv;
    Paint eraser;

    public TransparentCircleDialog(Context context) {
        super(context);
        Init();
    }

    public TransparentCircleDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        Init();
    }

    public TransparentCircleDialog(Context context, AttributeSet attrs,
                                   int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Init();
    }

    private void Init(){

        eraser = new Paint();
        eraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        eraser.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        if (w != oldw || h != oldh) {
            bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            cv = new Canvas(bm);
        }
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int w = getWidth();
        int h = getHeight();
        int radius = w > h ? h / 2 : w / 2;

        bm.eraseColor(Color.TRANSPARENT);
        cv.drawColor(getResources().getColor(R.color.dialog_background));
        cv.drawCircle(w / 2, h / 2, radius, eraser);
        canvas.drawBitmap(bm, 0, 0, null);
        super.onDraw(canvas);
    }
}