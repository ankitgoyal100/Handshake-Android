package com.handshake.Handshake;

import android.content.Context;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by ankitgoyal on 12/23/14.
 */
public class RestClientSync {
    private static final String BASE_URL = "https://handshakeapi11.herokuapp.com";
    private static SyncHttpClient client = new SyncHttpClient();
    private static final int DEFAULT_TIMEOUT = 20 * 1000;

    public static void get(Context context, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        SessionManager sessionManager = new SessionManager(context);
        client.setTimeout(DEFAULT_TIMEOUT);
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        params.put("auth_token", sessionManager.getToken());
        params.put("user_id", sessionManager.getID());
        client.get(context, getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(Context context, String url, JSONObject jsonObject, String contentType, AsyncHttpResponseHandler responseHandler) {
        SessionManager sessionManager = new SessionManager(context);
        client.setTimeout(DEFAULT_TIMEOUT);
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        try {
            if (!jsonObject.has("auth_token"))
                jsonObject.put("auth_token", sessionManager.getToken());
            if (!jsonObject.has("user_id"))
                jsonObject.put("user_id", sessionManager.getID());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            StringEntity params = new StringEntity(jsonObject.toString());
            client.post(context, getAbsoluteUrl(url), params, contentType, responseHandler);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public static void post(Context context, String url, ByteArrayEntity entity, AsyncHttpResponseHandler responseHandler) {
        client.setTimeout(DEFAULT_TIMEOUT);
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        client.post(context, getAbsoluteUrl(url), entity, "application/json", responseHandler);
    }

    public static void post(Context context, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        SessionManager sessionManager = new SessionManager(context);
        client.setTimeout(DEFAULT_TIMEOUT);
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        params.put("auth_token", sessionManager.getToken());
        params.put("user_id", sessionManager.getID());
        client.post(context, getAbsoluteUrl(url), params, responseHandler);
    }

    public static void put(Context context, String url, HttpEntity entity, AsyncHttpResponseHandler responseHandler) {
        client.setTimeout(DEFAULT_TIMEOUT);
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        client.put(context, getAbsoluteUrl(url), entity, "application/json", responseHandler);
    }

    public static void put(Context context, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        SessionManager sessionManager = new SessionManager(context);
        client.setTimeout(DEFAULT_TIMEOUT);
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        params.put("auth_token", sessionManager.getToken());
        params.put("user_id", sessionManager.getID());
        client.put(context, getAbsoluteUrl(url), params, responseHandler);
    }


    public static void delete(Context context, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        SessionManager sessionManager = new SessionManager(context);
        client.setTimeout(DEFAULT_TIMEOUT);
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        Header[] headers = {
                new BasicHeader("Accept", "application/json")
                , new BasicHeader("Content-type", "application/json")
        };
        params.put("auth_token", sessionManager.getToken());
        params.put("user_id", sessionManager.getID());
        client.delete(context, getAbsoluteUrl(url), headers, params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
