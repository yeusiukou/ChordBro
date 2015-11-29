package by.aleks.chordbro;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import com.gracenote.gnsdk.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity{

    // Get these from Gracenote
    private static String client_id = null;
    private static String client_tag = null;
    private static String license_data = null;
    private static String appString = null;

    // Gracenote objects
    private GnManager gnManager;
    private GnUser gnUser;
    private GnMusicIdStream gnMusicIdStream;
    private IGnAudioSource gnMicrophone;
    private GnLog						gnLog;
    private List<GnMusicId> idObjects				= new ArrayList<GnMusicId>();
    private List<GnMusicIdFile>			fileIdObjects			= new ArrayList<GnMusicIdFile>();
    private List<GnMusicIdStream>		streamIdObjects			= new ArrayList<GnMusicIdStream>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    gnMusicIdStream.identifyAlbumAsync();
                } catch (GnException e) {
                    e.printStackTrace();
                }
            }
        });

        /* Initialize from strings.xml */
        client_id    = getString(R.string.client_id);
        client_tag   = getString(R.string.client_tag);
        license_data = getString(R.string.license_text);
        appString = getString(R.string.app_name);

        try {

            // GnManager must be created first, it initializes GNSDK
            gnManager = new GnManager( getApplicationContext(), license_data, GnLicenseInputMode.kLicenseInputModeString );

            // provide handler to receive system events, such as locale update needed
            gnManager.systemEventHandler( new SystemEvents() );

            // get a user, if no user stored persistently a new user is registered and stored
            // Note: Android persistent storage used, so no GNSDK storage provider needed to store a user
            gnUser = new GnUser( new GnUserStore(this), client_id, client_tag, appString );

            // enable storage provider allowing GNSDK to use its persistent stores
            GnStorageSqlite.enable();

            // enable local MusicID-Stream recognition (GNSDK storage provider must be enabled as pre-requisite)
            GnLookupLocalStream.enable();

            // Loads data to support the requested locale, data is downloaded from Gracenote Service if not
            // found in persistent storage. Once downloaded it is stored in persistent storage (if storage
            // provider is enabled). Download and write to persistent storage can be lengthy so perform in
            // another thread
            Thread localeThread = new Thread(
                    new LocaleLoadRunnable(GnLocaleGroup.kLocaleGroupMusic,
                            GnLanguage.kLanguageEnglish,
                            GnRegion.kRegionGlobal,
                            GnDescriptor.kDescriptorDefault,
                            gnUser)
            );
            localeThread.start();

            // Ingest MusicID-Stream local bundle, perform in another thread as it can be lengthy
            Thread ingestThread = new Thread( new LocalBundleIngestRunnable(this) );
            ingestThread.start();

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
            gnMusicIdStream.options().resultSingle( true );

            // Retain GnMusicIdStream object so we can cancel an active identification if requested
            streamIdObjects.add( gnMusicIdStream );

        } catch ( GnException e ) {

            Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            //TODO: handle error
            return;

        } catch ( Exception e ) {
            if(e.getMessage() != null){
                Log.e(appString, e.getMessage());
                //TODO: handle error
            }
            else{
                e.printStackTrace();
            }
            return;

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
            }
        }

        @Override
        public void listUpdateNeeded( GnList list ) {
            // List update is detected
            try {
                list.update( gnUser );
            } catch (GnException e) {
                Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
            }
        }

        @Override
        public void systemMemoryWarning(long currentMemorySize, long warningMemorySize) {
            // only invoked if a memory warning limit is configured
        }
    }

    /**
     * Loads a locale
     */
    class LocaleLoadRunnable implements Runnable {
        GnLocaleGroup	group;
        GnLanguage		language;
        GnRegion		region;
        GnDescriptor	descriptor;
        GnUser			user;


        LocaleLoadRunnable(
                GnLocaleGroup group,
                GnLanguage		language,
                GnRegion		region,
                GnDescriptor	descriptor,
                GnUser			user) {
            this.group 		= group;
            this.language 	= language;
            this.region 	= region;
            this.descriptor = descriptor;
            this.user 		= user;
        }

        @Override
        public void run() {
            try {

                GnLocale locale = new GnLocale(group,language,region,descriptor,gnUser);
                locale.setGroupDefault();

            } catch (GnException e) {
                Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
            }
        }
    }

    /**
     * Loads a local bundle for MusicID-Stream lookups
     */
    class LocalBundleIngestRunnable implements Runnable {
        Context context;

        LocalBundleIngestRunnable(Context context) {
            this.context = context;
        }

        public void run() {
            try {

                // our bundle is delivered as a package asset
                // to ingest the bundle access it as a stream and write the bytes to
                // the bundle ingester
                // bundles should not be delivered with the package as this, rather they
                // should be downloaded from your own online service

                InputStream bundleInputStream 	= null;
                int				ingestBufferSize	= 1024;
                byte[] 			ingestBuffer 		= new byte[ingestBufferSize];
                int				bytesRead			= 0;

                GnLookupLocalStreamIngest ingester = new GnLookupLocalStreamIngest(new BundleIngestEvents());

                try {

                    bundleInputStream = context.getAssets().open("1557.b");

                    do {

                        bytesRead = bundleInputStream.read(ingestBuffer, 0, ingestBufferSize);
                        if ( bytesRead == -1 )
                            bytesRead = 0;

                        ingester.write( ingestBuffer, bytesRead );

                    } while( bytesRead != 0 );

                } catch (IOException e) {
                    e.printStackTrace();
                }

                ingester.flush();

            } catch (GnException e) {
                Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
            }

        }
    }

    /**
     * GNSDK bundle ingest status event delegate
     */
    private class BundleIngestEvents implements IGnLookupLocalStreamIngestEvents{

        @Override
        public void statusEvent(GnLookupLocalStreamIngestStatus status, String bundleId, IGnCancellable canceller) {
            //TODO: notify user about changed status
            setStatus("Bundle ingest progress: " + status.toString() , true);
        }
    }

    /**
     * GNSDK MusicID-Stream event delegate
     */
    private class MusicIDStreamEvents implements IGnMusicIdStreamEvents {

        HashMap<String, String> gnStatus_to_displayStatus;

        public MusicIDStreamEvents(){
            gnStatus_to_displayStatus = new HashMap<String,String>();
            gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingStarted.toString(), "Identification started");
            gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingFpGenerated.toString(), "Fingerprinting complete");
            gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingLocalQueryStarted.toString(), "Lookup started");
            gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingOnlineQueryStarted.toString(), "Lookup started");
