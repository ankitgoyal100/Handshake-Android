package com.handshake.models;

import io.realm.RealmObject;

/**
 * Created by ankitgoyal on 6/13/15.
 */
public class Phone extends RealmObject {
    private String label;
    private String number;
    private String countryCode;

    private Card card;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
