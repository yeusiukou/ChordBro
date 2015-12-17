package by.aleks.chordbro;

import android.app.Activity;
import android.util.Log;
import com.gracenote.gnsdk.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Alex on 12/1/15.
 */
public class Recognizer implements Runnable {

    private static String TAG = "Recognizer";

    // Gracenote objects
    private GnManager gnManager;
    private GnUser gnUser;
    private GnMusicIdStream gnMusicIdStream;
    private IGnAudioSource gnMicrophone;
    private List<GnMusicIdStream> streamIdObjects = new ArrayList<>();

    private Activity activity;

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
            gnMicrophone = new GnMic();
            gnMusicIdStream = new GnMusicIdStream( gnUser, GnMusicIdStreamPreset.kPresetMicrophone, new MusicIDStreamEvents() );
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
            gnMusicIdStream.identifyAlbumAsync();
        } catch (GnException e) {
            e.printStackTrace();
        }
    }

    /**
     * The method is called from the UI thread. Use just like onPostExecute
     * @param song recognized song
     */
    public void onResult(Song song){};

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
            }
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

        HashMap<String, String> gnStatus_to_displayStatus;

        public MusicIDStreamEvents(){
            gnStatus_to_displayStatus = new HashMap<>();
            gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingStarted.toString(), "Identification started");
            gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingFpGenerated.toString(), "Fingerprinting complete");
            gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingLocalQueryStarted.toString(), "Lookup started");
            gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingOnlineQueryStarted.toString(), "Lookup started");
        }

        @Override
        public void statusEvent( GnStatus status, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable cancellable ) {}

        @Override
        public void musicIdStreamProcessingStatusEvent( GnMusicIdStreamProcessingStatus status, IGnCancellable canceller ) {

            if(GnMusicIdStreamProcessingStatus.kStatusProcessingAudioStarted.compareTo(status) == 0)
            {
                //TODO: notify that process started
            }

        }

        @Override
        public void musicIdStreamIdentifyingStatusEvent( GnMusicIdStreamIdentifyingStatus status, IGnCancellable canceller ) {}


        /**
         * When the song is found send it to onPostExecute()
         */
        private int times = 0;
        @Override
        public void musicIdStreamAlbumResult( GnResponseAlbums result, IGnCancellable canceller ) {
            try {
                if (result.resultCount() == 0) {

                    Recognizer.this.notify("No match");
                    if(times < 4){
                        new Thread(new Recognizer(activity)).start();
                        times++;
                    } else times = 0;

                } else {

                    Recognizer.this.notify("Match found");
                    GnAlbumIterator iter = result.albums().getIterator();
                    if (iter.hasNext()) {
                        final GnAlbum album = iter.next();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Recognizer.this.onResult(new Song(
                                        album.artist().name().display(),
                                        album.title().display(),
                                        album.trackMatched().title().display()));
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
        public void musicIdStreamIdentifyCompletedWithError(GnError error) {
            //TODO: notify about connection problem
            if ( error.isCancelled() )
                Recognizer.this.notify("Cancelled");
            else
                Recognizer.this.notify(error.errorDescription());
        }
    }

    private void notify(String text){
        Log.d(TAG, text);
    }
}
