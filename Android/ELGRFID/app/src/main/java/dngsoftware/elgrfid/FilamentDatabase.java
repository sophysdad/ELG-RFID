package dngsoftware.elgrfid;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {DbFilament.class}, version = 1, exportSchema = false)
public abstract class FilamentDatabase extends RoomDatabase {

    public abstract FilamentDao filamentDao();

    private static final java.util.Map<String, FilamentDatabase> INSTANCES =
            new java.util.HashMap<>();

    public static FilamentDatabase getInstance(Context context, PrinterBrand brand) {
        String dbName = brand.id + "_filament_database";
        synchronized (FilamentDatabase.class) {
            FilamentDatabase db = INSTANCES.get(dbName);
            if (db == null || !db.isOpen()) {
                db = Room.databaseBuilder(
                                context.getApplicationContext(),
                                FilamentDatabase.class,
                                dbName)
                        .fallbackToDestructiveMigration()
                        .allowMainThreadQueries()
                        .build();
                INSTANCES.put(dbName, db);
            }
            return db;
        }
    }
}