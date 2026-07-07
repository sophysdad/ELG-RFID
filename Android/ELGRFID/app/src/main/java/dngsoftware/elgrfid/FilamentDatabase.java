package dngsoftware.elgrfid;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {DbFilament.class}, version = 1, exportSchema = false)
public abstract class FilamentDatabase extends RoomDatabase {

    private static volatile FilamentDatabase instance;

    public abstract FilamentDao filamentDao();

    public static FilamentDatabase getInstance(Context context) {
        if (instance == null || !instance.isOpen()) {
            synchronized (FilamentDatabase.class) {
                if (instance == null || !instance.isOpen()) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    FilamentDatabase.class,
                                    "anycubic_filament_database")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return instance;
    }

    public static void closeInstance() {
        if (instance != null && instance.isOpen()) {
            instance.close();
            instance = null;
        }
    }
}