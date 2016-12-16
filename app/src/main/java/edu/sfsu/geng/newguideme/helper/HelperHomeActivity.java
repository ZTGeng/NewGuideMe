package edu.sfsu.geng.newguideme.helper;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.http.ServerRequest;
import edu.sfsu.geng.newguideme.utils.ErrorCleanTextWatcher;
import edu.sfsu.geng.newguideme.login.WelcomeActivity;

/**
 * Created by geng on 7/15/16.
 */
public class HelperHomeActivity extends AppCompatActivity implements
        AdapterView.OnItemClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "HelperHome";

    private SharedPreferences pref;
    private String token;
    private SwipeRefreshLayout swipeRefreshLayout;

    private RoomListAdapter roomListAdapter;

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private boolean isReceiverRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_home);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        token = pref.getString("token", "");
        String username = pref.getString("username", "");
        pref.edit().putBoolean("logged", true).apply();

        setTitle("Hi, " + username);

        ListViewCompat roomList = (ListViewCompat) findViewById(R.id.room_list);
        roomListAdapter = new RoomListAdapter(this, -1, new ArrayList<JSONObject>());
        if (roomList != null) {
            roomList.setAdapter(roomListAdapter);
            roomList.setOnItemClickListener(this);
        }

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setOnRefreshListener(this);

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean sentToken = pref.getBoolean(RegistrationIntentService.SENT_TOKEN_TO_SERVER, false);
                if (!sentToken) {
                    Toast.makeText(getApplicationContext(), R.string.token_error_message_short, Toast.LENGTH_SHORT).show();
                }
            }
        };

        final TextInputLayout inviteCodeInputLayout = (TextInputLayout) findViewById(R.id.invite_code_inputlayout);
        final TextInputEditText inviteCodeEditText = (TextInputEditText) findViewById(R.id.invite_code_edittext);
        inviteCodeEditText.addTextChangedListener(new ErrorCleanTextWatcher(inviteCodeInputLayout));

        final AppCompatButton addFriendButton = (AppCompatButton) findViewById(R.id.helper_home_add_friend_button);
        addFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inviteCode = inviteCodeEditText.getText().toString();
                if (inviteCode.isEmpty()) {
                    inviteCodeInputLayout.setError(getString(R.string.invite_code_empty_error));
                    return;
                }
                ServerApi.addFriendByCode(token, inviteCode, new ServerRequest.DataListener() {
                    @Override
                    public void onReceiveData(String data) {
                        try {
                            JSONObject json = new JSONObject(data);
                            if (json.getBoolean("res")) {
                                Toast.makeText(HelperHomeActivity.this, json.getString("response"), Toast.LENGTH_SHORT).show();
                                inviteCodeEditText.setText("");
                                inviteCodeEditText.clearFocus();
                                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                                        .hideSoftInputFromWindow(inviteCodeEditText.getWindowToken(), 0);
                            } else {
                                inviteCodeInputLayout.setError(json.getString("response"));
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

        inviteCodeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addFriendButton.performClick();
                    return true;
                }
                return false;
            }
        });

        // Registering BroadcastReceiver
        registerReceiver();

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

        asyncUpdateMyRate();
        asyncGetFriendsList();
        asyncUpdateRooms();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // If come from GCM notify, join room directly.
        String roomId = intent.getStringExtra("roomId");
        String blindName = intent.getStringExtra("blindName");
        if (roomId != null && blindName != null) {
            tryJoinRoom(roomId, blindName);
        }
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

    /* 3 bars option menu */
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

                Intent welcomeActivity = new Intent(HelperHomeActivity.this, WelcomeActivity.class);
                startActivity(welcomeActivity);
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
     * @param itemId   The row token of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long itemId) {
        Log.v(TAG, "Helper click on a room");
        final JSONObject room = ((RoomListAdapter) parent.getAdapter()).getItem(position);
        if (room == null) {
            return;
        }

        try {
            final String roomId = room.getString("room_id");
            final String blindName = room.getString("username");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(String.format(getResources().getString(R.string.room_confirm_message), blindName));
            builder.setPositiveButton(R.string.room_enter_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    tryJoinRoom(roomId, blindName);
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

    /* OnRefreshListener */
    @Override
    public void onRefresh() {
        asyncUpdateRooms();
        swipeRefreshLayout.setRefreshing(false);
    }

    private void tryJoinRoom(final String roomId, final String blindName) {
        ServerApi.helperJoinRoom(token, roomId, new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        Intent helperWaitActivity = new Intent(HelperHomeActivity.this, HelperVideoActivity.class);
                        helperWaitActivity.putExtra("roomId", roomId);
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

    /* private methods */
    private void asyncUpdateRooms() {
        ServerApi.getRoomList(token, new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                roomListAdapter.clear();
                try {
                    JSONObject roomList = new JSONObject(data);
                    int size = roomList.getInt("size");
                    if (size == 0) {
                        Toast.makeText(getApplicationContext(), R.string.empty_list, Toast.LENGTH_SHORT).show();
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
        });
    }

    private void asyncUpdateMyRate() {
        ServerApi.getRate(token, new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        String newRate = String.valueOf(json.getDouble("rate"));
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
        ServerApi.getFriends(token, new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        JSONArray friendsJSONArray =  json.getJSONArray("friends");
                        HashSet<String> friendIds = new HashSet<>();
                        HashSet<String> friendNames = new HashSet<>();
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

    @NonNull
    private Dialog createChangePasswordDialog() {
        final Dialog changePasswordDialog = new Dialog(HelperHomeActivity.this);
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

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.room_list_item, parent, false);
            AppCompatTextView usernameText = (AppCompatTextView) rowView.findViewById(R.id.room_item_username);
            AppCompatTextView desText = (AppCompatTextView) rowView.findViewById(R.id.room_item_des);

            try {
                JSONObject room = rooms.get(position);
                usernameText.setText(room.getString("username"));
                desText.setText(room.getString("des"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return rowView;
        }
    }
}
