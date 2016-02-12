package by.aleks.chordbro.data;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.Date;
import java.util.List;

/**
 * Created by Alex on 12/18/15.
 */

@Table(name = "Songs")
public class Song extends Model {

    @Column(name = "Title", notNull = true)
    public String title;

    @Column(name = "Artist", notNull = true, onDelete = Column.ForeignKeyAction.CASCADE)
    public Artist artist;

    @Column(name = "Album")
    public String album;

    @Column(name = "Timestamp", index = true)
    public Date timestamp = new Date();

    @Column(name = "Favorite", notNull = true)
    public boolean favorite = false;

    @Column(name = "ChordCount")
    //Shows how many chord types the song has. The number get updated in MainActivity
    public int chordcount = 0;

    public static Song find(String title, String artistName){
        Artist artist = new Select()
                .from(Artist.class)
                .where("Name = ?", artistName)
                .executeSingle();
        if(artist == null)
            return null;

        return new Select()
                .from(Song.class)
                .where("Title = ?", title)
                .where("Artist = ?", artist.getId())
                .executeSingle();
    }

    public static List<Song> getAll(){
        return new Select()
                .from(Song.class)
                .orderBy("Timestamp DESC")
                .execute();
    }

    public static List<Song> getFavorite(){
        return new Select()
                .from(Song.class)
                .where("Favorite = ?", 1)
                .orderBy("Timestamp DESC")
                .execute();
    }
}
