package by.aleks.chordbro;

import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SongFragment extends Fragment {

    private static final String TEXT_PARAM = "text";
    private String text;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param text Song text
     * @return A new instance of fragment SongFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SongFragment newInstance(String text) {
        SongFragment fragment = new SongFragment();
        Bundle args = new Bundle();
        args.putString(TEXT_PARAM, text);
        fragment.setArguments(args);
        return fragment;
    }

    public SongFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_song, container, false);
        if (getArguments() != null) {
            text = getArguments().getString(TEXT_PARAM);
            TextView songTV = (TextView)view.findViewById(R.id.song_text);
            songTV.setText(styleString(text));
            adjustTextSize(songTV);
        }
        return view;
    }

    private Spanned styleString(String songText){
        String styled = songText.replace("<span class=\"line_end\"></span>", "<br>").replace(" ", "&nbsp;&nbsp;").replace("[ch]", "<font color=\"blue\">").replace("[/ch]", "</font>");
        return Html.fromHtml(styled);
    }

    private void adjustTextSize(TextView textView){

        //Find the longest line
        String[] lines = textView.getText().toString().split("\n");
        String longestLine = "";
        for(String line:lines){
            if(line.length()>longestLine.length())
                longestLine = line;
        }

        //Adjust the font size to fit the longest line
        Rect bounds = new Rect();
        int width = 0;
        while(true){
            Paint textPaint = textView.getPaint();
            textPaint.getTextBounds(longestLine,0,longestLine.length(),bounds);
            width = bounds.width();

            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
            float indents = textView.getTotalPaddingLeft() + textView.getTotalPaddingRight() + lp.leftMargin + lp.rightMargin;
            //0.7 is a value I found experimentally.
            if(textView.getWidth()*0.7 - indents > width){
                textView.setTextSize(pixelsToSp(textView.getTextSize())+0.3f);
                Log.d("adjustTS", "tv: " + textView.getWidth() + "width: " + width + ", size:" + textView.getTextSize());
            }
            else break;
        }
    }

    private float pixelsToSp(float px) {
        float scaledDensity = getView().getResources().getDisplayMetrics().scaledDensity;
        return px/scaledDensity;
    }
}
