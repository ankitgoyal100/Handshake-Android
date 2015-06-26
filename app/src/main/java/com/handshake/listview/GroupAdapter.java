package com.handshake.listview;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import com.handshake.Handshake.GroupActivity;
import com.handshake.Handshake.R;
import com.handshake.Handshake.TextViewCustomFont;
import com.handshake.models.Group;

import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/23/15.
 */
public class GroupAdapter extends RealmBaseAdapter<Group> implements ListAdapter {

    public GroupAdapter(Context context,
                        RealmResults<Group> realmResults,
                        boolean automaticUpdate) {
        super(context, realmResults, automaticUpdate);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.group_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.code = (TextViewCustomFont) convertView.findViewById(R.id.group_code);
            viewHolder.name = (TextViewCustomFont) convertView.findViewById(R.id.group_name);
            viewHolder.numMembers = (TextViewCustomFont) convertView.findViewById(R.id.num_members);
            viewHolder.parentLayout = (LinearLayout) convertView.findViewById(R.id.layout);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final Group item = realmResults.get(position);

        if(item.getCode().length() == 0) return convertView;

        String code = item.getCode().toUpperCase();
        code = code.substring(0, 2) + "-" + code.substring(2, 4) + "-" + code.substring(4);
        viewHolder.code.setText(code);
        viewHolder.name.setText(item.getName());
        if (item.getMembers().size() == 1)
            viewHolder.numMembers.setText("1 member");
        else
            viewHolder.numMembers.setText(item.getMembers().size() + " members");


        viewHolder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, GroupActivity.class);
                context.startActivity(intent);
            }
        });

        return convertView;
    }

    public RealmResults<Group> getRealmResults() {
        return realmResults;
    }

    class ViewHolder {
        LinearLayout parentLayout;
        TextViewCustomFont code;
        TextViewCustomFont name;
        TextViewCustomFont numMembers;
    }
}
