package edu.sfsu.geng.newguideme.login;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.http.ServerRequest;

import static edu.sfsu.geng.newguideme.Config.PASSWORDLIMIT;

/**
 * A simple {@link Fragment} subclass.
 */
public class LoginFragment extends Fragment {

    private AppCompatEditText emailEditText, passwordEditText;
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private AppCompatDialog resetDialog;

    private LoginListener loginListener;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        emailInputLayout = (TextInputLayout) view.findViewById(R.id.email_inputlayout);
        emailEditText = (AppCompatEditText) view.findViewById(R.id.email_edittext);
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (emailInputLayout != null) {
                    emailInputLayout.setError("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        passwordInputLayout = (TextInputLayout) view.findViewById(R.id.password_inputlayout);
        passwordEditText = (AppCompatEditText) view.findViewById(R.id.password_edittext);
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (passwordInputLayout != null) {
                    passwordInputLayout.setError("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        AppCompatButton loginButton = (AppCompatButton) view.findViewById(R.id.login_button);
        AppCompatTextView forgotPasswordButton = (AppCompatTextView) view.findViewById(R.id.forgot_password_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isValidEmail(emailEditText.getText())) {
                    emailInputLayout.setError(getString(R.string.login_email_invalid_error));
                    return;
                }
                String emailText = emailEditText.getText().toString();

                String passwordText = passwordEditText.getText().toString();
                if (passwordText.isEmpty()) {
                    passwordInputLayout.setError(getString(R.string.login_password_empty_error));
                    return;
                }

                if (loginListener == null) {
                    return;
                }

                loginListener.login(emailText, passwordText);
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

    void setListener(LoginListener listener) {
        this.loginListener = listener;
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

                                if (!isValidPassword(newPasswordEditText.getText())) {
                                    newPasswordInputLayout.setError(getString(R.string.login_password_short_error));
                                    return;
                                }
                                String newPassword = newPasswordEditText.getText().toString();

                                if (!newPassword.equals(confirmPasswordEditText.getText().toString())) {
                                    confirmPasswordInputLayout.setError(getString(R.string.login_password_not_match_error));
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

    private boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    private boolean isValidPassword(CharSequence target) {
        return !TextUtils.isEmpty(target) && target.length() >= PASSWORDLIMIT;
    }

}
