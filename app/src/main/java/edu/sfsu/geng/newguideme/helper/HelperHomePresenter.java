package edu.sfsu.geng.newguideme.helper;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;

import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.helper.video.HelperVideoActivity;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.utils.PreferencesUtil;

/**
 * Created by gengz on 3/12/17.
 */
class HelperHomePresenter {

    private static final long REFRESH_DELAY = 5000;

    @NonNull Context context;
    @NonNull Listener listener;
    @NonNull String token;

    @NonNull private Handler handler;
    @NonNull private Runnable refreshRunnable;

    private boolean openAutoRefresh;
    private boolean isRefreshing;

    public HelperHomePresenter(@NonNull Context context, @NonNull Listener listener) {
        this.context = context;
        this.listener = listener;

        token = PreferencesUtil.getInstance(context).getToken();
        PreferencesUtil.getInstance(context).setLogin();
    }

    void onCreate() {
        handler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (openAutoRefresh) {
                    asyncUpdateRooms();
                    handler.postDelayed(this, REFRESH_DELAY);
                }
            }
        };
        asyncUpdateMyRate();
        asyncGetFriendsList();
    }

    void onResume() {
        openAutoRefresh = true;
        handler.postDelayed(refreshRunnable, REFRESH_DELAY);
    }

    void onPause() {
        openAutoRefresh = false;
    }

    void refreshRooms() {
        asyncUpdateRooms();
    }

    void onRoomClicked(@NonNull JSONObject room) {
        try {
            final String blindName = room.getString("username");
            final String roomId = room.getString("room_id");
            listener.showJoinDialog(blindName, roomId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void onJoinRoom(@NonNull final String blindName, @NonNull final String roomId) {
        ServerApi.helperJoinRoom(token, roomId, new ServerApi.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        Intent helperWaitActivity = new Intent(context, HelperVideoActivity.class);
                        helperWaitActivity.putExtra("roomId", roomId);
                        helperWaitActivity.putExtra("blindName", blindName);
                        context.startActivity(helperWaitActivity);
                        listener.wantFinishActivity();
                    } else {
                        String response = json.getString("response");
                        Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                        asyncUpdateRooms();
                    }
                } catch (JSONException e) {
                    e.getStackTrace();
                    Toast.makeText(context, R.string.helper_home_call_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onClose() {}
        });
    }

    private void asyncUpdateMyRate() {
        ServerApi.getRate(token, new ServerApi.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        String newRate = String.valueOf(json.getDouble("rate"));
                        PreferencesUtil.getInstance(context).putHelperRate(newRate);
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
        ServerApi.getFriends(token, new ServerApi.DataListener() {
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
                        PreferencesUtil.getInstance(context).putFriends(friendIds, friendNames);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {}
        });
    }

    private void asyncUpdateRooms() {
        if (!openAutoRefresh || isRefreshing) {
            return;
        }
        isRefreshing = true;
//        roomRefreshProgress.setVisibility(View.VISIBLE);

        ServerApi.getRoomList(token, new ServerApi.DataListener() {
            @Override
            public void onReceiveData(String data) {
                isRefreshing = false;
//                roomRefreshProgress.setVisibility(View.INVISIBLE);
                listener.updateRoomList(data);
            }

            @Override
            public void onClose() {}
        });
    }

    interface Listener {
        void updateRoomList(@NonNull String data);
        void showJoinDialog(@NonNull String blindName, @NonNull String roomId);
        void wantFinishActivity();
    }
}
