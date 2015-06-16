package com.handshake.helpers;

import android.content.Context;
import android.os.Handler;

import com.handshake.Handshake.RestClientAsync;
import com.handshake.Handshake.RestClientSync;
import com.handshake.Handshake.SessionManager;
import com.handshake.Handshake.Utils;
import com.handshake.models.Card;
import com.handshake.models.User;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/15/15.
 */
public class CardServerSync {
    private static Handler handler = new Handler();

    private static SessionManager session;
    private static Context context;
    private static SyncCompleted listener;

    private static int counter = 0;

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
                System.out.println(response.toString());

                final Realm realm = Realm.getInstance(context);
                User user = realm.where(User.class).equalTo("userId", SessionManager.getID()).findFirst();

                if (user == null) return;

                final HashMap<Long, JSONObject> map = new HashMap<Long, JSONObject>();
                try {
                    JSONArray cards = response.getJSONArray("cards");
                    for (int i = 0; i < cards.length(); i++) {
                        map.put(cards.getJSONObject(i).getLong("id"), cards.getJSONObject(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                for (Card card : user.getCards()) {
                    if (card.getSyncStatus() == Utils.CardCreated) continue;

                    if (!map.containsKey(card.getCardId())) {
                        RealmList<Card> currCards = user.getCards();
                        currCards.remove(card);
                        user.setCards(currCards);
                    } else if (card.getSyncStatus() == Utils.CardSynced) {
                        realm.beginTransaction();
                        Card.updateCard(card, realm, map.get(card.getCardId()));
                    }

                    map.remove(card.getCardId());
                }

                for (long id : map.keySet()) {
                    JSONObject cardJSON = map.get(id);

                    realm.beginTransaction();
                    Card card = realm.createObject(Card.class);
                    Card.updateCard(card, realm, cardJSON);
                    card.setSyncStatus(Utils.CardSynced);
                    realm.commitTransaction();

                    RealmList<Card> currCards = user.getCards();
                    currCards.add(card);
                    user.setCards(currCards);
                }

                RealmResults<Card> cards = realm.where(Card.class).notEqualTo("syncStatus", Utils.CardSynced)
                        .equalTo("user.userId", user.getUserId()).findAll();

                for (final Card card : cards) {
                    if (card.getSyncStatus() == Utils.CardCreated) {
                        RequestParams params = Card.cardToParams(card);
                        RestClientAsync.post(context, "/cards", params, new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                Realm realm = Realm.getInstance(context);
                                realm.beginTransaction();
                                try {
                                    Card.updateCard(card, realm, response.getJSONObject("card"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                card.setSyncStatus(Utils.CardSynced);
                                realm.commitTransaction();
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                if (statusCode == 401) session.logoutUser();
                            }
                        });
                    } else if (card.getSyncStatus() == Utils.CardUpdated) {
                        RequestParams params = Card.cardToParams(card);
                        RestClientAsync.put(context, "/cards/" + card.getCardId(), params, new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                Realm realm = Realm.getInstance(context);
                                realm.beginTransaction();
                                try {
                                    Card.updateCard(card, realm, response.getJSONObject("card"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                card.setSyncStatus(Utils.CardSynced);
                                realm.commitTransaction();
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                if (statusCode == 401) session.logoutUser();
                            }
                        });
                    } else if (card.getSyncStatus() == Utils.CardDeleted) {
                        RestClientAsync.delete(context, "/cards/" + card.getCardId(), new RequestParams(), new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                if (statusCode == 401) session.logoutUser();
                            }
                        });
                    }
                }

                listener.syncCompletedListener();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (statusCode == 401) session.logoutUser();
            }
        });
    }
}
