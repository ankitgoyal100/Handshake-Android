package com.handshake.helpers;

import com.handshake.models.User;

import java.util.ArrayList;

/**
 * Created by ankitgoyal on 6/14/15.
 */
public interface UserSyncCompleted {
    void syncCompletedListener(ArrayList<User> users);
}