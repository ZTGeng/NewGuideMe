package edu.sfsu.geng.newguideme.helper.video;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

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

import java.util.List;

import edu.sfsu.geng.newguideme.R;
import pub.devrel.easypermissions.AppSettingsDialog;

/**
 * A placeholder fragment containing a simple view.
 */
public class HelperVideoFragment extends Fragment implements
        HelperVideoFragmentPresenter.Listener {

    private static final String TAG = "HelperVideoFragment";

    private static final int RC_SETTINGS_SCREEN_PERM = 123;
//    private static final int RC_AUDIO_APP_PERM = 125;

    private static final int MUTE_COLOR = Color.GRAY;
    @ColorInt private int unmuteColor;

    private GoogleMap mMap;
    private LatLng curLatLngTemp;
    private LatLng destLatLngTemp;
    private String destNameTemp;
    private Marker currentMarker, destinationMarker;
    private Polyline polyLine;

    private FloatingActionButton addFriendButton;
    private FloatingActionButton muteButton;

    private String blindName;

    private Listener listener;

    private AppCompatSeekBar mapRadioSeekBar;
    private LinearLayoutCompat mapLayout;
    private LinearLayoutCompat screenLayout;

    private HelperVideoFragmentPresenter presenter;

    public HelperVideoFragment() {}

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        presenter = new HelperVideoFragmentPresenter(this, this);

        Bundle bundle = getArguments();
        blindName = bundle.getString("blindName");

        View view = inflater.inflate(R.layout.fragment_helper_video, container, false);

        screenLayout = (LinearLayoutCompat) view.findViewById(R.id.helper_video_screen);
        mapLayout = (LinearLayoutCompat) view.findViewById(R.id.helper_video_map);
        initMap();

        mapRadioSeekBar = (AppCompatSeekBar) view.findViewById(R.id.map_radio_seekbar);
        mapRadioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    presenter.onMapRadioBarChanged(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
        unmuteColor = typedValue.data;

        muteButton = (FloatingActionButton) view.findViewById(R.id.helper_video_mute_button);
        muteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onMuteButtonClicked();
            }
        });

        addFriendButton = (FloatingActionButton) view.findViewById(R.id.helper_video_add_button);
        addFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onAddFriendButtonClicked();
            }
        });

        // video session
