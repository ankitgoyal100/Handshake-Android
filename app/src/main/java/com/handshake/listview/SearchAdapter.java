package com.handshake.listview;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.handshake.Handshake.R;
import com.handshake.Handshake.RestClientAsync;
import com.handshake.helpers.ContactServerSync;
import com.handshake.helpers.UserArraySyncCompleted;
import com.handshake.helpers.UserServerSync;
import com.handshake.models.User;
import com.handshake.views.CircleTransform;
import com.handshake.views.TextViewCustomFont;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

/**
 * Created by ankitgoyal on 7/9/15.
 */

public class SearchAdapter extends BaseAdapter implements Filterable {

    private Context mContext;
    private List<Long> resultList = new ArrayList<Long>();
    private Handler handler = new Handler();

    public SearchAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public Long getItem(int index) {
        return resultList.get(index);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.contact_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.personName = (TextViewCustomFont) convertView.findViewById(R.id.name);
            viewHolder.description = (TextViewCustomFont) convertView.findViewById(R.id.description);
            viewHolder.image = (ImageView) convertView.findViewById(R.id.image);
            viewHolder.contactsButtonLayout = (LinearLayout) convertView.findViewById(R.id.contacts_button);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final Long userId = getItem(position);
        Realm realm = Realm.getInstance(mContext);
        final User item = realm.where(User.class).equalTo("userId", userId).findFirst();
        viewHolder.image.setVisibility(View.VISIBLE);
        viewHolder.personName.setText(item.getFirstName() + " " + item.getLastName());

        if (!item.getThumb().isEmpty() && !item.getThumb().equals("null"))
            Picasso.with(mContext).load(item.getThumb()).transform(new CircleTransform()).into(viewHolder.image);
        else
            Picasso.with(mContext).load(R.drawable.default_profile).transform(new CircleTransform()).into(viewHolder.image);

        if (item.getMutual() == 1)
            viewHolder.description.setText(item.getMutual() + " mutual contact");
        else
            viewHolder.description.setText(item.getMutual() + " mutual contacts");

        viewHolder.contactsButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialogWrapper.Builder(mContext)
                        .setTitle("Delete contact")
                        .setMessage("Are you sure you want to delete this contact?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ContactServerSync.deleteContact(item);
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .show();
            }
        });

        return convertView;
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(final CharSequence constraint) {
                final FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    RestClientAsync.get(mContext, "/search/?q=" + Uri.encode(constraint.toString()), new RequestParams(), new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            try {
                                final JSONArray filteredResults = new JSONArray();

                                for (int i = 0; i < response.getJSONArray("results").length(); i++) {
                                    if (!response.getJSONArray("results").getJSONObject(i).getBoolean("is_contact")) {
                                        filteredResults.put(response.getJSONArray("results").getJSONObject(i));
                                    }
                                }

                                UserServerSync.cacheUser(mContext, filteredResults, new UserArraySyncCompleted() {
                                    @Override
                                    public void syncCompletedListener(final ArrayList<User> users) {
                                        ArrayList<Long> array = new ArrayList<Long>();
                                        for (User u : users)
                                            array.add(u.getUserId());
                                        filterResults.values = array;
                                        filterResults.count = array.size();
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                publishResults(constraint, filterResults);
                                            }
                                        });
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {

                        }
                    });

                }

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    resultList = (List<Long>) results.values;
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }

    class ViewHolder {
        ImageView image;
        TextViewCustomFont personName;
        TextViewCustomFont description;
        LinearLayout contactsButtonLayout;
    }
}