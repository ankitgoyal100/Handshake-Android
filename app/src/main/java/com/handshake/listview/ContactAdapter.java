package com.handshake.listview;

/**
 * Created by ankitgoyal on 6/19/15.
 */

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;

import com.handshake.Handshake.ContactUserProfileActivity;
import com.handshake.Handshake.GenericUserProfileActivity;
import com.handshake.Handshake.MainActivity;
import com.handshake.Handshake.R;
import com.handshake.Handshake.Utils;
import com.handshake.models.User;
import com.handshake.views.CircleTransform;
import com.handshake.views.TextViewCustomFont;
import com.squareup.picasso.Picasso;

import io.realm.Realm;
import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;

public class ContactAdapter extends RealmBaseAdapter<User> implements ListAdapter, AdapterView.OnItemClickListener {

    public ContactAdapter(Context context,
                          RealmResults<User> realmResults,
                          boolean automaticUpdate) {
        super(context, realmResults, automaticUpdate);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.user_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.personName = (TextViewCustomFont) convertView.findViewById(R.id.name);
            viewHolder.description = (TextViewCustomFont) convertView.findViewById(R.id.description);
            viewHolder.image = (ImageView) convertView.findViewById(R.id.image);
            viewHolder.buttonOne = (ImageView) convertView.findViewById(R.id.button_one);
            viewHolder.buttonTwo = (ImageView) convertView.findViewById(R.id.button_two);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final User item = realmResults.get(position);
        final long id = item.getUserId();
        viewHolder.image.setVisibility(View.VISIBLE);
        viewHolder.personName.setText(item.getFirstName() + " " + item.getLastName());

        if (!item.getThumb().isEmpty() && !item.getThumb().equals("null"))
            Picasso.with(context).load(item.getThumb())
                    .resize(Utils.dpToPx(context, 60), Utils.dpToPx(context, 60))
                    .transform(new CircleTransform()).into(viewHolder.image);
        else
            Picasso.with(context).load(R.drawable.default_profile)
                    .resize(Utils.dpToPx(context, 60), Utils.dpToPx(context, 60)).transform(new CircleTransform()).into(viewHolder.image);

        if (item.getMutual() == 1)
            viewHolder.description.setText(item.getMutual() + " mutual contact");
        else
            viewHolder.description.setText(item.getMutual() + " mutual contacts");

        MainActivity.setContactButtons(context, item.getUserId(), viewHolder.buttonOne, viewHolder.buttonTwo, null);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Long userId = id;
                Realm realm = Realm.getInstance(context);
                User user = realm.where(User.class).equalTo("userId", userId).findFirst();

                Intent i;
                if (user.isContact()) {
                    i = new Intent(context, ContactUserProfileActivity.class);
                } else {
                    i = new Intent(context, GenericUserProfileActivity.class);
                }
                i.putExtra("userId", userId);
                realm.close();
                context.startActivity(i);
            }
        });

        return convertView;
    }

    public RealmResults<User> getRealmResults() {
        return realmResults;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    class ViewHolder {
        ImageView image;
        TextViewCustomFont personName;
        TextViewCustomFont description;
        ImageView buttonOne;
        ImageView buttonTwo;
    }
}


