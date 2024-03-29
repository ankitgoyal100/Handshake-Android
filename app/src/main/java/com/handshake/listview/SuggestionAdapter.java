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
import com.handshake.models.Suggestion;
import com.handshake.models.User;
import com.handshake.views.CircleTransform;
import com.handshake.views.TextViewCustomFont;
import com.squareup.picasso.Picasso;

import io.realm.Realm;
import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;

public class SuggestionAdapter extends RealmBaseAdapter<Suggestion> implements ListAdapter, AdapterView.OnItemClickListener {
//    private static final int TYPE_ITEM = 0;
//    private static final int TYPE_SEPARATOR = 1;
//
//    private RealmResults<User> mData;
//    private TreeSet<Integer> sectionHeader = new TreeSet<Integer>();

    public SuggestionAdapter(Context context,
                             RealmResults<Suggestion> realmResults,
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

        final Suggestion suggestion = realmResults.get(position);
        final User item = suggestion.getUser();
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
                Long userId = item.getUserId();
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

//        viewHolder.contactsButtonLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                new AlertDialogWrapper.Builder(context)
//                        .setTitle("Delete contact")
//                        .setMessage("Are you sure you want to delete this contact?")
//                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int which) {
//                                ContactServerSync.deleteContact(item);
//                                dialog.cancel();
//                            }
//                        })
//                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.cancel();
//                            }
//                        })
//                        .show();
//            }
//        });

        return convertView;
    }

    public RealmResults<Suggestion> getRealmResults() {
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


