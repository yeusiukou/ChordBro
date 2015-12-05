package by.aleks.chordbro;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class SongActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final TextView songTV = (TextView)findViewById(R.id.song_text);

        final String artist = "Muse";
        final String title = "Uprising";

        new AsyncTask<Void, Void, Spanned>() {
            @Override
            protected Spanned doInBackground(Void... voids) {
                return loadSongSpannable(artist, title);
            }

            @Override
            protected void onPostExecute(Spanned spanned) {
                songTV.setText(spanned);
                adjustTextSize(songTV);
            }
        }.execute();
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
            //0.8 is a value I found experimentally.
            if(textView.getWidth()*0.8 - indents > width){
                textView.setTextSize(pixelsToSp(textView.getTextSize())+0.3f);
                Log.d("adjustTS", "tv: "+textView.getWidth()+"width: "+width+", size:"+textView.getTextSize());
            }
            else break;
        }
    }

    private float pixelsToSp(float px) {
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        return px/scaledDensity;
    }


    private String loadSongText(String urlString){

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw XML response as a string.
        String songStr = null;

        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            songStr = buffer.toString();
        } catch (IOException e) {
            Log.e("PlaceholderFragment", "Error ", e);
            // If the code didn't successfully get the template, there's no point in attemping
            // to parse it.
            return null;
        } finally{
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("TemplateLoader", "Error closing stream", e);
                }
            }
        }
        return songStr;
    }

    private Document parseXML(String xmlString){
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        try {
            Document document = builder.parse(new InputSource(new StringReader(xmlString)));
            return document;
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Spanned loadSongSpannable(String artist, String title){
        String songUrl = "http://app.ultimate-guitar.com/search.php?search_type=title&page=1&iphone=1&value="+artist+"+"+title;
        Document song = parseXML(loadSongText(songUrl));
        NodeList songList = song.getElementsByTagName("result");

        //Find the best version
        Element bestElement = null;
        for(int i=0; i<songList.getLength(); i++){

            Element currentElement = (Element)songList.item(i);
            if( currentElement.getAttribute("type").equals("chords")){
                if(bestElement == null){
                    bestElement = currentElement;
                    continue;
                }
                // Find a song with the best rating and the highest amount of votes
                if( Integer.valueOf(currentElement.getAttribute("rating")) > Integer.valueOf(bestElement.getAttribute("rating")))
                    bestElement = currentElement;
                else if( Integer.valueOf(currentElement.getAttribute("rating")) == Integer.valueOf(bestElement.getAttribute("rating"))
                        && Integer.valueOf(currentElement.getAttribute("votes")) > Integer.valueOf(bestElement.getAttribute("votes")))
                    bestElement = currentElement;
            }
        }

        String bestVersionUrl = bestElement.getAttribute("url");
        return styleString(loadSongText(bestVersionUrl));
    }

    private Spanned styleString(String songText){
        String styled = songText.replace("<span class=\"line_end\"></span>", "<br>").replace(" ", "&nbsp;&nbsp;").replace("[ch]", "<font color=\"blue\">").replace("[/ch]", "</font>");
        return Html.fromHtml(styled);
    }
}
