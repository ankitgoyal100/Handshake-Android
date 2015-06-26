package com.handshake.listview;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;

import com.handshake.Handshake.R;
import com.handshake.Handshake.SessionManager;
import com.handshake.Handshake.TextViewCustomFont;
import com.handshake.Handshake.Utils;
import com.handshake.models.Account;
import com.handshake.models.FeedItem;
import com.squareup.picasso.Picasso;

import io.realm.Realm;
import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/21/15.
 */
public class FeedAdapter extends RealmBaseAdapter<FeedItem> implements ListAdapter {

    public FeedAdapter(Context context,
                       RealmResults<FeedItem> realmResults,
                       boolean automaticUpdate) {
        super(context, realmResults, automaticUpdate);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.contact_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.title = (TextViewCustomFont) convertView.findViewById(R.id.name);
            viewHolder.description = (TextViewCustomFont) convertView.findViewById(R.id.description);
            viewHolder.image = (ImageView) convertView.findViewById(R.id.image);
            viewHolder.rightImage = (ImageView) convertView.findViewById(R.id.contacts_image);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.rightImage.setVisibility(View.GONE);
        viewHolder.image.setVisibility(View.VISIBLE);

        FeedItem item = realmResults.get(position);

        System.out.println(item.toString());

        if (item.getItemType().equals("new_contact") || item.getItemType().equals("card_updated") ||
                item.getItemType().equals("new_group_member"))
            if (item.getUser() == null) return convertView;
        if (item.getItemType().equals("group_joined") || item.getItemType().equals("new_group_member"))
            if (item.getGroup() == null) return convertView;

        viewHolder.title.setText(getMessageForItem(item));

        if (item.getUser() != null) {
            if (!item.getUser().getThumb().isEmpty() && !item.getUser().getThumb().equals("null"))
                Picasso.with(context).load(item.getUser().getThumb()).transform(new CircleTransform()).into(viewHolder.image);
            else
                Picasso.with(context).load(R.drawable.default_profile).transform(new CircleTransform()).into(viewHolder.image);
        } else {
            Realm realm = Realm.getInstance(context);
            Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();
            if (!account.getThumb().isEmpty() && !account.getThumb().equals("null"))
                Picasso.with(context).load(account.getThumb()).transform(new CircleTransform()).into(viewHolder.image);
            else
                Picasso.with(context).load(R.drawable.default_profile).transform(new CircleTransform()).into(viewHolder.image);
        }

        viewHolder.description.setText(Utils.getTimeSince(item.getCreatedAt()));

        return convertView;
    }

    public RealmResults<FeedItem> getRealmResults() {
        return realmResults;
    }

    class ViewHolder {
        ImageView image;
        TextViewCustomFont title;
        TextViewCustomFont description;
        ImageView rightImage;
    }

    private Spannable getMessageForItem(FeedItem item) {
            Spannable messageString = new SpannableString("");
            if (item.getItemType().equals("new_contact")) {
                messageString = new SpannableString(item.getUser().getFirstName() + " " + item.getUser().getLastName() + " added you!");
                messageString.setSpan(new StyleSpan(Typeface.BOLD), 0, (item.getUser().getFirstName() + " " + item.getUser().getLastName()).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                return messageString;
            } else if (item.getItemType().equals("card_updated")) {
                messageString = new SpannableString(item.getUser().getFirstName() + " " +
                        item.getUser().getLastName() + " got new contact information.");
                messageString.setSpan(new StyleSpan(Typeface.BOLD), 0, (item.getUser().getFirstName() + " " + item.getUser().getLastName()).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                return messageString;
            } else if (item.getItemType().equals("group_joined")) {
                messageString = new SpannableString("You joined " + item.getGroup().getName() + ".");
                messageString.setSpan(new StyleSpan(Typeface.BOLD), 0, (item.getGroup().getName()).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                return messageString;
            } else if (item.getItemType().equals("new_group_member")) {
                messageString = new SpannableString(item.getUser().getFirstName() + " " + item.getUser().getLastName() +
                        " joined " + item.getGroup().getName() + ".");
                messageString.setSpan(new StyleSpan(Typeface.BOLD), 0, (item.getUser().getFirstName() + " " + item.getUser().getLastName()).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                messageString.setSpan(new StyleSpan(Typeface.BOLD), (item.getUser().getFirstName() + " " + item.getUser().getLastName()).length() + 8, messageString.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                return messageString;
            }

            return messageString;
        }
    }