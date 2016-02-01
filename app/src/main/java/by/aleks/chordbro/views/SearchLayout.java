package by.aleks.chordbro.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import by.aleks.chordbro.LayoutCommunicator;
import by.aleks.chordbro.R;

/**
 * Created by Alex on 1/30/16.
 */
public class SearchLayout extends LinearLayout {

    private LinearLayout searchBar;
    private LinearLayout recognizerLayout;
    private VisualizerView visualizerView;
    private ImageView backButton;
    private SearchInput input;
    private TextView status;
    private LayoutCommunicator communicator;

    public SearchLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SearchLayout(Context context) {
        super(context);
    }


    public void init(){
        searchBar = (LinearLayout)findViewById(R.id.searchbar);
        recognizerLayout = (LinearLayout)findViewById(R.id.recognizer_layout);
        backButton = (ImageView)findViewById(R.id.search_back_button);
        visualizerView = (VisualizerView)findViewById(R.id.visualizer);
        status = (TextView)findViewById(R.id.recognizer_status);
        input = (SearchInput)findViewById(R.id.search_input);

        input.getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP); // Make the underline white

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hide();
            }
        });
        //TODO: Stop recognition, when user starts typing
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                recognizerLayout.setVisibility(View.GONE);
                communicator.onType();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    public void setLayoutCommunicator(LayoutCommunicator layoutCommunicator){
        communicator = layoutCommunicator;
    }

    public void reveal(){
        searchBar.setVisibility(View.VISIBLE);
        visualizerView.resetAnimation();

        input.requestFocus();
        // Custom listener as onBackPressed() is not called when the keyboard is shown
        input.setBackButtonListener(new BackButtonListener() {
            @Override
            public void onBackButtonPressed() {
                hide();
            }
        });
        // Show keyboard
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);

        status.setText(getContext().getString(R.string.listening));
        recognizerLayout.setVisibility(View.VISIBLE);

        if(communicator != null)
            communicator.onLayoutShow();
    }

    public void hide(){
        searchBar.setVisibility(View.GONE);
        recognizerLayout.setVisibility(View.GONE);

        input.setText("");
        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);

        if(communicator != null)
            communicator.onLayoutHide();
    }

    public void updateStatus(String text){
        status.setText(text);
    }

    public boolean isRevealed(){
        return searchBar.getVisibility() == VISIBLE;
    }
}
