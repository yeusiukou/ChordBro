package by.aleks.chordbro;

import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Alex on 1/26/16.
 */
public class Util {

    private static final String TAG = "Util";

    public static void adjustTextSize(TextView textView, int maxTextSize){

        //Find the longest line
        String[] lines = textView.getText().toString().split("\n");
        String longestLine = "";
        for(String line:lines){
            if(line.length()>longestLine.length())
                longestLine = line;
        }
        Log.d(TAG, longestLine);
        //Adjust the font size to fit the longest line
        while(true){
            Paint textPaint = textView.getPaint();
            float width = textPaint.measureText(longestLine);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
            float indents = textView.getTotalPaddingLeft() + textView.getTotalPaddingRight() + lp.leftMargin + lp.rightMargin;

            float textViewSizeSp = pixelsToSp(textView.getTextSize(), textView);
            if (textViewSizeSp > maxTextSize)
                break;
//            Log.d(TAG, "Indents = " + indents);
//            Log.d(TAG, "TextView width: " + textView.getWidth() + "Measured width: " + width + ", Text size:" + textViewSizeSp);

            if(textView.getMeasuredWidth()*0.95f - indents > width){ // 0.95f - experimental value
                textView.setTextSize(textViewSizeSp + 0.3f);
            } else break;
        }
    }

    public static float pixelsToSp(float px, View view) {
        float scaledDensity = view.getResources().getDisplayMetrics().scaledDensity;
        return px/scaledDensity;
    }

    public static float spToPixels(float sp, View view) {
        float scaledDensity = view.getResources().getDisplayMetrics().scaledDensity;
        return sp*scaledDensity;
    }

}
