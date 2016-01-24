package by.aleks.chordbro;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import by.aleks.chordbro.data.Content;
import by.aleks.chordbro.data.Song;
import com.squareup.picasso.Picasso;
import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.picasso.transformations.BlurTransformation;

import java.util.Map;

public class SongActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(
                R.id.toolbar_layout);
        collapsingToolbar.setTitleEnabled(false);

        final String artist = getIntent().getStringExtra(getString(R.string.artist_key));
        final String title = getIntent().getStringExtra(getString(R.string.title_key));
        Song song = Song.find(title, artist);

        getSupportActionBar().setTitle(title);
        getSupportActionBar().setSubtitle(artist);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView artistTv = (TextView)findViewById(R.id.song_artist);
        TextView titleTv = (TextView)findViewById(R.id.song_title);
        artistTv.setText(artist);
        titleTv.setText(title);

        // Set image
        final ImageView artistBackground = (ImageView)findViewById(R.id.artist_background);
        final CircleImageView artistImage = (CircleImageView)findViewById(R.id.artist_image);
        byte[] byteArray = song.artist.image;
        Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        artistBackground.setImageBitmap(bmp);
        artistImage.setImageBitmap(bmp);

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        final LinearLayout icons = (LinearLayout)findViewById(R.id.icons);
        final LinearLayout songInfo = (LinearLayout)findViewById(R.id.song_info);

        // Set inner views behaviour on toolbar collapsing
        AppBarLayout.OnOffsetChangedListener mListener = new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                float step = (collapsingToolbar.getHeight() - ViewCompat.getMinimumHeight(collapsingToolbar))/100;
                float alpha = (collapsingToolbar.getHeight() - ViewCompat.getMinimumHeight(collapsingToolbar) + verticalOffset)/(step*100);
                tabLayout.setAlpha(alpha); // fade tabs out on toolbar collapsing
                artistImage.setAlpha(alpha); // fade tabs out on toolbar collapsing
                icons.setAlpha(alpha*0.55f); // fade icons out on toolbar collapsing
                songInfo.setAlpha(alpha); // fade icons out on toolbar collapsing
                toolbar.setTitleTextColor(adjustAlpha(0xFFFFFFFF, 1-alpha < 0 ? 0 : 1-alpha)); // fade in the title
                toolbar.setSubtitleTextColor(adjustAlpha(0xFFFFFFFF, 1-alpha < 0 ? 0 : 1-alpha)); // fade in the title
            }
        };
        AppBarLayout appBar = (AppBarLayout)findViewById(R.id.app_bar);
        appBar.addOnOffsetChangedListener(mListener);

        /**
         * Load from db and display the chords
         */
        final Map<String, Content> contentMap = Content.getAll(song);
        // If only one type of content was found, hide the tab layout
        if(contentMap.size()<2){
            tabLayout.setVisibility(View.GONE);
            return;
        }

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        FragmentManager fm = getSupportFragmentManager();
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        for(String instrument : contentMap.keySet()){
            // Create tabs with names of instruments
            tabLayout.addTab(tabLayout.newTab().setText(instrument));
        }

        FragmentStatePagerAdapter pagerAdapter = new FragmentStatePagerAdapter(fm) {
            @Override
            public Fragment getItem(int position) {
                // Get the instrument name from the name of selected tab
                String instrument = tabLayout.getTabAt(position).getText().toString();
                // Create a fragment with the given type song content
                return SongFragment.newInstance(contentMap.get(instrument).text);
            }

            @Override
            public int getCount() {
                return tabLayout.getTabCount();
            }
        };
        viewPager.setAdapter(pagerAdapter);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        viewPager.setAdapter(pagerAdapter);

    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}
