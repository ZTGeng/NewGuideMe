package edu.sfsu.geng.newguideme;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.login.WelcomeActivity;
import edu.sfsu.geng.newguideme.utils.ErrorCleanTextWatcher;
import edu.sfsu.geng.newguideme.utils.PreferencesUtil;

/**
 * Created by gengz on 3/11/17.
 */
public class ActionBarPresenter {

    @NonNull Context context;
    @NonNull Listener listener;
    @NonNull String token;

    ActionBarPresenter(@NonNull Context context, @NonNull Listener listener) {
        this.context = context;
        this.listener = listener;

        token = PreferencesUtil.getInstance(context).getToken();
        String username = PreferencesUtil.getInstance(context).getUsername();
        listener.setBarTitle("Hi, " + username);
    }

    void onChangePassword() {
        Dialog changePasswordDialog = createChangePasswordDialog();
        changePasswordDialog.show();
    }

    void onLogout() {
        PreferencesUtil.getInstance(context).setLogout();
        Intent welcomeActivity = new Intent(context, WelcomeActivity.class);
        context.startActivity(welcomeActivity);
        listener.wantFinishActivity();
    }

    @NonNull
    private Dialog createChangePasswordDialog() {
        final Dialog changePasswordDialog = new Dialog(context);
        changePasswordDialog.setContentView(R.layout.dialog_change_password);
        changePasswordDialog.setTitle("Change Password");

        final AppCompatEditText oldPasswordEditText = (AppCompatEditText) changePasswordDialog
                .findViewById(R.id.change_password_old_edittext);
        final TextInputLayout oldPasswordInputLayout = (TextInputLayout) changePasswordDialog
                .findViewById(R.id.change_password_old_inputlayout);
        if (oldPasswordEditText != null && oldPasswordInputLayout != null) {
            oldPasswordEditText.addTextChangedListener(new ErrorCleanTextWatcher(oldPasswordInputLayout));
        }

        final AppCompatEditText newPasswordEditText = (AppCompatEditText) changePasswordDialog
                .findViewById(R.id.change_password_new_edittext);
        final TextInputLayout newPasswordInputLayout = (TextInputLayout) changePasswordDialog
                .findViewById(R.id.change_password_new_inputlayout);
        if (newPasswordEditText != null && newPasswordInputLayout != null) {
            newPasswordEditText.addTextChangedListener(new ErrorCleanTextWatcher(newPasswordInputLayout));
        }

        AppCompatButton changePasswordButton = (AppCompatButton) changePasswordDialog.findViewById(R.id.change_password_button);
        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (oldPasswordEditText == null
                        || oldPasswordInputLayout == null
                        || newPasswordEditText == null
                        || newPasswordInputLayout == null) {
                    return;
                }

                String oldPassword = oldPasswordEditText.getText().toString();
                if (oldPassword.isEmpty()) {
                    oldPasswordInputLayout.setError(context.getString(R.string.login_password_empty_error));
                    return;
                }
                String newPassword = newPasswordEditText.getText().toString();
                if (newPassword.isEmpty()) {
                    newPasswordInputLayout.setError(context.getString(R.string.login_password_empty_error));
                    return;
                }

                ServerApi.changePassword(token, oldPassword, newPassword, new ServerApi.DataListener() {
                    @Override
                    public void onReceiveData(String data) {
                        try {
                            JSONObject json = new JSONObject(data);
                            String message = json.getString("response");

                            if (json.getBoolean("res")) {
                                changePasswordDialog.dismiss();
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String field = json.getString("field");
                            switch (field) {
                                case "old_password":
                                    oldPasswordInputLayout.setError(message);
                                    break;
                                case "new_password":
                                    newPasswordInputLayout.setError(message);
                                    break;
                                default:
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.getStackTrace();
                        }
                    }

                    @Override
                    public void onClose() {}
                });
            }
        });

        AppCompatButton cancelButton = (AppCompatButton) changePasswordDialog.findViewById(R.id.change_password_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changePasswordDialog.dismiss();
            }
        });

        return changePasswordDialog;
    }

    public interface Listener {
        void setBarTitle(@NonNull String title);
        void wantFinishActivity();
    }
}
