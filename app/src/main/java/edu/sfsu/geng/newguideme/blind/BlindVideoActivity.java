package edu.sfsu.geng.newguideme.blind;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.MyRequest;
import edu.sfsu.geng.newguideme.http.ServerRequest;

public class BlindVideoActivity extends AppCompatActivity implements
        Session.SessionListener,
        Session.ConnectionListener,
        PublisherKit.PublisherListener,
        Subscriber.SubscriberListener,
        Subscriber.VideoListener,
        Session.SignalListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        DestinationDialogFragment.DestinationListener
{

    private static final String TAG = "BlindVideo";

    private Session mSession;
    private Publisher mPublisher;
//    private Connection helperConnection;

    private static final int UPDATE_INTERVAL = 2000;
    private static final int FASTEST_UPDATE_INTERVAL = 1000;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean googleClientInitialed, askForDestination;

    private AppCompatButton quitButton, addButton;
    private ToggleButton muteToggle, navigationToggle;
    private SharedPreferences pref;

    private String sessionId, token, id, helperId, helperName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blind_video);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        id = pref.getString("token", "");

        sessionId = getIntent().getStringExtra("session");
        token = getIntent().getStringExtra("token");
        helperId = getIntent().getStringExtra("helperId");
        helperName = getIntent().getStringExtra("helperName");

        googleClientInitialed = false;
        askForDestination = true;

        // buttons
        quitButton = (AppCompatButton) findViewById(R.id.blind_video_quit_btn);
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Quit button onClicked");
                AlertDialog.Builder builder = new AlertDialog.Builder(BlindVideoActivity.this);
                builder.setMessage(R.string.blind_video_quit_confirm_message);
                builder.setPositiveButton(R.string.blind_video_quit_confirm_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        quit();
                    }
                });
                builder.setNegativeButton(R.string.blind_video_quit_cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        addButton = (AppCompatButton) findViewById(R.id.blind_video_add_btn);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(BlindVideoActivity.this);
                builder.setMessage(String.format(getResources().getString(R.string.blind_video_add_confirm_message), helperName));
                builder.setPositiveButton(R.string.blind_video_add_confirm_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        send("add", "");
                        addButton.setEnabled(false);
                        addButton.setText(String.format(getResources().getString(R.string.blind_video_add_button_off), helperName));
                    }
                });
                builder.setNegativeButton(R.string.blind_video_add_cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        Set<String> friends = pref.getStringSet("friendIds", null);
        if (friends != null && friends.contains(helperId)) {
            addButton.setEnabled(false);
            addButton.setText(String.format(getResources().getString(R.string.blind_video_add_button_off), helperName));
        }

        muteToggle = (ToggleButton) findViewById(R.id.blind_video_mute_toggle);
        muteToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPublisher.setPublishAudio(!isChecked);
            }
        });

        navigationToggle = (ToggleButton) findViewById(R.id.blind_video_navigation_toggle);
        navigationToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (askForDestination) {
                        DestinationDialogFragment dialogFragment = new DestinationDialogFragment();
                        dialogFragment.show(getSupportFragmentManager(), "DestinationDialog");
                    }
                    if (!googleClientInitialed) {
                        googleClientInitialed = true;
                        createLocationRequest();
                        buildGoogleApiClient(BlindVideoActivity.this);
                    }
                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }
            }
        });

        // video session
        mSession = new Session(this, Config.APIKEY, sessionId);
        mSession.setSessionListener(this);
        mSession.setSignalListener(this);
        mSession.connect(token);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSession != null) {
            mSession.connect(token);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSession != null) {
            mSession.disconnect();
        }
    }

    private void quit() {
        if (mSession != null) {
            mSession.disconnect();
        }

        Intent rateActivity = new Intent(BlindVideoActivity.this, BlindRateActivity.class);
        rateActivity.putExtra("helperId", helperId);
        rateActivity.putExtra("helperName", helperName);
        startActivity(rateActivity);
        finish();
    }

    private void send(String event, String data) {
        Log.d(TAG, event + " - " + data);
        if (mSession != null) {// && helperConnection != null) {
            mSession.sendSignal(event, data);//, helperConnection);
        }
    }

    private void sendLocation(Location location) {
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());
        if (mSession != null) {
            mSession.sendSignal("location", latitude + "," + longitude);
        }
    }

    /* Google Map Api */
    /**
     * Set location request parameters
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Build Google API Client for use to get current location
     * @param context
     */
    private synchronized void buildGoogleApiClient(Context context) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        if (mGoogleApiClient == null) {
            buildGoogleApiClient(this);
        }
        else if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        //only when current fragment is being viewed and location permission is granted
        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            Log.d(TAG, "Location is updating...");
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * Start location update when Google API Client is connected
     * @param bundle
     */
    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    /**
     * Reconnects Google API Client when connection is suspended
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "GoogleApiClient connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient connection failed");
    }

    /* LocationListener */
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged is called");
        sendLocation(location);
    }

    /* DestinationListener */
    @Override
    public void onInput(String destination) {
        send("destination", destination);
    }

    @Override
    public void onNeverUse() {
        askForDestination = false;
    }

    /* SessionListener */
    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "SessionListener.onConnected is called");
        mPublisher = new Publisher(this);
        mPublisher.setPublisherListener(this);
        mSession.publish(mPublisher);
        mPublisher.cycleCamera();
    }

    @Override
    public void onDisconnected(Session session) {
        Log.d(TAG, "SessionListener.onDisconnected is called");
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.d(TAG, "SessionListener.onStreamReceived is called");
        Subscriber subscriber = new Subscriber(this, stream);
        subscriber.setVideoListener(this);
        session.subscribe(subscriber);
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.d(TAG, "SessionListener.onStreamDropped is called");
        quit();
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.d(TAG, "SessionListener.onError: " + opentokError.getMessage());
    }

    /* ConnectionListener */
