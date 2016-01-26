package by.aleks.chordbro;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Alex on 12/17/15.
 */
public class LastfmImageLoader extends AsyncTask<String, Void, byte[]> {

    private static String LAST_FM_KEY = "1ac29efd409812564c6afb4ed66b9e5c";
    private static String TAG = "LastFmImageLoader";
    @Override
    protected byte[] doInBackground(String... strings) {
        return loadBinary(getImageUrl(loadUrl("http://ws.audioscrobbler.com/2.0/?method=artist.getInfo&artist="
                + strings[0].replace(" ", "+") //artist name
                + "&format=json&api_key=" + LAST_FM_KEY)));
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
        JSONObject object;
        try {
            object = new JSONObject(jsonString);
            return object.getJSONObject("artist").getJSONArray("image").getJSONObject(2).getString("#text");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] loadBinary(String urlString){
        InputStream input = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage());
                return null;
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();

            // download the file
            input = connection.getInputStream();

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                // allow canceling with back button
                if (isCancelled()) {
                    input.close();
                    return null;
                }
                total += count;
                // publishing the progress....
//                if (fileLength > 0) // only if total length is known
//                    publishProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
            }
            return output.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
    }
}
