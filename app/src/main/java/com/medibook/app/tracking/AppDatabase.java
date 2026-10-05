package com.medibook.app.tracking;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {DiemToaDo.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract DiemToaDoDao diemToaDoDao();

    private static volatile AppDatabase INSTANCE;

    // Chỉ tạo 1 database duy nhất cho cả app (singleton)
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "medibook_local.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}