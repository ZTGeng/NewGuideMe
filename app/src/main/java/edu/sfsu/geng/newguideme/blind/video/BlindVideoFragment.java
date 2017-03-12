package edu.sfsu.geng.newguideme.blind.video;

import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment;
import com.google.android.gms.maps.model.LatLng;
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

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.ServerApi;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 */
public class BlindVideoFragment extends Fragment implements
        Session.SessionListener,
        PublisherKit.PublisherListener,
        Subscriber.VideoListener,
        Session.SignalListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{

    private static final String TAG = "BlindVideo";

    private static final int UPDATE_INTERVAL = 2000;
    private static final int FASTEST_UPDATE_INTERVAL = 1000;

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private AppCompatButton addFriendButton;

    private String token, videoSession, videoToken, helperId, helperName;
    private boolean closeAddFriendButton = false;

    private Listener listener;


    public BlindVideoFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        token = bundle.getString("token");
        videoSession = bundle.getString("videoSession");
        videoToken = bundle.getString("videoToken");
        helperId = bundle.getString("helperId");
        helperName = bundle.getString("helperName");

        View view = inflater.inflate(R.layout.fragment_blind_video, container, false);

        SupportPlaceAutocompleteFragment autocompleteFragment = (SupportPlaceAutocompleteFragment)
//                getActivity().getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
                getChildFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setHint(getString(R.string.blind_video_destination_hint));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.i(TAG, "Place: " + place.getName());
                LatLng latLng = place.getLatLng();
                send("destination", String.valueOf(latLng.latitude) + "," + String.valueOf(latLng.longitude) + "," + place.getName());
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "Error when inputting address: " + status);
                Toast.makeText(getContext(), R.string.blind_video_destination_error, Toast.LENGTH_SHORT).show();
            }
        });

        // buttons
        AppCompatButton quitButton = (AppCompatButton) view.findViewById(R.id.blind_video_quit_btn);
        if (quitButton != null) {
            quitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Quit button onClicked");
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setMessage(R.string.blind_video_quit_confirm_message);
                    builder.setPositiveButton(R.string.blind_video_quit_confirm_button,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    quit();
                                }
                            });
                    builder.setNegativeButton(R.string.blind_video_quit_cancel_button,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {}
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        }

        addFriendButton = (AppCompatButton) view.findViewById(R.id.blind_video_add_btn);
        if (closeAddFriendButton) {
            addFriendButton.setEnabled(false);
            addFriendButton.setText(String.format(getString(R.string.blind_video_add_button_off), helperName));
        }
        addFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(String.format(getString(R.string.blind_video_add_confirm_message), helperName));
                builder.setPositiveButton(R.string.blind_video_add_confirm_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        send("add", "");
                        addFriendButton.setEnabled(false);
                        addFriendButton.setText(String.format(getString(R.string.blind_video_add_button_off), helperName));
                    }
                });
                builder.setNegativeButton(R.string.blind_video_add_cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        ToggleButton muteToggle = (ToggleButton) view.findViewById(R.id.blind_video_mute_toggle);
        if (muteToggle != null) {
            muteToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mPublisher.setPublishAudio(!isChecked);
                }
            });
        }

        ToggleButton navigationToggle = (ToggleButton) view.findViewById(R.id.blind_video_navigation_toggle);
        if (navigationToggle != null) {
            navigationToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        startLocationUpdates();
                    } else {
                        stopLocationUpdates();
                    }
                }
            });
        }

        // Prepare location update
        createLocationRequest();

        // video session
        createSession();

        return view;
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        if (isRemoving()) {
            disconnectSession();
        }
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");

        disconnectSession();
        stopLocationUpdates();

        super.onDetach();
    }

    void closeAddFriendbutton() {
        closeAddFriendButton = true;
    }

    void setListener(@NonNull Listener listener) {
        this.listener = listener;
    }

    private void createSession() {
        mSession = new Session(getContext(), Config.APIKEY, videoSession);
        mSession.setSessionListener(this);
        mSession.setSignalListener(this);
        mSession.connect(videoToken);
    }

    private void quit() {
        listener.onQuitVideo(helperId, helperName);
    }

    private void send(String event, String data) {
        Log.d(TAG, event + " - " + data);
        if (mSession != null) {
            mSession.sendSignal(event, data);
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
        startLocationUpdates();
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        if (mGoogleApiClient == null) {
            buildGoogleApiClient(getContext());
            return;
        }
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
            return;
        }
        //noinspection MissingPermission
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        Log.d(TAG, "Location is updating...");
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
        Log.d(TAG, "onConnected");
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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient connection failed");
    }

    /* LocationListener */
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged is called");
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());
        if (mSession != null) {
            mSession.sendSignal("location", latitude + "," + longitude);
        }
    }

    private void disconnectSession() {
        if (mSession == null) {
            return;
        }

        if (mSubscriber != null) {
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
        mPublisher = new Publisher(getContext(), "blind to helper", true, true);
        mPublisher.setPublisherListener(this);
        mSession.publish(mPublisher);
        mPublisher.cycleCamera();
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
        mSubscriber = new Subscriber(getContext(), stream);
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

    /* VideoListener, */
    @Override
    public void onVideoDataReceived(SubscriberKit subscriberKit) {
        Log.d(TAG, "VideoListener.onVideoDataReceived");
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

    /* SignalListener */
    @Override
    public void onSignalReceived(Session session, String event, String data, Connection connection) {
        if (mSession.getConnection().equals(connection)) return;
        switch (event) {
            case "add":
                Log.d(TAG, "Receive add friend request");
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(String.format(getResources().getString(R.string.blind_video_add_request_message), helperName));
                builder.setPositiveButton(R.string.blind_video_add_confirm_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ServerApi.addFriend(token, helperId, new ServerApi.DataListener() {
                            @Override
                            public void onReceiveData(String data) {
                                try {
                                    JSONObject json = new JSONObject(data);
                                    if (json.getBoolean("res")) {
                                        addFriendButton.setEnabled(false);
                                        addFriendButton.setText(String.format(getResources().getString(R.string.blind_video_add_button_off), helperName));
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

    interface Listener {
        void onQuitVideo(@NonNull String helperId, @NonNull String helperName);
    }

}
