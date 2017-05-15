package edu.sfsu.geng.newguideme.blind.home;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import edu.sfsu.geng.newguideme.HomeActivity;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.utils.Destination;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by geng on 7/15/16.
 */
public class BlindHomeActivity extends HomeActivity implements
        BlindHomePresenter.Listener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        EasyPermissions.PermissionCallbacks
{
    private static final String TAG = "VIHome";
    private static final String[] BLIND_PERMISSIONS = {
            Manifest.permission.INTERNET,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};
    private static final int RC_BLIND_PERMS = 150;
    private static final int RC_SETTINGS_SCREEN_PERM = 123;

    private BlindHomePresenter blindHomePresenter;
    private AutoCompleteTextView descriptionInput;
    private TextInputLayout descriptionInputLayout;
    private AppCompatButton RequestButton;
    private LinearLayoutCompat destinationComponent;
    private ListViewCompat destinationHistory;
    private SupportPlaceAutocompleteFragment autocompleteFragment;
    private String destinationString = "navigation:0,0?";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blind_home);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        descriptionInputLayout = (TextInputLayout) findViewById(R.id.destination_inputlayout);
        descriptionInput = (AutoCompleteTextView) findViewById(R.id.destination_autocomplete);
        descriptionInput.setThreshold(1);
//        descriptionInput.setCompletionHint("Select address from history or input a new one");

        destinationComponent = (LinearLayoutCompat) findViewById(R.id.destination_component);
        destinationHistory = (ListViewCompat) findViewById(R.id.destination_history_list);

        autocompleteFragment = (SupportPlaceAutocompleteFragment)
                getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setHint("Input your destination");
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.i(TAG, "Place: " + place.getName());
                LatLng latLng = place.getLatLng();
                Destination destination = new Destination(latLng.latitude, latLng.longitude, place.getName().toString());
                destinationString = destination.toString();
//                send("destination", String.valueOf(latLng.latitude) + "," + String.valueOf(latLng.longitude) + "," + place.getName());
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "Error when inputting address: " + status);
                Toast.makeText(BlindHomeActivity.this, R.string.blind_video_destination_error, Toast.LENGTH_SHORT)
                        .show();
            }
        });

        blindHomePresenter = new BlindHomePresenter(this, this);

        RequestButton = (AppCompatButton) findViewById(R.id.request_button);
        RequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                blindHomePresenter.onRequestButtonClicked();
            }
        });
    }

    public void onCalleeSelect(@NonNull View view) {
        boolean checked = ((AppCompatRadioButton) view).isChecked();
        if (checked) {
            switch (view.getId()) {
                case R.id.call_favorites_radio:
                    blindHomePresenter.setCallFavorites();
                    break;
                case R.id.call_favorites_then_everyone_radio:
                    blindHomePresenter.setCallFavoritesThenEveryone();
                    break;
                case R.id.call_everyone_radio:
                    blindHomePresenter.setCallEveryone();
                    break;
            }
        }
    }

    public void onMapSelect(@NonNull View view) {
        boolean checked = ((AppCompatRadioButton) view).isChecked();
        if (checked) {
            switch (view.getId()) {
                case R.id.call_with_map_radio:
                    blindHomePresenter.setCallWithMap();
                    break;
                case R.id.call_without_map_radio:
                    blindHomePresenter.setCallWithoutMap();
                    break;
            }
        }
    }

    @Override
    public void hideDestinationInput() {
        descriptionInput.setVisibility(View.GONE);
    }

    @Override
    public void showDestinationInput() {
        destinationComponent.setVisibility(View.VISIBLE);
        descriptionInputLayout.setVisibility(View.GONE);
//        descriptionInputLayout.setHint("Input a destination");
    }

    @Override
    public void showDescriptionInput() {
        destinationComponent.setVisibility(View.GONE);
        descriptionInputLayout.setVisibility(View.VISIBLE);
//        descriptionInputLayout.setHint("Describe your need");
    }

    @Override
    public void setRequestButtonText(@StringRes int textId) {
        RequestButton.setText(textId);
    }

    @Override
    public void setDescriptionAdapter(ArrayAdapter<String> adapter) {
        descriptionInput.setAdapter(adapter);
    }

    @Override
    public void setAddressHistoryAdaptor(@NonNull final List<String> addresses) {
        destinationHistory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, addresses));
        destinationHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                destinationHistory.setVisibility(View.GONE);
                autocompleteFragment.setText(addresses.get(position));
            }
        });
    }

    @Override
    @AfterPermissionGranted(RC_BLIND_PERMS)
    public void showCallDialog(@NonNull String message, boolean warning) {
        if (EasyPermissions.hasPermissions(this, BLIND_PERMISSIONS)) {
//            final View view = getLayoutInflater().inflate(R.layout.start_call_dialog_layout, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage(message)
//                    .setMessage(R.string.start_call_hint)
//                    .setView(view)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (descriptionInputLayout.getVisibility() == View.VISIBLE) {
                                String description = descriptionInput.getText().toString();
                                blindHomePresenter.onCall(description);
                            } else {
                                blindHomePresenter.onCall(destinationString);
                            }
                        }
                    })
                    .setNegativeButton(R.string.start_call_cancel, null);
            if (warning) {
                builder.setView(R.layout.warning_dialog_layout);
            }
            builder.create().show();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_all), RC_BLIND_PERMS, BLIND_PERMISSIONS);
        }
    }

    @Override
    public void showWarningDialog() {
        final View view = getLayoutInflater().inflate(R.layout.warning_dialog_layout, null);
        ((AppCompatTextView) view.findViewById(R.id.warning_dialog_text)).setText(R.string.call_strangers_warning);
        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show();
    }

    @Override
    public void canCallEveryoneOnly() {
        AppCompatRadioButton callFriendsRadio = (AppCompatRadioButton) findViewById(R.id.call_favorites_radio);
        callFriendsRadio.setEnabled(false);
        callFriendsRadio.setChecked(false);
        callFriendsRadio.setText(callFriendsRadio.getText() + " (Found no favorites)");
        AppCompatRadioButton callAllRadio = (AppCompatRadioButton) findViewById(R.id.call_favorites_then_everyone_radio);
        callAllRadio.setEnabled(false);
        callAllRadio.setChecked(false);
        callAllRadio.setText(callAllRadio.getText() + " (Found no favorites)");

        ((AppCompatRadioButton) findViewById(R.id.call_everyone_radio)).setChecked(true);
        blindHomePresenter.setCallEveryone();

        Toast.makeText(BlindHomeActivity.this, R.string.no_friends_message_short, Toast.LENGTH_SHORT).show();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

}


