package com.handshake.Handshake;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.handshake.helpers.GroupServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.listview.GroupAdapter;
import com.handshake.models.Group;
import com.handshake.views.ButtonCustomFont;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/23/15.
 */
public class GroupFragment extends Fragment {
    private SwipeRefreshLayout swipeContainer;
    private Realm realm;
    private LinearLayout introView;

    public GroupFragment() {
        // Required empty public constructor
    }

    public static GroupFragment newInstance() {
        GroupFragment fragment = new GroupFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_group, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        swipeContainer.setColorSchemeResources(R.color.orange);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                GroupServerSync.performSync(getActivity(), new SyncCompleted() {
                    @Override
                    public void syncCompletedListener() {
                        swipeContainer.setRefreshing(false);
                        setIntroVisible();
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
        RealmResults<Group> groups = realm.where(Group.class).notEqualTo("syncStatus", Utils.GroupDeleted).findAll();
        groups.sort("createdAt", false);
        GroupAdapter adapter = new GroupAdapter(getActivity(), groups, true);

        final GridView gridView = (GridView) getView().findViewById(R.id.grid);
        gridView.setAdapter(adapter);

        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition =
                        (gridView == null || gridView.getChildCount() == 0) ?
                                0 : gridView.getChildAt(0).getTop();
                swipeContainer.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
            }
        });

        introView = (LinearLayout) getView().findViewById(R.id.intro_layout);
        setIntroVisible();

        ButtonCustomFont getStarted = (ButtonCustomFont) getView().findViewById(R.id.get_started);
        getStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), CreateEditGroupActivity.class);
                getActivity().startActivity(i);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        swipeContainer.setRefreshing(false);
        setIntroVisible();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (realm != null)
            realm.close();
    }

    public void setIntroVisible() {
        try {
            final Realm r = Realm.getInstance(getActivity());
            if (r.where(Group.class).notEqualTo("syncStatus", Utils.GroupDeleted).findAll().size() > 0) {
                introView.setVisibility(View.GONE);
            } else {
                introView.setVisibility(View.VISIBLE);
            }
            r.close();
        } catch(NullPointerException e) {
            e.printStackTrace();
        }
    }
}

