package com.jamie.a4yp.androidsctest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileWriter;
import java.io.IOException;

import uk.ac.ox.eng.stepcounter.StepCounter;


public class MainActivity extends AppCompatActivity {

    // sampling frequency in Hz
    private final int SAMPLING_FREQUENCY = 100;

    // request code for permissions
    private final int SDCARD_REQUEST_CODE = 991;

    // dump file name
    private final String LOG_FILENAME = "stepcounter.log";
    private FileWriter logwriter;

    // Layout elements
    private TextView tv_stepCount;
    private Button btn_toggleStepCounter;
    private Switch sw_logFile;

    // Internal state
    private boolean isEnabled = false;
    private boolean logToFile = false;

    // Step Counter objects
    private StepCounter stepCounter;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private int currentSteps = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sw_logFile = (Switch) findViewById(R.id.logSwitch);
        sw_logFile.setChecked(false);
        sw_logFile.setOnCheckedChangeListener(logFileSwitchListener);

        // Keep screen on.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        tv_stepCount = (TextView) findViewById(R.id.tv_stepCounter);
        btn_toggleStepCounter = (Button) findViewById(R.id.btn_toggleStepCounter);
        btn_toggleStepCounter.setOnClickListener(startClickListener);

        // Initialize step counter
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        stepCounter = new StepCounter(SAMPLING_FREQUENCY);
        stepCounter.addOnStepUpdateListener(new StepCounter.OnStepUpdateListener() {
            @Override
            public void onStepUpdate(final int steps) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentSteps = steps;
                        String text = "Steps: " + Integer.toString(currentSteps);
                        tv_stepCount.setText(text);
                    }
                });
            }
        });
        stepCounter.setOnFinishedProcessingListener(new StepCounter.OnFinishedProcessingListener() {
            @Override
            public void onFinishedProcessing() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btn_toggleStepCounter.setEnabled(true);
                    }
                });
            }
        });
    }

    private CompoundButton.OnCheckedChangeListener logFileSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                // Get permissions
                int permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    ActivityCompat.requestPermissions(MainActivity.this, permissions, SDCARD_REQUEST_CODE);
                } else {
                    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                            Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {
                        logToFile = true;
                    }
                }
            } else {
                logToFile = false;
            }
        }
    };


    private View.OnClickListener startClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isEnabled) {
                // Stop sampling
                sensorManager.unregisterListener(accelerometerEventListener);

                // Stop algorithm.
                isEnabled = false;
                btn_toggleStepCounter.setEnabled(false);
                btn_toggleStepCounter.setText("Start Step Counting");
                stepCounter.stop();

                try {
                    logwriter.close();
                } catch (IOException e) {
                }
            } else {
                // Start algorithm.
                tv_stepCount.setText("Steps: 0");
                isEnabled = true;
                currentSteps = 0;
                stepCounter.start();
                btn_toggleStepCounter.setText("Stop Step Counting");

                // start data log
                if (logToFile) {
                    String filepath = Environment.getExternalStorageDirectory() + "/" + LOG_FILENAME;
                    try {
                        logwriter = new FileWriter(filepath);
                    } catch (IOException ex) {
                        Toast.makeText(MainActivity.this, "Cannot create log file", Toast.LENGTH_SHORT);
                    }
                }

                // Start sampling
                int periodusecs = (int) ((1 / SAMPLING_FREQUENCY) * 1E6);
                sensorManager.registerListener(accelerometerEventListener, accelerometer, periodusecs);
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SDCARD_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                                Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState()))) {
                    // permission was granted and sd card is writeable
                    logToFile = true;
                } else {
                    logToFile = false;
                    sw_logFile.setChecked(false);
                }
                return;
            }
        }
    }

    private SensorEventListener accelerometerEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            stepCounter.processSample(event.timestamp, event.values);
            if (logToFile && logwriter != null) {
                String logline = event.timestamp + ",";
                for (float val : event.values)
                    logline += val + ",";
                logline += currentSteps;
                logline += "\n";
                try {
                    logwriter.append(logline);
                } catch (IOException e) {
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

}
