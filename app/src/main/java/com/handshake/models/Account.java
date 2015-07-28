package com.handshake.models;

import com.handshake.Handshake.Utils;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by ankitgoyal on 6/15/15.
 */
public class Account extends RealmObject {
    private Date createdAt;
    private String firstName;
    private String lastName;
    private String email;
    private String picture;
    private byte[] pictureData;
    private String thumb;
    private byte[] thumbData;
    private Date updatedAt;
    private short syncStatus;
    private long userId;

    private RealmList<Card> cards = new RealmList<>();

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public byte[] getPictureData() {
        return pictureData;
    }

    public void setPictureData(byte[] pictureData) {
        this.pictureData = pictureData;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public byte[] getThumbData() {
        return thumbData;
    }

    public void setThumbData(byte[] thumbData) {
        this.thumbData = thumbData;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public short getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(short syncStatus) {
        this.syncStatus = syncStatus;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public RealmList<Card> getCards() {
        return cards;
    }

    public void setCards(RealmList<Card> cards) {
        this.cards = cards;
    }

    public static Account updateAccount(Account account, Realm realm, JSONObject json) {
        try {
            account.setUserId(json.getLong("id"));
            account.setCreatedAt(Utils.formatDate(json.getString("created_at")));
            account.setUpdatedAt(Utils.formatDate(json.getString("updated_at")));
            account.setEmail(json.getString("email"));
            account.setFirstName(json.getString("first_name"));
            account.setLastName(json.getString("last_name"));

            // if no thumb or thumb is different - update
            if (json.isNull("thumb") || (account.getThumb() != null && (!account.getThumb().equals("") ||
                    !json.getString("thumb").equals(account.getThumb())))) {
                account.setThumb(json.getString("thumb"));
                account.setThumbData(new byte[0]);
            }

            // if no picture or picture is different - update
            if (json.isNull("picture") || (account.getPicture() != null && (!account.getPicture().equals("") ||
                    !json.getString("picture").equals(account.getPicture())))) {
                account.setPicture(json.getString("picture"));
                account.setPictureData(new byte[0]);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return account;
    }

    public static RequestParams accountToParams(Account account) {
        RequestParams params = new RequestParams();
        params.put("email", account.getEmail());
        if (account.getFirstName().length() > 0) params.put("first_name", account.getFirstName());
        if (account.getLastName().length() > 0 && !account.getLastName().equals("null"))
            params.put("last_name", account.getLastName());

        return params;
    }
}
