package com.handshake.models;

import com.handshake.Handshake.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/13/15.
 */
public class User extends RealmObject {
    private int contacts;
    private Date contactUpdated;
    private Date createdAt;
    private String firstName;
    private boolean isContact;
    private String lastName;
    private int mutual;
    private String picture;
    private byte[] pictureData;
    private boolean requestReceived;
    private boolean requestSent;
    private boolean saved;
    private boolean savesToPhone;
    private short syncStatus;
    private String thumb;
    private byte[] thumbData;
    private Date updatedAt;
    private long userId;

    private RealmList<Card> cards;
    private RealmList<FeedItem> feedItems;
    private RealmList<GroupMember> groups;

    public int getContacts() {
        return contacts;
    }

    public void setContacts(int contacts) {
        this.contacts = contacts;
    }

    public Date getContactUpdated() {
        return contactUpdated;
    }

    public void setContactUpdated(Date contactUpdated) {
        this.contactUpdated = contactUpdated;
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

    public boolean isContact() {
        return isContact;
    }

    public void setIsContact(boolean isContact) {
        this.isContact = isContact;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getMutual() {
        return mutual;
    }

    public void setMutual(int mutual) {
        this.mutual = mutual;
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

    public boolean isRequestReceived() {
        return requestReceived;
    }

    public void setRequestReceived(boolean requestReceived) {
        this.requestReceived = requestReceived;
    }

    public boolean isRequestSent() {
        return requestSent;
    }

    public void setRequestSent(boolean requestSent) {
        this.requestSent = requestSent;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public boolean isSavesToPhone() {
        return savesToPhone;
    }

    public void setSavesToPhone(boolean savesToPhone) {
        this.savesToPhone = savesToPhone;
    }

    public short getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(short syncStatus) {
        this.syncStatus = syncStatus;
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

    public void addCard(Card card) {
        this.cards.add(card);
    }

    public RealmList<FeedItem> getFeedItems() {
        return feedItems;
    }

    public void setFeedItems(RealmList<FeedItem> feedItems) {
        this.feedItems = feedItems;
    }

    public RealmList<GroupMember> getGroups() {
        return groups;
    }

    public void setGroups(RealmList<GroupMember> groups) {
        this.groups = groups;
    }

    public void updateContact(Realm realm, JSONObject json) {
        try {
            this.userId = json.getInt("id");
            this.createdAt = Utils.formatDate(json.getString("created_at"));
            this.updatedAt = Utils.formatDate(json.getString("updated_at"));

            this.firstName = json.getString("first_name");
            this.lastName = json.getString("last_name");

            this.isContact = json.getBoolean("is_contact");
            this.requestSent = json.getBoolean("request_sent");
            this.requestReceived = json.getBoolean("request_received");

            // if no thumb or thumb is different - update
            if (json.isNull("thumb") || !this.thumb.equals("") ||
                    !json.getString("thumb").equals(this.thumb)) {
                this.thumb = json.getString("thumb");
                this.thumbData = null;
            }

            // if no picture or picture is different - update
            if (json.isNull("picture") || !this.picture.equals("") ||
                    !json.getString("picture").equals(this.picture)) {
                this.picture = json.getString("picture");
                this.pictureData = null;
            }

            this.contacts = json.getInt("contacts");
            this.mutual = json.getInt("mutual");

            JSONArray cards = json.getJSONArray("cards");
            for(int i = 0; i < cards.length(); i++) {
                RealmResults<Card> result = realm.where(Card.class).equalTo("cardId", cards.getJSONObject(i).getInt("id")).findAll();
                if(result.size() > 0) {
                    Card card = result.get(0);
                    card.updateCard(realm, cards.getJSONObject(i));
                    card.setUser(this);
                } else {
                    Card card = realm.createObject(Card.class);
                    card.updateCard(realm, cards.getJSONObject(i));
                    this.addCard(card);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
