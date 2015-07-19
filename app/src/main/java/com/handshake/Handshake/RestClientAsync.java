package com.handshake.Handshake;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by ankitgoyal on 12/23/14.
 */
public class RestClientAsync {
    private static final String BASE_URL = "https://handshakeapi11.herokuapp.com";

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(Context context, String url, AsyncHttpResponseHandler responseHandler) {
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        client.get(context, getAbsoluteUrl(url), new RequestParams(), responseHandler);
    }

    public static void get(Context context, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        params.put("auth_token", SessionManager.getToken());
        params.put("user_id", SessionManager.getID());

        System.out.println(params.toString());
        client.get(context, getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(Context context, String url, JSONObject jsonObject, String contentType, AsyncHttpResponseHandler responseHandler) {
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json");
        try {
            jsonObject.put("auth_token", SessionManager.getToken());
            jsonObject.put("user_id", SessionManager.getID());
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

    public static void put(Context context, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
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
