package com.handshake.helpers;

import com.handshake.models.User;

/**
 * Created by ankitgoyal on 6/16/15.
 */
public interface UserSyncCompleted {
    void syncCompletedListener(User users);
    void syncFailedListener();
}
