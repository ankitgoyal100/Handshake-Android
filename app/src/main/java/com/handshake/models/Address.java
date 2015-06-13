package com.handshake.models;

import io.realm.RealmObject;

/**
 * Created by ankitgoyal on 6/12/15.
 */
public class Address extends RealmObject {
    private String city;
    private String label;
    private String state;
    private String street1;
    private String street2;
    private String zip;
}
