package com.handshake.listview;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.handshake.Handshake.ContactUserProfileActivity;
import com.handshake.Handshake.GenericUserProfileActivity;
import com.handshake.Handshake.MainActivity;
import com.handshake.Handshake.R;
import com.handshake.Handshake.Utils;
import com.handshake.models.User;
import com.handshake.views.CircleTransform;
import com.handshake.views.TextViewCustomFont;
import com.squareup.picasso.Picasso;

import java.util.List;

import io.realm.Realm;

/**
 * Created by ankitgoyal on 8/27/15.
 */
public class StockContactAdapter extends ArrayAdapter<Long> {

    private Context context;

    public StockContactAdapter(Context context, int resource, List<Long> items) {
        super(context, resource, items);
        this.context = context;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.user_list_item, parent, false);
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

        Realm realm = Realm.getInstance(context);
        final User item = realm.where(User.class).equalTo("userId", getItem(position)).findFirst();
        viewHolder.image.setVisibility(View.VISIBLE);
        viewHolder.personName.setText(item.getFirstName() + " " + item.getLastName());

        if (!item.getThumb().isEmpty() && !item.getThumb().equals("null"))
            Picasso.with(context).load(item.getThumb())
                    .resize(Utils.dpToPx(context, 60), Utils.dpToPx(context, 60)).transform(new CircleTransform()).into(viewHolder.image);
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
                Long userId = getItem(position);
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

        realm.close();

        return convertView;
    }

    class ViewHolder {
        ImageView image;
        TextViewCustomFont personName;
        TextViewCustomFont description;
        ImageView buttonOne;
        ImageView buttonTwo;
    }
}