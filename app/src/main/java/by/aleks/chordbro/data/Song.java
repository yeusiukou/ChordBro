package by.aleks.chordbro.data;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Created by Alex on 12/18/15.
 */

@Table(name = "Songs")
public class Song extends Model {

    @Column(name = "Title")
    public String title;

    @Column(name = "Artist")
    public Artist artist;

    @Column(name = "Favorite")
    public boolean favorite;
}
