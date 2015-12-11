package by.aleks.chordbro;

import android.content.res.Resources;
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

import java.util.Map;
import java.util.Set;

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

        final String artist = "Muse";
        final String title = "Uprising";

        getSupportActionBar().setTitle(artist + " - " + title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ImageView artistImage = (ImageView)findViewById(R.id.artist_image);
        artistImage.setImageResource(R.drawable.muse);

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        // Set inner views behaviour on toolbar collapsing
        AppBarLayout.OnOffsetChangedListener mListener = new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                float step = (collapsingToolbar.getHeight() - ViewCompat.getMinimumHeight(collapsingToolbar))/100;
                float alpha = (collapsingToolbar.getHeight() - ViewCompat.getMinimumHeight(collapsingToolbar) + verticalOffset)/(step*100);
                tabLayout.setAlpha(alpha); // fade tabs out on toolbar collapsing
                toolbar.setTitleTextColor(adjustAlpha(0xFFFFFFFF, 1-alpha < 0 ? 0 : 1-alpha)); // fade in the title
            }
        };
        AppBarLayout appBar = (AppBarLayout)findViewById(R.id.app_bar);
        appBar.addOnOffsetChangedListener(mListener);

        // Load and display content
        new SongContentLoader(){

            private final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
            private final FragmentManager fm = getSupportFragmentManager();

            /**
             * To reduce the waiting time we don't wait until the all content is loaded.
             * While the first fragment is being showed we load other possible fragments.
             */
            @Override
            public void onFirstResultLoaded(final String loadedType, final String firstLoadedContent, final Set<String> allContentTypes) {
                SongActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        // If only one type of content was found, hide the tab layout
                        if(allContentTypes.size()<2){
                            tabLayout.setVisibility(View.GONE);
                            return;
                        }

                        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

                        // Add the preferred tab first
                        tabLayout.addTab(tabLayout.newTab().setText(loadedType));
                        allContentTypes.remove(loadedType);

                        for(String type : allContentTypes){
                            // Create tabs with names of content type
                            tabLayout.addTab(tabLayout.newTab().setText(type));
                        }

                        // Temporary adapter while we don't have the
                        viewPager.setAdapter(new FragmentStatePagerAdapter(fm) {
                            @Override
                            public Fragment getItem(int position) {
                                return SongFragment.newInstance(firstLoadedContent);
                            }

                            @Override
                            public int getCount() {
                                return 1;
                            }
                        });
                    }
                });
            }

            /**
             * At this point all the possible content is loaded.
             * @param map - HashMap with the key as a type name(displayed on the tab) and the value as the actual content
             */
            @Override
            protected void onPostExecute(final Map<String, String> map) {

                FragmentStatePagerAdapter pagerAdapter = new FragmentStatePagerAdapter(fm) {
                    @Override
                    public Fragment getItem(int position) {
                        // Get the type name from the name of selected tab
                        String type = tabLayout.getTabAt(position).getText().toString();
                        // Create a fragment with the given type song content
                        return SongFragment.newInstance(map.get(type));
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
            }
        }.execute(artist, title);

    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}
