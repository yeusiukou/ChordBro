package by.aleks.chordbro;

import android.os.AsyncTask;
import android.util.Log;
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
import java.util.*;

/**
 * Created by Alex on 12/7/15.
 */
public class SongContentLoader extends AsyncTask<String, Void, Map<String, String>>{

    @Override
    protected Map doInBackground(String... strings) {
        return loadSongSpannable(strings[0], strings[1]);
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
            //TODO: FIX no internet exception
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

        for(Element element : bestElements.values()){
            resultMap.put(element.getAttribute("type"), loadSongText(element.getAttribute("url")));
        }
        Log.d("Loader", "DONE!");
        return resultMap;
    }

}
