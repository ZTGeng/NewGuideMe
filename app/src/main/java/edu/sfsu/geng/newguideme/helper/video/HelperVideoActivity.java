package edu.sfsu.geng.newguideme.helper.video;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import java.util.Set;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.helper.home.HelperHomeActivity;
import edu.sfsu.geng.newguideme.http.ServerApi;

public class HelperVideoActivity extends AppCompatActivity
        implements HelperWaitFragment.Listener, HelperVideoFragment.Listener {

    private static final String TAG = "HelperVideo";

    private String token, roomId, blindName, des;
    private boolean isVideoStart, isMap;
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_video);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        token = pref.getString("token", "");

        roomId = getIntent().getStringExtra("roomId");
        blindName = getIntent().getStringExtra("blindName");
        isMap = getIntent().getBooleanExtra("isMap", false);
        des = getIntent().getStringExtra("des");

        isVideoStart = false;

        FloatingActionButton quitButton = (FloatingActionButton) findViewById(R.id.helper_video_quit_button);
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Quit button onClicked");
                AlertDialog.Builder builder = new AlertDialog.Builder(HelperVideoActivity.this);
                builder.setMessage(R.string.helper_video_quit_confirm_message);
                builder.setPositiveButton(R.string.helper_video_quit_confirm_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!isVideoStart) {
                                    ServerApi.helperLeaveRoom(token, roomId, new ServerApi.DataListener() {
                                        @Override
                                        public void onReceiveData(String data) {}

                                        @Override
                                        public void onClose() {}
                                    });
                                }
                                quit();
                            }
                        });
                builder.setNegativeButton(R.string.helper_video_quit_cancel_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        // initial fragment
        if (savedInstanceState == null) {

            Bundle bundle = new Bundle();
            bundle.putString("token", token);
            bundle.putString("roomId", roomId);
            bundle.putString("blindName", blindName);

            HelperWaitFragment helperWaitFragment = new HelperWaitFragment();
            helperWaitFragment.setListener(this);
            helperWaitFragment.setArguments(bundle);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.helper_video_fragment, helperWaitFragment)
                    .commit();
        }
    }

    private void quit() {
        Intent homeActivity = new Intent(HelperVideoActivity.this, HelperHomeActivity.class);
        startActivity(homeActivity);
        finish();
    }

    @Override
    public void onSelected(@NonNull String sessionId, @NonNull String videoToken) {
        String blindId = roomId;

        Bundle bundle = new Bundle();
        bundle.putString("token", token);
        bundle.putString("sessionId", sessionId);
        bundle.putString("videoToken", videoToken);
        bundle.putString("blindId", blindId);
        bundle.putString("blindName", blindName);
        bundle.putString("des", des);
        bundle.putBoolean("isMap", isMap);

        HelperVideoFragment helperVideoFragment = new HelperVideoFragment();
        helperVideoFragment.setListener(this);
        helperVideoFragment.setArguments(bundle);

//        Set<String> friends = pref.getStringSet("friendIds", null);
//        if (friends != null && friends.contains(blindId)) {
//            helperVideoFragment.hideAddFriendButton();
//        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.helper_video_fragment, helperVideoFragment)
                .commit();

        isVideoStart = true;
    }

    @Override
    public void onNotSelected() {
        quit();
    }

    @Override
    public void onVideoEnd() {
        quit();
    }
}
