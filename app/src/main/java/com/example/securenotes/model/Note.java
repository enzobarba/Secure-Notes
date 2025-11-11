package com.example.securenotes.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity (tableName = "notes")
public class Note {

    @PrimaryKey (autoGenerate = true)
    public int id;
    public String title;
    public String content;
    public long timestamp;
    public int color;

    public Note (String title, String content, long timestamp, int color) {

        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.color = color;

    }
}
