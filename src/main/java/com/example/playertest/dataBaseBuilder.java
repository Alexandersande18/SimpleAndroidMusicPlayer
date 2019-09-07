package com.example.playertest;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteTransactionListener;
import android.os.Environment;

import java.util.ArrayList;

public class dataBaseBuilder extends SQLiteOpenHelper {
    final String CREATE_SONG_TABLE = "CREATE TABLE music(" +
            "    song_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    song_path ," +
            "    song_name ," +
            "    song_artist )";

    final String CREATE_LISTS_TABLE = "CREATE TABLE playlists(list_id INTEGER PRIMARY KEY AUTOINCREMENT, list_name)";

    final String CREATE_BELONG_TO  = "CREATE TABLE belongto(_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "key_song INTEGER, value_list INTEGER, FOREIGN KEY(key_song) REFERENCES music(song_id) ON DELETE CASCADE," +
            "FOREIGN KEY(value_list) REFERENCES playlists(list_id) ON DELETE CASCADE)";

    final String CREATE_HISTORY = "CREATE TABLE history(_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "his_song_fullname)";

    public dataBaseBuilder(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(this.CREATE_LISTS_TABLE);
        db.execSQL(this.CREATE_SONG_TABLE);
        db.execSQL(this.CREATE_BELONG_TO);
        db.execSQL(CREATE_HISTORY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public static void insertSong(SQLiteDatabase db, String path, String name[]){
        String cmd = "INSERT INTO music(song_path, song_name, song_artist) VALUES(?, ?, ?)";
        db.execSQL(cmd, new String[]{path, name[1], name[0]});
    }

    public static void initList(SQLiteDatabase db, int songcnt){
        db.execSQL("INSERT INTO playlists(list_id, list_name) VALUES(null, '所有音乐')");
        String cmd = "INSERT INTO belongto(key_song, value_list) VALUES(?, 1)";
        for(int i = 1; i<=songcnt; ++i){
            db.execSQL(cmd, new String[]{""+i});
        }
    }
    public static void instertList(SQLiteDatabase db, String name){
        String cmd = "INSERT INTO playlists(list_name) VALUES(?)";
        db.execSQL(cmd, new String[]{name});
    }
    public static void insertBelongTo(SQLiteDatabase db, String song_num, String list_name){
        Cursor c = db.rawQuery("select list_id from playlists where list_name = '"+list_name+"'",null);
        c.moveToNext();
        int index = c.getInt(0);
        String cmd = "INSERT INTO belongto(key_song, value_list) VALUES(?, ?)";
        db.execSQL(cmd, new String[]{song_num, index+""});
    }

    public static void insertHistory(SQLiteDatabase db, String name){
        db.execSQL("INSERT INTO history VALUES(null, ?)", new String[]{name});
        int cnt = 0;
        try {
            Cursor cursor = db.rawQuery("select * from history", null);
            cursor.moveToNext();
            cnt = cursor.getInt(0);
        }catch (Exception r){   }
        if(cnt>=100)
            db.execSQL("delete from history where _id in (select _id from history limit 5)");
    }

    public static void deleteList(SQLiteDatabase db, String listname){
        db.execSQL("delete from playlists where playlists.list_name = '" +listname+ "'" );
    }
    public static int getListNum(SQLiteDatabase db, String name){
        Cursor c = db.rawQuery("select list_id from playlists where list_name = '"+name+"'",null);
        c.moveToNext();
        return c.getInt(0);
    }
}
