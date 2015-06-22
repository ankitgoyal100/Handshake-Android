package com.handshake.listview;

/**
 * Created by ankitgoyal on 6/19/15.
 */

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.handshake.Handshake.R;
import com.handshake.models.User;
import com.squareup.picasso.Picasso;

import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;

public class ContactAdapter extends RealmBaseAdapter<User> implements ListAdapter {
//    private static final int TYPE_ITEM = 0;
//    private static final int TYPE_SEPARATOR = 1;
//
//    private RealmResults<User> mData;
//    private TreeSet<Integer> sectionHeader = new TreeSet<Integer>();

    public ContactAdapter(Context context,
                          RealmResults<User> realmResults,
                          boolean automaticUpdate) {
        super(context, realmResults, automaticUpdate);
//        mData = realmResults;
    }

//    @Override
//    public int getItemViewType(int position) {
//        return sectionHeader.contains(position) ? TYPE_SEPARATOR : TYPE_ITEM;
//    }
//
//    @Override
//    public int getViewTypeCount() {
//        return 2;
//    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.contact_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.personName = (TextView) convertView.findViewById(R.id.name);
            viewHolder.description = (TextView) convertView.findViewById(R.id.description);
            viewHolder.image = (ImageView) convertView.findViewById(R.id.image);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        User item = realmResults.get(position);

        viewHolder.personName.setText(item.getFirstName() + " " + item.getLastName());

        if (!item.getThumb().isEmpty())
            Picasso.with(context).load(item.getThumb()).into(viewHolder.image);
        else


        if (item.getMutual() == 1)
            viewHolder.description.setText(item.getMutual() + " mutual contact");
        else
            viewHolder.description.setText(item.getMutual() + " mutual contacts");

        return convertView;
    }

    public RealmResults<User> getRealmResults() {
        return realmResults;
    }

    class ViewHolder {
        ImageView image;
        TextView personName;
        TextView description;
    }
}


