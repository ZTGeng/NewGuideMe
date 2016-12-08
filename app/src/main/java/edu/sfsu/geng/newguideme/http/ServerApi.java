package edu.sfsu.geng.newguideme.http;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.http.ServerRequest.DataListener;

/**
 * Created by Geng on 2016/11/20.
 */
public class ServerApi {

    /* Account APIs */
    public static void login(String email, String password, DataListener listener) {
        new HttpRequest("/login", listener)
                .add("email", email)
                .add("password", password)
                .send();
    }

    public static void requestResetCode(String email, DataListener listener) {
        new HttpRequest("/api/resetpass", listener)
                .add("email", email)
                .send();
    }

    public static void resetPasswordWithCode(String email, String code, String newPassword, DataListener listener) {
        new HttpRequest("/api/resetpass/chg", listener)
                .add("email", email)
                .add("code", code)
                .add("newpass", newPassword)
                .send();
    }

    public static void register(String email, String username, String password, String role, DataListener listener) {
        new HttpRequest("/register", listener)
                .add("email", email)
                .add("username", username)
                .add("password", password)
                .add("role", role)
                .send();
    }

    public static void changePassword(String token, String oldPassword, String newPassword, DataListener listener) {
        new HttpRequest("/api/chgpass", listener)
                .add("token", token)
                .add("oldpass", oldPassword)
                .add("newpass", newPassword)
                .send();
    }
    /* Account APIs ends */

    /* Video chat APIs */
    public static void getRoomList(String token, DataListener listener) {
        new HttpRequest("/api/getroomlist", listener)
                .add("token", token)
                .send();
    }

    public static void createPublicRoom(String token, String description, DataListener listener) {
        new HttpRequest("/api/createpublicroom", listener)
                .add("token", token)
                .add("des", description)
                .send();
    }

    public static void blindDeleteRoom(String token, DataListener listener) {
        new HttpRequest("/api/deleteroom", listener)
                .add("token", token)
                .send();
    }

    public static void helperJoinRoom(String token, String roomId, DataListener listener) {
        new HttpRequest("/api/helperjoinroom", listener)
                .add("token", token)
                .add("room_id", roomId)
                .send();
    }

    public static void helperLeaveRoom(String token, String roomId, DataListener listener) {
        new HttpRequest("/api/helperleaveroom", listener)
                .add("token", token)
                .add("room_id", roomId)
                .send();
    }

    public static void blindKeepAlive(String token, DataListener listener) {
        ServerRequest.keepResAlive(Config.SERVER_ADDRESS + "/api/blindkeepalive/" + token, listener);
    }

    public static void helperKeepAlive(String token, String roomId, DataListener listener) {
        ServerRequest.keepResAlive(Config.SERVER_ADDRESS + "/api/helperkeepalive/" + token + "/" + roomId, listener);
    }

    public static void selectHelper(String blindId, String helperId, DataListener listener) {
        new HttpRequest("/api/select", listener)
                .add("blind_id", blindId)
                .add("helper_id", helperId)
                .send();
    }
    /* Video chat APIs ends */

    /* Friend APIs */
    public static void getFriends(String token, DataListener listener) {
        new HttpRequest("/api/getfriendlist", listener)
                .add("token", token)
                .send();
    }

    public static void addFriend(String myId, String friendId, DataListener listener) {
        new HttpRequest("/api/addfriend", listener)
                .add("m_id", myId)
                .add("f_id", friendId)
                .send();
    }

    public static void callFriendsById(String token, String friends, String description, DataListener listener) {
        new HttpRequest("/api/callfriendsbyid", listener)
                .add("token", token)
                .add("friends", friends)
                .add("des", description)
                .send();
    }

    public static void updateGcmToken(String token, String gcmToken, DataListener listener) {
        new HttpRequest("/api/updategcmtoken", listener)
                .add("token", token)
                .add("gcm_token", gcmToken)
                .send();
    }
    /* Friend APIs ends */

    /* Rate APIs */
    public static void getRate(String token, DataListener listener) {
        new HttpRequest("/api/getrate", listener)
                .add("token", token)
                .send();
    }

    public static void rateHelper(String token, String helperId, String rate, DataListener listener) {
        new HttpRequest("/api/rate", listener)
                .add("token", token)
                .add("ratee_id", helperId)
                .add("rate", rate)
                .send();
    }
    /* Rate APIs ends */

    /* Map APIs */
    public static void getRoute(LatLng start, LatLng end, DataListener listener) {
        String url = "http://maps.googleapis.com/maps/api/directions/json" +
                "?origin=" + start.latitude + "," + start.longitude +
                "&destination=" + end.latitude + "," + end.longitude +
                "&sensor=false&mode=walking";
        ServerRequest.getJSON(url, new ArrayList<NameValuePair>(), listener);
    }
    /* Map APIs ends */

    private static class HttpRequest {

        private ArrayList<NameValuePair> params = new ArrayList<>();
        private String api;
        private DataListener listener;

        private HttpRequest(@NonNull String api, @NonNull DataListener listener) {
            this.api = api;
            this.listener = listener;
        }

        @NonNull
        HttpRequest add(@NonNull String name, @NonNull String value) {
            params.add(new BasicNameValuePair(name, value));
            return this;
        }

        void send() {
            ServerRequest.getJSON(Config.SERVER_ADDRESS + api, params, listener);
        }
    }
}
