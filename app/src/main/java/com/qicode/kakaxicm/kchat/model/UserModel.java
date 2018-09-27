package com.qicode.kakaxicm.kchat.model;

/**
 * Created by chenming on 2018/9/22
 */
public class UserModel {
    private String name;
    private boolean isOnline;

    public void setName(String name) {
        this.name = name;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getName() {

        return name;
    }

    public boolean isOnline() {
        return isOnline;
    }
}
