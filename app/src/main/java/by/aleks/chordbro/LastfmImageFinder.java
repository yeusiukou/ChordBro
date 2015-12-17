package by.aleks.chordbro;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Alex on 12/17/15.
 */
public class LastfmImageFinder extends AsyncTask<String, Void, String> {

    private static String LAST_FM_KEY = "1ac29efd409812564c6afb4ed66b9e5c";
    @Override
    protected String doInBackground(String... strings) {
        return getImageUrl(loadUrl("http://ws.audioscrobbler.com/2.0/?method=artist.getInfo&artist="
                +strings[0] //artist name
                +"&format=json&api_key="+LAST_FM_KEY));
    }


    private String loadUrl(String urlString){

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
            Log.e("Lastfm", "Error ", e);
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
                    Log.e("Lastfm", "Error closing stream", e);
                }
            }
        }
        return songStr;
    }

    private String getImageUrl(String jsonString){
        JSONObject object = null;
        try {
            object = new JSONObject(jsonString);
            return object.getJSONObject("artist").getJSONArray("image").getJSONObject(2).getString("#text");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
