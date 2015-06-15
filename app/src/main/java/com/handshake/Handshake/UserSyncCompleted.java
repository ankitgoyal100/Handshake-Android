package com.handshake.Handshake;

import com.handshake.models.User;

import java.util.ArrayList;

/**
 * Created by ankitgoyal on 6/14/15.
 */
interface UserSyncCompleted {
    void syncCompletedListener(ArrayList<User> users);
}