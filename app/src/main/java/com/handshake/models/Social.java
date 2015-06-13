package com.handshake.models;

import io.realm.RealmObject;

/**
 * Created by ankitgoyal on 6/13/15.
 */
public class Social extends RealmObject {
    private String network;
    private String username;

    private Card card;

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }
}
