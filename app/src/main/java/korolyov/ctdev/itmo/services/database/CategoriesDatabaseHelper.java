package korolyov.ctdev.itmo.services.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import korolyov.ctdev.itmo.services.contracts.ExternalContract;
import korolyov.ctdev.itmo.services.tools.Serializer;

public class CategoriesDatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "categories";
    public static final String DROP_DATABASE = "DROP TABLE IF EXISTS " + DATABASE_NAME;
    public static final int DATABASE_VERSION = 1;
    public static final String ID = "_id";
    public static final String TITLE = "title";
    public static final String SUBS = "subs";
    public static final String CREATE_DATABASE = "CREATE TABLE " + DATABASE_NAME
            + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + TITLE + " TEXT, " + SUBS + " TEXT);";


    public CategoriesDatabaseHelper(Context context) {
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

    public Cursor getItems(SQLiteDatabase db) {
        return db.query(DATABASE_NAME, null, null, null, null, null, null);
    }

    public void deleteAll(SQLiteDatabase db) {
        db.delete(DATABASE_NAME, null, null);
    }

    private void uploadData(SQLiteDatabase db, String databaseName, int id, String title, Set<Integer> subs) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ID, id);
        contentValues.put(TITLE, title);
        contentValues.put(SUBS, Serializer.serialize(subs));
        db.insert(databaseName, null, contentValues);
    }

    private void uploadData(SQLiteDatabase db, String databaseName, String title, Set<Integer> subs) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(TITLE, title);
        contentValues.put(SUBS, Serializer.serialize(subs));
        db.insert(databaseName, null, contentValues);
    }

    public boolean isEmpty(SQLiteDatabase db) {
        Cursor cursor = this.getItems(db);
        int count = cursor.getCount();
        cursor.close();
        return count == 0;
    }

    public void uploadJSON(SQLiteDatabase categoriesDB, SQLiteDatabase goodsDB, JSONArray array) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            JSONObject currentObject = array.getJSONObject(i);
            String title = currentObject.getString(ExternalContract.TITLE);
            if (currentObject.has(ExternalContract.SUBS)) {
                Map<Integer, String> goods = new HashMap<>();
                JSONArray jsonArray = currentObject.getJSONArray(ExternalContract.SUBS);
                for (int j = 0; j < jsonArray.length(); j++) {
                    JSONObject good = jsonArray.getJSONObject(j);
                    int goodId = good.getInt(ExternalContract.ID);
                    String goodTitle = good.getString(ExternalContract.TITLE);
                    goods.put(goodId, goodTitle);
                    Set<Integer> goodsSubs = null;
                    if (good.has(ExternalContract.SUBS)) {
                        goodsSubs = new HashSet<>();
                        JSONArray subs = good.getJSONArray(ExternalContract.SUBS);
                        for (int k = 0; k < subs.length(); k++) {
                            JSONObject sub = subs.getJSONObject(k);
                            int subId = sub.getInt(ExternalContract.ID);
                            String subTitle = sub.getString(ExternalContract.TITLE);
                            goodsSubs.add(subId);
                            uploadData(goodsDB, GoodsDatabaseHelper.DATABASE_NAME, subId, subTitle, null);
                        }
                    }
                    uploadData(goodsDB, GoodsDatabaseHelper.DATABASE_NAME, goodId, goodTitle, goodsSubs);
                }
                uploadData(categoriesDB, DATABASE_NAME, title, goods.keySet());
                goods.clear();
            }
        }
    }

}
