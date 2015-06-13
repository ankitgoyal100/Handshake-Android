package com.handshake.models;

import io.realm.RealmObject;

/**
 * Created by ankitgoyal on 6/13/15.
 */
public class Email extends RealmObject {
    private String address;
    private String label;

    private Card card;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }
}
