package com.emo_tunes.javafxapp;

public class SongInfo {

    private String songID;
    private String songName;
    private String artistName;
    private String releaseDate;
    private String coverURL;
    private Integer duration;
    private String songURL;

    public SongInfo() {} // default constructor required by Jackson

    // Getters and setters
    public String getSongID() { return songID; }
    public void setSongID(String songID) { this.songID = songID; }

    public String getSongName() { return songName; }
    public void setSongName(String songName) { this.songName = songName; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public String getCoverURL() { return coverURL; }
    public void setCoverURL(String coverURL) { this.coverURL = coverURL; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getSongURL() { return songURL; }
    public void setSongURL(String songURL) { this.songURL = songURL; }
}

