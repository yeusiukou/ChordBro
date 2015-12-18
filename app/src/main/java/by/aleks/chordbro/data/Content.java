package by.aleks.chordbro.data;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Created by Alex on 12/18/15.
 */

@Table(name = "Contents")
public class Content extends Model {

    @Column(name = "Song")
    public Song song;

    @Column(name = "Text")
    public String text;
}
