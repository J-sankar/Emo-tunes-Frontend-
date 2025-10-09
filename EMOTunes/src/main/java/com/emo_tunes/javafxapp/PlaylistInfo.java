package com.emo_tunes.javafxapp;

import java.util.List;

public class PlaylistInfo {
    private Integer playlistId;
    private String playlistName;
    private String coverUrl;
    private List<SongInfo> songs;
    private String emotion;

    public PlaylistInfo() {} // no-arg constructor

    public PlaylistInfo(Integer playlistId, String playlistName) {
        this.playlistId = playlistId;
        this.playlistName = playlistName;
    }

    // getters and setters
    public Integer getPlaylistId() { return playlistId; }
    public void setPlaylistId(Integer playlistId) { this.playlistId = playlistId; }

    public String getPlaylistName() { return playlistName; }
    public void setPlaylistName(String playlistName) { this.playlistName = playlistName; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public List<SongInfo> getSongs() { return songs; }
    public void setSongs(List<SongInfo> songs) { this.songs = songs; }

    public void setEmotion(String emotion) { this.emotion = emotion;

    }
    public String getEmotion() { return emotion; }
    // **Important:** Override toString() for proper display in ChoiceDialog
    @Override
    public String toString() {
        return playlistName != null ? playlistName : "Unnamed Playlist";
    }
}
