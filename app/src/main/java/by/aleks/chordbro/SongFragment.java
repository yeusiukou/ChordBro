package by.aleks.chordbro;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

public class SongFragment extends Fragment {

    private static final String TEXT_PARAM = "text";
    private String text;
    private static final String TAG = "SongFragment";

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
            DynamicTextView songTV = (DynamicTextView)view.findViewById(R.id.song_text);
            songTV.setText(styleString(text));

            Typeface myTypeface = Typeface.createFromAsset(getActivity().getAssets(), "monaco.ttf");
            songTV.setTypeface(myTypeface);

            Log.d("fragment", "DONE!");
        }
        return view;
    }

    private Spanned styleString(String songText){
        String styled = songText.replace("<span class=\"line_end\"></span>", "<br>").replace(" ", "&nbsp;").replace("[ch]", "<font color="+ getResources().getColor(R.color.colorAccentDark)+">").replace("[/ch]", "</font>");
        return Html.fromHtml(styled);
    }
}
