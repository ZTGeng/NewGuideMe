package edu.sfsu.geng.newguideme.blind.video;


import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.ServerApi;

/**
 * A simple {@link Fragment} subclass.
 */
public class BlindRateFragment extends Fragment {

    private TextView rateNumberText;
    private float rateFloat;
    private String helperId, helperName, token;

    private Listener listener;

    public BlindRateFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        token = bundle.getString("token");
        helperId = bundle.getString("helperId");
        helperName = bundle.getString("helperName");

        View view = inflater.inflate(R.layout.fragment_bling_rate, container, false);

        rateFloat = 5.0f;
        rateNumberText = (TextView) view.findViewById(R.id.vi_rate_number);

        AppCompatButton decreaseButton = (AppCompatButton) view.findViewById(R.id.vi_rate_decrease_btn);
        decreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rateFloat > 0) {
                    rateFloat -= 0.5f;
                    rateNumberText.setText(String.valueOf(rateFloat));
                }
            }
        });

        AppCompatButton increaseButton = (AppCompatButton) view.findViewById(R.id.vi_rate_increase_btn);
        increaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rateFloat < 5) {
                    rateFloat += 0.5f;
                    rateNumberText.setText(String.valueOf(rateFloat));
                }
            }
        });

        AppCompatButton submitButton = (AppCompatButton) view.findViewById(R.id.vi_rate_btn);
        assert submitButton != null;
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmit(v);
            }
        });

        AppCompatButton cancelButton = (AppCompatButton) view.findViewById(R.id.vi_rate_cancel_btn);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancel(v);
            }
        });

        return view;
    }

    void setListener(@NonNull Listener listener) {
        this.listener = listener;
    }

    private void onSubmit(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(String.format(getString(R.string.vi_rate_message), helperName, String.valueOf(rateFloat)));
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ServerApi.rateHelper(token, helperId, String.valueOf(rateFloat), new ServerApi.DataListener() {
                    @Override
                    public void onReceiveData(String data) {
                        try {
                            JSONObject json = new JSONObject(data);
                            if (json.getBoolean("res")) {
                                Toast.makeText(getContext(), json.getString("response"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onClose() {}
                });

                listener.onQuitRate();
            }
        });
        builder.setNegativeButton(R.string.vi_rate_change_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    private void onCancel(View v) {
        listener.onQuitRate();
    }

    interface Listener {
        void onQuitRate();
    }

}
