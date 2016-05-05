package com.example.nationalgeography;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.nationalgeography.adapter.NationAdapter;
import com.example.nationalgeography.database.MyDatabaseHelper;
import com.example.nationalgeography.model.Item;
import com.example.nationalgeography.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String GET_NATION_JSON = "https://dl.dropboxusercontent.com/u/746330/facts.json";

    public List<Item> items;

    private NationAdapter adapter;

    private ListView listView;
    private Button update;

    private ActionBar actionBar;

    private MyDatabaseHelper dbHelper;

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

            if(msg.what == 0x0000){
                adapter.notifyDataSetChanged();
            }else if(msg.what == 0x0001){
                String jsonData = (String) msg.obj;
                try {
                    JSONObject jsonObject = new JSONObject(jsonData);
                    String actionBarTitle = jsonObject.getString("title");
                    actionBar.setTitle(actionBarTitle);
                    JSONArray jsonArray = jsonObject.getJSONArray("rows");
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    for(int i=0; i<jsonArray.length(); i++){
                        JSONObject object = jsonArray.getJSONObject(i);
                        String title = object.getString("title");
                        String desc = object.getString("description");
                        String imageUrl = object.getString("imageHref");
                        items.add(new Item(title,desc,imageUrl));

                        //Add item data to database
                        values.put("title",title);
                        values.put("description",desc);
                        values.put("imageHref",imageUrl);
                        db.insert("Nation",null,values);
                    }
                    adapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actionBar = getSupportActionBar();

        listView = (ListView) findViewById(R.id.list_detail);
        update = (Button) findViewById(R.id.update);

        //Initialize database
        dbHelper = new MyDatabaseHelper(this, "NationGeography.db", null, 1);

        //Click to refresh data
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Utils.isNetworkAvailable(MainActivity.this)){
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete("Nation",null,null);
                    items.clear();
                    Message msg = new Message();
                    msg.what = 0x0000;
                    handler.sendMessage(msg);
                    Utils.getNationJSON(GET_NATION_JSON,handler);
                }else{
                    Toast.makeText(MainActivity.this,"No network",Toast.LENGTH_LONG).show();
                }
            }
        });

        items = new ArrayList<>();

        adapter = new NationAdapter(this,items);

        listView.setAdapter(adapter);

        //Check data first, if not null, get data from database other than server
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query("Nation",null,null,null,null,null,null);

        if(cursor.moveToNext()){
            do{
                String title = cursor.getString(cursor.getColumnIndex("title"));
                String description = cursor.getString(cursor.getColumnIndex("description"));
                String imageHref = cursor.getString(cursor.getColumnIndex("imageHref"));
                items.add(new Item(title,description,imageHref));
            }while (cursor.moveToNext());
            adapter.notifyDataSetChanged();
        }else{
            Utils.getNationJSON(GET_NATION_JSON,handler);
        }

    }
}
