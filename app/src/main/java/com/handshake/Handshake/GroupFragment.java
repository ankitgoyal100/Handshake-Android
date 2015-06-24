package com.handshake.Handshake;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.handshake.listview.GroupAdapter;
import com.handshake.models.Group;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/23/15.
 */
public class GroupFragment  extends Fragment {
    public static GroupFragment newInstance() {
        GroupFragment fragment = new GroupFragment();
        return fragment;
    }

    public GroupFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_group, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Realm realm = Realm.getInstance(getActivity());
        RealmResults<Group> groups = realm.where(Group.class).notEqualTo("syncStatus", Utils.GroupDeleted).findAll();
        groups.sort("createdAt", false);
        GroupAdapter adapter = new GroupAdapter(getActivity(), groups, true);

        GridView gridView = (GridView) getView().findViewById(R.id.grid);
        gridView.setAdapter(adapter);
    }

}

