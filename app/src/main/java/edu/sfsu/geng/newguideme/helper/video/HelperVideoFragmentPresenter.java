package edu.sfsu.geng.newguideme.helper.video;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.utils.PreferencesUtil;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by gengz on 3/13/17.
 */
public class HelperVideoFragmentPresenter implements
        Session.SessionListener,
        Session.SignalListener,
        PublisherKit.PublisherListener,
        Subscriber.VideoListener,
        EasyPermissions.PermissionCallbacks {

    private static final String TAG = "HelperVideoFragment";
    private static final int RC_AUDIO_APP_PERM = 125;

    @NonNull private Fragment fragment;
    @NonNull private Listener listener;

    @Nullable private String token;
    @Nullable private String blindId;
    @Nullable private String sessionId;
    @Nullable private String videoToken;

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    private boolean enableAudio;

    public HelperVideoFragmentPresenter(@NonNull Fragment fragment, @NonNull Listener listener) {

        this.fragment = fragment;
        this.listener = listener;

        Bundle bundle = fragment.getArguments();
        token = bundle.getString("token");
        sessionId = bundle.getString("sessionId");
        videoToken = bundle.getString("videoToken");
        blindId = bundle.getString("blindId");

        enableAudio = true;
    }

    void onCreate() {
        Set<String> friends = PreferencesUtil.getInstance(fragment.getContext()).getFriendIds();
        listener.setAddFriendButtonEnable(friends == null || !friends.contains(blindId));

        initialSession();
    }

    void onResume() {
        if (mSession == null) {
            return;
        }
        mSession.onResume();
    }

    void onPause() {
        if (mSession == null) {
            return;
        }
        mSession.onPause();

        if (fragment.isDetached()) {
            disconnectSession();
        }
    }

    void onDestroy() {
        disconnectSession();
    }

    void onMuteButtonClicked() {
        enableAudio = !enableAudio;
        mPublisher.setPublishAudio(enableAudio);
        listener.setMuteButtonEnable(enableAudio);
    }

    void onAddFriendButtonClicked() {
        listener.showAddFriendDialog();
    }

    void sendFriendRequest() {
        send("add", "");
        listener.setAddFriendButtonEnable(false);
    }

    void addFriend() {
        if (token == null || blindId == null) {
            return;
        }
        ServerApi.addFriend(token, blindId, new ServerApi.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        listener.setAddFriendButtonEnable(false);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Server error while adding friend");
                }
            }

            @Override
            public void onClose() {}
        });
    }

    void requestNavigationRoute(@NonNull LatLng currentLocation, @NonNull LatLng destination) {
        ServerApi.getRoute(currentLocation, destination, new ServerApi.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    JSONArray routes = json.getJSONArray("routes");

                    JSONArray steps = routes.getJSONObject(0)
                            .getJSONArray("legs")
                            .getJSONObject(0)
                            .getJSONArray("steps");

                    List<LatLng> lines = new ArrayList<>();

                    for(int i = 0; i < steps.length(); i++) {
                        String polyline = steps.getJSONObject(i).getJSONObject("polyline").getString("points");

                        for(LatLng p : decodePolyline(polyline)) {
                            lines.add(p);
                        }
                    }

                    listener.drawNavigationRoute(lines);
                } catch (JSONException e) {
                    Log.e(TAG, "Error while getting route from Google");
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {}
        });
    }

    @AfterPermissionGranted(RC_AUDIO_APP_PERM)
    void initialSession() {
        String[] perms = { Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO };
        if (EasyPermissions.hasPermissions(fragment.getContext(), perms)) {
            mSession = new Session(fragment.getContext(), Config.APIKEY, sessionId);
            mSession.setSessionListener(this);
            mSession.setSignalListener(this);
            mSession.connect(videoToken);
        } else {
            EasyPermissions.requestPermissions(
                    fragment,
                    fragment.getString(R.string.rationale_audio_app),
                    RC_AUDIO_APP_PERM,
                    perms);
        }
    }

    private void disconnectSession() {
        if (mSession == null) {
            return;
        }

        if (mSubscriber != null) {
//            subscriberView.removeView(mSubscriber.getView());
            listener.removeSubscriberView(mSubscriber.getView());
            mSession.unsubscribe(mSubscriber);
            mSubscriber.destroy();
            mSubscriber = null;
        }

        if (mPublisher != null) {
            mSession.unpublish(mPublisher);
            mPublisher.destroy();
            mPublisher = null;
        }
        mSession.disconnect();
    }

    private void send(String event, String data) {
        if (mSession != null) {
            mSession.sendSignal(event, data);
        }
    }

    /** POLYLINE DECODER - http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java **/
    private List<LatLng> decodePolyline(String encoded) {

        List<LatLng> poly = new ArrayList<>();

        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((double) lat / 1E5, (double) lng / 1E5);
            poly.add(p);
        }

        return poly;
    }

    private void subscribeToStream(Stream stream) {
        mSubscriber = new Subscriber(fragment.getContext(), stream);
        mSubscriber.setVideoListener(this);
        mSession.subscribe(mSubscriber);
    }

    /* SessionListener */
    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "SessionListener.onConnectedd");
        mPublisher = new Publisher(fragment.getContext(), "helper to blind", true, false);
        mPublisher.setPublisherListener(this);

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.d(TAG, "SessionListener.onDisconnected");
        mSession = null;
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.d(TAG, "SessionListener.onStreamReceived");

        if (mSubscriber != null) {
            return;
        }

        subscribeToStream(stream);
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.d(TAG, "SessionListener.onStreamDropped");

        if (mSubscriber != null && mSubscriber.getStream().equals(stream)) {
            listener.removeSubscriberView(mSubscriber.getView());
            mSubscriber.destroy();
            mSubscriber = null;
        }

        listener.onVideoEnd();
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.d(TAG, "SessionListener.onError: " + opentokError.getMessage());
        listener.onVideoEnd();
    }

    /* SignalListener */
    @Override
    public void onSignalReceived(Session session, String event, String data, Connection connection) {
        if (mSession.getConnection().equals(connection)) return;
//        Log.d(TAG, session + " - " + data);
        switch (event) {
            case "add":
                Log.d(TAG, "Receive add friend request");
                listener.showFriendRequestDialog();
                break;
            case "location":
                String[] locLatLng = data.split(",");
                if (locLatLng.length != 2) {
                    Log.e(TAG, "Location format error: " + data);
                    break;
                }
                listener.setBlindLocation(new LatLng(Double.parseDouble(locLatLng[0]), Double.parseDouble(locLatLng[1])));
                break;
            case "destination":
                Log.d(TAG, "Receive destination: " + data);
                String[] destData = data.split(",", 3);
                if (destData.length != 3) {
                    Log.d(TAG, "Destination format error: " + data);
                    break;
                }
                listener.setDestination(new LatLng(Double.parseDouble(destData[0]), Double.parseDouble(destData[1])), destData[2]);
                break;
            default:
                Log.d(TAG, "Unhandled event: " + event + " - " + data);
                break;
        }
    }

    /* PublisherListener */
    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "PublisherListener.onStreamCreated");
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "PublisherListener.onStreamDestroyed");
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.d(TAG, "PublisherListener.onError: " + opentokError.getMessage());
        listener.onVideoEnd();
    }

    /* VideoListener */
    @Override
    public void onVideoDataReceived(SubscriberKit subscriberKit) {
        Log.d(TAG, "VideoListener.onVideoDataReceived");
        mSubscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        listener.addSubscriberView(mSubscriber.getView());
    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriberKit, String s) {
        Log.d(TAG, "VideoListener.onVideoDisabled");
    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriberKit, String s) {
        Log.d(TAG, "VideoListener.onVideoEnabled");
    }

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriberKit) {
        Log.d(TAG, "VideoListener.onVideoDisableWarning");
    }

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriberKit) {
        Log.d(TAG, "VideoListener.onVideoDisableWarningLifted");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            listener.showPermissionDeniedDialog();
        }
    }

    interface Listener {
        void setMuteButtonEnable(boolean enableAudio);
        void setAddFriendButtonEnable(boolean showAddFriend);
        void showAddFriendDialog();
        void showFriendRequestDialog();
        void showPermissionDeniedDialog();
        void setBlindLocation(@NonNull LatLng location);
        void setDestination(@NonNull LatLng location, @NonNull String placeName);
        void drawNavigationRoute(List<LatLng> lines);
        void addSubscriberView(View view);
        void removeSubscriberView(View view);
        void onVideoEnd();
    }
}
