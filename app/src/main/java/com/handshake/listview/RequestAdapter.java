package com.handshake.listview;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;

import com.handshake.Handshake.MainActivity;
import com.handshake.Handshake.R;
import com.handshake.models.User;
import com.handshake.views.CircleTransform;
import com.handshake.views.TextViewCustomFont;
import com.squareup.picasso.Picasso;

import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/21/15.
 */
public class RequestAdapter extends RealmBaseAdapter<User> implements ListAdapter {

    public RequestAdapter(Context context,
                          RealmResults<User> realmResults,
                          boolean automaticUpdate) {
        super(context, realmResults, automaticUpdate);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.request_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.personName = (TextViewCustomFont) convertView.findViewById(R.id.name);
            viewHolder.description = (TextViewCustomFont) convertView.findViewById(R.id.description);
            viewHolder.image = (ImageView) convertView.findViewById(R.id.image);
            viewHolder.acceptButton = (ImageView) convertView.findViewById(R.id.accept_image);
            viewHolder.declineButton = (ImageView) convertView.findViewById(R.id.decline_image);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final User item = realmResults.get(position);

        viewHolder.personName.setText(item.getFirstName() + " " + item.getLastName());
        if (!item.getThumb().isEmpty() && !item.getThumb().equals("null"))
            Picasso.with(context).load(item.getThumb()).transform(new CircleTransform()).into(viewHolder.image);
        else
            Picasso.with(context).load(R.drawable.default_profile).transform(new CircleTransform()).into(viewHolder.image);

        if (item.getMutual() == 1)
            viewHolder.description.setText(item.getMutual() + " mutual contact");
        else
            viewHolder.description.setText(item.getMutual() + " mutual contacts");

        MainActivity.setContactButtons(context, item, viewHolder.acceptButton, viewHolder.declineButton);

//        viewHolder.acceptButton.setVisibility(View.GONE);
//        viewHolder.declineButton.setVisibility(View.GONE);
//        if (item.isContact())
//            viewHolder.description.setText("Request accepted");
//        else if (!item.isRequestReceived())
//            viewHolder.description.setText("Request declined");
//        else {
//            if (item.getMutual() == 1)
//                viewHolder.description.setText(item.getMutual() + " mutual contact");
//            else
//                viewHolder.description.setText(item.getMutual() + " mutual contacts");
//
//            viewHolder.acceptButton.setVisibility(View.VISIBLE);
//            viewHolder.declineButton.setVisibility(View.VISIBLE);
//        }
//
//        viewHolder.acceptButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                viewHolder.acceptButton.setVisibility(View.GONE);
//                viewHolder.declineButton.setVisibility(View.GONE);
//
//                Toast.makeText(context, "Request accepted", Toast.LENGTH_SHORT).show();
//
//                RequestServerSync.acceptRequest(item, new UserSyncCompleted() {
//                    @Override
//                    public void syncCompletedListener(User users) {
//
//                    }
//
//                    @Override
//                    public void syncFailedListener() {
//                        Toast.makeText(context, "Could not accept request at this time. Please try again later.", Toast.LENGTH_LONG).show();
//                    }
//                });
//            }
//        });
//
//        viewHolder.declineButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                viewHolder.acceptButton.setVisibility(View.GONE);
//                viewHolder.declineButton.setVisibility(View.GONE);
//
//                Toast.makeText(context, "Request declined", Toast.LENGTH_SHORT).show();
//
//                RequestServerSync.declineRequest(item, new UserSyncCompleted() {
//                    @Override
//                    public void syncCompletedListener(User users) {
//
//                    }
//
//                    @Override
//                    public void syncFailedListener() {
//                        Toast.makeText(context, "Could not decline request at this time. Please try again later.", Toast.LENGTH_LONG).show();
//                    }
//                });
//            }
//        });

        return convertView;
    }

    public RealmResults<User> getRealmResults() {
        return realmResults;
    }

    class ViewHolder {
        ImageView image;
        TextViewCustomFont personName;
        TextViewCustomFont description;
        ImageView acceptButton;
        ImageView declineButton;
    }
}

