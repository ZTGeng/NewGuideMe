package edu.sfsu.geng.newguideme.blind.video;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

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

    private boolean secondCall;
    private int secondCallAfter;
    private String description;
    private CountDownTimer countDownTimer;

    private BlindWaitListener blindWaitListener;

    BlindWaitFragmentPresenter(@NonNull Context context, @NonNull Listener listener) {
        this.context = context;
        this.listener = listener;
        token = PreferencesUtil.getInstance(context).getToken();
    }

    void onCreate(@NonNull Bundle bundle) {
        secondCall = bundle.getBoolean("secondCall");
        secondCallAfter = bundle.getInt("secondCallAfter");
        description = bundle.getString("des");

        listener.setCountDownVisibility(secondCall);
        if (secondCall) {
            countDownTimer = new CountDownTimer(secondCallAfter * 60000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int seconds = (int) millisUntilFinished / 1000;
                    listener.setCountDownMessage(String.format(Locale.getDefault(),
                            context.getString(R.string.blind_wait_countdown_message),
                            seconds / 60, seconds % 60));
                }

                @Override
                public void onFinish() {
                    wantCallEveryone();
                }
            };
            countDownTimer.start();
        }
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

    void wantCallEveryone() {
        listener.setCountDownVisibility(false);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        ServerApi.callStrangers(token, description, new ServerApi.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        Toast.makeText(context, "Sending request to everyone.", Toast.LENGTH_SHORT).show();
                    } else {
                        String response = json.getString("response");
                        Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(context, R.string.vi_home_call_error, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {}
        });
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
        void setCountDownVisibility(boolean visible);
        void setCountDownMessage(@NonNull String message);
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
