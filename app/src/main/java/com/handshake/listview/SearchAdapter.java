package com.handshake.listview;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;

import com.handshake.Handshake.MainActivity;
import com.handshake.Handshake.R;
import com.handshake.Handshake.RestClientAsync;
import com.handshake.helpers.UserArraySyncCompleted;
import com.handshake.helpers.UserServerSync;
import com.handshake.models.User;
import com.handshake.views.CircleTransform;
import com.handshake.views.TextViewCustomFont;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;

import org.apache.http.Header;
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
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.user_list_item, parent, false);
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

        MainActivity.setContactButtons(mContext, item.getUserId(), viewHolder.buttonOne, viewHolder.buttonTwo, null);
        realm.close();

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
                        public void onSuccess(int statusCode, Header[] headers, final JSONObject response) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        UserServerSync.cacheUser(mContext, response.getJSONArray("results"), new UserArraySyncCompleted() {
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
                            });
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
        ImageView buttonOne;
        ImageView buttonTwo;
    }
}