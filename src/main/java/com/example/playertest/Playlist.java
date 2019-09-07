package com.example.playertest;

import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.Random;


public class Playlist {
    private int length;
    private int currentindex;
    public String listname;
    public ArrayList<Song> content;
    public SimpleAdapter adapter;
    Playlist(){
        this.content = new ArrayList<Song>();
        this.length = 0;
        this.currentindex = 0;
        this.listname = "默认列表--所有歌曲";
    }
    public void AddToList(Song song){
        this.length++;
        this.content.add(song);
    }
    public int getCurrentindex(){
        return currentindex;
    }
    public boolean isEmpty(){
        return this.length == 0 ? true:false;
    }
    public int GetLength(){
        return this.length;
    }
    public Song PlayByIndex(int in){
        if(in>=0 && in<=length-1){
            currentindex = in;
            return content.get(in);
        }
        else{
            return null;
        }
    }

    public Song NextMusic(int status){
        if(status == 2 || status == 1) {
            if (this.currentindex >= length - 1) {
                currentindex = 0;
            } else {
                this.currentindex++;
            }
        }
        else if(status == 0){
            Random ran = new Random();
            currentindex = ran.nextInt(length-1);
        }
        return content.get(currentindex);
    }

    public Song PrevMusic(int status){
        if(status == 2 || status == 1) {
            if (this.currentindex == 0) {
                currentindex = this.length - 1;
            } else {
                this.currentindex--;
            }
        }
        else if(status == 0){
            Random ran = new Random();
            currentindex = ran.nextInt(length-1);
        }
        return content.get(currentindex);
    }


}
