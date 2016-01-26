package by.aleks.chordbro;

import android.app.Fragment;
import android.os.Bundle;
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

    private View rootView;
    private SongAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_song_list, container, false);

        final List<Song> songList = Song.getAll();
        adapter = new SongAdapter(getActivity(), R.layout.song_list_item, songList);
        final ListView songListView = (ListView) rootView.findViewById(R.id.song_list_view);
        songListView.setAdapter(adapter);
        songListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Song song = songList.get(i);
                if (song.chordcount > 0)
                    ((MainActivity) getActivity()).startSongActivity(song.title, song.artist.name);
                else Toast.makeText(getActivity(), getString(R.string.no_chords), Toast.LENGTH_SHORT).show();
            }
        });
        songListView.setDivider(null);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }
}
