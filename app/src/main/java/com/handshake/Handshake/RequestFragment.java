package com.handshake.Handshake;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.handshake.helpers.RequestServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.listview.ContactAdapter;
import com.handshake.listview.SuggestionAdapter;
import com.handshake.models.Suggestion;
import com.handshake.models.User;
import com.handshake.views.ButtonCustomFont;
import com.handshake.views.TextViewCustomFont;

import io.realm.Realm;
import io.realm.RealmResults;


public class RequestFragment extends ListFragment {
    Handler handler = new Handler();
    private SwipeRefreshLayout swipeContainer;
    private Realm realm;
    private TextViewCustomFont suggestionText;
    private LinearLayout introView;
    private ListView suggestionListView;

    public RequestFragment() {
        // Required empty public constructor
    }

    public static RequestFragment newInstance() {
        RequestFragment fragment = new RequestFragment();
        return fragment;
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
                        setIntroVisible();
                        setSuggestionText();
                    }
                });
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SessionManager session = new SessionManager(getActivity());
        if (!session.isLoggedIn()) return;

        realm = Realm.getInstance(getActivity());
        RealmResults<User> users = realm.where(User.class).equalTo("requestReceived", true).findAll();
        introView = (LinearLayout) getView().findViewById(R.id.intro_layout);
        users.sort("createdAt", false);
        ContactAdapter contactAdapter = new ContactAdapter(getActivity(), users, true);
        setListAdapter(contactAdapter);

        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition =
                        (getListView() == null || getListView().getChildCount() == 0) ?
                                0 : getListView().getChildAt(0).getTop();
                swipeContainer.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
            }
        });

        final RealmResults<Suggestion> suggestionItems = realm.where(Suggestion.class).findAll();
        suggestionText = (TextViewCustomFont) getView().findViewById(R.id.suggestion_text);
        SuggestionAdapter suggestionAdapter = new SuggestionAdapter(getActivity(), suggestionItems, true);
        suggestionListView = (ListView) getView().findViewById(R.id.listView2);
        suggestionListView.setAdapter(suggestionAdapter);

        setIntroVisible();
        setSuggestionText();

        Utils.setDynamicHeight(getListView());
        Utils.setDynamicHeight(suggestionListView);

        ButtonCustomFont getStarted = (ButtonCustomFont) getView().findViewById(R.id.get_started);
        getStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).selectSearchView();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        swipeContainer.setRefreshing(false);
//        setSuggestionText();
//        setIntroVisible();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (realm != null)
            realm.close();
    }

    public void setSuggestionText() {
        if(getListView() == null || suggestionListView == null) return;

        Utils.setDynamicHeight(getListView());
        Utils.setDynamicHeight(suggestionListView);

        final Realm r = Realm.getInstance(getActivity());
        if (r.where(Suggestion.class).findAll().size() > 0) {
            suggestionText.setVisibility(View.VISIBLE);
        } else {
            suggestionText.setVisibility(View.GONE);
        }
        r.close();
    }

    public void setIntroVisible() {
        if(getListView() == null || suggestionListView == null) return;

        Utils.setDynamicHeight(getListView());
        Utils.setDynamicHeight(suggestionListView);

        final Realm r = Realm.getInstance(getActivity());
        if (r.where(User.class).equalTo("requestReceived", true).findAll().size() > 0) {
            introView.setVisibility(View.GONE);
        } else {
            introView.setVisibility(View.VISIBLE);
        }
        r.close();
    }
}
