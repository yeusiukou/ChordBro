package by.aleks.chordbro;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.Log;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Alex on 12/7/15.
 */
public class SongContentLoader extends AsyncTask<String, Void, Map<String, String>>{

    private Context context;

    public SongContentLoader(Context context){
        this.context = context;
    }

    @Override
    protected Map doInBackground(String... strings) {
        return loadSongSpannable(strings[0], strings[1]);
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
                Log.d("adjustTS", "tv: " + textView.getWidth() + "width: " + width + ", size:" + textView.getTextSize());
            }
            else break;
        }
    }

    private float pixelsToSp(float px) {
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
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

    private Map loadSongSpannable(String artist, String title){
        String songUrl = "http://app.ultimate-guitar.com/search.php?search_type=title&page=1&iphone=1&value="+artist+"+"+title;
        Document song = parseXML(loadSongText(songUrl));
        NodeList songList = song.getElementsByTagName("result");

        //Find the best version
        Map<String, Element> bestElements = new HashMap<>();

        for(int i=0; i<songList.getLength(); i++){

            Element currentElement = (Element)songList.item(i);

            String type = currentElement.getAttribute("type");
            if( !bestElements.containsKey(type) ){
                bestElements.put(type, currentElement);
                continue;
            }
            Element bestElement = bestElements.get(type);
            // Find a song with the best rating and the highest amount of votes
            if (Integer.valueOf(currentElement.getAttribute("rating")) > Integer.valueOf(bestElement.getAttribute("rating")))
                bestElements.put(type, currentElement);
            else if (Integer.valueOf(currentElement.getAttribute("rating")) == Integer.valueOf(bestElement.getAttribute("rating"))
                    && Integer.valueOf(currentElement.getAttribute("votes")) > Integer.valueOf(bestElement.getAttribute("votes")))
                bestElements.put(type, currentElement);
        }

        Map<String, String> resultMap = new LinkedHashMap<>();

        // Load preferred type in the first place
        String preferredType = "chords";
        Element prefElement = bestElements.get(preferredType);
        if(prefElement != null) {
            resultMap.put(preferredType, loadSongText(prefElement.getAttribute("url")));
            bestElements.remove(preferredType);
        }

        for(Element element : bestElements.values()){
            resultMap.put(element.getAttribute("type"), loadSongText(element.getAttribute("url")));
        }
        return resultMap;
    }

}