//			gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingEnded.toString(), "Identification complete");
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


        @Override
        public void musicIdStreamAlbumResult( GnResponseAlbums result, IGnCancellable canceller ) {
//            lastLookup_matchTime = SystemClock.elapsedRealtime() - lastLookup_startTime;
            MainActivity.this.runOnUiThread(new UpdateResultsRunnable(result));
        }

        @Override
        public void musicIdStreamIdentifyCompletedWithError(GnError error) {
            if ( error.isCancelled() )
                setStatus( "Cancelled", true );
            else
                setStatus( error.errorDescription(), true );
//            setUIState( UIState.READY );
        }
    }

    /**
     * Adds album results to UI via Runnable interface
     */
    class UpdateResultsRunnable implements Runnable {

        GnResponseAlbums albumsResult;

        UpdateResultsRunnable(GnResponseAlbums albumsResult) {
            this.albumsResult = albumsResult;
        }

        @Override
        public void run() {
            try {
                if (albumsResult.resultCount() == 0) {

                    setStatus("No match", true);

                } else {

                    setStatus("Match found", true);
                    GnAlbumIterator iter = albumsResult.albums().getIterator();
                    while (iter.hasNext()) {
                        GnAlbum album = iter.next();
                        Log.d(appString, "Album: "+album.title().display());

                        if ( album.trackMatched() != null ) {
                            GnArtist artist = album.trackMatched().artist();
                            Log.d(appString, "Artist: " + artist.name().display());
                            Log.d(appString, "Title: " + album.trackMatched().title().display());
                        }

                    }

                }
            } catch (GnException e) {
                setStatus(e.errorDescription(), true);
                return;
            }

        }
    }

    private void setStatus(String text, boolean blank){
        Log.d(appString, text);
    }



}
