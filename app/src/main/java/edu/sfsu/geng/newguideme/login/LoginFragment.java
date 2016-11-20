package edu.sfsu.geng.newguideme.login;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.Role;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.http.ServerRequest;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class LoginFragment extends Fragment {

    private AppCompatEditText emailEditText, passwordEditText;
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private AppCompatDialog resetDialog;

    private Listener listener;
    private SharedPreferences pref;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        emailEditText = (AppCompatEditText) view.findViewById(R.id.email_edittext);
        passwordEditText = (AppCompatEditText) view.findViewById(R.id.password_edittext);
        emailInputLayout = (TextInputLayout) view.findViewById(R.id.email_inputlayout);
        passwordInputLayout = (TextInputLayout) view.findViewById(R.id.password_inputlayout);

        AppCompatButton loginButton = (AppCompatButton) view.findViewById(R.id.login_button);
        AppCompatTextView forgotPasswordButton = (AppCompatTextView) view.findViewById(R.id.forgot_password_button);

        pref = getActivity().getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String emailText = emailEditText.getText().toString();
                if (emailText.isEmpty()) {
                    emailInputLayout.setError(getString(R.string.login_email_empty_error));
                    return;
                }
                String passwordText = passwordEditText.getText().toString();
                if (passwordText.isEmpty()) {
                    passwordInputLayout.setError(getString(R.string.login_password_empty_error));
                    return;
                }

                if (listener == null) {
                    return;
                }

                ServerApi.login(emailText, passwordText, new ServerRequest.DataListener() {
                    @Override
                    public void onReceiveData(String data) {
                        try{
                            JSONObject json = new JSONObject(data);
                            if(json.getBoolean("res")){
                                String token = json.getString("token"); // token is id
                                String username = json.getString("username");
                                String role = json.getString("role");
                                String rate = json.getString("rate");

                                SharedPreferences.Editor edit = pref.edit();
                                edit.putString("token", token);
                                edit.putString("username", username);
                                edit.putString("role", role);
                                edit.putString("rate", rate);
                                edit.apply();

                                listener.goHome(Role.valueOf(role));
                            } else {
                                String jsonStr = json.getString("response");
                                Toast.makeText(getContext(), jsonStr, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onClose() {}
                });
            }
        });

        forgotPasswordButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                createResetDialog();
                resetDialog.show();
            }
        });

        return view;
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    private void createResetDialog() {
        resetDialog = new AppCompatDialog(getContext());
        resetDialog.setTitle(R.string.reset_password_title);
        resetDialog.setContentView(R.layout.reset_pass_init);

        AppCompatButton cancelButton = (AppCompatButton) resetDialog.findViewById(R.id.reset_cancel_button);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    resetDialog.dismiss();
                }
            });
        }

        final AppCompatEditText resetEmailEditText = (AppCompatEditText) resetDialog.findViewById(R.id.reset_email_edittext);
        final TextInputLayout resetEmailInputLayout = (TextInputLayout) resetDialog.findViewById(R.id.reset_email_inputlayout);

        AppCompatButton continueButton = (AppCompatButton) resetDialog.findViewById(R.id.reset_continue_button);
        if (continueButton != null) {
            continueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (resetEmailEditText == null) {
                        return;
                    }
                    final String email = resetEmailEditText.getText().toString();

                    if (email.isEmpty() && resetEmailInputLayout != null) {
                        resetEmailInputLayout.setError(getString(R.string.login_email_empty_error));
                        return;
                    }

                    ServerApi.requestResetCode(email, createRequestCodeDataListener(email));
                }
            });
        }
    }

    private ServerRequest.DataListener createRequestCodeDataListener(final String email) {
        return new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    String jsonStr = json.getString("response");
                    if (!json.getBoolean("res")) {
                        Toast.makeText(getContext(), jsonStr, Toast.LENGTH_SHORT).show();
                        return;
                    }
//                                            Log.d("JSON", jsonStr);
//                                Toast.makeText(getContext(), jsonStr, Toast.LENGTH_LONG).show();
                    resetDialog.setContentView(R.layout.reset_pass_code);

                    AppCompatButton cancelButton = (AppCompatButton) resetDialog.findViewById(R.id.reset_cancel_button);
                    if (cancelButton != null) {
                        cancelButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                resetDialog.dismiss();
                            }
                        });
                    }

                    final AppCompatEditText codeEditText = (AppCompatEditText) resetDialog.findViewById(R.id.reset_code_edittext);
                    final TextInputLayout codeInputLayout = (TextInputLayout) resetDialog.findViewById(R.id.reset_code_inputlayout);
                    final AppCompatEditText newPasswordEditText = (AppCompatEditText) resetDialog.findViewById(R.id.reset_new_password_edittext);
                    final TextInputLayout newPasswordInputLayout = (TextInputLayout) resetDialog.findViewById(R.id.reset_new_password_inputlayout);
                    final AppCompatEditText confirmPasswordEditText = (AppCompatEditText) resetDialog.findViewById(R.id.reset_confirm_password_edittext);
                    final TextInputLayout confirmPasswordInputLayout = (TextInputLayout) resetDialog.findViewById(R.id.reset_confirm_password_inputlayout);

                    AppCompatButton okButton = (AppCompatButton) resetDialog.findViewById(R.id.reset_ok_button);
                    if (okButton != null) {
                        okButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (codeEditText == null
                                        || codeInputLayout == null
                                        || newPasswordEditText == null
                                        || newPasswordInputLayout == null
                                        || confirmPasswordEditText == null
                                        || confirmPasswordInputLayout == null) {
                                    return;
                                }

                                String code = codeEditText.getText().toString();
                                if (code.isEmpty()) {
                                    codeInputLayout.setError(getString(R.string.reset_password_code_empty_error));
                                    return;
                                }

                                String newPassword = newPasswordEditText.getText().toString();
                                if (newPassword.isEmpty()) {
                                    newPasswordInputLayout.setError(getString(R.string.login_password_empty_error));
                                    return;
                                }

                                String confirmPassword = confirmPasswordEditText.getText().toString();
                                if (confirmPassword.isEmpty()) {
                                    confirmPasswordInputLayout.setError(getString(R.string.login_password_empty_error));
                                    return;
                                }

                                if (!newPassword.equals(confirmPassword)) {
                                    confirmPasswordInputLayout.setError(getString(R.string.reset_password_not_match_error));
                                    return;
                                }
//                                                        Log.d("Code", code_txt);
//                                                        Log.d("New pass", npass_txt);
                                ServerApi.changePassword(email, code, newPassword, createChangePasswordDataListener());
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {
            }
        };
    }

    private ServerRequest.DataListener createChangePasswordDataListener() {
        return new ServerRequest.DataListener() {
            @Override
            public void onReceiveData(String data) {
                try {
                    JSONObject json = new JSONObject(data);
                    String jsonStr = json.getString("response");
                    if (json.getBoolean("res")) {
                        resetDialog.dismiss();
                    }
                    Toast.makeText(getContext(), jsonStr, Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose() {
            }
        };
    }

    interface Listener {
        void goHome(Role role);
    }
}
