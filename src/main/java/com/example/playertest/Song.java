package com.example.playertest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;


public class Song {

    public String path;
    public String[] name;
    public Bitmap albempic;

    Song(){

    }

    Song(String path, String[] name){
        this.path = path;
        this.name = name;
        this.albempic = createAlbumArt(path);
    }

    public static Bitmap createAlbumArt(final String filePath) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            byte[] embedPic = retriever.getEmbeddedPicture();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            bitmap = BitmapFactory.decodeByteArray(embedPic, 0, embedPic.length);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return bitmap;
    }
}
