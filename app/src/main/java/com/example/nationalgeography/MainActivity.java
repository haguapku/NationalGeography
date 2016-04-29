package com.example.nationalgeography;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.example.nationalgeography.adapter.NationAdapter;
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
                    for(int i=0; i<jsonArray.length(); i++){
                        JSONObject object = jsonArray.getJSONObject(i);
                        String title = object.getString("title");
                        String desc = object.getString("description");
                        String imageUrl = object.getString("imageHref");
                        items.add(new Item(title,desc,imageUrl));
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

        //Click to refresh data
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                items.clear();
                Message msg = new Message();
                msg.what = 0x0000;
                handler.sendMessage(msg);
                Utils.getNationJSON(GET_NATION_JSON,handler);
            }
        });

        items = new ArrayList<>();

        adapter = new NationAdapter(this,items);

        listView.setAdapter(adapter);

        Utils.getNationJSON(GET_NATION_JSON,handler);

    }
}
