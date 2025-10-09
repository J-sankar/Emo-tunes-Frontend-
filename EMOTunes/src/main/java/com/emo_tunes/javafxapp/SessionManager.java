package com.emo_tunes.javafxapp;

public class SessionManager {

    private static SessionManager instance;
    private UserInfo currentUser;

    private SessionManager() { }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setUser(UserInfo user) {
        this.currentUser = user;
    }

    public UserInfo getUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
