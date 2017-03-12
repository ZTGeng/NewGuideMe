package edu.sfsu.geng.newguideme.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.HashSet;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.helper.RegistrationIntentService;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by gengz on 3/11/17.
 */
public class PreferencesUtil {

    private static PreferencesUtil preferencesUtil;

    public static PreferencesUtil getInstance(@NonNull Context context) {
        if (preferencesUtil == null) {
            preferencesUtil = new PreferencesUtil(context);
        }
        return preferencesUtil;
    }

    private SharedPreferences pref;

    private PreferencesUtil(@NonNull Context context) {
        pref = context.getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
    }

    @NonNull
    public String getToken() {
        return pref.getString("token", "");
    }

    @NonNull
    public String getUsername() {
        return pref.getString("username", "");
    }

    @NonNull
    public String getInviteCode() {
        return pref.getString("invite_code", "");
    }

    public boolean isTokenSent() {
        return pref.getBoolean(RegistrationIntentService.SENT_TOKEN_TO_SERVER, false);
    }

    public void setLogin() {
        pref.edit().putBoolean("logged", true).apply();
    }

    public void setLogout() {
        pref.edit().putBoolean("logged", false).apply();
    }

    public void putFriends(@NonNull HashSet<String> friendIds, @NonNull HashSet<String> friendNames) {
        pref.edit()
                .putStringSet("friendIds", friendIds)
                .putStringSet("friendNames", friendNames)
                .apply();
    }

    public void putHelperRate(@NonNull String newRate) {
        pref.edit().putString("rate", newRate).apply();
    }
}
