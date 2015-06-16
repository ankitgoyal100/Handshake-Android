package com.handshake.helpers;

import android.content.Context;
import android.os.Handler;

import com.handshake.Handshake.RestClientAsync;
import com.handshake.Handshake.RestClientSync;
import com.handshake.Handshake.SessionManager;
import com.handshake.Handshake.Utils;
import com.handshake.models.Account;
import com.handshake.models.Card;
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
                Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();

                if (account == null) return;

                System.out.println("Account: " + account.toString());

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
                        RealmList<Card> currCards = account.getCards();
                        currCards.remove(card);
                        account.setCards(currCards);
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

                    RealmList<Card> currCards = account.getCards();
                    currCards.add(card);
                    account.setCards(currCards);
                    realm.commitTransaction();

                    System.out.println("Card added: " + card.toString());
                }

                RealmResults<Card> cards = realm.where(Card.class).notEqualTo("syncStatus", Utils.CardSynced)
                        .equalTo("account.userId", account.getUserId()).findAll();

                for (final Card c : cards) {
                    if (c.getSyncStatus() == Utils.CardCreated) {
                        RequestParams params = Card.cardToParams(c);
                        RestClientAsync.post(context, "/cards", params, new JsonHttpResponseHandler() {
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
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                if (statusCode == 401) session.logoutUser();
                            }
                        });
                    } else {
                        if (c.getSyncStatus() == Utils.CardUpdated) {
                            RequestParams params = Card.cardToParams(c);
                            RestClientAsync.put(context, "/cards/" + c.getCardId(), params, new JsonHttpResponseHandler() {
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
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                    if (statusCode == 401) session.logoutUser();
                                }
                            });
                        } else if (c.getSyncStatus() == Utils.CardDeleted) {
                            RestClientAsync.delete(context, "/cards/" + c.getCardId(), new RequestParams(), new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                    Card card = c;

                                    Realm realm = Realm.getInstance(context);
                                    realm.beginTransaction();
                                    card.removeFromRealm();
                                    realm.commitTransaction();
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                    if (statusCode == 401) session.logoutUser();
                                }
                            });
                        }
                    }
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.syncCompletedListener();
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (statusCode == 401) session.logoutUser();
            }
        });
    }
}
