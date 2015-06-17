package com.handshake.helpers;

import com.handshake.models.Group;

import java.util.ArrayList;

/**
 * Created by ankitgoyal on 6/16/15.
 */
public interface GroupArraySyncCompleted {
    void syncCompletedListener(ArrayList<Group> groups);
}
