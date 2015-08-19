package korolyov.ctdev.itmo.services.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import korolyov.ctdev.itmo.services.R;
import korolyov.ctdev.itmo.services.contracts.InternalContract;
import korolyov.ctdev.itmo.services.database.CategoriesDatabaseHelper;
import korolyov.ctdev.itmo.services.database.GoodsDatabaseHelper;
import korolyov.ctdev.itmo.services.items.Category;
import korolyov.ctdev.itmo.services.tools.FloatingActionButton;
import korolyov.ctdev.itmo.services.tools.Serializer;


public class MainActivity extends ActionBarActivity {
    private final String TAG = this.getClass().getCanonicalName();
    private final List<Category> categoriesList = new ArrayList<>();
    private ArrayAdapter<Category> arrayAdapter;
    private ListView lvCategories;
    private Context applicationContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applicationContext = this.getApplicationContext();

        FloatingActionButton fabButton = new FloatingActionButton.Builder(this)
                .withDrawable(getResources().getDrawable(R.drawable.ic_refresh_white))
                .withButtonColor(Color.BLUE)
                .withGravity(Gravity.BOTTOM | Gravity.RIGHT)
                .withMargins(0, 0, 16, 16)
                .create();
        fabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                categoriesList.clear();
                arrayAdapter.notifyDataSetChanged();
                Toast.makeText(applicationContext, R.string.init_refresh_toast_message, Toast.LENGTH_LONG).show();
                DownloadJSON downloadJSON = new DownloadJSON();
                downloadJSON.execute();
                Log.d(TAG, "Refreshing started with download new JSON");
            }
        });

        lvCategories = (ListView) findViewById(R.id.lvCategories);
        arrayAdapter = new ArrayAdapter<>(this, R.layout.list_item, categoriesList);
        lvCategories.setAdapter(arrayAdapter);
        lvCategories.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Category category = categoriesList.get(position);
                if (category.getSubs() != null) {
                    Log.d(TAG, "Category have subs. Showing subs level");
                    Intent intent = new Intent(applicationContext, GoodsActivity.class);
                    intent.putExtra(InternalContract.SUBS, Serializer.serialize(categoriesList.get(position).getSubs()));
                    intent.putExtra(InternalContract.TITLE, categoriesList.get(position).getTitle());
                    startActivity(intent);
                }
            }
        });

        loadContent();
    }

    private void loadContent() {
        LoadFromDB loadFromDB = new LoadFromDB();
        loadFromDB.execute();
        Log.d(TAG, "Try to load data from db");
    }

    private class LoadFromDB extends AsyncTask<Boolean, Void, Boolean> {
        private boolean needDownloadAfter;

        @Override
        protected Boolean doInBackground(Boolean... params) {
            needDownloadAfter = (params.length == 0) ? true : params[0];
            CategoriesDatabaseHelper categoriesDatabaseHelper = new CategoriesDatabaseHelper(applicationContext);
            SQLiteDatabase categories = categoriesDatabaseHelper.getReadableDatabase();
            Cursor cursor = categoriesDatabaseHelper.getItems(categories);
            int columnTitle = cursor.getColumnIndex(CategoriesDatabaseHelper.TITLE);
            int columnSubs = cursor.getColumnIndex(CategoriesDatabaseHelper.SUBS);
            if (cursor.getCount() == 0) {
                return false;
            }
            while (cursor.moveToNext()) {
                categoriesList.add(new Category(cursor.getString(columnTitle), Serializer.deserialize(cursor.getString(columnSubs))));
            }
            categories.close();
            categoriesDatabaseHelper.close();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (!aBoolean) {
                Log.d(TAG, "DB empty.");
                if (needDownloadAfter) {
                    Log.d(TAG, "Download JSON");
                    DownloadJSON downloadJSON = new DownloadJSON();
                    downloadJSON.execute();
                }
            } else {
                Log.d(TAG, "Loaded from DB");
                arrayAdapter.notifyDataSetChanged();
            }
        }
    }

    private class UploadToDB extends AsyncTask<JSONArray, Void, Boolean> {

        @Override
        protected Boolean doInBackground(JSONArray... params) {
            GoodsDatabaseHelper goodsDatabaseHelper = new GoodsDatabaseHelper(applicationContext);
            CategoriesDatabaseHelper categoriesDatabaseHelper = new CategoriesDatabaseHelper(applicationContext);
            SQLiteDatabase categories = categoriesDatabaseHelper.getWritableDatabase();
            SQLiteDatabase goods = goodsDatabaseHelper.getWritableDatabase();
            boolean transaction = true;
            if (categoriesDatabaseHelper.isEmpty(categories)) {
                Log.d(TAG, "DB is empty. No need for transaction");
                transaction = false;
            }
            if (transaction) {
                Log.d(TAG, "Refreshing data in DB. Transaction started");
                categories.beginTransaction();
                goods.beginTransaction();
                categoriesDatabaseHelper.deleteAll(categories);
                goodsDatabaseHelper.deleteAll(goods);
            }
            try {
                categoriesDatabaseHelper.uploadJSON(categories, goods, params[0]);
            } catch (JSONException e) {
                Log.e(TAG, "Bad json " + e.getMessage());
                return false;
            }

            if (transaction) {
                goods.setTransactionSuccessful();
                categories.setTransactionSuccessful();
                categories.endTransaction();
                goods.endTransaction();
                Log.d(TAG, "Refreshing data in DB. Transaction finished");
            }

            categories.close();
            categoriesDatabaseHelper.close();
            goods.close();
            goodsDatabaseHelper.close();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            Log.d(TAG, "Data uploaded to DB. Starting load data from db");
            LoadFromDB loadFromDB = new LoadFromDB();
            loadFromDB.execute();
        }

    }

    private class DownloadJSON extends AsyncTask<Void, Void, JSONArray> {

        private JSONArray extractJSONFromInputStream(InputStream stream) throws JSONException {
            StringBuilder builder = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(stream);
                 BufferedReader in = new BufferedReader(reader)) {
                String s;
                while ((s = in.readLine()) != null) {
                    builder.append(s);
                }
            } catch (IOException e) {
                Log.e(TAG, "Problem during read input stream " + e.getMessage());
            }
            return new JSONArray(builder.toString());
        }

        @Override
        protected JSONArray doInBackground(Void... params) {
            try {
                URL url = new URL(getString(R.string.url));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    return extractJSONFromInputStream(connection.getInputStream());
                }
            } catch (MalformedURLException e) {
                Log.e(TAG, "Bad url " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "Something bad during download " + e.getMessage());
            } catch (JSONException e) {
                Log.e(TAG, "Bad json " + e.getMessage());
            }
            return null;

        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            super.onPostExecute(jsonArray);
            if (jsonArray != null) {
                Log.d(TAG, "JSON downloaded. Uploading to DB");
                UploadToDB uploadToDB = new UploadToDB();
                uploadToDB.execute(jsonArray);
            } else {
                Log.e(TAG, "Error while downloading JSON");
                Toast.makeText(applicationContext, R.string.download_error, Toast.LENGTH_LONG).show();
                Log.d(TAG, "Try to recover data from database");
                LoadFromDB loadFromDB = new LoadFromDB();
                loadFromDB.execute(false);
            }
        }
    }

}
