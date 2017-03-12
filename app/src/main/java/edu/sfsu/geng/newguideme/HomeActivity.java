package edu.sfsu.geng.newguideme;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Created by gengz on 3/12/17.
 */
public class HomeActivity extends AppCompatActivity implements ActionBarPresenter.Listener {

    private ActionBarPresenter actionBarPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        actionBarPresenter = new ActionBarPresenter(this, this);
    }

    @Override
    public void setBarTitle(@NonNull String title) {
        setTitle(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.change_password_menu:
                actionBarPresenter.onChangePassword();
                return true;
            case R.id.logout_menu:
                actionBarPresenter.onLogout();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void wantFinishActivity() {
        finish();
    }
}
