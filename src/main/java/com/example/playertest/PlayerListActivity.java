package com.example.playertest;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlayerListActivity extends AppCompatActivity {
    ListView lv;
    dataBaseBuilder dbh;
    SQLiteDatabase db;
    String[] Songnames;
    ArrayList<Map<String, Object>> playlists;
    boolean [] selected;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_list);
        lv = findViewById(R.id.playlists);
        dbh = new dataBaseBuilder(this, "music.db3", null,1);
        db = dbh.getReadableDatabase();
        setListView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_button, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.addnewlist){
            final EditText et = new EditText(this);
            new AlertDialog.Builder(this).setTitle("请输入播放列表名")
                    .setView(et)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dataBaseBuilder.instertList(db, et.getText().toString());
                            setListView();
                    }
                    }).setNegativeButton("取消",null).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getSongNames(){
        Cursor c = db.rawQuery("SELECT song_name, song_artist FROM music order by song_artist", null);
        Songnames = new String[c.getCount()];
        selected = new boolean[c.getCount()];
        int i = 0;
        while(c.moveToNext()){
            String temp = new String();
            temp = c.getString(0) + " - " + c.getString(1);
            Songnames[i] = temp;
            selected[i++] = false;
        }
    }
    private void setListItems(){
        Cursor c = db.rawQuery("SELECT * FROM playlists", null);
        playlists = new ArrayList<Map<String, Object>>();
        int i = 1;
        while(c.moveToNext()){
            Map<String, Object> map = new HashMap<String, Object>();
            String str1 = (i++) + "";
            String str2 = c.getString(1);
            map.put("list_number", str1);
            map.put("list_name", str2);
            playlists.add(map);
        }
        ListAdapter listAdapter = new SimpleAdapter(this, playlists, R.layout.array_item_list,
                new String[]{"list_number","list_name"},
                new int[]{R.id.list_number, R.id.list_name});
        lv.setAdapter(listAdapter);
    }

    private void setListView(){
        setListItems();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(PlayerListActivity.this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("list_num",dataBaseBuilder.getListNum(db, (String)playlists.get(position).get("list_name")));
                intent.putExtras(bundle);
                setResult(0x11, intent);
                finish();
            }

        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new android.app.AlertDialog.Builder(PlayerListActivity.this).setTitle("请选择对列表进行的操作")
                        .setMessage("添加歌曲或删除列表")
                        .setPositiveButton("添加歌曲", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                AlertDialog.Builder builder = new AlertDialog.Builder(PlayerListActivity.this);
                                builder.setTitle("选择歌曲添加");
                                getSongNames();
                                builder.setMultiChoiceItems(Songnames, selected,
                                        new DialogInterface.OnMultiChoiceClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                            }
                                        });
                                builder.setPositiveButton("添加", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        for (int i = 0; i < Songnames.length; i++) {
                                            if (selected[i]) {
                                                dataBaseBuilder.insertBelongTo(db, ""+(i+1), (String)playlists.get(position).get("list_name"));
                                            }
                                        }
                                        dialog.dismiss();
                                    }
                                });
                                builder.show();

                            }
                        }).setNegativeButton("删除列表", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dataBaseBuilder.deleteList(db, (String)playlists.get(position).get("list_name"));
                        playlists.remove(position);
                        setListItems();
                    }
                }).show();

                return true;
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(dbh!=null)
            dbh.close();
    }
}
