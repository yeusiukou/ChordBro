package by.aleks.chordbro;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import by.aleks.chordbro.data.Song;
import de.hdodenhof.circleimageview.CircleImageView;

import java.util.List;

/**
 * Created by Alex on 1/25/16.
 */
public class SongAdapter extends ArrayAdapter<Song> {


    public SongAdapter(Context context, int resource, List<Song> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if (view == null) {
            LayoutInflater viewInflated;
            viewInflated = LayoutInflater.from(getContext());
            view = viewInflated.inflate(R.layout.song_list_item, null);
        }

        Song currentSong = getItem(position);

        if (currentSong != null) {
            TextView artist = (TextView) view.findViewById(R.id.item_artist);
            TextView title = (TextView) view.findViewById(R.id.item_title);
            TextView album = (TextView) view.findViewById(R.id.item_album);
            CircleImageView image = (CircleImageView) view.findViewById(R.id.item_image);

            if (artist != null)
                artist.setText(currentSong.artist.name);

            if (image != null){
                byte[] byteArray = currentSong.artist.image;
                Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                image.setImageBitmap(bmp);

            if (title != null)
                title.setText(currentSong.title);

            if (album != null)
                if(currentSong.album == null || currentSong.album.equals(""))
                    album.setVisibility(View.GONE);
                album.setText(currentSong.album);
            }
        }

        return view;
    }

}