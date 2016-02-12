package by.aleks.chordbro.api;

import android.app.Activity;
import android.util.Log;
import by.aleks.chordbro.R;
import com.gracenote.gnsdk.*;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alex on 12/1/15.
 */
public class Recognizer implements Runnable {

    private static String TAG = "Recognizer";
    private Activity activity;

    // Gracenote objects
    private GnManager gnManager;
    private GnUser gnUser;
    private GnMusicIdStream gnMusicIdStream;
    private IGnAudioSource gnMicrophone;
    private List<GnMusicIdStream> streamIdObjects = new ArrayList<>();

    //flags
    private boolean isProcessing;
    private int failCount = 0;

    public Recognizer(Activity activity){

        this.activity = activity;

        // Gracenote credentials
        String client_id = activity.getString(R.string.client_id);
        String client_tag = activity.getString(R.string.client_tag);
        String license_data = activity.getString(R.string.license_text);

        try {

            // GnManager must be created first, it initializes GNSDK
            gnManager = new GnManager( activity, license_data, GnLicenseInputMode.kLicenseInputModeString );

            // provide handler to receive system events, such as locale update needed
            gnManager.systemEventHandler( new SystemEvents() );

            // get a user, if no user stored persistently a new user is registered and stored
            // Note: Android persistent storage used, so no GNSDK storage provider needed to store a user
            gnUser = new GnUser( new GnUserStore(activity), client_id, client_tag, TAG);

            // enable storage provider allowing GNSDK to use its persistent stores
            GnStorageSqlite.enable();

            // enable local MusicID-Stream recognition (GNSDK storage provider must be enabled as pre-requisite)
            GnLookupLocalStream.enable();

            // Set up for continuous listening from the microphone
            // - create microphone, this can live for lifetime of app
            // - create GnMusicIdStream instance, this can live for lifetime of app
            // - configure
            // Starting and stopping continuous listening should be started and stopped
            // based on Activity life-cycle, see onPause and onResume for details
            gnMicrophone = new AudioVisualizeAdapter(new GnMic(), activity);
            gnMusicIdStream = new GnMusicIdStream( gnUser, GnMusicIdStreamPreset.kPresetMicrophone, new MusicIDStreamEvents());
            gnMusicIdStream.options().lookupData(GnLookupData.kLookupDataContent, true);
            gnMusicIdStream.options().lookupData(GnLookupData.kLookupDataSonicData, true);
            gnMusicIdStream.options().resultSingle(true);
            // Retain GnMusicIdStream object so we can cancel an active identification if requested
            streamIdObjects.add( gnMusicIdStream );


        } catch ( GnException e ) {

            Log.e(TAG, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            //TODO: handle error
            return;

        } catch ( Exception e ) {
            if(e.getMessage() != null){
                Log.e(TAG, e.getMessage());
                //TODO: handle error
            }
            else{
                e.printStackTrace();
            }
            return;

        }

    }

    @Override
    public void run() {
        try {
            if(isInternetAvailable()){
                gnMusicIdStream.identifyAlbumAsync();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onStart();
                    }
                });
            }
            else callUIError(activity.getString(R.string.connection_error));

        } catch (GnException e) {
            e.printStackTrace();
        }
    }

    /**
     * The method is called from the UI thread. Use just like onPostExecute
     * @param title - name of the recognized song
     * @param artist - name of the artist
     */
    public void onResult(String title, String artist, String album){};

    public void onError(String error){};

    public void onStart(){};



    public void startAudioProcess() {

        if ( gnMusicIdStream != null ) {

            // Create a thread to process the data pulled from GnMic
            // Internally pulling data is a blocking call, repeatedly called until
            // audio processing is stopped. This cannot be called on the main thread.
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        gnMusicIdStream.audioProcessStart(gnMicrophone);
                    } catch (GnException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            isProcessing = true;
        }
    }

    public void stopAudioProcess() {

        if ( gnMusicIdStream != null ) {

            try {
                // to ensure no pending identifications deliver results while your app is
                // paused it is good practice to call cancel
                // it is safe to call identifyCancel if no identify is pending
                gnMusicIdStream.identifyCancel();

                // stopping audio processing stops the audio processing thread started
                // in onResume
                gnMusicIdStream.audioProcessStop();

            } catch (GnException e) {

                Log.e(TAG, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            } finally {
                isProcessing = false;
            }
        }
    }

    public boolean isProcessing(){
        return isProcessing;
    }

    public void cancelId(){
        if ( gnMusicIdStream != null && isProcessing())
            try {
                gnMusicIdStream.identifyCancel();
            } catch (GnException e) {
                e.printStackTrace();
            }
    }

    /**
     * Receives system events from GNSDK
     */
    class SystemEvents implements IGnSystemEvents {
        @Override
        public void localeUpdateNeeded( GnLocale locale ){

            // Locale update is detected
            try {
                locale.update( gnUser );
            } catch (GnException e) {
                Log.e(TAG, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
            }
        }

        @Override
        public void listUpdateNeeded( GnList list ) {
            // List update is detected
            try {
                list.update( gnUser );
            } catch (GnException e) {
                Log.e(TAG, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
            }
        }

        @Override
        public void systemMemoryWarning(long currentMemorySize, long warningMemorySize) {
            // only invoked if a memory warning limit is configured
        }
    }

    /**
     * GNSDK MusicID-Stream event delegate
     */
    private class MusicIDStreamEvents implements IGnMusicIdStreamEvents {

        @Override
        public void statusEvent( GnStatus status, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable cancellable ) {}

        @Override
        public void musicIdStreamProcessingStatusEvent( GnMusicIdStreamProcessingStatus status, IGnCancellable canceller ) {}

        @Override
        public void musicIdStreamIdentifyingStatusEvent( GnMusicIdStreamIdentifyingStatus status, IGnCancellable canceller ) {}


        /**
         * When the song is found send it to onPostExecute()
         */
        @Override
        public void musicIdStreamAlbumResult( GnResponseAlbums result, IGnCancellable canceller ) {
            try {
                if (result.resultCount() == 0) {
                    tryAgain();
                } else {

                    GnAlbumIterator iter = result.albums().getIterator();
                    if (iter.hasNext()) {
                        final GnAlbum album = iter.next();

                        final String title = album.trackMatched().title().display();
                        final String artistName = album.artist().name().display();
                        final String albumName = album.title().display();

                        if( (title == null | title.equals("")) || (artistName == null | artistName.equals("")) )
                            tryAgain();
                        else activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Recognizer.this.onResult(title, artistName, albumName);
                            }
                        });
                    }

                }
            } catch (GnException e) {
                Log.e(TAG, e.errorDescription());
                return;
            }
        }

        @Override
        public void musicIdStreamIdentifyCompletedWithError(final GnError error) {
            // notify about connection problem
            if ( error.isCancelled() )
                Log.d(TAG, "Cancelled");
            else callUIError(activity.getString(R.string.server_error));
        }
    }

    public void notify(String text){
        Log.d(TAG, text);
    }

    public boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com"); //You can replace it with your name

            if (ipAddr.equals("")) {
                return false;
            } else {
                return true;
            }

        } catch (Exception e) {
            return false;
        }

    }

    private void callUIError(final String message){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onError(message);
            }
        });
    }

    // Notify if recognition fails 3 times
    private void tryAgain() throws GnException {
        if(failCount < 3){
            Log.d(TAG, "Failed "+failCount+" time");
            gnMusicIdStream.identifyAlbumAsync();
            failCount++;
        } else {
            failCount = 0;
            Recognizer.this.notify(activity.getString(R.string.no_match));
        }
    }
}
