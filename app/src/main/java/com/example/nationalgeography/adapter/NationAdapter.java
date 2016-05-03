package com.example.nationalgeography.adapter;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.nationalgeography.R;
import com.example.nationalgeography.model.Item;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import libcore.io.DiskLruCache;


/**
 * Created by MarkYoung on 16/4/28.
 */
public class NationAdapter extends BaseAdapter{

    public final static String TAG = "=====tag=====";

    static final int DISK_CACHE_DEFAULT_SIZE = 10 * 1024 * 1024;

    private List<Item> items;
    private Context context;
    private LayoutInflater inflater;

    private Bitmap bitmap;

    /*
        Cache for downloaded images
     */

    private LruCache<String,BitmapDrawable> memoryCache;

    private DiskLruCache diskLruCache;


    public NationAdapter(Context context,List<Item> items) {

        this.context = context;
        this.items = items;
        this.inflater = LayoutInflater.from(context);
        this.bitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.default_image);


        //First level cache on memory
        //Get the maximum value of usable memory
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory/6;
        memoryCache = new LruCache<String ,BitmapDrawable>(cacheSize){
            @Override
            protected int sizeOf(String key, BitmapDrawable value) {
                return value.getBitmap().getByteCount();
            }
        };

        //Second level cache on disk
        try {
            File cacheDir = getDiskCacheDir(context, "bitmap");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            diskLruCache = DiskLruCache.open(cacheDir, getAppVersion(context), 1, DISK_CACHE_DEFAULT_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        //Try to get cache from memory
        BitmapDrawable drawable = getBitmapDrawableFromMemoryCache(imageUrl);
        if (drawable != null) {
            row_image.setImageDrawable(drawable);
            return view;
        }

        //Try to get cache from disk
        drawable = getBitmapDrawableFromDisk(imageUrl);
        if (drawable != null) {
            addBitmapDrawableToMemoryCache(imageUrl,drawable);
            row_image.setImageDrawable(drawable);
            return view;
        }else if(cancelPotentialTask(imageUrl,row_image)) {
            BitmapWorkerTask task = new BitmapWorkerTask(row_image);
            AsyncDrawable asyncDrawable = new AsyncDrawable(context.getResources(),bitmap,task);
            row_image.setImageDrawable(asyncDrawable);
            task.execute(imageUrl);
        }

        return view;
    }

    /*
        Cancel potential task when there is another request task for current ImageView
        Cancel succeed return true ,otherwise false
     */

    private boolean cancelPotentialTask(String imageUrl, ImageView imageView) {
        BitmapWorkerTask task = getBitmapWorkerTask(imageView);
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

    /*
        Get an image from LruCache
     */

    private BitmapDrawable getBitmapDrawableFromMemoryCache(String imageUrl) {
        return memoryCache.get(imageUrl);
    }

    /*
        Add an image to LruCache
     */

    private void addBitmapDrawableToMemoryCache(String imageUrl,BitmapDrawable drawable){
        if (getBitmapDrawableFromMemoryCache(imageUrl) == null ){
            memoryCache.put(imageUrl, drawable);
        }
    }

    /*
        Get an image from DiskLruCache
     */

    public BitmapDrawable getBitmapDrawableFromDisk(String url) {
        try {
            String key = hashKeyForDisk(url);
            DiskLruCache.Snapshot snapShot = diskLruCache.get(key);
            if (snapShot != null) {
                InputStream is = snapShot.getInputStream(0);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                return new BitmapDrawable(context.getResources(),bitmap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /*
        Get BitmapWorkerTask related to specific ImageView
     */

    private BitmapWorkerTask getBitmapWorkerTask(ImageView imageView){
        if (imageView != null){
            Drawable drawable  = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable ){
                return  ((AsyncDrawable) drawable).getDownLoadTaskFromAsyncDrawable();
            }
        }
        return null;
    }

    /*
        Customised Drawable which has the WeakReference of BitmapWorkerTask
     */

    class AsyncDrawable extends  BitmapDrawable{
        private  WeakReference<BitmapWorkerTask> bitmapWorkerTaskWeakReference;

        public AsyncDrawable(Resources resources, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask){
            super(resources,bitmap);
            bitmapWorkerTaskWeakReference = new WeakReference<>(bitmapWorkerTask);
        }

        private BitmapWorkerTask getDownLoadTaskFromAsyncDrawable(){
            return bitmapWorkerTaskWeakReference.get();
        }
    }

    /*
        AsyncTask for downloading images
     */

    class BitmapWorkerTask extends AsyncTask<String,Void,BitmapDrawable> {

        String url;
        private WeakReference<ImageView> imageViewWeakReference;

        public BitmapWorkerTask(ImageView imageView) {
            imageViewWeakReference = new WeakReference<>(imageView);
        }

        @Override
        protected BitmapDrawable doInBackground(String... strings) {
            try {
                url = strings[0];
                String key = hashKeyForDisk(url);
                DiskLruCache.Editor editor = diskLruCache.edit(key);
                if (editor != null) {
                    OutputStream outputStream = editor.newOutputStream(0);
                    if (downloadUrlToStream(url, outputStream)) {
                        editor.commit();
                    } else {
                        editor.abort();
                    }
                }
                diskLruCache.flush();
                BitmapDrawable drawable = getBitmapDrawableFromDisk(url);
                if(drawable != null){
                    addBitmapDrawableToMemoryCache(url,drawable);
                }

                return drawable;
            }catch(IOException e){
                e.printStackTrace();
            }
            return null;
        }


        private boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
            HttpURLConnection urlConnection = null;
            BufferedOutputStream out = null;
            BufferedInputStream in = null;
            try {

                final URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(5*1000);
                urlConnection.setReadTimeout(10*1000);
                urlConnection.setRequestProperty("User-Agent","Mozilla/4.0(compatible;MSIE 5.0;Windows NT;DigExt)");
                if (urlConnection.getResponseCode() == 200) {
                    in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
                    out = new BufferedOutputStream(outputStream, 8 * 1024);
                    int b;
                    while ((b = in.read()) != -1) {
                        out.write(b);
                    }
                    return true;
                }else if(urlConnection.getResponseCode() == 301){
                    String newUrl = urlConnection.getHeaderField("Location");
                    urlConnection = (HttpURLConnection) new URL(newUrl).openConnection();
                    in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
                    out = new BufferedOutputStream(outputStream, 8 * 1024);
                    int b;
                    while ((b = in.read()) != -1) {
                        out.write(b);
                    }
                    return true;
                }else{
                    in = new BufferedInputStream(Bitmap2IS(BitmapFactory.decodeResource(context.getResources(),R.drawable.default_image)),
                            8*1024);
                    out = new BufferedOutputStream(outputStream, 8 * 1024);
                    int b;
                    while ((b = in.read()) != -1) {
                        out.write(b);
                    }
                    return true;
                }
            } catch (final IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /*
            Get ImageView related to current BitmapWorkerTask
         */

        private ImageView getAttachedImageView() {
            ImageView imageView = imageViewWeakReference.get();
            if (imageView != null){
                BitmapWorkerTask task = getBitmapWorkerTask(imageView);
                if (this == task ){
                    return  imageView;
                }
            }
            return null;
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

    //Get the directory for disk cache
    private File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    //Get version of the app
    private int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    //MD5
    private String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    //Transfer Bitmap to InputStream
    private InputStream Bitmap2IS(Bitmap bm){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        InputStream sbs = new ByteArrayInputStream(baos.toByteArray());
        return sbs;
    }
}
