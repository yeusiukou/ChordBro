package by.aleks.chordbro;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
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

    private List<Song> items;

    public SongAdapter(Context context, int resource, List<Song> items) {
        super(context, resource, items);
        this.items = items;
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
            View favicon = view.findViewById(R.id.item_favorite);
            View noChords = view.findViewById(R.id.item_no_chords);
            TextView letter = (TextView)view.findViewById(R.id.item_image_letter);

            letter.setVisibility(View.GONE);

            if (artist != null)
                artist.setText(currentSong.artist.name);

            if (image != null){
                byte[] byteArray = currentSong.artist.image;
                //TODO: Add a blank image
                if(byteArray != null){
                    Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                    image.setImageBitmap(bmp);
                } else {
                    Drawable color = new ColorDrawable(ContextCompat.getColor(getContext(), R.color.grey));
                    image.setImageDrawable(color);
                    letter.setText(Character.toString(currentSong.artist.name.charAt(0)));
                    letter.setVisibility(View.VISIBLE);
                }

            if (title != null)
                title.setText(currentSong.title);

            if (album != null)
                if(currentSong.album == null || currentSong.album.equals(""))
                    album.setVisibility(View.GONE);
                album.setText(currentSong.album);
            }
            time.setText(dateToString(currentSong.timestamp));

            // display icons
            favicon.setVisibility(View.INVISIBLE); // Hide both of the icons before
            noChords.setVisibility(View.INVISIBLE);
            if(currentSong.chordcount == 0)
                noChords.setVisibility(View.VISIBLE);
            else if(currentSong.favorite)
                favicon.setVisibility(View.VISIBLE);
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