//        requestPermissions();

        presenter.onCreate();

        return view;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        presenter.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        presenter.onPause();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        presenter.onDestroy();
        super.onDestroy();
    }

    void setListener(@NonNull Listener listener) {
        this.listener = listener;
    }

    @Override
    public void setMapRadio(@IntRange(from = 0, to = 4) int mapWeight, @IntRange(from = 0, to = 4) int screenWeight) {
        LinearLayoutCompat.LayoutParams mapParams = (LinearLayoutCompat.LayoutParams) mapLayout.getLayoutParams();
        mapParams.weight = mapWeight;
        mapLayout.setLayoutParams(mapParams);
        LinearLayoutCompat.LayoutParams screenParams = (LinearLayoutCompat.LayoutParams) screenLayout.getLayoutParams();
        screenParams.weight = screenWeight;
        screenLayout.setLayoutParams(screenParams);
    }

    @Override
    public void setMuteButtonEnable(boolean enableAudio) {
        muteButton.setBackgroundTintList(ColorStateList.valueOf(enableAudio ? unmuteColor : MUTE_COLOR));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            muteButton.setImageDrawable(getResources().getDrawable(
                    enableAudio ? R.drawable.ic_mic_white_24dp : R.drawable.ic_mic_off_white_24dp,
                    getContext().getTheme()));
        } else {
            muteButton.setImageDrawable(getResources().getDrawable(
                    enableAudio? R.drawable.ic_mic_white_24dp : R.drawable.ic_mic_off_white_24dp));
        }
    }

    @Override
    public void setAddFriendButtonEnable(boolean showAddFriend) {
        addFriendButton.setVisibility(showAddFriend ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showAddFriendDialog() {
        new AlertDialog.Builder(getContext())
                .setMessage(String.format(getResources().getString(R.string.helper_video_add_confirm_message), blindName))
                .setPositiveButton(R.string.helper_video_add_confirm_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.sendFriendRequest();
                    }
                })
                .setNegativeButton(R.string.helper_video_add_cancel_button, null)
                .create()
                .show();
    }

    @Override
    public void showFriendRequestDialog() {
        new AlertDialog.Builder(getContext())
                .setMessage(String.format(getResources().getString(R.string.helper_video_add_request_message), blindName))
                .setPositiveButton(R.string.helper_video_add_confirm_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.addFriend();
                    }
                })
                .setNegativeButton(R.string.helper_video_add_cancel_button, null)
                .create()
                .show();
    }

    @Override
    public void showPermissionDeniedDialog() {
        new AppSettingsDialog.Builder(this, getString(R.string.rationale_ask_again))
                .setTitle(getString(R.string.title_settings_dialog))
                .setPositiveButton(getString(R.string.setting))
                .setNegativeButton(getString(R.string.cancel), null)
                .setRequestCode(RC_SETTINGS_SCREEN_PERM)
                .build()
                .show();
    }

    private void initMap() {
        if (mapLayout != null) {
            mapLayout.setVisibility(View.VISIBLE);
        }
        SupportMapFragment mapFragment = new SupportMapFragment();
        getActivity().getSupportFragmentManager().beginTransaction().add(R.id.helper_video_map, mapFragment).commit();
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                if (curLatLngTemp != null) {
                    setBlindLocation(curLatLngTemp);
                }
                if (destLatLngTemp != null) {
                    setDestination(destLatLngTemp, destNameTemp);
                }
            }
        });
    }

    @Override
    public void setBlindLocation(@NonNull LatLng location) {
        if (mMap == null) {
            curLatLngTemp = location;
            return;
        }

        if (currentMarker == null) {
            currentMarker = mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_person_pin_black_24dp))
                    .title("User Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(15f));
        } else {
            currentMarker.setPosition(location);
        }
    }

    @Override
    public void setDestination(@NonNull LatLng destination, @NonNull String placeName) {
        if (mMap == null) {
            destLatLngTemp = destination;
            destNameTemp = placeName;
            return;
        }

        if (destinationMarker == null) {
            destinationMarker = mMap.addMarker(new MarkerOptions()
                    .position(destination)
                    .snippet(placeName)
                    .title("Destination"));
            destinationMarker.showInfoWindow();
            mMap.moveCamera(CameraUpdateFactory.newLatLng(destination));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(15f));
        } else {
            destinationMarker.hideInfoWindow();
            destinationMarker.setPosition(destination);
            destinationMarker.setSnippet(placeName);
            destinationMarker.showInfoWindow();
            if (polyLine != null) {
                polyLine.remove();
            }
        }

        if (currentMarker != null) {
            presenter.requestNavigationRoute(currentMarker.getPosition(), destinationMarker.getPosition());
        }
    }

    @Override
    public void drawNavigationRoute(List<LatLng> lines) {
        if (polyLine != null) {
            polyLine.remove();
        }
        polyLine = mMap.addPolyline(new PolylineOptions().addAll(lines).width(5).color(Color.BLUE));
    }

    @Override
    public void addSubscriberView(View view) {
        screenLayout.addView(view);
    }

    @Override
    public void removeSubscriberView(View view) {
        screenLayout.removeView(view);
    }

    @Override
    public void onVideoEnd() {
        listener.onVideoEnd();
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
//    }
//
//    @Override
//    public void onPermissionsGranted(int requestCode, List<String> perms) {
//        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
//    }
//
//    @Override
//    public void onPermissionsDenied(int requestCode, List<String> perms) {
//        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());
//
//        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
//            new AppSettingsDialog.Builder(this, getString(R.string.rationale_ask_again))
//                    .setTitle(getString(R.string.title_settings_dialog))
//                    .setPositiveButton(getString(R.string.setting))
//                    .setNegativeButton(getString(R.string.cancel), null)
//                    .setRequestCode(RC_SETTINGS_SCREEN_PERM)
//                    .build()
//                    .show();
//        }
//    }
//
//    @AfterPermissionGranted(RC_AUDIO_APP_PERM)
//    private void requestPermissions() {
//        String[] perms = { Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO };
//        if (EasyPermissions.hasPermissions(getContext(), perms)) {
//            presenter.initialSession();
//        } else {
//            EasyPermissions.requestPermissions(this, getString(R.string.rationale_audio_app), RC_AUDIO_APP_PERM, perms);
//        }
//    }

    interface Listener {
        void onVideoEnd();
    }
}
