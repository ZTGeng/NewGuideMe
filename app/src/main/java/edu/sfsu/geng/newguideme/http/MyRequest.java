package edu.sfsu.geng.newguideme.http;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;

import edu.sfsu.geng.newguideme.Config;

/**
 * Created by geng on 7/16/16.
 */
public class MyRequest {
    ArrayList<NameValuePair> params;

    public MyRequest() {
        params = new ArrayList<>();
    }

    public void add(String key, String value) {
        params.add(new BasicNameValuePair(key, value));
    }

    public void getJSON(String api, ServerRequest.DataListener dataListener) {
        ServerRequest.getJSON(Config.SERVER_ADDRESS + api, params, dataListener);
    }

    public void blindJoin(String id, ServerRequest.DataListener blindWaitActivity) {
        ServerRequest.keepResAlive(Config.SERVER_ADDRESS + "/api/blindkeepalive/" + id, blindWaitActivity);
    }

    public void helperJoin(String helperId, String roomId, ServerRequest.DataListener helperWaitActivity) {
        ServerRequest.keepResAlive(Config.SERVER_ADDRESS + "/api/helperkeepalive/" + helperId + "/" + roomId, helperWaitActivity);
    }

    public void getRoute(double olat, double olng, double dlat, double dlng, ServerRequest.DataListener dataListener) {
        String url = "http://maps.googleapis.com/maps/api/directions/json?origin="
                + olat + "," + olng + "&destination=" + dlat + "," + dlng + "&sensor=false&mode=walking";
        ServerRequest.getJSON(url, params, dataListener);
    }
}
