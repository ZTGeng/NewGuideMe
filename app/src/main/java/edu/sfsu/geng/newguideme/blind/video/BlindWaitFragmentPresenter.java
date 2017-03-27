package edu.sfsu.geng.newguideme.blind.video;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.utils.PreferencesUtil;

/**
 * Created by gengz on 3/24/17.
 */

public class BlindWaitFragmentPresenter {

    private static final String TAG = "BlindWait";

    @NonNull private Context context;
    @NonNull private Listener listener;
    @NonNull private String token;

    private BlindWaitListener blindWaitListener;

    BlindWaitFragmentPresenter(@NonNull Context context, @NonNull Listener listener) {
        this.context = context;
        this.listener = listener;
        token = PreferencesUtil.getInstance(context).getToken();
    }

    void setBlindWaitListener(@NonNull BlindWaitListener blindWaitListener) {
        this.blindWaitListener = blindWaitListener;
    }

    void onQuitClicked() {
        listener.showQuitDialog();
    }

    void onSelectHelper(@NonNull final String helperName, @NonNull final String helperId) {
        listener.showStartCallDialog(helperName, helperId);
    }

    void wantStartCall(@NonNull final String helperName, @NonNull final String helperId) {
        // call select, go to videoactivity, NO NEED TO delete room
        ServerApi.selectHelper(token, helperId, new ServerApi.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        String videoSession = json.getString("session");
                        String videoToken = json.getString("token");

                        blindWaitListener.onSelectHelper(videoSession, videoToken, helperId, helperName);
                    } else {
                        Toast.makeText(context, R.string.vi_wait_call_error, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(context, R.string.vi_wait_call_error, Toast.LENGTH_SHORT).show();
                    e.getStackTrace();
                }
            }

            @Override
            public void onClose() {}
        });
    }

    void wantQuit() {
        ServerApi.blindDeleteRoom(token, new ServerApi.DataListener() {
            @Override
            public void onReceiveData(String data) {}

            @Override
            public void onClose() {}
        });
        blindWaitListener.onQuitWaiting();
    }

    void keepResAlive() {
        ServerApi.blindKeepAlive(token, new ServerApi.DataListener() {
            @Override
            public void onReceiveData(String helperListJSON) {
                listener.refresh(helperListJSON);
            }

            @Override
            public void onClose() {
                Log.d(TAG, "blind user response is closed");
            }
        });
    }

    interface Listener {
        void showStartCallDialog(@NonNull final String helperName, @NonNull final String helperId);
        void showQuitDialog();
        void refresh(@NonNull String helperListJSON);
    }

    interface BlindWaitListener {
        void onSelectHelper(@NonNull String videoSession,
                @NonNull String videoToken,
                @NonNull String helperId,
                @NonNull String helperName);
        void onQuitWaiting();
    }
}
