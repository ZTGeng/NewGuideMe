package edu.sfsu.geng.newguideme.blind.home;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;

import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.blind.video.BlindVideoActivity;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.utils.PreferencesUtil;

/**
 * Created by gengz on 3/11/17.
 */
public class BlindHomePresenter {

    private static final String TAG = "BlindHome";

    @NonNull private Context context;
    @NonNull private Listener listener;
    @NonNull private String token;
    @NonNull private HashSet<String> friendIds;
    @NonNull private HashSet<String> friendNames;
    @NonNull private Callee callee;
    private boolean callWithMap;
    private boolean hasWarned;

    public BlindHomePresenter(@NonNull Context context, @NonNull Listener listener) {
        this.context = context;
        this.listener = listener;

        token = PreferencesUtil.getInstance(context).getToken();
        PreferencesUtil.getInstance(context).setLogin();

        callee = Callee.favorites;
        callWithMap = true;
        hasWarned = false;

        friendIds = new HashSet<>();
        friendNames = new HashSet<>();

        listener.showDestinationInput();

//        asyncGetAddressHistory();
        String[] str = {"SFSU", "San Francisco", "safeway", "Market Street"};
        listener.setDestinationAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, str));
        asyncGetFriendsList();
    }

    void setCallFavorites() {
        callee = Callee.favorites;
        listener.setRequestButtonText(R.string.call_friends_button);
    }

    void setCallFavoritesThenEveryone() {
        callee = Callee.favoritesThenEveryone;
        listener.setRequestButtonText(R.string.call_friends_then_everyone_button);
    }

    void setCallEveryone() {
        callee = Callee.everyone;
        listener.setRequestButtonText(R.string.call_strangers_button);
    }

    void setCallWithMap() {
        callWithMap = true;
        listener.showDestinationInput();
    }

    void setCallWithoutMap() {
        callWithMap = false;
        listener.showDescriptionInput();
    }

    void onRequestButtonClicked() {
        listener.showCallDialog();
    }

    void onCall(@NonNull String description) {
        if (callee != Callee.everyone && friendIds.isEmpty()) {
            Toast.makeText(context, R.string.no_friends_message_short, Toast.LENGTH_SHORT).show();
            return;
        }

        if (callee != Callee.favorites && !hasWarned) {
            hasWarned = true;
            listener.showWarningDialog();
        }

        internalCall(description);
    }

    private void asyncGetAddressHistory() {
        ServerApi.getAddressHistory(token, new ServerApi.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        JSONArray addressesJSONArray =  json.getJSONArray("addresses");
                        String[] addresses = new String[addressesJSONArray.length()];
                        for (int i = 0; i < addressesJSONArray.length(); i++) {
                            addresses[i] = addressesJSONArray.getString(i);
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                context,
                                android.R.layout.simple_dropdown_item_1line,
                                addresses);
                        listener.setDestinationAdapter(adapter);
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
                        JSONArray friendsJSONArray = json.getJSONArray("friends");
                        for (int i = 0; i < friendsJSONArray.length(); i++) {
                            String friendJSON = friendsJSONArray.getString(i);
                            JSONObject friend = new JSONObject(friendJSON);
                            friendIds.add(friend.getString("token"));
                            friendNames.add(friend.getString("username"));
                        }
                        PreferencesUtil.getInstance(context).putFriends(friendIds, friendNames);

                        if (friendIds.isEmpty()) {
                            listener.canCallEveryoneOnly();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {}
        });
    }

    private void internalCall(@NonNull final String description) {
        ServerApi.DataListener dataListener = new ServerApi.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        Intent intent = new Intent(context, BlindVideoActivity.class);
                        intent.putExtra("callFriend", callee != Callee.everyone);
                        intent.putExtra("secondCall", callee == Callee.favoritesThenEveryone);
                        intent.putExtra("secondCallAfter", 5);
                        intent.putExtra("description", description);
                        intent.putExtra("withMap", callWithMap);
                        context.startActivity(intent);
                        listener.wantFinishActivity();
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
        };

        if (callee == Callee.everyone) {
            ServerApi.callStrangers(token, description, dataListener);
        } else {
            ServerApi.callAllFriends(token, description, dataListener);
        }
    }

    private void internalCall(@NonNull String description, final boolean isCallFavorites, final boolean isWithMap) {
        ServerApi.DataListener dataListener = new ServerApi.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (json.getBoolean("res")) {
                        Intent blindWaitActivity = new Intent(context, BlindVideoActivity.class);
                        blindWaitActivity.putExtra("callFriend", isCallFavorites);
                        blindWaitActivity.putExtra("withMap", isWithMap);
                        context.startActivity(blindWaitActivity);
                        listener.wantFinishActivity();
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
        };

        if (isCallFavorites) {
            ServerApi.callAllFriends(token, description, dataListener);
        } else {
            ServerApi.callStrangers(token, description, dataListener);
        }
    }

    interface Listener {
        void canCallEveryoneOnly();
        void hideDestinationInput();
        void showDestinationInput();
        void showDescriptionInput();
        void setRequestButtonText(@StringRes int textId);
        void setDestinationAdapter(ArrayAdapter<String> adapter);
        void showCallDialog();
        void showWarningDialog();
        void wantFinishActivity();
    }

    private enum Callee {
        favorites, favoritesThenEveryone, everyone
    }
}
