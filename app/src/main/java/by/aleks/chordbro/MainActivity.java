package by.aleks.chordbro;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;
import by.aleks.chordbro.api.LastfmImageLoader;
import by.aleks.chordbro.api.Recognizer;
import by.aleks.chordbro.data.Artist;
import by.aleks.chordbro.data.Content;
import by.aleks.chordbro.data.Song;
import by.aleks.chordbro.views.SearchLayout;
import com.activeandroid.ActiveAndroid;

import java.util.Map;


public class MainActivity extends AppCompatActivity implements LayoutCommunicator {

    private Recognizer recognizer;
    private TabLayout tabLayout;
    private FloatingActionButton fab;
    private SearchLayout searchLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActiveAndroid.initialize(this);

        setContentView(R.layout.activity_main);
        searchLayout = (SearchLayout)findViewById(R.id.search_layout);
        searchLayout.init();
        searchLayout.setLayoutCommunicator(this);
        /*
        ** SETTING THE UPPER TABS AND SONG LIST FRAGMENTS
         */
        tabLayout = (TabLayout) findViewById(R.id.main_tabs);
        tabLayout.addTab(tabLayout.newTab().setIcon(ContextCompat.getDrawable(this, R.drawable.ic_history_white_24dp)));
        tabLayout.addTab(tabLayout.newTab().setIcon(ContextCompat.getDrawable(this, R.drawable.ic_favorite_white_24dp)));
        tabLayout.getTabAt(1).getIcon().setAlpha(150); //Add opacity to the unselected icon

        final ViewPager viewPager = (ViewPager) findViewById(R.id.main_pager);
        FragmentManager fm = getSupportFragmentManager();
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        FragmentStatePagerAdapter pagerAdapter = new FragmentStatePagerAdapter(fm) {
            @Override
            public Fragment getItem(int position) {
                // Create a fragment with the given type song content
                return SongListFragment.newInstance(position != 0);
            }

            @Override
            public int getCount() {
                return tabLayout.getTabCount();
            }
        };
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                tab.getIcon().setAlpha(255);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.getIcon().setAlpha(150);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        viewPager.setAdapter(pagerAdapter);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchLayout.reveal();
                new Thread(recognizer).start();
            }
        });

        // Initialise the sound recognising system
        recognizer = new Recognizer(this){
            @Override
            public void notify(String text) {
                super.notify(text);
                final String sText = text;
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        searchLayout.updateStatus(sText);
                        searchLayout.hide();
                    }
                });
            }

            @Override
            public void onResult(String title, String artist, String album) {
                loadAndStart(title, artist, album);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!recognizer.isProcessing())
            recognizer.startAudioProcess();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(recognizer!=null)
            recognizer.stopAudioProcess();
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

    public void openSong(String title, String artistName){
        Song song = Song.find(title, artistName);
        if (song.chordcount > 0) {
            searchLayout.hide();

            Intent songIntent = new Intent().setClassName(MainActivity.this, "by.aleks.chordbro.SongActivity");
            songIntent.putExtra(getString(R.string.artist_key), artistName);
            songIntent.putExtra(getString(R.string.title_key), title);
            startActivity(songIntent);
        }
        else Toast.makeText(this, getString(R.string.no_chords), Toast.LENGTH_SHORT).show();
    }

    private void loadAndStart(final String title, final String artistName, final String album){

        // If the song already exists in db, don't load anything
        if(Song.find(title, artistName) != null){
            openSong(title, artistName);
            return;
        }

        Artist artist = Artist.findByName(artistName);
        if(artist == null){ // If no such artist in the db, download the image and add the artist
            new LastfmImageLoader(){
                @Override
                protected void onPostExecute(byte[] bytes) {

                    Artist artist = new Artist();
                    artist.name = artistName;
                    artist.image = bytes;
                    artist.save();

                    addSongToDb(title, artist, album);
                    openSong(title, artistName);
                }
            }.execute(artistName);
        } else {
            addSongToDb(title, artist, album);
            openSong(title, artistName);
        }
    }

    private void addSongToDb(final String title, final Artist artist, final String album){
        final Song song = new Song();
        song.artist = artist;
        song.title = title;
        song.album = album;
        song.save();

        new SongContentLoader(){
            @Override
            protected void onPostExecute(Map<String, String> resultMap) {
                for(String instrument : resultMap.keySet()){
                    Content content = new Content();
                    content.instrument = instrument;
                    content.song = song;
                    content.text = resultMap.get(instrument);
                    content.save();

                    song.chordcount++; //Increase the number of chord types
                    song.save();
                }
                openSong(title, artist.name);
            }
        }.execute(artist.name, title);
    }

    @Override
    public void onBackPressed() {
        if(searchLayout.isRevealed()){
            searchLayout.hide();
        }
        else super.onBackPressed();
    }

    /*
    Show and hide the FAB and Tab with the search layout.
     */
    @Override
    public void onLayoutShow() {
        fab.setVisibility(View.GONE);
        tabLayout.setVisibility(View.GONE);
    }

    @Override
    public void onLayoutHide() {
        fab.setVisibility(View.VISIBLE);
        tabLayout.setVisibility(View.VISIBLE);

        if(recognizer!=null)
            recognizer.cancelId();
    }
}