package com.handshake.Handshake;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.handshake.listview.FeedAdapter;
import com.handshake.models.FeedItem;

import io.realm.Realm;
import io.realm.RealmResults;


public class FeedFragment extends ListFragment {
    public static FeedFragment newInstance() {
        FeedFragment fragment = new FeedFragment();
        return fragment;
    }

    public FeedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Realm realm = Realm.getInstance(getActivity());
        RealmResults<FeedItem> feedItems = realm.where(FeedItem.class).findAll();
        feedItems.sort("updatedAt", false);
        FeedAdapter adapter = new FeedAdapter(getActivity(), feedItems, true);
        setListAdapter(adapter);
    }

}