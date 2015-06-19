package com.handshake.Handshake;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.handshake.models.User;

import io.realm.Realm;
import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;


public class HomeFragment extends ListFragment {
    private static Realm realm;

    public static HomeFragment newInstance(Realm r) {
        HomeFragment fragment = new HomeFragment();
        realm = r;
        return fragment;
    }

    public HomeFragment() {
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

        MyAdapter myAdapter = new MyAdapter(getActivity(), realm.where(User.class).findAll(), true);
        setListAdapter(myAdapter);
    }

}

class MyAdapter extends RealmBaseAdapter<User> implements ListAdapter {

    private static class ViewHolder {
        TextView cardName;
    }

    public MyAdapter(Context context,
                     RealmResults<User> realmResults,
                     boolean automaticUpdate) {
        super(context, realmResults, automaticUpdate);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_1,
                    parent, false);
            viewHolder = new ViewHolder();
            viewHolder.cardName = (TextView) convertView.findViewById(android.R.id.text1);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        User item = realmResults.get(position);
        viewHolder.cardName.setText(item.getFirstName() + " " + item.getLastName());
        return convertView;
    }

    public RealmResults<User> getRealmResults() {
        return realmResults;
    }
}

