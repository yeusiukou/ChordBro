package by.aleks.chordbro;

import android.app.ActionBar;
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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import by.aleks.chordbro.data.Artist;
import by.aleks.chordbro.data.Content;
import by.aleks.chordbro.data.Song;
import com.activeandroid.ActiveAndroid;

import java.util.Map;


public class MainActivity extends AppCompatActivity{

    private Recognizer recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActiveAndroid.initialize(this);

        setContentView(R.layout.activity_main);

        /*
        ** SETTING THE UPPER TABS AND SONG LIST FRAGMENTS
         */
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.main_tabs);
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
                return SongListFragment.newInstance(position == 0 ? false : true);
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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(recognizer).start();
            }
        });

        // Initialise the sound recognising system
        recognizer = new Recognizer(this){
            @Override
            public void onResult(String title, String artist, String album) {
                loadAndStart(title, artist, album);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(recognizer!=null)
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

    private void loadAndStart(final String title, final String artistName, final String album){

        // If the song already exists in db, don't load anything
        if(Song.find(title, artistName) != null){
            startSongActivity(title, artistName);
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
                    startSongActivity(title, artistName);
                }
            }.execute(artistName);
        } else {
            addSongToDb(title, artist, album);
            startSongActivity(title, artistName);
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
                startSongActivity(title, artist.name);
            }
        }.execute(artist.name, title);
    }

    public void startSongActivity(String title, String artistName){
        Intent songIntent = new Intent().setClassName(MainActivity.this, "by.aleks.chordbro.SongActivity");
        songIntent.putExtra(getString(R.string.artist_key), artistName);
        songIntent.putExtra(getString(R.string.title_key), title);
        startActivity(songIntent);
    }

}