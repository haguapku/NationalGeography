package com.example.nationalgeography.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.nationalgeography.R;
import com.example.nationalgeography.model.Item;
import com.example.nationalgeography.util.Utils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by MarkYoung on 16/4/28.
 */
public class NationAdapter extends BaseAdapter{

    private List<Item> items;
    private Context context;

    public NationAdapter(Context context,List<Item> items) {
        this.items = items;
        this.context = context;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Item getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null){
            view = LayoutInflater.from(context).inflate(R.layout.row_layout,null);
        }
        TextView row_titile = (TextView) view.findViewById(R.id.row_title);
        TextView row_desc = (TextView) view.findViewById(R.id.row_desc);
        ImageView row_image = (ImageView) view.findViewById(R.id.row_image);

        Item item = items.get(i);
        row_titile.setText(item.getTitle());
        row_desc.setText(item.getDescription());

        String imageUrl = item.getImageHref();
        if(imageUrl!=null){
            BitmapWorkerTask task = new BitmapWorkerTask(row_image);
            task.execute(imageUrl);
        }

        return view;
    }

    class BitmapWorkerTask extends AsyncTask<String,Void,BitmapDrawable> {

        String url;
        ImageView imageView;

        public BitmapWorkerTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected BitmapDrawable doInBackground(String... strings) {
            url = strings[0];
            Bitmap bitmap = downloadBitmap(url);
            BitmapDrawable drawable = new BitmapDrawable(context.getResources(),bitmap);
            return drawable;
        }

        private Bitmap downloadBitmap(String imageUrl){
            Bitmap bitmap = null;
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(imageUrl).openConnection();
                conn.setConnectTimeout(5*1000);
                conn.setReadTimeout(10*1000);
                bitmap = BitmapFactory.decodeStream(conn.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(conn != null){
                    conn.disconnect();
                }
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(BitmapDrawable drawable) {
                imageView.setImageDrawable(drawable);
        }
    }
}
