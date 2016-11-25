package edu.sfsu.geng.newguideme.blind;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import java.util.Arrays;
import java.util.HashSet;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.MyRequest;
import edu.sfsu.geng.newguideme.http.ServerRequest;
import edu.sfsu.geng.newguideme.login.WelcomeActivity;

/**
 * Created by geng on 7/15/16.
 */
public class BlindHomeActivity extends AppCompatActivity
        implements StartCallDialogFragment.StartCallDialogListener,
        SelectFriendDialogFragment.SelectFriendDialogListener
{
    private static final String TAG = "VIHome";

    SharedPreferences pref;
    String id, usernameStr, oldpassStr, newpassStr, desString, rateStr;
    String[] friendIds, friendNames;
    AppCompatButton callBtn, chgpassfrBtn, cancelBtn;
    Dialog dlg;
    AppCompatEditText oldpassEditText, newpassEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blind_home);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        id = pref.getString("id", "");
//        grav = pref.getString("grav", "");
        usernameStr = pref.getString("username", "");
        rateStr = pref.getString("rate", "5.0");
        pref.edit().putBoolean("logged", true).apply();

        setTitle("Hi, " + usernameStr);

        callBtn = (AppCompatButton) findViewById(R.id.call_btn);
        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCall();
            }
        });

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

        // Asyc call to get friends list
        asyncUpdateMyRate();
        asyncGetFriendsList();
    }

    // friends array should not be null after this method
    private void asyncGetFriendsList() {
        MyRequest myRequest = new MyRequest();
        myRequest.add("id", id);

        myRequest.getJSON("/api/getfriendlist", new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        JSONArray friendsJSONArray =  json.getJSONArray("friends");
                        if (friendsJSONArray == null) {
                            Log.d(TAG, "Friend list is Null!");
                        } else {
                            friendIds = new String[friendsJSONArray.length()];
                            friendNames = new String[friendsJSONArray.length()];
                            for (int i = 0; i < friendsJSONArray.length(); i++) {
                                String friendJSON = friendsJSONArray.getString(i);
                                JSONObject friend = new JSONObject(friendJSON);
                                friendIds[i] = friend.getString("id");
                                friendNames[i] = friend.getString("username");
                            }
                            pref.edit().putStringSet("friendIds", new HashSet<>(Arrays.asList(friendIds))).apply();
                            pref.edit().putStringSet("friendNames", new HashSet<>(Arrays.asList(friendNames))).apply();
                            Log.d(TAG, "Friend list: " + Arrays.toString(friendNames));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {}
        });
    }

    private void asyncUpdateMyRate() {
        MyRequest myRequest = new MyRequest();
        myRequest.add("id", id);
        myRequest.getJSON("/api/getrate", new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        String newRate = String.valueOf(json.getDouble("rate"));
                        rateStr = newRate;
                        pref.edit().putString("rate", newRate).apply();
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
    public void onDialogPositiveClick(DialogFragment dialog, String desStr) {
        desString = desStr; // des could be ""
        // if no friends, call strangers.
        if (friendIds == null || friendIds.length == 0) {
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
    public void onDialogPositiveClick(DialogFragment dialog, ArrayList<Integer> selectedFriends) {
        // friends is not null, otherwise the selectFriendDialog will not start
        if (selectedFriends.isEmpty()) {
            callStrangers();
        } else {
            String friendsStr = "";// TODO Use StringBuilder
            for (int i : selectedFriends) {
                if (i < friendIds.length) {
                    friendsStr += ",";
                    friendsStr += friendIds[i];
                }
            }
            callFriends(friendsStr.substring(1));
        }
    }

    @Override
    // friends should not be null. Just in case.
    public String[] getFriends() {
        return friendNames != null ? friendNames : new String[0];
    }


    /* Methods for the Calling */
    private void callStrangers() {
        MyRequest myRequest = new MyRequest();
        myRequest.add("id", id);
        myRequest.add("des", desString);

        myRequest.getJSON("/api/createpublicroom", new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        Intent viWaitActivity = new Intent(BlindHomeActivity.this, BlindWaitActivity.class);
                        startActivity(viWaitActivity);
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

    private void callFriends(String friendsStr) {
        MyRequest myRequest = new MyRequest();
        myRequest.add("id", id);
        myRequest.add("friends", friendsStr);
        myRequest.add("des", desString);

        myRequest.getJSON("/api/callfriendsbyid", new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        Intent viWaitActivity = new Intent(BlindHomeActivity.this, BlindWaitActivity.class);
                        startActivity(viWaitActivity);
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

    /* Methods for the menu button */
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
                dlg = new Dialog(BlindHomeActivity.this);
                dlg.setContentView(R.layout.change_password_frag);
                dlg.setTitle("Change Password");
                chgpassfrBtn = (AppCompatButton) dlg.findViewById(R.id.change_btn);

                chgpassfrBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        oldpassEditText = (AppCompatEditText) dlg.findViewById(R.id.oldpass);
                        newpassEditText = (AppCompatEditText) dlg.findViewById(R.id.newpass);
                        oldpassStr = oldpassEditText.getText().toString();
                        newpassStr = newpassEditText.getText().toString();

                        MyRequest myRequest = new MyRequest();
                        myRequest.add("id", id);
                        myRequest.add("oldpass", oldpassStr);
                        myRequest.add("newpass", newpassStr);
                        myRequest.getJSON("/api/chgpass", new ServerRequest.DataListener() {
                            @Override
                            public void onReceiveData(String data) {
                                try {
                                    JSONObject json = new JSONObject(data);
                                    String jsonStr = json.getString("response");
                                    Toast.makeText(getApplication(), jsonStr, Toast.LENGTH_SHORT).show();
                                    if (json.getBoolean("res")) {
                                        dlg.dismiss();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onClose() {}
                        });
                    }
                });
                cancelBtn = (AppCompatButton) dlg.findViewById(R.id.cancelbtn);
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dlg.dismiss();
                    }
                });
                dlg.show();
                return true;
            case R.id.logout_menu:
                SharedPreferences.Editor edit = pref.edit();
                //Storing Data using SharedPreferences
//                edit.putString("id", "");
                edit.putBoolean("logged", false);
                edit.apply();
                Intent loginactivity = new Intent(BlindHomeActivity.this, WelcomeActivity.class);

                startActivity(loginactivity);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}


