package by.aleks.chordbro.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import by.aleks.chordbro.R;

/**
 * Created by Alex on 1/29/16.
 */

public class VisualizerView extends View {

    private byte[] mBytes;
    private float[] mPoints;
    private Rect mRect = new Rect();
    private Paint mForePaint = new Paint();
    private int frameCount = 0;
    private Handler mHandler = new Handler();

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBytes = null;
        mForePaint.setStrokeWidth(1.5f);
        mForePaint.setAntiAlias(true);
        mForePaint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
    }

    public void updateVisualizer(byte[] bytes) {
        mBytes = bytes;
        invalidate();
    }

    public void resetAnimation(){
        frameCount = 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mRect.set(0, 0, getWidth(), getHeight());

        int width = mRect.width();
        int height = mRect.height();
        int threshold = 30;
        if (mBytes == null) {
            return;
        }

        if (frameCount < threshold){
            canvas.drawLine( (width - width/threshold*frameCount)/2, height/2, (width + width/threshold*frameCount)/2, height/2, mForePaint);
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    frameCount++;
                    invalidate();
                }
            }, 1000/25); //update with 25fps
            return;
        }

        int byteLength = mBytes.length;
        if (mPoints == null || mPoints.length < byteLength * 4) { //Show only the values with even indexes, as the odd ones are -1
            // I show every 4th byte to simplify the diagram
            mPoints = new float[byteLength*2];
        }

        for (int i = 4; i < byteLength/2 - 2; i+=2) {
            // Every line consists of 4 values: x1, y1, x2, y2
            mPoints[i * 4] = mRect.width() * i / (byteLength/2 - 1);
            mPoints[i * 4 + 1] = mRect.height() / 2+mBytes[i]*mRect.height()/2/128; // As the values are from -128 to 128
            mPoints[i * 4 + 2] = mRect.width() * (i + 1) / (byteLength/2 - 1);
            mPoints[i * 4 + 3] = mRect.height() / 2+mBytes[i+2]*mRect.height()/2/128;
        }

        canvas.drawLines(mPoints, mForePaint);
    }



}