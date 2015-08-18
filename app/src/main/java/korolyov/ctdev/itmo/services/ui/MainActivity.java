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
    }

    private class LoadFromDB extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
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
                DownloadJSON downloadJSON = new DownloadJSON();
                downloadJSON.execute();
            } else {
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
            Cursor cursor = categoriesDatabaseHelper.getItems(categories);
            boolean transaction = true;
            if (cursor.getCount() == 0) {
                transaction = false;
            }
            if (transaction) {
                categories.beginTransaction();
                goods.beginTransaction();
                categoriesDatabaseHelper.deleteAll(categories);
                goodsDatabaseHelper.deleteAll(goods);
            }
            try {
                categoriesDatabaseHelper.uploadJSON(categories, goods, params[0]);
            } catch (JSONException e) {
                return false;
            }

            if (transaction) {
                goods.setTransactionSuccessful();
                categories.setTransactionSuccessful();
                categories.endTransaction();
                goods.endTransaction();
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
            LoadFromDB loadFromDB = new LoadFromDB();
            loadFromDB.execute();
        }

    }

    private class DownloadJSON extends AsyncTask<Void, Void, JSONArray> {

        private JSONArray extractJSONFromInputStream(InputStream stream) {
            StringBuilder builder = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(stream);
                 BufferedReader in = new BufferedReader(reader)) {
                String s;
                while ((s = in.readLine()) != null) {
                    builder.append(s);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            String string = builder.toString();
            JSONArray result = null;
            try {
                result = new JSONArray(string);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected JSONArray doInBackground(Void... params) {
            try {
                URL url = new URL("https://money.yandex.ru/api/categories-list");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Log.d("ALEXEI", "OK");
                    return extractJSONFromInputStream(connection.getInputStream());

                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            super.onPostExecute(jsonArray);
            if (jsonArray != null) {
                UploadToDB uploadToDB = new UploadToDB();
                uploadToDB.execute(jsonArray);
            }
        }
    }

}
