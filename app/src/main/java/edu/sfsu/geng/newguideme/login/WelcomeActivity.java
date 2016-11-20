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

import java.util.ArrayList;
import java.util.List;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.Role;
import edu.sfsu.geng.newguideme.blind.BlindHomeActivity;
import edu.sfsu.geng.newguideme.helper.HelperHomeActivity;

public class WelcomeActivity extends AppCompatActivity implements LoginFragment.Listener {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        /* If already login in, go to home activity */
        if (pref.getBoolean("logged", false)) {
            String id = pref.getString("id", "");
            String username = pref.getString("username", "");
            String role = pref.getString("role", "");
            String rate = pref.getString("rate", "");
            if (!id.isEmpty() && !username.isEmpty() && !role.isEmpty() && !rate.isEmpty()) {
                goHome(Role.valueOf(role));
            }
        }
        /* Always run code above when open the app */

        setContentView(R.layout.activity_welcome);

//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        adapter.addFragment(loginFragment, "LOGIN");

        viewPager.setAdapter(adapter);
    }

    @Override
    public void goHome(Role role) {
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
