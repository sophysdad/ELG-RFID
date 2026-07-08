package dngsoftware.elgrfid;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FilamentDao {

    @Insert
    void addItem(DbFilament item);

    @Update
    void updateItem(DbFilament item);

    @Delete
    void deleteItem(DbFilament item);

    @Query("SELECT COUNT(dbKey) FROM filament_table")
    int getItemCount();

    @Query("SELECT * FROM filament_table ORDER BY filament_position ASC")
    List<DbFilament> getAllItems();

    @Query("SELECT * FROM filament_table WHERE filament_name = :filamentName")
    DbFilament getFilamentByName(String filamentName);

    @Query("SELECT * FROM filament_table WHERE filament_id = :filamentId LIMIT 1")
    DbFilament getFilamentById(String filamentId);

    @Query("SELECT * FROM filament_table WHERE filament_vendor = :vendor ORDER BY filament_position ASC")
    List<DbFilament> getFilamentsByVendor(String vendor);
}