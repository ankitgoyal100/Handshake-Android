package com.handshake.helpers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.handshake.Handshake.RestClientSync;
import com.handshake.Handshake.SessionManager;
import com.handshake.Handshake.Utils;
import com.handshake.models.Account;
import com.handshake.models.Card;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/15/15.
 */
public class CardServerSync {
    private static Handler handler = new Handler();

    private static SessionManager session;
    private static Context context;
    private static SyncCompleted listener;

    public static void performSync(final Context c, final SyncCompleted l) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                context = c;
                listener = l;
                session = new SessionManager(context);
                performSyncHelper();
            }
        }).start();

    }

    private static void performSyncHelper() {
        RestClientSync.get(context, "/cards", new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                final Realm realm = Realm.getInstance(context);
                SessionManager sessionManager = new SessionManager(context);
                Account account = realm.where(Account.class).equalTo("userId", sessionManager.getID()).findFirst();

                if (account == null) return;

                final HashMap<Long, JSONObject> map = new HashMap<Long, JSONObject>();
                try {
                    JSONArray cards = response.getJSONArray("cards");
                    for (int i = 0; i < cards.length(); i++) {
                        map.put(cards.getJSONObject(i).getLong("id"), cards.getJSONObject(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                for (Card card : account.getCards()) {
                    if (card.getSyncStatus() == Utils.CardCreated) continue;

                    if (!map.containsKey(card.getCardId())) {
                        account.getCards().add(realm.copyToRealm(card));
                    } else if (card.getSyncStatus() == Utils.CardSynced) {
                        realm.beginTransaction();
                        card = Card.updateCard(card, realm, map.get(card.getCardId()));
                        realm.commitTransaction();
                    }

                    map.remove(card.getCardId());
                }

                for (long id : map.keySet()) {
                    JSONObject cardJSON = map.get(id);

                    realm.beginTransaction();
                    Card card = realm.createObject(Card.class);
                    card = Card.updateCard(card, realm, cardJSON);
                    card.setSyncStatus(Utils.CardSynced);
                    card.setAccount(account);

                    account.getCards().add(realm.copyToRealm(card));

                    realm.commitTransaction();
                }

                RealmResults<Card> cards = realm.where(Card.class).notEqualTo("syncStatus", Utils.CardSynced)
                        .equalTo("account.userId", account.getUserId()).findAll();

                if (Looper.myLooper() == null) {
                    Looper.prepare();
                }

                for (int i = 0; i < cards.size(); i++) {
                    final Card c = cards.get(i);
                    if (c.getSyncStatus() == Utils.CardCreated) {
                        RestClientSync.post(context, "/cards", Card.cardToJSONObject(context, c), "application/json", new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                Card card = c;

                                Realm realm = Realm.getInstance(context);
                                realm.beginTransaction();
                                try {
                                    card = Card.updateCard(card, realm, response.getJSONObject("card"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                card.setSyncStatus(Utils.CardSynced);
                                realm.commitTransaction();
                                realm.close();
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                if (statusCode == 401) session.logoutUser();
                            }
                        });
                    } else {
                        if (c.getSyncStatus() == Utils.CardUpdated) {
                            try {
                                StringEntity entity = new StringEntity(Card.cardToJSONObject(context, c).toString());
                                RestClientSync.put(context, "/cards/" + c.getCardId(), entity, new JsonHttpResponseHandler() {
                                    @Override
                                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                        Card card = c;

                                        Realm realm = Realm.getInstance(context);
                                        realm.beginTransaction();
                                        try {
                                            card = Card.updateCard(card, realm, response.getJSONObject("card"));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        card.setSyncStatus(Utils.CardSynced);
                                        realm.commitTransaction();
                                        realm.close();
                                    }

                                    @Override
                                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                        if (statusCode == 401) session.logoutUser();
                                    }
                                });
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        } else if (c.getSyncStatus() == Utils.CardDeleted) {
                            RestClientSync.delete(context, "/cards/" + c.getCardId(), new RequestParams(), new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                    Card card = c;

                                    Realm realm = Realm.getInstance(context);
                                    realm.beginTransaction();
                                    card.removeFromRealm();
                                    realm.commitTransaction();
                                    realm.close();
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                    if (statusCode == 401) session.logoutUser();
                                }
                            });
                        }
                    }
                }

                realm.close();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.syncCompletedListener();
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (errorResponse == null) return;
                if (statusCode == 401) session.logoutUser();
            }
        });
    }
}
