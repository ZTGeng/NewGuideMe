package edu.sfsu.geng.newguideme.http;

import com.google.android.gms.maps.model.LatLng;

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

    public static void resetPasswordWithCode(String email, String code, String newPassword, ServerRequest.DataListener listener) {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("email", email));
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("newpass", newPassword));
        ServerRequest.getJSON(Config.SERVER_ADDRESS + "/api/resetpass/chg", params, listener);
    }

    public static void register(String email, String username, String password, String role, ServerRequest.DataListener listener) {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("email", email));
        params.add(new BasicNameValuePair("username", username));
        params.add(new BasicNameValuePair("password", password));
        params.add(new BasicNameValuePair("role", role));
        ServerRequest.getJSON(Config.SERVER_ADDRESS + "/register", params, listener);
    }

    public static void changePassword(String token, String oldPassword, String newPassword, ServerRequest.DataListener listener) {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("token", token));
        params.add(new BasicNameValuePair("oldpass", oldPassword));
        params.add(new BasicNameValuePair("newpass", newPassword));
        ServerRequest.getJSON(Config.SERVER_ADDRESS + "/api/chgpass", params, listener);
    }

    public static void helperJoinRoom(String token, String roomId, ServerRequest.DataListener listener) {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("token", token));
        params.add(new BasicNameValuePair("room_id", roomId));
        ServerRequest.getJSON(Config.SERVER_ADDRESS + "/api/helperjoinroom", params, listener);
    }

    public static void getRoomList(String token, ServerRequest.DataListener listener) {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("token", token));
        ServerRequest.getJSON(Config.SERVER_ADDRESS + "/api/getroomlist", params, listener);
    }

    public static void getRate(String token, ServerRequest.DataListener listener) {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("token", token));
        ServerRequest.getJSON(Config.SERVER_ADDRESS + "/api/getrate", params, listener);
    }

    public static void getFriends(String token, ServerRequest.DataListener listener) {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("token", token));
        ServerRequest.getJSON(Config.SERVER_ADDRESS + "/api/getfriendlist", params, listener);
    }

    public static void blindKeepAlive(String token, ServerRequest.DataListener listener) {
        ServerRequest.keepResAlive(Config.SERVER_ADDRESS + "/api/blindkeepalive/" + token, listener);
    }

    public static void helperKeepAlive(String token, String roomId, ServerRequest.DataListener listener) {
        ServerRequest.keepResAlive(Config.SERVER_ADDRESS + "/api/helperkeepalive/" + token + "/" + roomId, listener);
    }

    public static void helperLeaveRoom(String token, String roomId, ServerRequest.DataListener listener) {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("token", token));
        params.add(new BasicNameValuePair("room_id", roomId));
        ServerRequest.getJSON(Config.SERVER_ADDRESS + "/api/helperleaveroom", params, listener);
    }

    public static void getRoute(LatLng start, LatLng end, ServerRequest.DataListener listener) {
        String url = "http://maps.googleapis.com/maps/api/directions/json" +
                "?origin=" + start.latitude + "," + start.longitude +
                "&destination=" + end.latitude + "," + end.longitude +
                "&sensor=false&mode=walking";
        ServerRequest.getJSON(url, new ArrayList<NameValuePair>(), listener);
    }

    public static void createPublicRoom(String token, String description, ServerRequest.DataListener listener) {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("token", token));
        params.add(new BasicNameValuePair("des", description));
        ServerRequest.getJSON(Config.SERVER_ADDRESS + "/api/createpublicroom", params, listener);
    }

    public static void callFriendsById(String token, String friends, String description, ServerRequest.DataListener listener) {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("token", token));
        params.add(new BasicNameValuePair("friends", friends));
        params.add(new BasicNameValuePair("des", description));
        ServerRequest.getJSON(Config.SERVER_ADDRESS + "/api/callfriendsbyid", params, listener);
    }

    public static void blindDeleteRoom(String token, ServerRequest.DataListener listener) {
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("token", token));
        ServerRequest.getJSON(Config.SERVER_ADDRESS + "/api/deleteroom", params, listener);
    }
}
