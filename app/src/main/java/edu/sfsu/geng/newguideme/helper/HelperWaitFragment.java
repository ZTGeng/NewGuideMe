package edu.sfsu.geng.newguideme.helper;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.json.JSONException;
import org.json.JSONObject;

import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.http.ServerRequest;

/**
 * A simple {@link Fragment} subclass.
 */
public class HelperWaitFragment extends Fragment {

    private static final String TAG = "HelperWaitFragment";

    private String token, roomId, blindName;
    private Listener listener;

    private ProgressBar progressBar;

    public HelperWaitFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        token = bundle.getString("token");
        roomId = bundle.getString("roomId");
        blindName = bundle.getString("blindName");

        View view = inflater.inflate(R.layout.fragment_helper_wait, container, false);

        progressBar = (ProgressBar) view.findViewById(R.id.helper_wait_progress);

        keepResAlive();
        return view;
    }

    void setListener(@NonNull Listener listener) {
        this.listener = listener;
    }

    private void keepResAlive() {
        ServerApi.helperKeepAlive(token, roomId, new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                progressBar.setVisibility(View.INVISIBLE);
                try {
                    JSONObject select = new JSONObject(data);
                    Log.d(TAG, data);
                    if (select.getBoolean("select")) {
                        String videoSession = select.getString("session");
                        String videoToken = select.getString("token");

                        listener.onSelected(videoSession, videoToken);
                    } else {
                        listener.onNotSelected();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {
                Log.d(TAG, "helper response is closed");
            }
        });
    }

    interface Listener {
        void onSelected(@NonNull String sessionId, @NonNull String videoToken);
        void onNotSelected();
    }
}
