package com.example.nationalgeography.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.nationalgeography.R;

/**
 * Created by MarkYoung on 16/4/29.
 */
public class ViewCache {

    private View baseView;
    private TextView title;
    private TextView desc;
    private ImageView imageView;

    public ViewCache(View baseView) {
        this.baseView = baseView;
    }

    public TextView getTitle() {
        if(title == null){
            title = (TextView) baseView.findViewById(R.id.row_title);
        }
        return title;
    }

    public TextView getDesc() {
        if(desc == null){
            desc = (TextView) baseView.findViewById(R.id.row_desc);
        }
        return desc;
    }

    public ImageView getImageView() {
        if(imageView == null){
            imageView = (ImageView) baseView.findViewById(R.id.row_image);
        }
        return imageView;
    }
}
