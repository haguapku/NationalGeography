package com.example.nationalgeography.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.nationalgeography.R;
import com.example.nationalgeography.model.Item;

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
    private LayoutInflater inflater;

    private Bitmap bitmap;

    private LruCache<String,BitmapDrawable> memoryCache;


    public NationAdapter(Context context,List<Item> items) {

        this.context = context;
        this.items = items;
        this.inflater = LayoutInflater.from(context);
        this.bitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.default_image);

        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory/6;
        memoryCache = new LruCache<String ,BitmapDrawable>(cacheSize);
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
        ViewCache viewCache;
        if(view == null){
            view = inflater.inflate(R.layout.row_layout,null);
            viewCache = new ViewCache(view);
            view.setTag(viewCache);
        }else {
            viewCache = (ViewCache) view.getTag();
        }

        Item item = items.get(i);
        TextView row_title = viewCache.getTitle();
        TextView row_desc = viewCache.getDesc();
        row_title.setText(item.getTitle());
        row_desc.setText(item.getDescription());


        ImageView row_image = viewCache.getImageView();
        String imageUrl = item.getImageHref();
        if(imageUrl != null){
            BitmapDrawable drawable = getBitmapDrawableFromMemoryCache(imageUrl);
            if (drawable != null){
                row_image.setImageDrawable(drawable);
            }else if (cancelPotentialTask(imageUrl,row_image)){
                //Download image
                BitmapWorkerTask task = new BitmapWorkerTask(row_image);
                AsyncDrawable asyncDrawable = new AsyncDrawable(context.getResources(),bitmap,task);
                row_image.setImageDrawable(asyncDrawable);
                task.execute(imageUrl);
            }
        }else{
//            viewCache.getImageView().setImageResource(R.drawable.default_image);
        }

        return view;
    }

    private boolean cancelPotentialTask(String imageUrl, ImageView imageView) {
        BitmapWorkerTask task = getDownLoadTask(imageView);
        if (task != null) {
            String url = task.url;
            if (url == null || !url .equals(imageUrl)){
                task.cancel(true);
            }else{
                return false;
            }
        }
        return true;
    }

    private BitmapDrawable getBitmapDrawableFromMemoryCache(String imageUrl) {
        return memoryCache.get(imageUrl);
    }

    private void addBitmapDrawableToMemoryCache(String imageUrl,BitmapDrawable drawable){
        if (getBitmapDrawableFromMemoryCache(imageUrl) == null ){
            memoryCache.put(imageUrl, drawable);
        }
    }

    private BitmapWorkerTask getDownLoadTask(ImageView imageView){
        if (imageView != null){
            Drawable drawable  = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable ){
                return  ((AsyncDrawable) drawable).getDownLoadTaskFromAsyncDrawable();
            }
        }
        return null;
    }

    class AsyncDrawable extends  BitmapDrawable{
        private  WeakReference<BitmapWorkerTask> bitmapWorkerTaskWeakReference;

        public AsyncDrawable(Resources resources, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask){
            super(resources,bitmap);
            bitmapWorkerTaskWeakReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        private BitmapWorkerTask getDownLoadTaskFromAsyncDrawable(){
            return bitmapWorkerTaskWeakReference.get();
        }
    }

    class BitmapWorkerTask extends AsyncTask<String,Void,BitmapDrawable> {

        String url;
        private WeakReference<ImageView> imageViewWeakReference;

        public BitmapWorkerTask(ImageView imageView) {
            imageViewWeakReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected BitmapDrawable doInBackground(String... strings) {
            url = strings[0];
            Bitmap bitmap = downloadBitmap(url);
            BitmapDrawable drawable = new BitmapDrawable(context.getResources(),bitmap);
            addBitmapDrawableToMemoryCache(url,drawable);
            return drawable;
        }

        private ImageView getAttachedImageView() {
            ImageView imageView = imageViewWeakReference.get();
            if (imageView != null){
                BitmapWorkerTask task = getDownLoadTask(imageView);
                if (this == task ){
                    return  imageView;
                }
            }
            return null;
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
            super.onPostExecute(drawable);
            ImageView imageView = getAttachedImageView();
            if ( imageView != null && drawable != null){
                imageView.setImageDrawable(drawable);
            }
        }
    }
}
