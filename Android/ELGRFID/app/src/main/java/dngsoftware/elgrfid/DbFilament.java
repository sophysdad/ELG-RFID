package dngsoftware.elgrfid;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "filament_table")
public class DbFilament {

    @PrimaryKey(autoGenerate = true)
    public int dbKey;

    @ColumnInfo(name = "filament_position")
    public int position;

    @ColumnInfo(name = "filament_name")
    public String filamentName;

    @ColumnInfo(name = "filament_id")
    public String filamentID;

    @ColumnInfo(name = "filament_vendor")
    public String filamentVendor;

    @ColumnInfo(name = "filament_param")
    public String filamentParam;
}