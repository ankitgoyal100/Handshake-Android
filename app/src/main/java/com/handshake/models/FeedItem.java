package com.handshake.models;

import com.handshake.Handshake.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmObject;

/**
 * Created by ankitgoyal on 6/13/15.
 */
public class FeedItem extends RealmObject {
    private Date createdAt;
    private long feedId;
    private String itemType;
    private Date updatedAt;

    private Group group;
    private User user;

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

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

    public static FeedItem updateFeedItem(FeedItem feedItem, Realm realm, JSONObject json) {
        try {
            feedItem.setFeedId(json.getLong("id"));
            feedItem.setCreatedAt(Utils.formatDate(json.getString("created_at")));
            feedItem.setUpdatedAt(Utils.formatDate(json.getString("updated_at")));
            feedItem.setItemType(json.getString("item_type"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return feedItem;
    }

}
