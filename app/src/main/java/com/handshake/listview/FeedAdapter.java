package com.handshake.listview;

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

/**
 * Created by ankitgoyal on 6/21/15.
 */
public class FeedAdapter extends RealmBaseAdapter<User> implements ListAdapter {

    public FeedAdapter(Context context,
                          RealmResults<User> realmResults,
                          boolean automaticUpdate) {
        super(context, realmResults, automaticUpdate);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.contact_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.personName = (TextView) convertView.findViewById(R.id.name);
            viewHolder.description = (TextView) convertView.findViewById(R.id.description);
            viewHolder.image = (ImageView) convertView.findViewById(R.id.image);
            viewHolder.rightImage = (ImageView) convertView.findViewById(R.id.contacts_image);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        User item = realmResults.get(position);

        viewHolder.personName.setText(item.getFirstName() + " " + item.getLastName());

        viewHolder.rightImage.setVisibility(View.GONE);

        if (!item.getThumb().isEmpty())
            Picasso.with(context).load(item.getThumb()).into(viewHolder.image);
        else
            Picasso.with(context).load(R.drawable.default_profile).into(viewHolder.image);

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
        ImageView rightImage;
    }
}