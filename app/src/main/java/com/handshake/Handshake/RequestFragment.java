package com.handshake.Handshake;


import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.handshake.listview.RequestAdapter;
import com.handshake.models.User;

import io.realm.Realm;
import io.realm.RealmResults;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestFragment extends ListFragment {
    public static RequestFragment newInstance() {
        RequestFragment fragment = new RequestFragment();
        return fragment;
    }

    public RequestFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_request, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Realm realm = Realm.getInstance(getActivity());
        RealmResults<User> users = realm.where(User.class).equalTo("requestReceived", true).findAll();
        users.sort("createdAt", false);
        RequestAdapter adapter = new RequestAdapter(getActivity(), users, true);
        setListAdapter(adapter);
    }

}
