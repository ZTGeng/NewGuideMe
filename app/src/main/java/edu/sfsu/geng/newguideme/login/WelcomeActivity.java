package edu.sfsu.geng.newguideme.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.Role;
import edu.sfsu.geng.newguideme.blind.BlindHomeActivity;
import edu.sfsu.geng.newguideme.helper.HelperHomeActivity;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.http.ServerRequest;

public class WelcomeActivity extends AppCompatActivity implements LoginListener {

    private static final String TAG = "HelperHome";

    private TabLayout tabLayout;
    private ViewPager viewPager;

    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        Log.d(TAG, "============== " + pref.getBoolean("logged", false));
        /* If already login in, go to home activity */
        if (pref.getBoolean("logged", false)) {
            String token = pref.getString("token", "");
            String username = pref.getString("username", "");
            String role = pref.getString("role", "");
            String rate = pref.getString("rate", "");
            if (!token.isEmpty() && !username.isEmpty() && !role.isEmpty() && !rate.isEmpty()) {
                goHome(Role.valueOf(role));
            }
        }
        /* Always run code above when open the app */

        setContentView(R.layout.activity_welcome);

        viewPager = (ViewPager) findViewById(R.id.welcome_viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.welcome_tabs);
        if (tabLayout != null) {
            tabLayout.setupWithViewPager(viewPager);
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        LoginFragment loginFragment = new LoginFragment();
        loginFragment.setListener(this);
        adapter.addFragment(loginFragment, getString(R.string.login_login));

        RegisterFragment registerFragment = new RegisterFragment();
        registerFragment.setListener(this);
        adapter.addFragment(registerFragment, getString(R.string.login_register));

        viewPager.setAdapter(adapter);
    }

    @Override
    public void login(String email, String password) {
        ServerApi.login(email, password, new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try{
                    JSONObject json = new JSONObject(data);
                    if(json.getBoolean("res")){
                        String token = json.getString("token");
                        String username = json.getString("username");
                        String role = json.getString("role");
                        String rate = json.getString("rate");

                        SharedPreferences.Editor edit = pref.edit();
                        edit.putString("token", token);
                        edit.putString("username", username);
                        edit.putString("role", role);
                        edit.putString("rate", rate);
                        edit.apply();

                        goHome(Role.valueOf(role));
                    } else {
                        String jsonStr = json.getString("response");
                        Toast.makeText(WelcomeActivity.this, jsonStr, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {}
        });
    }

    private void goHome(Role role) {
        Intent homeActivity = null;
        switch (role) {
            case blind:
                homeActivity = new Intent(WelcomeActivity.this, BlindHomeActivity.class);
                break;
            case helper:
                homeActivity = new Intent(WelcomeActivity.this, HelperHomeActivity.class);
                break;
        }
        startActivity(homeActivity);
        finish();
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

}
