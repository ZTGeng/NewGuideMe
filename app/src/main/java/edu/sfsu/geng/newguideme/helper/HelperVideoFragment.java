package edu.sfsu.geng.newguideme.helper;

import android.Manifest;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.http.ServerRequest;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * A placeholder fragment containing a simple view.
 */
public class HelperVideoFragment extends Fragment
        implements EasyPermissions.PermissionCallbacks,
        Session.SessionListener,
        Session.SignalListener,
        PublisherKit.PublisherListener,
        Subscriber.VideoListener
{

    private static final String TAG = "HelperVideoFragment";

    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;

    private static final int MUTE_COLOR = Color.GRAY;

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    private GoogleMap mMap;
    private Marker curLocationMarker, destinationMarker;
    private Polyline polyLine;

    private LinearLayoutCompat subscriberView;
    private FloatingActionButton addFriendButton;

    private String token, blindId, blindName;
    private String sessionId, videoToken;
    private boolean mapInitialed, isMute;

    private Listener listener;
    private LinearLayoutCompat mapLayout;

    public HelperVideoFragment() {
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Bundle bundle = getArguments();
        token = bundle.getString("token");
        sessionId = bundle.getString("sessionId");
        videoToken = bundle.getString("videoToken");
        blindId = bundle.getString("blindId");
        blindName = bundle.getString("blindName");

        View view = inflater.inflate(R.layout.fragment_helper_video, container, false);

        mapLayout = (LinearLayoutCompat) view.findViewById(R.id.helper_video_map);

        mapInitialed = false;
        isMute = false;
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
        final int fabColor = typedValue.data;

        // video screen
        subscriberView = (LinearLayoutCompat) view.findViewById(R.id.helper_video_screen);

        final FloatingActionButton muteSwitch = (FloatingActionButton) view.findViewById(R.id.helper_video_mute_button);
        if (muteSwitch != null) {
            muteSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mPublisher.setPublishAudio(isMute);
                    isMute = !isMute;

                    muteSwitch.setBackgroundTintList(ColorStateList.valueOf(isMute ? MUTE_COLOR : fabColor));

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        muteSwitch.setImageDrawable(getResources().getDrawable(
                                isMute ? R.drawable.ic_mic_off_white_24dp : R.drawable.ic_mic_white_24dp,
                                getContext().getTheme()));
                    } else {
                        muteSwitch.setImageDrawable(getResources().getDrawable(
                                isMute ? R.drawable.ic_mic_off_white_24dp : R.drawable.ic_mic_white_24dp));
                    }
                }
            });
        }

        addFriendButton = (FloatingActionButton) view.findViewById(R.id.helper_video_add_button);
        addFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(String.format(getResources().getString(R.string.helper_video_add_confirm_message), blindName));
                builder.setPositiveButton(R.string.helper_video_add_confirm_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        send("add", "");
                        addFriendButton.setVisibility(View.GONE);
                    }
                });
                builder.setNegativeButton(R.string.helper_video_add_cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        // video session
        requestPermissions();

        return view;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");

        super.onResume();

        if (mSession == null) {
            return;
        }
        mSession.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");

        super.onPause();

        if (mSession == null) {
            return;
        }
        mSession.onPause();

        if (isDetached()) {
            disconnectSession();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        disconnectSession();

        super.onDestroy();
    }

    void hideAddFriendButton() {
        addFriendButton.setVisibility(View.GONE);
    }

    void setListener(@NonNull Listener listener) {
        this.listener = listener;
    }

    private void send(String event, String data) {
        if (mSession != null) {
            mSession.sendSignal(event, data);
        }
    }

    private void initMap(final LatLng curLatLng) {
        mapInitialed = true;

        if (mapLayout != null) {
            mapLayout.setVisibility(View.VISIBLE);
        }
        SupportMapFragment mapFragment = new SupportMapFragment();
        getActivity().getSupportFragmentManager().beginTransaction().add(R.id.helper_video_map, mapFragment).commit();
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
//                LatLng sf = new LatLng(37.722, -122.48);
                curLocationMarker = mMap.addMarker(new MarkerOptions().position(curLatLng).title("User Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(curLatLng));
                mMap.moveCamera(CameraUpdateFactory.zoomTo(15f));
            }
        });
    }

    private void setDestination(String destination) {
        if (mMap == null) return;

        if (destinationMarker != null) {
            destinationMarker.remove();
        }

        Geocoder geocoder = new Geocoder(getContext());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocationName(destination, 1);
            if (addresses.isEmpty()) {
                Log.d(TAG, "Cannot find destination address");
                return;
            }
            Address address = addresses.get(0);
            destinationMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(address.getLatitude(), address.getLongitude()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title("Destination"));
            setDirection();
        } catch (IOException e) {
            Log.e(TAG, "Error while get destination address");
            e.printStackTrace();
        }
    }

    private void setDirection() {
        if (curLocationMarker == null || destinationMarker == null) {
            return;
        }
        ServerApi.getRoute(curLocationMarker.getPosition(), destinationMarker.getPosition(), new ServerRequest.DataListener() {
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

                    if (polyLine != null) {
                        polyLine.remove();
                    }
                    polyLine = mMap.addPolyline(new PolylineOptions().addAll(lines).width(5).color(Color.BLUE));
                } catch (JSONException e) {
                    Log.e(TAG, "Error while getting route from Google");
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {}
        });
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

    private void disconnectSession() {
        if (mSession == null) {
            return;
        }

        if (mSubscriber != null) {
            subscriberView.removeView(mSubscriber.getView());
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

    private void subscribeToStream(Stream stream) {
        mSubscriber = new Subscriber(getContext(), stream);
        mSubscriber.setVideoListener(this);
        mSession.subscribe(mSubscriber);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

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
            new AppSettingsDialog.Builder(this, getString(R.string.rationale_ask_again))
                    .setTitle(getString(R.string.title_settings_dialog))
                    .setPositiveButton(getString(R.string.setting))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setRequestCode(RC_SETTINGS_SCREEN_PERM)
                    .build()
                    .show();
        }
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = { Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO };
        if (EasyPermissions.hasPermissions(getContext(), perms)) {
            mSession = new Session(getContext(), Config.APIKEY, sessionId);
            mSession.setSessionListener(this);
            mSession.setSignalListener(this);
            mSession.connect(videoToken);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
        }
    }

    /* SessionListener */
    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "SessionListener.onConnectedd");
        mPublisher = new Publisher(getContext(), "helper to blind", true, false);
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
            subscriberView.removeView(mSubscriber.getView());
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
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(String.format(getResources().getString(R.string.helper_video_add_request_message), blindName));
                builder.setPositiveButton(R.string.helper_video_add_confirm_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ServerApi.addFriend(token, blindId, new ServerRequest.DataListener() {
                            @Override
                            public void onReceiveData(String data) {
                                try {
                                    JSONObject json = new JSONObject(data);
                                    if (json.getBoolean("res")) {
                                        addFriendButton.setVisibility(View.GONE);
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "Server error while adding friend");
                                }
                            }

                            @Override
                            public void onClose() {}
                        });


                    }
                });
                builder.setNegativeButton(R.string.helper_video_add_cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            case "location":
                String[] latLngStr = data.split(",");
                if (latLngStr.length != 2) {
                    Log.e(TAG, "Invalid location data");
                    break;
                }
                Log.d(TAG, "Receive location update: " + data);
                LatLng latLng = new LatLng(Double.parseDouble(latLngStr[0]), Double.parseDouble(latLngStr[1]));
                if (mapInitialed) {
                    if (curLocationMarker != null) {
                        curLocationMarker.setPosition(latLng);
                    }
                } else {
                    initMap(latLng);
                }
                break;
            case "destination":
                Log.d(TAG, "Receive destination: " + data);
                setDestination(data);
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
        subscriberView.addView(mSubscriber.getView());
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

    interface Listener {
        void onVideoEnd();
    }
}