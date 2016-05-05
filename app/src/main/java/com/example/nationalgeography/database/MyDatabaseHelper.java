package com.example.nationalgeography.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by MarkYoung on 16/5/5.
 */
public class MyDatabaseHelper extends SQLiteOpenHelper {

    public static final String CREATE_NATION = "create table Nation ("
            + "id integer primary key autoincrement, "
            + "title text, "
            + "description text, "
            + "imageHref text)";

    public MyDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_NATION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
