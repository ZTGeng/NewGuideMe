package edu.sfsu.geng.newguideme.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import edu.sfsu.geng.newguideme.HomeActivity;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.utils.PreferencesUtil;

/**
 * Created by geng on 7/15/16.
 */
public class HelperHomeActivity extends HomeActivity implements
        HelperHomePresenter.Listener,
        AdapterView.OnItemClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "HelperHome";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RoomListAdapter roomListAdapter;

    private AppCompatTextView emptyListTextView;
//    private ProgressBar roomRefreshProgress;

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private boolean isReceiverRegistered;

    HelperHomePresenter helperHomePresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_home);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        helperHomePresenter = new HelperHomePresenter(this, this);

        ListViewCompat roomList = (ListViewCompat) findViewById(R.id.room_list);
        roomListAdapter = new RoomListAdapter(this, -1, new ArrayList<JSONObject>());
        if (roomList != null) {
            roomList.setAdapter(roomListAdapter);
            roomList.setOnItemClickListener(this);
        }

        emptyListTextView = (AppCompatTextView) findViewById(R.id.helper_home_empty_list_textview);
//        roomRefreshProgress = (ProgressBar) findViewById(R.id.helper_home_refresh_progress);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this);
        }

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean sentToken = PreferencesUtil.getInstance(HelperHomeActivity.this).isTokenSent();
                if (!sentToken) {
                    Toast.makeText(getApplicationContext(), R.string.token_error_message_short, Toast.LENGTH_SHORT).show();
                }
            }
        };

//        final TextInputLayout inviteCodeInputLayout = (TextInputLayout) findViewById(R.id.invite_code_inputlayout);
//        final TextInputEditText inviteCodeEditText = (TextInputEditText) findViewById(R.id.invite_code_edittext);
//        inviteCodeEditText.addTextChangedListener(new ErrorCleanTextWatcher(inviteCodeInputLayout));

//        final AppCompatButton addFriendButton = (AppCompatButton) findViewById(R.id.helper_home_add_friend_button);
//        addFriendButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String inviteCode = inviteCodeEditText.getText().toString();
//                if (inviteCode.isEmpty()) {
//                    inviteCodeInputLayout.setError(getString(R.string.invite_code_empty_error));
//                    return;
//                }
//                ServerApi.addFriendByCode(token, inviteCode, new ServerApi.DataListener() {
//                    @Override
//                    public void onReceiveData(String data) {
//                        try {
//                            JSONObject json = new JSONObject(data);
//                            if (json.getBoolean("res")) {
//                                Toast.makeText(HelperHomeActivity.this, json.getString("response"), Toast.LENGTH_SHORT).show();
//                                inviteCodeEditText.setText("");
//                                inviteCodeEditText.clearFocus();
//                                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
//                                        .hideSoftInputFromWindow(inviteCodeEditText.getWindowToken(), 0);
//                            } else {
//                                inviteCodeInputLayout.setError(json.getString("response"));
//                            }
//                        } catch (JSONException e) {
//                            e.getStackTrace();
//                        }
//                    }
//
//                    @Override
//                    public void onClose() {}
//                });
//            }
//        });
//
//        inviteCodeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
//                if (actionId == EditorInfo.IME_ACTION_DONE) {
//                    addFriendButton.performClick();
//                    return true;
//                }
//                return false;
//            }
//        });

//        AppCompatImageButton closeKeyboardButton = (AppCompatImageButton) findViewById(R.id
//                .helper_home_close_keyboard_button);
//        closeKeyboardButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//                imm.hideSoftInputFromWindow(inviteCodeEditText.getWindowToken(), 0);
//            }
//        });

        // Registering BroadcastReceiver
//        registerReceiver();

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

        helperHomePresenter.onCreate();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // If come from GCM notify, join room directly.
        String roomId = intent.getStringExtra("roomId");
        String blindName = intent.getStringExtra("blindName");
        if (roomId != null && blindName != null) {
            helperHomePresenter.onJoinRoom(blindName, roomId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        helperHomePresenter.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        unregisterReceiver();
        helperHomePresenter.onPause();
        super.onPause();
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
        final JSONObject room = ((RoomListAdapter) parent.getAdapter()).getItem(position);
        if (room != null) {
            helperHomePresenter.onRoomClicked(room);
        }
    }

    @Override
    public void updateRoomList(@NonNull String data) {
        roomListAdapter.clear();
        try {
            JSONObject roomList = new JSONObject(data);
            int size = roomList.getInt("size");
            if (size == 0) {
                emptyListTextView.setVisibility(View.VISIBLE);
                return;
            }
            emptyListTextView.setVisibility(View.INVISIBLE);
            JSONArray listJson = new JSONArray(roomList.getString("list"));
            for (int i = 0; i < size; i++) {
                roomListAdapter.add(listJson.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showJoinDialog(@NonNull final String blindName, @NonNull final String roomId) {
        new AlertDialog.Builder(this)
                .setMessage(String.format(getResources().getString(R.string.room_confirm_message), blindName))
                .setPositiveButton(R.string.room_enter_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        helperHomePresenter.onJoinRoom(blindName, roomId);
                    }
                })
                .setNegativeButton(R.string.room_cancel_button, null)
                .create()
                .show();
    }

    @Override
    public void wantFinishActivity() {
        finish();
    }

    /* OnRefreshListener */
    @Override
    public void onRefresh() {
        helperHomePresenter.refreshRooms();
        swipeRefreshLayout.setRefreshing(false);
    }

    private void registerReceiver() {
        if(!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter(RegistrationIntentService.REGISTRATION_COMPLETE));
            isReceiverRegistered = true;
        }
    }

    private void unregisterReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        isReceiverRegistered = false;
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
