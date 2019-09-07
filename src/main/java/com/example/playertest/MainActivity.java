package com.example.playertest;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    SeekBar playseek;
    MediaPlayer mp;
    Button play, prev, next, mode, enterlist;
    Playlist defaultlist, selectedlist, nowplayinglist;
    ListView l1;
    int playmode = 2;
    TextView nowplaying;
    int currentpos;
    ImageView nowalbum;
    SimpleAdapter listAdapter;
    String[] names;
    dataBaseBuilder dbh;
    SQLiteDatabase db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setWidgets();
        try{
            currentpos = savedInstanceState.getInt("pos");
            onplay(nowplayinglist.content.get(nowplayinglist.getCurrentindex()));
            mp.seekTo(currentpos);
        }catch (Exception e){}
        runtimePermission();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(mp != null){
                    try{
                        Message msg = new Message();
                        msg.what = mp.getCurrentPosition();
                        handler.sendMessage(msg);
                        Thread.sleep(1000);
                    }catch (Exception e){}
                }
            }
        }).start();
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            currentpos = msg.what;
            playseek.setProgress(currentpos);
        }
    };

    public void setWidgets(){
        mp = new MediaPlayer();
        mp.seekTo(0);
        mp.setVolume(1.0f,1.0f);
        dbh = new dataBaseBuilder(MainActivity.this, "music.db3", null,1);
        db = dbh.getWritableDatabase();
        //db.execSQL("PRAGMA foreign_keys = ON");
        l1 = findViewById(R.id.playlist);
        play = findViewById(R.id.playbutton);
        prev = findViewById(R.id.prevbutton);
        next = findViewById(R.id.nextbutton);
        mode = findViewById(R.id.playmod);
        playseek = findViewById(R.id.playbar);
        enterlist = findViewById(R.id.enterplaylist);
        nowplaying = findViewById(R.id.nowplaying);
        nowalbum = findViewById(R.id.nowalbum);
    }

    public void onplay(Song song){
        mp.reset();
        try {
            mp.setDataSource(song.path);
            mp.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String temp = song.name[0] + " - " + song.name[1];
        play.setBackgroundResource(R.drawable.icon_pause);
        nowplaying.setText(temp);
        if(song.albempic == null)
            nowalbum.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.osutest, null));
        else{
            nowalbum.setImageBitmap(song.albempic);
        }
        dataBaseBuilder.insertHistory(db,temp);
        playseek.setMax(mp.getDuration());
        mp.seekTo(0);
        mp.start();
    }

    public void Onmodechange(View v){
        if(playmode == 0){
            mode.setBackgroundResource(R.drawable.icon_playmod_singleloop);
            Toast toast = Toast.makeText(getApplicationContext(),"单曲循环",Toast.LENGTH_SHORT);
            toast.show();
            playmode = 1;
        }
        else if(playmode == 1){
            Toast toast = Toast.makeText(getApplicationContext(),"列表循环",Toast.LENGTH_SHORT);
            toast.show();
            mode.setBackgroundResource(R.drawable.icon_playmod_listloop);
            playmode = 2;
        }
        else{
            mode.setBackgroundResource(R.drawable.icon_random_play);
            Toast toast = Toast.makeText(getApplicationContext(), "随机播放", Toast.LENGTH_SHORT);
            toast.show();
            playmode = 0;
        }
    }

    public void runtimePermission(){
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Cursor cursor = db.rawQuery("SELECT * FROM music", null);
                        if(cursor.getCount()!=0)
                            displayOnNextBoot();
                        else
                            displayOnFirstBoot();
                    }
                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.cancelPermissionRequest();
                    }
                }).check();
    }

    public ArrayList<File> findsongs(File file){
        ArrayList<File> arr = new ArrayList<File>();
        File[] files = file.listFiles();
        for(File singlefile : files){
            if(singlefile.getName().endsWith(".mp3") || singlefile.getName().endsWith(".wav")){
                arr.add(singlefile);
            }
        }
        return arr;
    }

    public void displayOnFirstBoot(){
        ArrayList<File> mysongs = new ArrayList<File>();
        ArrayList<File> netease_file = new ArrayList<File>();
        try {
            mysongs = findsongs(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));
        }catch (Exception e){
            Toast toast = Toast.makeText(getApplicationContext(), "找不到系统音乐文件夹", Toast.LENGTH_SHORT);
            toast.show();
        }
        try {
            File f = new File("/storage/emulated/0/netease/cloudmusic/Music");
            netease_file = findsongs(f);
            mysongs.addAll(netease_file);
        }catch (Exception r){
            Toast toast = Toast.makeText(getApplicationContext(), "找不到网易云音乐文件夹", Toast.LENGTH_SHORT);
            toast.show();
        }
        int sum = mysongs.size();
        names = new String[sum];
        List<Map<String, Object>> list1= new ArrayList<Map<String, Object>>();
        defaultlist = new Playlist();
        for(int i = 0; i < sum; ++i){
            String filename = mysongs.get(i).getName().replace(".mp3","").replace(".wav","");
            Song song = new Song(mysongs.get(i).toString(), filename.split(" - "));
            defaultlist.AddToList(song);
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("name",song.name[1]);
            m.put("auther", song.name[0]);
            if(song.albempic == null)
                m.put("albempic", colorselctor(i));
            else
                m.put("albempic", song.albempic);
            list1.add(m);
        }
        for( Song s:defaultlist.content ) {
            dataBaseBuilder.insertSong(db, s.path, s.name);
        }
        dataBaseBuilder.initList(db, defaultlist.GetLength());
        listAdapter = new SimpleAdapter(this, list1, R.layout.array_item,
                new String[]{"name","albempic", "auther"},
                new int[]{R.id.name, R.id.albempic, R.id.auther});
        listAdapter.setViewBinder(new myViewBinder());
        defaultlist.adapter = listAdapter;
        defaultlist.listname = "所有歌曲";
        l1.setAdapter(listAdapter);
        nowplayinglist = defaultlist;
        initControllers();
    }

    public void displayOnNextBoot(){
        defaultlist = new Playlist();
        Cursor cursor = db.rawQuery("SELECT * FROM music order by song_artist", null);
        List<Map<String, Object>> list1= new ArrayList<Map<String, Object>>();
        while (cursor.moveToNext()){
            String[] str = new String[2];
            str[0] = cursor.getString(3);
            str[1] = cursor.getString(2);
            Song song = new Song(cursor.getString(1), str);
            defaultlist.AddToList(song);
        }
        int i = 0;
        for(Song so:defaultlist.content){
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("name", so.name[1]);
            map.put("auther", so.name[0]);
            if(so.albempic == null)
                map.put("albempic", colorselctor(i++));
            else
                map.put("albempic", so.albempic);
            list1.add(map);
        }
        listAdapter = new SimpleAdapter(this, list1, R.layout.array_item,
                new String[]{"name","albempic", "auther"},
                new int[]{R.id.name, R.id.albempic, R.id.auther});
        listAdapter.setViewBinder(new myViewBinder());
        defaultlist.adapter = listAdapter;
        defaultlist.listname = "所有歌曲";
        l1.setAdapter(listAdapter);
        nowplayinglist = defaultlist;
        initControllers();
    }

    public void initControllers(){
        l1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                onplay(nowplayinglist.PlayByIndex(pos));
            }
        });

        l1.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return false;
            }
        });

        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if(playmode == 1)
                    onplay(nowplayinglist.content.get(nowplayinglist.getCurrentindex()));
                else {
                    onplay(nowplayinglist.NextMusic(playmode));
                }
            }
        });

        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return true;
            }
        });

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onplay(nowplayinglist.PrevMusic(playmode));
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onplay(nowplayinglist.NextMusic(playmode));
            }
        });

        playseek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b){
                    mp.seekTo(i);
                    playseek.setProgress(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mp.isPlaying()){
                    mp.start();
                    play.setBackgroundResource(R.drawable.icon_pause);
                }
                else{
                    mp.pause();
                    play.setBackgroundResource(R.drawable.icon_play);
                }
            }
        });

        enterlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in = new Intent(MainActivity.this, PlayerListActivity.class);
                startActivityForResult(in, 0x11);

            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0x11 && resultCode == 0x11){
            Bundle bundle = data.getExtras();
            int list_num = bundle.getInt("list_num");
            selectedlist = new Playlist();
            try {
                Cursor cursor = db.rawQuery("select song_id from music where song_id in (select key_song from belongto where value_list = ?)",
                        new String[]{list_num + ""});
                while (cursor.moveToNext()) {
                    int index = cursor.getInt(0) - 1;
                    selectedlist.AddToList(defaultlist.content.get(index));
                }
                List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                for (Song so : selectedlist.content) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("name", so.name[1]);
                    map.put("auther", so.name[0]);
                    if (so.albempic == null)
                        map.put("albempic", colorselctor(0));
                    else
                        map.put("albempic", so.albempic);
                    list.add(map);
                }
                SimpleAdapter newAdapter = new SimpleAdapter(this, list, R.layout.array_item,
                        new String[]{"name", "albempic", "auther"},
                        new int[]{R.id.name, R.id.albempic, R.id.auther});
                newAdapter.setViewBinder(new myViewBinder());
                selectedlist.adapter = newAdapter;
                l1.setAdapter(newAdapter);
                nowplayinglist = selectedlist;
                onplay(selectedlist.content.get(0));
            }catch(Exception e){
                Toast toast = Toast.makeText(getApplicationContext(),"列表为空，请添加歌曲",Toast.LENGTH_SHORT);
                toast.show();
                nowplayinglist = defaultlist;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_button, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.history){
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("历史记录");
            builder.setItems(getHistory(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String[] getHistory(){
        Cursor cursor;
        try{
            cursor = db.rawQuery("SELECT his_song_fullname FROM history", null);
        } catch (Exception r){
            return null;
        }
        int count = cursor.getCount();
        if(count == 0)
            return null;
        String[] str = new String[count];
        count = 0;
        while(cursor.moveToNext()){
            str[count++] = cursor.getString(0);
        }
        return str;
    }

    @Override
    protected void onStop() {
        super.onStop();
        //mp.reset();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("pos", currentpos);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //currentpos = savedInstanceState.getInt("pos");
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(dbh!=null)
            dbh.close();
    }



    public int colorselctor(int i){
        int mod = i%5;
        switch (mod){
            case 0:return R.drawable.icon_null_red;
            case 1:return R.drawable.icon_null_orange;
            case 2:return R.drawable.icon_null_yellow;
            case 3:return R.drawable.icon_null_green;
            case 4:return R.drawable.icon_null_blue;
            default:return R.drawable.icon_null;
        }
    }
}