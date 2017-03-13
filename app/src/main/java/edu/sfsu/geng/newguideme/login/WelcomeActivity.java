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

import java.util.ArrayList;
import java.util.List;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.Role;
import edu.sfsu.geng.newguideme.blind.home.BlindHomeActivity;
import edu.sfsu.geng.newguideme.helper.home.HelperHomeActivity;

public class WelcomeActivity extends AppCompatActivity implements LoginListener {

    private static final String TAG = "Welcome";

    private TabLayout tabLayout;
    private ViewPager viewPager;

    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
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
        WelcomeViewPagerAdapter adapter = new WelcomeViewPagerAdapter(getSupportFragmentManager());

        LoginFragment loginFragment = new LoginFragment();
        adapter.addFragment(loginFragment, getString(R.string.login_login));

        RegisterFragment registerFragment = new RegisterFragment();
        adapter.addFragment(registerFragment, getString(R.string.login_register));

        viewPager.setAdapter(adapter);
    }

    @Override
    public void login(String token, String username, String role, String rate, String inviteCode) {
        pref.edit()
                .putString("token", token)
                .putString("username", username)
                .putString("role", role)
                .putString("rate", rate)
                .putString("invite_code", inviteCode)
                .apply();

        goHome(Role.valueOf(role));
    }

    private void goHome(Role role) {
        Intent homeActivity;
        switch (role) {
            case blind:
                homeActivity = new Intent(WelcomeActivity.this, BlindHomeActivity.class);
                break;
            case helper:
                homeActivity = new Intent(WelcomeActivity.this, HelperHomeActivity.class);
                break;
            default:
                Log.d(TAG, "Role error");
                return;
        }
        startActivity(homeActivity);
        finish();
    }

    private class WelcomeViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        WelcomeViewPagerAdapter(FragmentManager manager) {
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
