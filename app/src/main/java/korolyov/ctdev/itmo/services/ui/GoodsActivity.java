package korolyov.ctdev.itmo.services.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import korolyov.ctdev.itmo.services.R;
import korolyov.ctdev.itmo.services.contracts.InternalContract;
import korolyov.ctdev.itmo.services.database.GoodsDatabaseHelper;
import korolyov.ctdev.itmo.services.items.Good;
import korolyov.ctdev.itmo.services.tools.Serializer;

public class GoodsActivity extends ActionBarActivity {
    private static Context applicationContext;
    private final String TAG = this.getClass().getCanonicalName();
    private final List<Good> goodsList = new ArrayList<>();
    private ArrayAdapter<Good> arrayAdapter;


    private Set<Integer> readExtras() {
        Bundle extras = getIntent().getExtras();
        String subs = extras.getString(InternalContract.SUBS);
        String title = extras.getString(InternalContract.TITLE);
        getSupportActionBar().setTitle(title);
        return Serializer.deserialize(subs);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goods);
        applicationContext = getApplicationContext();
        Set<Integer> set = readExtras();

        arrayAdapter = new ArrayAdapter<>(applicationContext, R.layout.list_item, goodsList);
        ListView lvGoods = (ListView) findViewById(R.id.lvGoods);
        lvGoods.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Good good = goodsList.get(position);
                if (good.getSubs() != null) {
                    Log.d(TAG, "Category have subs. Showing subs level");
                    Intent intent = new Intent(applicationContext, GoodsActivity.class);
                    intent.putExtra(InternalContract.SUBS, Serializer.serialize(goodsList.get(position).getSubs()));
                    intent.putExtra(InternalContract.TITLE, goodsList.get(position).getTitle());
                    startActivity(intent);
                }
            }
        });
        lvGoods.setAdapter(arrayAdapter);

        LoadGoodsFromDB task = new LoadGoodsFromDB();
        task.execute(set);
        Log.d(TAG, "Loading goods from DB");
    }

    private class LoadGoodsFromDB extends AsyncTask<Set<Integer>, Void, Void> {

        @Override
        protected Void doInBackground(Set<Integer>... params) {
            loadGoods(params[0]);
            return null;
        }

        private void loadGoods(Set<Integer> set) {
            GoodsDatabaseHelper goodsDatabaseHelper = new GoodsDatabaseHelper(applicationContext);
            SQLiteDatabase goodsDB = goodsDatabaseHelper.getReadableDatabase();
            Cursor cursor = goodsDatabaseHelper.getItemsByIds(goodsDB, set);
            int columnId = cursor.getColumnIndex(GoodsDatabaseHelper.ID);
            int columnTitle = cursor.getColumnIndex(GoodsDatabaseHelper.TITLE);
            int columnSubs = cursor.getColumnIndex(GoodsDatabaseHelper.SUBS);
            while (cursor.moveToNext()) {
                goodsList.add(new Good(cursor.getInt(columnId), cursor.getString(columnTitle), Serializer.deserialize(cursor.getString(columnSubs))));
            }
            goodsDB.close();
            goodsDatabaseHelper.close();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(TAG, "Data loaded from DB");
            arrayAdapter.notifyDataSetChanged();
        }
    }

}
