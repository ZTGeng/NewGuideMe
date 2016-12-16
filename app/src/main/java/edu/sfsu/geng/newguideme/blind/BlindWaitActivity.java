package edu.sfsu.geng.newguideme.blind;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.ListViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.http.ServerRequest;

/**
 * Created by geng on 7/16/16.
 */
public class BlindWaitActivity extends AppCompatActivity implements
        AdapterView.OnItemClickListener
{

    private static final String TAG = "VIWait";

    private String token;
    private boolean callFriend;

    private HelperListAdapter helperListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Wait activity create");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blind_wait);

        SharedPreferences pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        token = pref.getString("token", "");
        callFriend = getIntent().getBooleanExtra("callFriend", false);

        ListViewCompat waitingHelperList = (ListViewCompat) findViewById(R.id.waiting_helper_list);
        helperListAdapter = new HelperListAdapter(this, -1, new ArrayList<JSONObject>());
        if (waitingHelperList != null) {
            waitingHelperList.setAdapter(helperListAdapter);
            waitingHelperList.setOnItemClickListener(this);
        }

        AppCompatButton quitButton = (AppCompatButton) findViewById(R.id.vi_wait_quit_btn);
        if (quitButton != null) {
            quitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onQuitClicked();
                }
            });
        }

        keepResAlive();
    }

    private void keepResAlive() {
        ServerApi.blindKeepAlive(token, new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String helperListJSON) {
                refresh(helperListJSON);
            }

            @Override
            public void onClose() {
                Log.d(TAG, "blind user response is closed");
            }
        });
    }

    /**
     * When blind user clicks one of the helpers in the list.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row token of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "Blind click on a helper on the list");
        final JSONObject helper = ((HelperListAdapter) parent.getAdapter()).getItem(position);
        if (helper == null) {
            return;
        }

        try {
            final String helperName = helper.getString("username");
            final String helperId = helper.getString("token");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(String.format(getResources().getString(R.string.vi_wait_confirm_accept_helper), helperName));
            builder.setPositiveButton(R.string.vi_wait_accept_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // call select, go to videoactivity, NO NEED TO delete room
                    ServerApi.selectHelper(token, helperId, new ServerRequest.DataListener() {
                        @Override
                        public void onReceiveData(String data) {
                            try {
                                JSONObject json = new JSONObject(data);
                                if (json.getBoolean("res")) {
                                    String videoSession = json.getString("session");
                                    String videoToken = json.getString("token");

                                    Intent videoActivity = new Intent(BlindWaitActivity.this, BlindVideoActivity.class);
                                    videoActivity.putExtra("sessionId", videoSession);
                                    videoActivity.putExtra("videoToken", videoToken);
                                    videoActivity.putExtra("helperId", helperId);
                                    videoActivity.putExtra("helperName", helperName);
                                    videoActivity.putExtra("callFriend", callFriend);
                                    startActivity(videoActivity);
                                    finish();
                                } else {
                                    Toast.makeText(getApplication(), R.string.vi_wait_call_error, Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                Toast.makeText(getApplication(), R.string.vi_wait_call_error, Toast.LENGTH_SHORT).show();
                                e.getStackTrace();
                            }
                        }

                        @Override
                        public void onClose() {}
                    });
                }
            });
            builder.setNegativeButton(R.string.vi_wait_cancel_button, new DialogInterface.OnClickListener() {
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

    /* When a helper join or leave the waiting list, need to refresh it */
    private void refresh(String helperListJson) {
        helperListAdapter.clear();
        JSONArray helperArray;
        try {
            helperArray = new JSONArray(helperListJson);
            for (int i = 0; i < helperArray.length(); i++) {
                helperListAdapter.add(helperArray.getJSONObject(i));
            }
            if (helperArray.length() > 0) {
                Ringtone r = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                r.play();
                Toast.makeText(this, String.format(getResources().getString(R.string.vi_wait_helper_notice), helperArray.length()), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.vi_wait_helper_empty_notice, Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onQuitClicked() {
        Log.d(TAG, "Quit button onClicked");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.vi_wait_quit_confirm_message);
        builder.setPositiveButton(R.string.vi_wait_quit_confirm_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                quit();
            }
        });
        builder.setNegativeButton(R.string.vi_wait_quit_cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void quit() {
        ServerApi.blindDeleteRoom(token, new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {}

            @Override
            public void onClose() {}
        });

        Intent homeActivity = new Intent(BlindWaitActivity.this, BlindHomeActivity.class);
        startActivity(homeActivity);
        finish();
    }

    @Override
    public void onBackPressed() {
        quit();
    }

    private class HelperListAdapter extends ArrayAdapter<JSONObject> {

        private final Context context;
        private final List<JSONObject> helpers;

        /**
         * Constructor
         *
         * @param context  The current context.
         * @param resource The resource ID for a layout file containing a TextView to use when
         *                 instantiating views.
         * @param objects  The objects to represent in the ListView.
         */
        HelperListAdapter(Context context, int resource, List<JSONObject> objects) {
            super(context, resource, objects);
            this.context = context;
            this.helpers = objects;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.helper_list_item, parent, false);
            AppCompatTextView usernameText = (AppCompatTextView) rowView.findViewById(R.id.helper_item_username);
            AppCompatTextView ratingText = (AppCompatTextView) rowView.findViewById(R.id.helper_item_rating);

            try {
                JSONObject helper = helpers.get(position);
                usernameText.setText(helper.getString("username"));
                ratingText.setText(String.valueOf(helper.getDouble("rate")));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return rowView;
        }
    }
}
