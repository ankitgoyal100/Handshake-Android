package com.handshake.Handshake;

import android.content.Context;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;
import com.loopj.android.http.SyncHttpClient;

import org.apache.http.Header;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;

/**
 * Created by ankitgoyal on 12/23/14.
 */
public class RestClientSync {
    private static final String BASE_URL = "https://handshakeapi11.herokuapp.com";

    private static SyncHttpClient client = new SyncHttpClient();

    public static void get(Context context, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        params.put("auth_token", SessionManager.getToken());
        params.put("user_id", SessionManager.getID());
        client.get(context, getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(Context context, String url, ByteArrayEntity entity, AsyncHttpResponseHandler responseHandler) {
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        client.post(context, getAbsoluteUrl(url), entity, "application/json", responseHandler);
    }

    public static void post(Context context, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        params.put("auth_token", SessionManager.getToken());
        params.put("user_id", SessionManager.getID());
        client.post(context, getAbsoluteUrl(url), params, responseHandler);
    }

    public static void put(Context context, String url, ByteArrayEntity entity, AsyncHttpResponseHandler responseHandler) {
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        client.put(context, getAbsoluteUrl(url), entity, "application/json", responseHandler);
    }

    public static void put(Context context, String url, RequestParams params, ResponseHandlerInterface responseHandler) {
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        params.put("auth_token", SessionManager.getToken());
        params.put("user_id", SessionManager.getID());
        client.put(getAbsoluteUrl(url), params, responseHandler);
    }


    public static void delete(Context context, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        Header[] headers = {
                new BasicHeader("Accept", "application/json")
                , new BasicHeader("Content-type", "application/json")
        };
        params.put("auth_token", SessionManager.getToken());
        params.put("user_id", SessionManager.getID());
        client.delete(context, getAbsoluteUrl(url), headers, params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
