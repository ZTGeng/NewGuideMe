package edu.sfsu.geng.newguideme.helper;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.MyRequest;
import edu.sfsu.geng.newguideme.http.ServerRequest;
import edu.sfsu.geng.newguideme.login.WelcomeActivity;

/**
 * Created by geng on 7/15/16.
 */
public class HelperHomeActivity extends AppCompatActivity implements
        AdapterView.OnItemClickListener,
        ServerRequest.DataListener,
        SwipeRefreshLayout.OnRefreshListener
{

    private static final String TAG = "HelperHome";

    SharedPreferences pref;
    String id, usernameStr, oldpassStr, newpassStr, rateStr;
    String[] friends;
    AppCompatButton chgpassfrBtn, cancelBtn;//, getRoomListBtn;
    Dialog dlg;
    AppCompatEditText oldpassEditText, newpassEditText;
    SwipeRefreshLayout swipeRefreshLayout;

    ListViewCompat roomList;
    RoomListAdapter roomListAdapter;

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
//    private ProgressBar mRegistrationProgressBar;
    private boolean isReceiverRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_home);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        id = pref.getString("id", "");
//        grav = pref.getString("grav", "");
        usernameStr = pref.getString("username", "");
        rateStr = pref.getString("rate", "5.0");
        pref.edit().putBoolean("logged", true).apply();

        setTitle("Hi, " + usernameStr);

        roomList = (ListViewCompat) findViewById(R.id.room_list);
        roomListAdapter = new RoomListAdapter(this, -1, new ArrayList<JSONObject>());
        roomList.setAdapter(roomListAdapter);

        roomList.setOnItemClickListener(this);

//        getRoomListBtn = (AppCompatButton) findViewById(R.id.get_room_list_btn);
//        getRoomListBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                asyncUpdateRooms();
//            }
//        });
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setOnRefreshListener(this);

//        mRegistrationProgressBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
//                mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
//                SharedPreferences sharedPreferences = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
                boolean sentToken = pref.getBoolean(RegistrationIntentService.SENT_TOKEN_TO_SERVER, false);
                if (!sentToken) {
                    Toast.makeText(getApplicationContext(), R.string.token_error_message_short, Toast.LENGTH_SHORT).show();
                }
            }
        };

        // Registering BroadcastReceiver
        registerReceiver();

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

        // Ask for permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.RECORD_AUDIO }, 0);
        }

        asyncUpdateMyRate();
        asyncGetFriendsList();
        asyncUpdateRooms();

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        isReceiverRegistered = false;
        super.onPause();
    }

    /* option menu */
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
                dlg = new Dialog(HelperHomeActivity.this);
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
                                    e.getStackTrace();
                                }
                            }

                            @Override
                            public void onClose() {

                            }
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
                Intent loginactivity = new Intent(HelperHomeActivity.this, WelcomeActivity.class);
                startActivity(loginactivity);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Helper clicks one of the Rooms.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param itemId   The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long itemId) {
        Log.v(TAG, "Helper click on a room");
        final JSONObject room = ((RoomListAdapter) parent.getAdapter()).getItem(position);
        try {
            final String blindId = room.getString("room_id");
            final String blindName = room.getString("username");
            final String myId = id;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(String.format(getResources().getString(R.string.room_confirm_message), blindName));
            builder.setPositiveButton(R.string.room_enter_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    MyRequest myRequest = new MyRequest();
                    myRequest.add("id", myId);
                    myRequest.add("room_id", blindId);
                    myRequest.getJSON("/api/helperjoinroom", new ServerRequest.DataListener() {
                        @Override
                        public void onReceiveData(String data) {
                            try {
                                JSONObject json = new JSONObject(data);
                                if (json.getBoolean("res")) {
                                    Intent helperWaitActivity = new Intent(HelperHomeActivity.this, HelperWaitActivity.class);
                                    helperWaitActivity.putExtra("blindId", blindId);
                                    helperWaitActivity.putExtra("blindName", blindName);
                                    startActivity(helperWaitActivity);
                                    finish();
                                } else {
                                    Toast.makeText(getApplication(), R.string.helper_home_join_error, Toast.LENGTH_SHORT).show();
                                    asyncUpdateRooms();
                                }
                            } catch (JSONException e) {
                                e.getStackTrace();
                                Toast.makeText(getApplication(), R.string.helper_home_call_error, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onClose() {}
                    });
                }
            });
            builder.setNegativeButton(R.string.room_cancel_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void asyncUpdateRooms() {
        MyRequest myRequest = new MyRequest();
        myRequest.add("id", id);
        myRequest.getJSON("/api/getroomlist", this);
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
                            friends = new String[friendsJSONArray.length()];
                            for (int i = 0; i < friendsJSONArray.length(); i++) {
                                String friendJSON = friendsJSONArray.getString(i);
                                JSONObject friend = new JSONObject(friendJSON);
                                friends[i] = friend.getString("id");
                            }
                            pref.edit().putStringSet("friends", new HashSet<>(Arrays.asList(friends))).apply();
                            Log.d(TAG, "Friend list: " + Arrays.toString(friends));
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

    /* OnRefreshListener */
    @Override
    public void onRefresh() {
        asyncUpdateRooms();
        swipeRefreshLayout.setRefreshing(false);
    }

    /* DataListener */
    @Override
    public void onReceiveData(String data) {
        if (data.isEmpty()) return;

        roomListAdapter.clear();
        try {
            JSONObject roomList = new JSONObject(data);
            int size = roomList.getInt("size");
            if (size == 0) {
                Toast.makeText(getApplication(), R.string.empty_list, Toast.LENGTH_LONG).show();
                return;
            }
            JSONArray listJson = new JSONArray(roomList.getString("list"));
            for (int i = 0; i < size; i++) {
                roomListAdapter.add(listJson.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose() {}


    private class RoomListAdapter extends ArrayAdapter<JSONObject> {

        private final Context context;
        private final List<JSONObject> rooms;

        /**
         * Constructor
         *
         * @param context  The current context.
         * @param resource The resource ID for a layout file containing a TextView to use when
         *                 instantiating views.
         * @param objects  The objects to represent in the ListView.
         */
        public RoomListAdapter(Context context, int resource, List<JSONObject> objects) {
            super(context, resource, objects);
            this.context = context;
            this.rooms = objects;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.room_list_item, parent, false);
            AppCompatTextView usernameText = (AppCompatTextView) rowView.findViewById(R.id.room_item_username);
            RatingBar ratingBar = (RatingBar) rowView.findViewById(R.id.room_item_ratingBar);
            AppCompatTextView desText = (AppCompatTextView) rowView.findViewById(R.id.room_item_des);

            try {
                JSONObject room = rooms.get(position);
                usernameText.setText(room.getString("username"));
                desText.setText(room.getString("des"));
                ratingBar.setRating((float) room.getDouble("rate"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return rowView;
        }
    }

    private void registerReceiver(){
        if(!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter(RegistrationIntentService.REGISTRATION_COMPLETE));
            isReceiverRegistered = true;
        }
    }
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
            }
            return false;
        }
        return true;
    }

}
