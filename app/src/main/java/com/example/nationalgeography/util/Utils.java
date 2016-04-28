package com.example.nationalgeography.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by MarkYoung on 16/4/28.
 */
public class Utils {

    public static void getNationJSON(final String url, final Handler handler) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn;
                InputStream is;
                try {
                    conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.connect();
                    is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line;
                    StringBuilder sb = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    Message msg = new Message();
                    msg.obj = sb.toString();
                    handler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void setPicBitmap(final ImageView imageView, final String url){

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
}