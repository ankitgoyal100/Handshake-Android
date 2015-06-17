package com.handshake.models;

import com.handshake.Handshake.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by ankitgoyal on 6/13/15.
 */
public class Group extends RealmObject {
    private String code;
    private Date createdAt;
    private long groupId;
    private String name;
    private short syncStatus;
    private Date updatedAt;

    private RealmList<FeedItem> feedItems = new RealmList<>();
    private RealmList<GroupMember> members = new RealmList<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public short getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(short syncStatus) {
        this.syncStatus = syncStatus;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public RealmList<FeedItem> getFeedItems() {
        return feedItems;
    }

    public void setFeedItems(RealmList<FeedItem> feedItems) {
        this.feedItems = feedItems;
    }

    public RealmList<GroupMember> getMembers() {
        return members;
    }

    public void setMembers(RealmList<GroupMember> members) {
        this.members = members;
    }

    public static Group updateGroup(Group group, Realm realm, JSONObject json) {
        try {
            group.setGroupId(json.getLong("id"));
            group.setCreatedAt(Utils.formatDate(json.getString("created_at")));
            group.setUpdatedAt(Utils.formatDate(json.getString("updated_at")));
            group.setName(json.getString("name"));
            group.setCode(json.getString("code"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return group;
    }
}
