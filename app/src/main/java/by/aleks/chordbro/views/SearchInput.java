package by.aleks.chordbro.views;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import by.aleks.chordbro.R;

/**
 * Created by Alex on 1/30/16.
 */

public class SearchInput extends EditText {

    private BackButtonListener listener;

    public SearchInput(Context context) {
        super(context);
    }

    public SearchInput(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchInput(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setBackButtonListener(BackButtonListener listener){
        this.listener = listener;
    }



    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(listener != null)
                listener.onBackButtonPressed();
        }
        return false;
    }
}