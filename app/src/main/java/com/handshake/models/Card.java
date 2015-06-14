package com.handshake.models;

import com.handshake.Handshake.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by ankitgoyal on 6/12/15.
 */
public class Card extends RealmObject {
    private long cardId;
    private Date createdAt;
    private String name;
    private short syncStatus;
    private Date updatedAt;

    private RealmList<Address> addresses;
    private RealmList<Email> emails;
    private RealmList<Phone> phones;
    private RealmList<Social> socials;
    private User user;

    public long getCardId() {
        return cardId;
    }

    public void setCardId(long cardId) {
        this.cardId = cardId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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

    public RealmList<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(RealmList<Address> addresses) {
        this.addresses = addresses;
    }

    public void addAddress(Address address) {
        this.addresses.add(address);
    }

    public RealmList<Email> getEmails() {
        return emails;
    }

    public void setEmails(RealmList<Email> emails) {
        this.emails = emails;
    }

    public void addEmail(Email email) {
        this.emails.add(email);
    }

    public RealmList<Phone> getPhones() {
        return phones;
    }

    public void setPhones(RealmList<Phone> phones) {
        this.phones = phones;
    }

    public void addPhone(Phone phone) {
        this.phones.add(phone);
    }

    public RealmList<Social> getSocials() {
        return socials;
    }

    public void setSocials(RealmList<Social> socials) {
        this.socials = socials;
    }

    public void addSocial(Social social) {
        this.socials.add(social);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void updateCard(Realm realm, JSONObject json) {
        try {
            this.setCardId(json.getInt("id"));
            this.setCreatedAt(Utils.formatDate(json.getString("createdAt")));
            this.setUpdatedAt(Utils.formatDate(json.getString("updatedAt")));
            this.name = json.getString("name");

            this.phones.clear();
            JSONArray phones = json.getJSONArray("phones");
            for(int i = 0; i < phones.length(); i++) {
                Phone phone = realm.createObject(Phone.class);
                phone.setNumber(phones.getJSONObject(i).getString("number"));
                phone.setLabel(phones.getJSONObject(i).getString("label"));

                this.addPhone(phone);
            }

            this.emails.clear();
            JSONArray emails = json.getJSONArray("emails");
            for(int i = 0; i < emails.length(); i++) {
                Email email = realm.createObject(Email.class);
                email.setAddress(emails.getJSONObject(i).getString("address"));
                email.setLabel(emails.getJSONObject(i).getString("label"));

                this.addEmail(email);
            }

            this.addresses.clear();
            JSONArray addresses = json.getJSONArray("addresses");
            for(int i = 0; i < addresses.length(); i++) {
                Address address = realm.createObject(Address.class);
                address.setStreet1(addresses.getJSONObject(i).getString("street1"));
                address.setStreet2(addresses.getJSONObject(i).getString("street2"));
                address.setCity(addresses.getJSONObject(i).getString("city"));
                address.setState(addresses.getJSONObject(i).getString("state"));
                address.setZip(addresses.getJSONObject(i).getString("zip"));
                address.setLabel(addresses.getJSONObject(i).getString("label"));

                this.addAddress(address);
            }

            this.socials.clear();
            JSONArray socials = json.getJSONArray("socials");
            for(int i = 0; i < socials.length(); i++) {
                Social social = realm.createObject(Social.class);
                social.setUsername(socials.getJSONObject(i).getString("username"));
                social.setNetwork(socials.getJSONObject(i).getString("network"));

                this.addSocial(social);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
