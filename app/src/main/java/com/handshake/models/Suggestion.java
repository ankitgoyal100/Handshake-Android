package com.handshake.models;

import io.realm.RealmObject;

/**
 * Created by ankitgoyal on 6/17/15.
 */
public class Suggestion extends RealmObject {
    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
