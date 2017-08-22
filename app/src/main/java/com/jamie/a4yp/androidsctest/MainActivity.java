package com.jamie.a4yp.androidsctest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import uk.ac.ox.eng.stepcounter.StepCounter;


public class MainActivity extends AppCompatActivity implements StepCounter.OnStepUpdateListener, StepCounter.OnFinishedProcessingListener {

    // Layout elements
    private TextView tv_stepCount;
    private Button btn_toggleStepCounter;

    // Internal state
    private boolean isEnabled = false;

    // Step Counter objects
    private StepCounter stepCounter;
    private AccelerometerSampler accelerometerSampler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get permissions
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, 1);
        }

        // Keep screen on.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initialize step counter
        stepCounter = new StepCounter(100f);
        stepCounter.addOnStepUpdateListener(this);
        stepCounter.setOnFinishedProcessingListener(this);

        // Initialize sampler
        accelerometerSampler = new AccelerometerSampler(this, stepCounter);

        // Setup layout elements and callbacks
        setupLayout();
    }

    private void setupLayout() {
        tv_stepCount = (TextView) findViewById(R.id.tv_stepCounter);

        btn_toggleStepCounter = (Button) findViewById(R.id.btn_toggleStepCounter);
        btn_toggleStepCounter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEnabled) {
                    // Stop sampling
                    accelerometerSampler.stop();

                    // Stop algorithm.
                    isEnabled = false;
                    btn_toggleStepCounter.setEnabled(false);
                    btn_toggleStepCounter.setText("Start Step Counting");
                    stepCounter.stop();
                }
                else {
                    // Start algorithm.
                    tv_stepCount.setText("Steps: 0");
                    isEnabled = true;
                    stepCounter.start();
                    btn_toggleStepCounter.setText("Stop Step Counting");

                    // Start sampling
                    accelerometerSampler.start();
                }
            }
        });
    }

    public void onFinishedProcessing() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btn_toggleStepCounter.setEnabled(true);
            }
        });

    }

    public void onStepUpdate(final int steps) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String text = "Steps: " + Integer.toString(steps);
                tv_stepCount.setText(text);
            }
        });
    }
}
