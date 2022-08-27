package com.akapps.randomcaller.Models;

public class User {
    private String uid, name, photo, city;
    private int  coin;


    public User() {

    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getCoin() {
        return coin;
    }

    public void setCoin(int coin) {
        this.coin = coin;
    }

    public User(String uid, String name, String photo, String city, int coin) {
        this.uid = uid;
        this.name = name;
        this.photo = photo;
        this.city = city;
        this.coin = coin;
    }

}