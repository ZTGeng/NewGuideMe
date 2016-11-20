package edu.sfsu.geng.newguideme.http;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;

import edu.sfsu.geng.newguideme.Config;

/**
 * Created by Geng on 2016/11/20.
 */

public class ServerApi {

    public static void login(String email, String password, ServerRequest.DataListener listener) {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("email", email));
        params.add(new BasicNameValuePair("password", password));
        ServerRequest.getJSON(Config.SERVER_ADDRESS + "/login", params, listener);
    }

    public static void requestResetCode(String email, ServerRequest.DataListener listener) {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("email", email));
        ServerRequest.getJSON(Config.SERVER_ADDRESS + "/api/resetpass", params, listener);
    }

    public static void changePassword(String email, String code, String newPassword, ServerRequest.DataListener listener) {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("email", email));
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("newpass", newPassword));
        ServerRequest.getJSON(Config.SERVER_ADDRESS + "/api/resetpass/chg", params, listener);
    }
}
