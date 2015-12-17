package by.aleks.chordbro;

import android.graphics.Bitmap;

/**
 * Created by Alex on 12/1/15.
 */
public class Song {

    private String artist;
    private String album;
    private String title;
    private String image;

    public Song(String artist, String album, String title, String image){
        this.artist = artist;
        this.album = album;
        this.title = title;
        this.image = image;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }
    public String getTitle() {
        return title;
    }
    public String getImage() {
        return image;
    }
}