//    @Override
//    public void onConnectionCreated(Session sessionId, Connection connection) {
//        Log.d(TAG, "ConnectionListener.onConnectionCreated is called");
//    }
//
//    @Override
//    public void onConnectionDestroyed(Session sessionId, Connection connection) {
//        Log.d(TAG, "ConnectionListener.onConnectionDestroyed is called");
//    }

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
    }

    /* SubscriberListener */
    @Override
    public void onConnected(SubscriberKit subscriberKit) {

    }

    @Override
    public void onDisconnected(SubscriberKit subscriberKit) {

    }

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {

    }

    /* VideoListener, */
    @Override
    public void onVideoDataReceived(SubscriberKit subscriberKit) {

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
        switch (event) {
            case "add":
                Log.d(TAG, "Receive add friend request");
//                if (!addButton.isEnabled()) {
//                    break;
//                }
                AlertDialog.Builder builder = new AlertDialog.Builder(BlindVideoActivity.this);
                builder.setMessage(String.format(getResources().getString(R.string.blind_video_add_request_message), helperName));
                builder.setPositiveButton(R.string.blind_video_add_confirm_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MyRequest myRequest = new MyRequest();
                        myRequest.add("m_id", id);
                        myRequest.add("f_id", helperId);
                        myRequest.getJSON("/api/addfriend", new ServerRequest.DataListener() {
                            @Override
                            public void onReceiveData(String data) {
                                try {
                                    JSONObject json = new JSONObject(data);
                                    if (json.getBoolean("res")) {
                                        addButton.setEnabled(false);
                                        addButton.setText(String.format(getResources().getString(R.string.blind_video_add_button_off), helperName));
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
                builder.setNegativeButton(R.string.blind_video_add_cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            default:
                Log.d(TAG, "Unhandled event: " + event + " - " + data);
                break;
        }
    }

    /* ConnectionListener */
    @Override
    public void onConnectionCreated(Session session, Connection connection) {
//        helperConnection = connection;
    }

    @Override
    public void onConnectionDestroyed(Session session, Connection connection) {
//        helperConnection = null;
    }
}
