package edu.sfsu.geng.newguideme.blind;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.http.ServerRequest;
import edu.sfsu.geng.newguideme.login.WelcomeActivity;
import edu.sfsu.geng.newguideme.utils.ErrorCleanTextWatcher;

/**
 * Created by geng on 7/15/16.
 */
public class BlindHomeActivity extends AppCompatActivity
        implements StartCallDialogFragment.StartCallDialogListener,
        SelectFriendDialogFragment.SelectFriendDialogListener
{
    private static final String TAG = "VIHome";

    private SharedPreferences pref;
    private String token, description;
    private HashSet<String> friendIds, friendNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blind_home);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        token = pref.getString("token", "");
        String username = pref.getString("username", "");
        pref.edit().putBoolean("logged", true).apply();

        setTitle("Hi, " + username);

        AppCompatButton callButton = (AppCompatButton) findViewById(R.id.call_btn);
        if (callButton != null) {
            callButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startCall();
                }
            });
        }

        // Ask for permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION

            }, 0);
        }

        friendIds = new HashSet<>();
        friendNames = new HashSet<>();

        asyncGetFriendsList();
    }

    // friends array should not be null after this method
    private void asyncGetFriendsList() {
        ServerApi.getFriends(token, new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        JSONArray friendsJSONArray =  json.getJSONArray("friends");
                        for (int i = 0; i < friendsJSONArray.length(); i++) {
                            String friendJSON = friendsJSONArray.getString(i);
                            JSONObject friend = new JSONObject(friendJSON);
                            friendIds.add(friend.getString("token"));
                            friendNames.add(friend.getString("username"));
                        }
                        pref.edit().putStringSet("friendIds", friendIds)
                                .putStringSet("friendNames", friendNames)
                                .apply();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {}
        });
    }

    /* Methods for the StartCallDialog */
    private void startCall() {
        StartCallDialogFragment dialogFragment = new StartCallDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), "StartCallDialogFragment");
    }

    @Override
    public void onDescriptionInput(String description) {
        this.description = description;
        // if no friends, call strangers.
        if (friendIds == null || friendIds.isEmpty()) {
            callStrangers();
        } else {
            selectFriend();
        }
    }

    /* Methods for the SelectFriendDialog */
    private void selectFriend() {
        SelectFriendDialogFragment friendDialogFragment = new SelectFriendDialogFragment();
        friendDialogFragment.show(getSupportFragmentManager(), "SelectFriendDialogFragment");
    }

    @Override
    public void onFriendSelect(ArrayList<String> selectedFriends) {
        // friends is not null, otherwise the selectFriendDialog will not start
        if (selectedFriends.isEmpty()) {
            callStrangers();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            for (String friend : selectedFriends) {
                stringBuilder.append(friend).append(",");
            }
            callFriends(stringBuilder.substring(0, stringBuilder.length() - 1));
        }
    }

    // friends should not be null. Just in case.
    @Override
    public Set<String> getFriends() {
        return friendNames != null ? friendNames : new HashSet<String>();
    }


    /* Methods for the Calling */
    private void callStrangers() {
        ServerApi.createPublicRoom(token, description, new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        Intent blindWaitActivity = new Intent(BlindHomeActivity.this, BlindWaitActivity.class);
                        startActivity(blindWaitActivity);
                        finish();
                    } else {
                        Toast.makeText(BlindHomeActivity.this, R.string.vi_home_call_error, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(BlindHomeActivity.this, R.string.vi_home_call_error, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {}
        });
    }

    private void callFriends(String friends) {
        ServerApi.callFriendsById(token, friends, description, new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        Intent blindWaitActivity = new Intent(BlindHomeActivity.this, BlindWaitActivity.class);
                        startActivity(blindWaitActivity);
                        finish();
                    } else {
                        Log.e(TAG, "Fail to call friends!");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {}
        });
    }

    /* 3 bars menu button */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.change_password_menu:
                Dialog changePasswordDialog = createChangePasswordDialog();
                changePasswordDialog.show();
                return true;
            case R.id.logout_menu:
                pref.edit().putBoolean("logged", false).apply();

                Intent welcomeActivity = new Intent(BlindHomeActivity.this, WelcomeActivity.class);
                startActivity(welcomeActivity);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @NonNull
    private Dialog createChangePasswordDialog() {
        final Dialog changePasswordDialog = new Dialog(BlindHomeActivity.this);
        changePasswordDialog.setContentView(R.layout.dialog_change_password);
        changePasswordDialog.setTitle("Change Password");

        final AppCompatEditText oldPasswordEditText = (AppCompatEditText) changePasswordDialog
                .findViewById(R.id.change_password_old_edittext);
        final TextInputLayout oldPasswordInputLayout = (TextInputLayout) changePasswordDialog
                .findViewById(R.id.change_password_old_inputlayout);
        if (oldPasswordEditText != null && oldPasswordInputLayout != null) {
            oldPasswordEditText.addTextChangedListener(new ErrorCleanTextWatcher(oldPasswordInputLayout));
        }

        final AppCompatEditText newPasswordEditText = (AppCompatEditText) changePasswordDialog
                .findViewById(R.id.change_password_new_edittext);
        final TextInputLayout newPasswordInputLayout = (TextInputLayout) changePasswordDialog
                .findViewById(R.id.change_password_new_inputlayout);
        if (newPasswordEditText != null && newPasswordInputLayout != null) {
            newPasswordEditText.addTextChangedListener(new ErrorCleanTextWatcher(newPasswordInputLayout));
        }

        AppCompatButton changePasswordButton = (AppCompatButton) changePasswordDialog.findViewById(R.id.change_password_button);
        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (oldPasswordEditText == null
                        || oldPasswordInputLayout == null
                        || newPasswordEditText == null
                        || newPasswordInputLayout == null) {
                    return;
                }

                String oldPassword = oldPasswordEditText.getText().toString();
                if (oldPassword.isEmpty()) {
                    oldPasswordInputLayout.setError(getString(R.string.login_password_empty_error));
                    return;
                }
                String newPassword = newPasswordEditText.getText().toString();
                if (newPassword.isEmpty()) {
                    newPasswordInputLayout.setError(getString(R.string.login_password_empty_error));
                    return;
                }

                ServerApi.changePassword(token, oldPassword, newPassword, new ServerRequest.DataListener() {
                    @Override
                    public void onReceiveData(String data) {
                        try {
                            JSONObject json = new JSONObject(data);
                            String message = json.getString("response");

                            if (json.getBoolean("res")) {
                                changePasswordDialog.dismiss();
                                Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String field = json.getString("field");
                            switch (field) {
                                case "old_password":
                                    oldPasswordInputLayout.setError(message);
                                    break;
                                case "new_password":
                                    newPasswordInputLayout.setError(message);
                                    break;
                                default:
                                    Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.getStackTrace();
                        }
                    }

                    @Override
                    public void onClose() {}
                });
            }
        });

        AppCompatButton cancelButton = (AppCompatButton) changePasswordDialog.findViewById(R.id.change_password_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changePasswordDialog.dismiss();
            }
        });

        return changePasswordDialog;
    }


}


