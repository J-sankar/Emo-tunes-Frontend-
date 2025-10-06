package com.emo_tunes.javafxapp;

import java.util.List;

public class PlaylistInfo {
            String playlistId;
            String playlistName;
            String coverUrl;
            List<SongInfo> songs;
    public PlaylistInfo(){ }                           // no‑arg ctor for Jackson

    public String getPlaylistId()          { return playlistId; }
    public void setPlaylistId(String i)   { this.playlistId = i; }

    public String getPlaylistName()        { return playlistName; }
    public void setPlaylistName(String n){ this.playlistName = n; }

    public String getCoverUrl()            { return coverUrl; }
    public void setCoverUrl(String u)     { this.coverUrl = u; }

    public List<SongInfo> getSongs()        { return songs; }
    public void setSongs(List<SongInfo> s){ this.songs = s; }
}
