package com.handshake.Handshake;


import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.handshake.helpers.RequestServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.listview.ContactAdapter;
import com.handshake.models.User;
import com.handshake.views.TextViewCustomFont;

import io.realm.Realm;
import io.realm.RealmResults;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestFragment extends ListFragment {
    private SwipeRefreshLayout swipeContainer;

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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        swipeContainer.setColorSchemeResources(R.color.orange);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                RequestServerSync.performSync(getActivity(), new SyncCompleted() {
                    @Override
                    public void syncCompletedListener() {
                        swipeContainer.setRefreshing(false);
                    }
                });
            }
        });

        View empty = getActivity().getLayoutInflater().inflate(R.layout.empty_list_view, null, false);
        TextViewCustomFont text = (TextViewCustomFont) empty.findViewById(R.id.empty_list_item);
        text.setText("No requests");
        ((ViewGroup) getListView().getParent().getParent()).addView(empty);
        getListView().setEmptyView(empty);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Realm realm = Realm.getInstance(getActivity());
        RealmResults<User> users = realm.where(User.class).equalTo("requestReceived", true).findAll();
        users.sort("createdAt", false);
        ContactAdapter adapter = new ContactAdapter(getActivity(), users, true);
        setListAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        swipeContainer.setRefreshing(false);
    }
}
