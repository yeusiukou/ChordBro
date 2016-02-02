package by.aleks.chordbro;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import by.aleks.chordbro.data.Song;

import java.util.List;

/**
 * Created by Alex on 1/25/16.
 */
public class SongListFragment extends Fragment {

    public static final String FAV_PARAM = "favorite_only";

    private View rootView;
    private SongAdapter adapter;
    private List<Song> songList;
    private boolean isFavorite;

    public static SongListFragment newInstance(boolean isFavorite) {
        SongListFragment fragment = new SongListFragment();
        Bundle args = new Bundle();
        args.putBoolean(FAV_PARAM, isFavorite);
        fragment.setArguments(args);
        return fragment;
    }

    public SongListFragment() {
        // Required empty public constructor
    }

    public void refresh(){
        if(songList != null){
            // Get the song list in the background thread and when it's done update the view
            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... voids) {
                    songList.clear();
                    songList.addAll(isFavorite ? Song.getFavorite() : Song.getAll());
                    return null;
                }
                @Override
                protected void onPostExecute(Void aVoid) {
                    adapter.notifyDataSetChanged();
                }
            }.execute();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_song_list, container, false);

        isFavorite = getArguments().getBoolean(FAV_PARAM);

        // Get the song list in the background thread and when it's done assign it to the adapter
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                songList = isFavorite ? Song.getFavorite() : Song.getAll();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                adapter = new SongAdapter(getActivity(), R.layout.song_list_item, songList);
                final ListView songListView = (ListView) rootView.findViewById(R.id.song_list_view);
                songListView.setAdapter(adapter);
                songListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Song song = songList.get(i);
                        if(song.chordcount > 0)
                            ((MainActivity)getActivity()).openSong(song.title, song.artist.name);
                        else Toast.makeText(getContext(), getString(R.string.no_chords), Toast.LENGTH_SHORT).show();
                    }
                });
                songListView.setDivider(null);
            }
        }.execute();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }


}
