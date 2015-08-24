package com.handshake.listview;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;

import com.handshake.Handshake.GroupActivity;
import com.handshake.Handshake.R;
import com.handshake.models.Group;
import com.handshake.views.TextViewCustomFont;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

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
            viewHolder.groupCode = (TextViewCustomFont) convertView.findViewById(R.id.group_code);
            viewHolder.name = (TextViewCustomFont) convertView.findViewById(R.id.group_name);
            viewHolder.numMembers = (TextViewCustomFont) convertView.findViewById(R.id.num_members);
            viewHolder.parentLayout = (LinearLayout) convertView.findViewById(R.id.layout);
            viewHolder.groupPhoto1Options = (RelativeLayout) convertView.findViewById(R.id.group_photo_1_options);
            viewHolder.groupPhoto2Options = (RelativeLayout) convertView.findViewById(R.id.group_photo_2_options);
            viewHolder.groupPhoto3Options = (RelativeLayout) convertView.findViewById(R.id.group_photo_3_options);
            viewHolder.groupPhoto4Options = (RelativeLayout) convertView.findViewById(R.id.group_photo_4_options);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final Group item = realmResults.get(position);

        ArrayList<String> thumbUrls = new ArrayList<String>();

        for(int i = 0; i < item.getMembers().size(); i++) {
            String thumb = item.getMembers().get(i).getUser().getThumb();
            if (thumb != null && !thumb.equals("null") && !thumb.isEmpty()) {
                thumbUrls.add(thumb);
            }
        }

        if (thumbUrls.size() == 0) {
            viewHolder.groupCode.setVisibility(View.VISIBLE);
            viewHolder.groupPhoto1Options.setVisibility(View.GONE);
            viewHolder.groupPhoto2Options.setVisibility(View.GONE);
            viewHolder.groupPhoto3Options.setVisibility(View.GONE);
            viewHolder.groupPhoto4Options.setVisibility(View.GONE);
        } else if (thumbUrls.size() == 1) {
            viewHolder.groupCode.setVisibility(View.GONE);
            viewHolder.groupPhoto1Options.setVisibility(View.VISIBLE);
            viewHolder.groupPhoto2Options.setVisibility(View.GONE);
            viewHolder.groupPhoto3Options.setVisibility(View.GONE);
            viewHolder.groupPhoto4Options.setVisibility(View.GONE);

            Picasso.with(context).load(thumbUrls.get(0))
                    .into((ImageView) viewHolder.groupPhoto1Options.findViewById(R.id.image));
        } else if (thumbUrls.size() == 2) {
            viewHolder.groupCode.setVisibility(View.GONE);
            viewHolder.groupPhoto1Options.setVisibility(View.GONE);
            viewHolder.groupPhoto2Options.setVisibility(View.VISIBLE);
            viewHolder.groupPhoto3Options.setVisibility(View.GONE);
            viewHolder.groupPhoto4Options.setVisibility(View.GONE);

            Picasso.with(context).load(thumbUrls.get(0))
                    .into((ImageView) viewHolder.groupPhoto2Options.findViewById(R.id.left_image));
            Picasso.with(context).load(thumbUrls.get(1))
                    .into((ImageView) viewHolder.groupPhoto2Options.findViewById(R.id.right_image));
        } else if (thumbUrls.size() == 3) {
            viewHolder.groupCode.setVisibility(View.GONE);
            viewHolder.groupPhoto1Options.setVisibility(View.GONE);
            viewHolder.groupPhoto2Options.setVisibility(View.GONE);
            viewHolder.groupPhoto3Options.setVisibility(View.VISIBLE);
            viewHolder.groupPhoto4Options.setVisibility(View.GONE);

            Picasso.with(context).load(thumbUrls.get(0))
                    .into((ImageView) viewHolder.groupPhoto3Options.findViewById(R.id.top_left_image));
            Picasso.with(context).load(thumbUrls.get(1))
                    .into((ImageView) viewHolder.groupPhoto3Options.findViewById(R.id.bottom_left_image));
            Picasso.with(context).load(thumbUrls.get(2))
                    .into((ImageView) viewHolder.groupPhoto3Options.findViewById(R.id.right_image));
        } else {
            viewHolder.groupCode.setVisibility(View.GONE);
            viewHolder.groupPhoto1Options.setVisibility(View.GONE);
            viewHolder.groupPhoto2Options.setVisibility(View.GONE);
            viewHolder.groupPhoto3Options.setVisibility(View.GONE);
            viewHolder.groupPhoto4Options.setVisibility(View.VISIBLE);

            Picasso.with(context).load(thumbUrls.get(0))
                    .into((ImageView) viewHolder.groupPhoto4Options.findViewById(R.id.top_left_image));
            Picasso.with(context).load(thumbUrls.get(1))
                    .into((ImageView) viewHolder.groupPhoto4Options.findViewById(R.id.bottom_left_image));
            Picasso.with(context).load(thumbUrls.get(2))
                    .into((ImageView) viewHolder.groupPhoto4Options.findViewById(R.id.top_right_image));
            Picasso.with(context).load(thumbUrls.get(3))
                    .into((ImageView) viewHolder.groupPhoto4Options.findViewById(R.id.bottom_right_image));
        }

        String code = item.getCode().toUpperCase();

        if (code.length() != 6) return convertView;

        code = code.substring(0, 2) + "-" + code.substring(2, 4) + "-" + code.substring(4);
        viewHolder.groupCode.setText(code);
        viewHolder.name.setText(item.getName());
        if (item.getMembers().size() == 1)
            viewHolder.numMembers.setText("1 member");
        else
            viewHolder.numMembers.setText(item.getMembers().size() + " members");


        viewHolder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, GroupActivity.class);
                intent.putExtra("id", item.getGroupId());
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
        TextViewCustomFont name;
        TextViewCustomFont numMembers;
        TextViewCustomFont groupCode;
        RelativeLayout groupPhoto1Options;
        RelativeLayout groupPhoto2Options;
        RelativeLayout groupPhoto3Options;
        RelativeLayout groupPhoto4Options;
    }
}
