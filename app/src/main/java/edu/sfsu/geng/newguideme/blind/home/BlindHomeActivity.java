package edu.sfsu.geng.newguideme.blind.home;

import android.Manifest;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import java.util.List;

import edu.sfsu.geng.newguideme.HomeActivity;
import edu.sfsu.geng.newguideme.R;
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
    private AutoCompleteTextView destinationInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blind_home);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        destinationInput = (AutoCompleteTextView) findViewById(R.id.destination_autocomplete);
        destinationInput.setThreshold(1);
        destinationInput.setCompletionHint("Select address from history or input a new one");

        blindHomePresenter = new BlindHomePresenter(this, this);

        AppCompatButton RequestButton = (AppCompatButton) findViewById(R.id.request_button);
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
        destinationInput.setVisibility(View.GONE);
    }

    @Override
    public void showDestinationInput() {
        destinationInput.setVisibility(View.VISIBLE);
    }

    @Override
    public void setDestinationAdapter(ArrayAdapter<String> adapter) {
        destinationInput.setAdapter(adapter);
    }

    @Override
    @AfterPermissionGranted(RC_BLIND_PERMS)
    public void showCallDialog() {
        if (EasyPermissions.hasPermissions(this, BLIND_PERMISSIONS)) {
            final View view = getLayoutInflater().inflate(R.layout.start_call_dialog_layout, null);
            new AlertDialog.Builder(this)
                    .setMessage(R.string.start_call_hint)
                    .setView(view)
                    .setPositiveButton(R.string.start_call_next, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String description =
                                    ((EditText) view.findViewById(R.id.start_call_des_string)).getText().toString();
                            blindHomePresenter.onCall(description);
                        }
                    })
                    .setNegativeButton(R.string.start_call_cancel, null)
                    .create()
                    .show();
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


