package edu.sfsu.geng.newguideme.helper;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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

public class HelperVideoActivity extends AppCompatActivity implements
        EasyPermissions.PermissionCallbacks,
        Session.SessionListener,
        Session.ConnectionListener,
        PublisherKit.PublisherListener,
        Subscriber.SubscriberListener,
        Subscriber.VideoListener,
        Session.SignalListener {

    private static final String TAG = "HelperVideo";

    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    private GoogleMap mMap;
    private Marker curLocationMarker, destinationMarker;
    private Polyline polyLine;

    private LinearLayoutCompat subscriberView;
    private AppCompatButton addButton;

    private String token, blindId, blindName;
    private String sessionId, videoToken;
    private boolean mapInitialed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_video);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SharedPreferences pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        token = pref.getString("token", "");

        sessionId = getIntent().getStringExtra("sessionId");
        videoToken = getIntent().getStringExtra("videoToken");
        blindId = getIntent().getStringExtra("blindId");
        blindName = getIntent().getStringExtra("blindName");

        mapInitialed = false;

        // video screen
        subscriberView = (LinearLayoutCompat) findViewById(R.id.helper_video_screen);

        // buttons
        FloatingActionButton quitButton = (FloatingActionButton) findViewById(R.id.helper_video_quit_btn);
        if (quitButton != null) {
            quitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Quit button onClicked");
                    AlertDialog.Builder builder = new AlertDialog.Builder(HelperVideoActivity.this);
                    builder.setMessage(R.string.helper_video_quit_confirm_message);
                    builder.setPositiveButton(R.string.helper_video_quit_confirm_button,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    quit();
                                }
                            });
                    builder.setNegativeButton(R.string.helper_video_quit_cancel_button,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {}
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        }

        SwitchCompat muteSwitch = (SwitchCompat) findViewById(R.id.helper_video_mute_switch);
        if (muteSwitch != null) {
            muteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mPublisher.setPublishAudio(!isChecked);
                }
            });
        }

        addButton = (AppCompatButton) findViewById(R.id.helper_video_add_btn);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(HelperVideoActivity.this);
                builder.setMessage(String.format(getResources().getString(R.string.helper_video_add_confirm_message), blindName));
                builder.setPositiveButton(R.string.helper_video_add_confirm_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        send("add", "");
                        addButton.setVisibility(View.GONE);
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

        Set<String> friends = pref.getStringSet("friends", null);
        if (friends != null && friends.contains(blindId)) {
            addButton.setVisibility(View.GONE);
        }

        // video session
        requestPermissions();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");

        super.onResume();

        if (mSession == null) {
            return;
        }
        mSession.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

        super.onPause();

        if (mSession == null) {
            return;
        }
        mSession.onPause();

        if (isFinishing()) {
            disconnectSession();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        disconnectSession();

        super.onDestroy();
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
        if (EasyPermissions.hasPermissions(this, perms)) {
            mSession = new Session(HelperVideoActivity.this, Config.APIKEY, sessionId);
            mSession.setSessionListener(this);
            mSession.setSignalListener(this);
            mSession.connect(videoToken);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
        }
    }

    private void quit() {
        Intent homeActivity = new Intent(HelperVideoActivity.this, HelperHomeActivity.class);
        startActivity(homeActivity);
        finish();
    }

    private void send(String event, String data) {
        if (mSession != null) {
            mSession.sendSignal(event, data);
        }
    }

    private void initMap(final LatLng curLatLng) {
        mapInitialed = true;
        LinearLayoutCompat mapLayout = (LinearLayoutCompat) findViewById(R.id.helper_video_map);
        Log.d(TAG, "====================" + 1);
        if (mapLayout != null) {
            Log.d(TAG, "====================" + 2);
            mapLayout.setVisibility(View.VISIBLE);
        }
        SupportMapFragment mapFragment = new SupportMapFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.helper_video_map, mapFragment).commit();
        Log.d(TAG, "====================" + 3);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                Log.d(TAG, "====================" + 4);
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

        Geocoder geocoder = new Geocoder(this);
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

    /* SessionListener */
    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "SessionListener.onConnected is called");
        mPublisher = new Publisher(HelperVideoActivity.this, "helper to blind", true, false);
        mPublisher.setPublisherListener(this);

//        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.d(TAG, "SessionListener.onDisconnected is called");
        mSession = null;
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.d(TAG, "SessionListener.onStreamReceived is called");

        if (mSubscriber != null) {
            return;
        }

        subscribeToStream(stream);
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.d(TAG, "SessionListener.onStreamDropped is called");

        if (mSubscriber != null && mSubscriber.getStream().equals(stream)) {
            subscriberView.removeView(mSubscriber.getView());
            mSubscriber.destroy();
            mSubscriber = null;
        }

        quit();
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.d(TAG, "SessionListener.onError: " + opentokError.getMessage());
        quit();
    }

    private void subscribeToStream(Stream stream) {
        mSubscriber = new Subscriber(HelperVideoActivity.this, stream);
        mSubscriber.setVideoListener(this);
        mSession.subscribe(mSubscriber);
    }

    /* PublisherListener */
    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "PublisherListener.onStreamCreated is called");
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "PublisherListener.onStreamDestroyed is called");
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.d(TAG, "PublisherListener.onError: " + opentokError.getMessage());
        quit();
    }

    /* SubscriberListener */
    @Override
    public void onConnected(SubscriberKit subscriberKit) {

    }

    @Override
    public void onDisconnected(SubscriberKit subscriberKit) {
        Log.d(TAG, "onDisconnected");
    }

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {

    }

    /* VideoListener, */
    @Override
    public void onVideoDataReceived(SubscriberKit subscriberKit) {
        Log.d(TAG, "onVideoDataReceived");
        mSubscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        subscriberView.addView(mSubscriber.getView());
    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriberKit, String s) {

    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriberKit, String s) {

    }

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriberKit) {

    }

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriberKit) {

    }

    /* SignalListener */
    @Override
    public void onSignalReceived(Session session, String event, String data, Connection connection) {
        if (mSession.getConnection().equals(connection)) return;
        Log.d(TAG, session + " - " + data);
        switch (event) {
            case "add":
                Log.d(TAG, "Receive add friend request");
//                if (addButton.getVisibility() == View.GONE) {
//                    break;
//                }
                AlertDialog.Builder builder = new AlertDialog.Builder(HelperVideoActivity.this);
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
                                        addButton.setVisibility(View.GONE);
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
                setDestination(data);
                break;
            default:
                Log.d(TAG, "Unhandled event: " + event + " - " + data);
                break;
        }
    }

    /* ConnectionListener */
    @Override
    public void onConnectionCreated(Session session, Connection connection) {
    }

    @Override
    public void onConnectionDestroyed(Session session, Connection connection) {

    }
}
