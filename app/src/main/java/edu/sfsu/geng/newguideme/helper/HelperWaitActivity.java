package edu.sfsu.geng.newguideme.helper;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.MyRequest;
import edu.sfsu.geng.newguideme.http.ServerRequest;

public class HelperWaitActivity extends AppCompatActivity implements ServerRequest.DataListener{

    private static final String TAG = "HelperWait";

    private SharedPreferences pref;
    private String token, roomId, blindName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_wait);

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        token = pref.getString("token", "");

        roomId = getIntent().getStringExtra("blindId");
        blindName = getIntent().getStringExtra("blindName");

        FloatingActionButton quitBtn = (FloatingActionButton) findViewById(R.id.helper_wait_quit_btn);
        if (quitBtn != null) {
            quitBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onQuitClicked();
                }
            });
        }

        keepResAlive();

    }

    private void keepResAlive() {
        MyRequest myRequest = new MyRequest();
        myRequest.helperJoin(token, roomId, HelperWaitActivity.this);
    }

    private void onQuitClicked() {
        Log.d(TAG, "Quit button onClicked");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.helper_wait_quit_confirm_message);
        builder.setPositiveButton(R.string.helper_wait_quit_confirm_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                quit();
            }
        });
        builder.setNegativeButton(R.string.helper_wait_quit_cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void quit() {
        MyRequest myRequest = new MyRequest();
        myRequest.add("token", token);
        myRequest.add("room_id", roomId);
        myRequest.getJSON("/api/helperleaveroom", null);

        Intent homeActivity = new Intent(HelperWaitActivity.this, HelperHomeActivity.class);
        startActivity(homeActivity);
        finish();
    }

    /* DataListener */
    @Override
    public void onReceiveData(String selectJSON) {
        JSONObject select;
        try {
            select = new JSONObject(selectJSON);
            if (select.getBoolean("select")) {
                String videoSession = select.getString("session");
                String videoToken = select.getString("token");

                Intent videoActivity = new Intent(HelperWaitActivity.this, HelperVideoActivity.class);
                videoActivity.putExtra("session", videoSession);
                videoActivity.putExtra("token", videoToken);
                videoActivity.putExtra("blindId", roomId);
                videoActivity.putExtra("blindName", blindName);
                startActivity(videoActivity);
                finish();
            } else {
                Intent homeActivity = new Intent(HelperWaitActivity.this, HelperHomeActivity.class);
                startActivity(homeActivity);
                finish();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose() {
        Log.d(TAG, "helper response is closed");
    }
}
