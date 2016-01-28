package by.aleks.chordbro;

import android.content.Context;
import android.graphics.Paint;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Alex on 1/28/16.
 */
public class ChordView extends TextView {

    private static final String TAG = "ChordView";

    public ChordView(Context context) {
        super(context);
    }

    public ChordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ChordView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        adjustTextSize();
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(styleString(text.toString()), type);
        adjustTextSize();
    }

    private void adjustTextSize(){

        float viewWidth = getMeasuredWidth();

        float maxTextSizeSp = pixelsToSp(getTextSize());

        //Find the longest line
        String[] lines = getText().toString().split("\n");
        String longestLine = "";
        for(String line:lines){
            if(line.length()>longestLine.length())
                longestLine = line;
        }
//        Log.d(TAG, longestLine);
        setTextSize(6); //start with the small text size
        //Adjust the font size to fit the longest line
        while(true){
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)getLayoutParams();
            if(viewWidth == 0 || lp == null) // Exit if the view hasn't appeared yet
                return;

            Paint textPaint = getPaint();
            float textWidth = textPaint.measureText(longestLine);
            float indents = getTotalPaddingLeft() + getTotalPaddingRight() + lp.leftMargin + lp.rightMargin;

            float textViewSizeSp = pixelsToSp(getTextSize());
            if (textViewSizeSp > maxTextSizeSp)
                break;
            Log.d(TAG, "Indents = " + indents);
            Log.d(TAG, "TextView width: " + getWidth() + "Measured width: " + textWidth + ", Text size:" + textViewSizeSp);

            if(viewWidth*0.95f - indents > textWidth){ // 0.95f - experimental value
                setTextSize(textViewSizeSp + 0.6f);
            } else break;
        }
    }

    public float pixelsToSp(float px) {
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        return px/scaledDensity;
    }

    private Spanned styleString(String songText){
        String styled = songText.replace("<span class=\"line_end\"></span>", "<br>").replace(" ", "&nbsp;").replace("[ch]", "<font color="+ getResources().getColor(R.color.colorAccentDark)+">").replace("[/ch]", "</font>");
        return Html.fromHtml(styled);
    }
}
