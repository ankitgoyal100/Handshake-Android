package com.handshake.models;

import io.realm.RealmObject;

/**
 * Created by ankitgoyal on 6/13/15.
 */
public class GroupMember extends RealmObject {
    private Group group;
    private User user;

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
