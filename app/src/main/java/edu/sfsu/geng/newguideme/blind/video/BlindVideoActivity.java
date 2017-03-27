package edu.sfsu.geng.newguideme.blind.video;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import java.util.Set;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.blind.home.BlindHomeActivity;

public class BlindVideoActivity extends AppCompatActivity implements
        BlindWaitFragmentPresenter.BlindWaitListener, BlindVideoFragment.Listener, BlindRateFragment.Listener {

    private static final String TAG = "BlindVideoActivity";

    private String token;
    private boolean callFriend;
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blind_video);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        token = pref.getString("token", "");

        callFriend = getIntent().getBooleanExtra("callFriend", false);

        // initial fragment
        if (savedInstanceState == null) {

            Bundle bundle = new Bundle();
            bundle.putString("token", token);

            BlindWaitFragment blindWaitFragment = new BlindWaitFragment();
            blindWaitFragment.setListener(this);
            blindWaitFragment.setArguments(bundle);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.blind_video_fragment, blindWaitFragment)
                    .commit();
        }
    }

    /* BlindWaitFragment */
    @Override
    public void onSelectHelper(@NonNull String videoSession,
            @NonNull String videoToken,
            @NonNull String helperId,
            @NonNull String helperName) {
        Bundle bundle = new Bundle();
        bundle.putString("token", token);
        bundle.putString("videoSession", videoSession);
        bundle.putString("videoToken", videoToken);
        bundle.putString("helperId", helperId);
        bundle.putString("helperName", helperName);

        BlindVideoFragment blindVideoFragment = new BlindVideoFragment();
        blindVideoFragment.setListener(this);
        blindVideoFragment.setArguments(bundle);

        Set<String> friends = pref.getStringSet("friendIds", null);
        if (friends != null && friends.contains(helperId)) {
            blindVideoFragment.closeAddFriendbutton();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.blind_video_fragment, blindVideoFragment)
                .commit();
    }

    @Override
    public void onQuitWaiting() {
        quitToHome();
    }

    /* BlindVideoFragment */
    @Override
    public void onQuitVideo(@NonNull String helperId, @NonNull String helperName) {
        if (callFriend) {
            quitToHome();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("token", token);
            bundle.putString("helperId", helperId);
            bundle.putString("helperName", helperName);

            BlindRateFragment blindRateFragment = new BlindRateFragment();
            blindRateFragment.setListener(this);
            blindRateFragment.setArguments(bundle);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.blind_video_fragment, blindRateFragment)
                    .commit();
        }
    }

    /* BlindRateFragment */
    @Override
    public void onQuitRate() {
        quitToHome();
    }

    private void quitToHome() {
        Intent homeActivity = new Intent(BlindVideoActivity.this, BlindHomeActivity.class);
        startActivity(homeActivity);
        finish();
    }
}
