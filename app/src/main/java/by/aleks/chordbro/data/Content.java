package by.aleks.chordbro.data;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alex on 12/18/15.
 */

@Table(name = "Contents")
public class Content extends Model {

    @Column(name = "Song")
    public Song song;

    @Column(name = "Instrument")
    public String instrument;

    @Column(name = "Text")
    public String text;

    public static Map<String, Content> getAll(Song song) {
        List<Content> contents = new Select()
                .from(Content.class)
                .where("Song = ?", song.getId())
                .execute();

        Map<String, Content> contentMap = new HashMap();
        for(Content content : contents){
            contentMap.put(content.instrument, content);
        }
        return contentMap;
    }
}
