package com.handshake.models;

import com.handshake.Handshake.Utils;
import com.loopj.android.http.RequestParams;

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

    private RealmList<Address> addresses = new RealmList<>();
    private RealmList<Email> emails = new RealmList<>();
    private RealmList<Phone> phones = new RealmList<>();
    private RealmList<Social> socials = new RealmList<>();
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

    public RealmList<Email> getEmails() {
        return emails;
    }

    public void setEmails(RealmList<Email> emails) {
        this.emails = emails;
    }

    public RealmList<Phone> getPhones() {
        return phones;
    }

    public void setPhones(RealmList<Phone> phones) {
        this.phones = phones;
    }

    public RealmList<Social> getSocials() {
        return socials;
    }

    public void setSocials(RealmList<Social> socials) {
        this.socials = socials;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public static Card updateCard(Card card, Realm realm, JSONObject json) {
        try {
            card.setCardId(json.getInt("id"));
            card.setCreatedAt(Utils.formatDate(json.getString("created_at")));
            card.setUpdatedAt(Utils.formatDate(json.getString("updated_at")));
            card.setName(json.getString("name"));

            card.setPhones(new RealmList<Phone>());
            JSONArray phones = json.getJSONArray("phones");
            for (int i = 0; i < phones.length(); i++) {
                Phone phone = realm.createObject(Phone.class);
                phone.setNumber(phones.getJSONObject(i).getString("number"));
                phone.setCountryCode(phones.getJSONObject(i).getString("country_code"));
                phone.setLabel(phones.getJSONObject(i).getString("label"));

                RealmList<Phone> cardPhones = card.getPhones();
                cardPhones.add(phone);
                card.setPhones(cardPhones);
            }

            card.setEmails(new RealmList<Email>());
            JSONArray emails = json.getJSONArray("emails");
            for (int i = 0; i < emails.length(); i++) {
                Email email = realm.createObject(Email.class);
                email.setAddress(emails.getJSONObject(i).getString("address"));
                email.setLabel(emails.getJSONObject(i).getString("label"));

                RealmList<Email> cardEmails = card.getEmails();
                cardEmails.add(email);
                card.setEmails(cardEmails);
            }

            card.setAddresses(new RealmList<Address>());
            JSONArray addresses = json.getJSONArray("addresses");
            for (int i = 0; i < addresses.length(); i++) {
                Address address = realm.createObject(Address.class);
                address.setStreet1(addresses.getJSONObject(i).getString("street1"));
                address.setStreet2(addresses.getJSONObject(i).getString("street2"));
                address.setCity(addresses.getJSONObject(i).getString("city"));
                address.setState(addresses.getJSONObject(i).getString("state"));
                address.setZip(addresses.getJSONObject(i).getString("zip"));
                address.setLabel(addresses.getJSONObject(i).getString("label"));

                RealmList<Address> cardAddresses = card.getAddresses();
                cardAddresses.add(address);
                card.setAddresses(cardAddresses);
            }

            card.setSocials(new RealmList<Social>());
            JSONArray socials = json.getJSONArray("socials");
            for (int i = 0; i < socials.length(); i++) {
                Social social = realm.createObject(Social.class);
                social.setUsername(socials.getJSONObject(i).getString("username"));
                social.setNetwork(socials.getJSONObject(i).getString("network"));

                RealmList<Social> cardSocials = card.getSocials();
                cardSocials.add(social);
                card.setSocials(cardSocials);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return card;
    }

    public static RequestParams cardToParams(Card card) {
        RequestParams params = new RequestParams();
        if (!card.getName().isEmpty()) params.put("name", card.getName());

        JSONArray phones = new JSONArray();
        for (Phone phone : card.getPhones()) {
            if (phone.getNumber().length() > 0 && phone.getLabel().length() > 0) {
                JSONObject phoneJSON = new JSONObject();
                try {
                    phoneJSON.put("number", phone.getNumber());
                    phoneJSON.put("label", phone.getLabel());
                    phoneJSON.put("country_code", phone.getCountryCode());
                    phones.put(phoneJSON);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        params.put("phones_attributes", phones);

        JSONArray emails = new JSONArray();
        for (Email email : card.getEmails()) {
            if (email.getAddress().length() > 0 && email.getLabel().length() > 0) {
                JSONObject emailJSON = new JSONObject();
                try {
                    emailJSON.put("address", email.getAddress());
                    emailJSON.put("label", email.getLabel());
                    emails.put(emailJSON);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        params.put("emails_attributes", emails);

        JSONArray addresses = new JSONArray();
        for (Address address : card.getAddresses()) {
            if ((address.getStreet1().length() > 0 || address.getStreet2().length() > 0 ||
                    address.getCity().length() > 0 || address.getState().length() > 0 ||
                    address.getZip().length() > 0) && address.getLabel().length() > 0) {
                JSONObject addressJSON = new JSONObject();
                try {
                    if (address.getStreet1().length() > 0)
                        addressJSON.put("street1", address.getStreet1());
                    if (address.getStreet2().length() > 0)
                        addressJSON.put("street2", address.getStreet2());
                    if (address.getCity().length() > 0) addressJSON.put("city", address.getCity());
                    if (address.getState().length() > 0)
                        addressJSON.put("state", address.getState());
                    if (address.getZip().length() > 0) addressJSON.put("zip", address.getZip());
                    addressJSON.put("label", address.getLabel());
                    addresses.put(addressJSON);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        params.put("addresses_attributes", addresses);

        JSONArray socials = new JSONArray();
        for(Social social : card.getSocials()) {
            if(social.getUsername().length() > 0 && social.getNetwork().length() > 0) {
                JSONObject socialJSON = new JSONObject();

                try {
                    socialJSON.put("username", social.getUsername());
                    socialJSON.put("network", social.getNetwork());
                    socials.put(socialJSON);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        params.put("socials_attributes", socials);

        return params;
    }
}
