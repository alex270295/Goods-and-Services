package korolyov.ctdev.itmo.services.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Set;

public class GoodsDatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "goods";
    public static final int DATABASE_VERSION = 1;
    public static final String ID = "_id";
    public static final String TITLE = "title";
    public static final String SUBS = "subs";
    public static final String CREATE_DATABASE = "CREATE TABLE " + DATABASE_NAME
            + " (" + ID + " INTEGER PRIMARY KEY, "
            + TITLE + " TEXT, " + SUBS + " TEXT);";
    public static final String DROP_DATABASE = "DROP TABLE IF EXISTS " + DATABASE_NAME;


    public GoodsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DATABASE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            db.execSQL(DROP_DATABASE);
            onCreate(db);
        }
    }

    public Cursor getItemsByIds(SQLiteDatabase db, Set<Integer> set) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ID);
        stringBuilder.append(" in (");
        for (Integer i : set) {
            stringBuilder.append(i);
            stringBuilder.append(", ");
        }
        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
        stringBuilder.append(")");
        return db.query(DATABASE_NAME, null, stringBuilder.toString(), null, null, null, null);
    }

    public void deleteAll(SQLiteDatabase db) {
        db.delete(DATABASE_NAME, null, null);
    }


}
