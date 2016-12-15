package edu.sfsu.geng.newguideme.blind;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import edu.sfsu.geng.newguideme.Config;
import edu.sfsu.geng.newguideme.R;
import edu.sfsu.geng.newguideme.http.ServerApi;
import edu.sfsu.geng.newguideme.http.ServerRequest;

public class BlindRateActivity extends AppCompatActivity {

    private TextView rateNumberText;
    private float rateFloat;
    private String helperId, helperName, token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("RateActivity", "Rate activity create");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blind_rate);
        helperId = getIntent().getStringExtra("helperId");
        helperName = getIntent().getStringExtra("helperName");

        SharedPreferences pref = getSharedPreferences(Config.PREF_KEY, MODE_PRIVATE);
        token = pref.getString("token", "");

        rateFloat = 5.0f;
        rateNumberText = (TextView) findViewById(R.id.vi_rate_number);

        AppCompatButton decreaseButton = (AppCompatButton) findViewById(R.id.vi_rate_decrease_btn);
        decreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rateFloat > 0) {
                    rateFloat -= 0.5f;
                    rateNumberText.setText(String.valueOf(rateFloat));
                }
            }
        });

        AppCompatButton increaseButton = (AppCompatButton) findViewById(R.id.vi_rate_increase_btn);
        increaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rateFloat < 5) {
                    rateFloat += 0.5f;
                    rateNumberText.setText(String.valueOf(rateFloat));
                }
            }
        });

        AppCompatButton submitButton = (AppCompatButton) findViewById(R.id.vi_rate_btn);
        assert submitButton != null;
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmit(v);
            }
        });

        AppCompatButton cancelButton = (AppCompatButton) findViewById(R.id.vi_rate_cancel_btn);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancel(v);
            }
        });

        Toast.makeText(this, R.string.vi_rate_hint, Toast.LENGTH_SHORT).show();

    }

    public void onSubmit(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(String.format(getResources().getString(R.string.vi_rate_message), helperName, String.valueOf(rateFloat)));
        builder.setPositiveButton(R.string.vi_rate_ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ServerApi.rateHelper(token, helperId, String.valueOf(rateFloat), new ServerRequest.DataListener() {
                    @Override
                    public void onReceiveData(String data) {
                        try {
                            JSONObject json = new JSONObject(data);
                            if (json.getBoolean("res")) {
                                Toast.makeText(getApplication(), json.getString("response"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onClose() {}
                });

                Intent homeActivity = new Intent(BlindRateActivity.this, BlindHomeActivity.class);
                startActivity(homeActivity);
                finish();
            }
        });
        builder.setNegativeButton(R.string.vi_rate_change_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    public void onCancel(View v) {
        Intent homeActivity = new Intent(BlindRateActivity.this, BlindHomeActivity.class);
        startActivity(homeActivity);
        finish();
    }
}
