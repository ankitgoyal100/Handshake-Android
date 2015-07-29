package com.handshake.Handshake;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.handshake.helpers.FeedItemServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.listview.FeedAdapter;
import com.handshake.listview.SuggestionAdapter;
import com.handshake.models.FeedItem;
import com.handshake.models.Suggestion;

import io.realm.Realm;
import io.realm.RealmResults;


public class FeedFragment extends ListFragment {
    private SwipeRefreshLayout swipeContainer;

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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        swipeContainer.setColorSchemeResources(R.color.orange);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                FeedItemServerSync.performSync(getActivity(), new SyncCompleted() {
                    @Override
                    public void syncCompletedListener() {
                        swipeContainer.setRefreshing(false);
                    }
                });
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Realm realm = Realm.getInstance(getActivity());
        RealmResults<FeedItem> feedItems = realm.where(FeedItem.class).findAll();
        feedItems.sort("updatedAt", false);
        FeedAdapter feedAdapter = new FeedAdapter(getActivity(), feedItems, true);
        setListAdapter(feedAdapter);

        RealmResults<Suggestion> suggestionItems = realm.where(Suggestion.class).findAll();
        SuggestionAdapter suggestionAdapter = new SuggestionAdapter(getActivity(), suggestionItems, true);
        ListView suggestionListView = (ListView) getView().findViewById(R.id.listView2);
        suggestionListView.setAdapter(suggestionAdapter);

        Utils.setDynamicHeight(getListView());
        Utils.setDynamicHeight(suggestionListView);
    }

    @Override
    public void onResume() {
        super.onResume();
        swipeContainer.setRefreshing(false);
    }
}