package by.aleks.chordbro.data;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Created by Alex on 12/18/15.
 */

@Table(name = "Artists")
public class Artist extends Model {

    @Column(name = "Name", unique = true, onUniqueConflict = Column.ConflictAction.FAIL)
    public String name;

    @Column(name = "Image")
    public byte[] image;
}
