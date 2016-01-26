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

import java.util.Date;
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
            TextView time = (TextView) view.findViewById(R.id.item_time);
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
            time.setText(dateToString(currentSong.timestamp));

            // display icons
            if(currentSong.chordcount == 0)
                view.findViewById(R.id.item_no_chords).setVisibility(View.VISIBLE);
            else if(currentSong.favorite)
                view.findViewById(R.id.item_favorite).setVisibility(View.VISIBLE);
        }

        return view;
    }

    private String dateToString(Date date){
        long ms = new Date().getTime() - date.getTime(); // milliseconds since
        float hs = ((float)ms) / ( 60*60*1000 );
        if(hs < 1)
            return (int)(hs*60)+"m";
        else if (hs < 24)
            return (int)hs+"h";
        else {
            int days = (int)(hs/24);
            if(days < 365)
                return days+"d";
            else return days/365+"y";
        }
    }

}