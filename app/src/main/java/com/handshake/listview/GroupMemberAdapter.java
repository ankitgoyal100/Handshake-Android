package com.handshake.listview;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import com.handshake.Handshake.R;
import com.handshake.models.GroupMember;
import com.handshake.views.CircleTransform;
import com.handshake.views.TextViewCustomFont;
import com.squareup.picasso.Picasso;

import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/27/15.
 */
public class GroupMemberAdapter extends RealmBaseAdapter<GroupMember> implements ListAdapter {

    public GroupMemberAdapter(Context context,
                              RealmResults<GroupMember> realmResults,
                              boolean automaticUpdate) {
        super(context, realmResults, automaticUpdate);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.group_member_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.personName = (TextViewCustomFont) convertView.findViewById(R.id.name);
            viewHolder.image = (ImageView) convertView.findViewById(R.id.image);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final GroupMember item = realmResults.get(position);
        viewHolder.personName.setText(item.getName());

        if (!item.getUser().getThumb().isEmpty() && !item.getUser().getThumb().equals("null"))
            Picasso.with(context).load(item.getUser().getThumb()).transform(new CircleTransform()).into(viewHolder.image);
        else
            Picasso.with(context).load(R.drawable.default_profile).transform(new CircleTransform()).into(viewHolder.image);

        return convertView;
    }

    public RealmResults<GroupMember> getRealmResults() {
        return realmResults;
    }

    class ViewHolder {
        ImageView image;
        TextViewCustomFont personName;
        TextViewCustomFont description;
        LinearLayout contactsButtonLayout;
    }
}

