package uk.ac.ox.eng.stepcounter.app;

import android.Manifest;
import android.app.ProgressDialog;
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
import android.util.Log;
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
    private final int BT_REQUEST_CODE = 992;

    // dump file name
    private final String LOG_FILENAME = "stepcounter";
    private FileWriter logwriter;

    // Layout elements
    private TextView tv_stepCount;
    private TextView tv_GT;
    private TextView tv_HWSteps;
    private Button btn_toggleStepCounter;
    private Switch sw_logFile;
    private Switch sw_connectGT;

    // Internal state
    private boolean isEnabled = false;
    private boolean logToFile = false;

    // Step Counter objects
    private StepCounter stepCounter;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor hwstepsCounter;

    // Ground Truth device objets
    private GroundTruthDevice gtdevice;

    private ProgressDialog gtConnectingDialog;

    private int currentSteps = 0;
    private int gtsteps = 0;
    private int lastSteps = -1;
    private int hwsteps = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen on.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        tv_stepCount = (TextView) findViewById(R.id.stepsTextView);
        tv_GT  = (TextView) findViewById(R.id.gtTextView);
        tv_HWSteps = (TextView) findViewById(R.id.hwstepsTextView);
        btn_toggleStepCounter = (Button) findViewById(R.id.btn_toggleStepCounter);
        btn_toggleStepCounter.setOnClickListener(startClickListener);

        sw_logFile = (Switch) findViewById(R.id.logSwitch);
        sw_logFile.setChecked(false);
        sw_logFile.setOnCheckedChangeListener(logFileSwitchListener);

        sw_connectGT = (Switch) findViewById(R.id.gtdeviceSwitch);
        sw_connectGT.setChecked(false);
        sw_connectGT.setOnCheckedChangeListener(groundtruthSwitchListener);

        gtConnectingDialog = new ProgressDialog(this);
        gtConnectingDialog.setTitle("Connecting");
        gtConnectingDialog.setMessage("Trying to connect to the ground truth device...");
        gtConnectingDialog.setCancelable(false); // disable dismiss by tapping outside of the dialog


        // Initialize step counter
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        hwstepsCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
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

        // Init Ground Truth device
        gtdevice = new GroundTruthDevice(gtdeviceCallback, this);
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

    private CompoundButton.OnCheckedChangeListener groundtruthSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                // Get permissions
                int btpermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH);
                int btadminpermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_ADMIN);
                int locationpermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);

                if (btpermission != PackageManager.PERMISSION_GRANTED ||
                        btadminpermission != PackageManager.PERMISSION_GRANTED ||
                        locationpermission != PackageManager.PERMISSION_GRANTED) {
                    String[] permissions = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION};
                    ActivityCompat.requestPermissions(MainActivity.this, permissions, BT_REQUEST_CODE);
                } else {
                    gtConnectingDialog.show();
                    gtdevice.connect();
                }
            } else {
                gtConnectingDialog.cancel();
                gtdevice.disconnect();
            }
        }
    };


    private View.OnClickListener startClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isEnabled) {
                // Stop sampling
                sensorManager.unregisterListener(accelerometerEventListener);
                if(hwstepsCounter != null) sensorManager.unregisterListener(hwStepsEventListener);

                // Stop algorithm.
                isEnabled = false;
                btn_toggleStepCounter.setEnabled(false);
                btn_toggleStepCounter.setText("Start Step Counting");
                stepCounter.stop();

                try {
                    if(logwriter != null) logwriter.close();
                } catch (IOException e) {
                }
            } else {
                // Start algorithm.
                tv_stepCount.setText("Steps: 0");
                tv_GT.setText("Ground truth: 0");
                if(hwstepsCounter != null)tv_HWSteps.setText("HW steps: 0");
                isEnabled = true;
                currentSteps = 0;
                gtsteps = 0;
                hwsteps = 0;
                lastSteps = -1;
                stepCounter.start();
                btn_toggleStepCounter.setText("Stop Step Counting");

                // start data log
                if (logToFile) {
                    String filepath = Environment.getExternalStorageDirectory() + "/" + LOG_FILENAME + "_" + System.currentTimeMillis() + ".csv";
                    try {
                        logwriter = new FileWriter(filepath);
                    } catch (IOException ex) {
                        Toast.makeText(MainActivity.this, "Cannot create log file", Toast.LENGTH_SHORT).show();
                    }
                }

                // Start sampling
                int periodusecs = (int) (1E6 / SAMPLING_FREQUENCY);
                Log.d(MainActivity.class.getSimpleName(), "Sampling at " + periodusecs + " usec");
                sensorManager.registerListener(accelerometerEventListener, accelerometer, periodusecs);
                if(hwstepsCounter != null) sensorManager.registerListener(hwStepsEventListener, hwstepsCounter, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SDCARD_REQUEST_CODE: {
                if (allPermissionsGranted(grantResults) &&
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
            case BT_REQUEST_CODE: {
                if (allPermissionsGranted(grantResults)){
                    gtdevice.connect();
                } else {
                    Toast.makeText(this, "Cannot get permissions for Bluetooth", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private boolean allPermissionsGranted(int[] grantResults){
        for(int p : grantResults){
            if(p != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    private SensorEventListener accelerometerEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            stepCounter.processSample(event.timestamp, event.values);
            if (logToFile && logwriter != null) {
                String logline = event.timestamp + ",";
                for (float val : event.values)
                    logline += val + ",";
                logline += currentSteps + ",";
                logline += gtsteps + ",";
                logline += hwsteps;
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

    private SensorEventListener hwStepsEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            int steps = (int)event.values[0];
            if(lastSteps == -1){
                hwsteps = 0;
                lastSteps = steps;
            } else {
                hwsteps = steps - lastSteps;
            }
            Log.d(MainActivity.class.getSimpleName(), "HW steps: " + steps + ", i.e. " + hwsteps);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String hwstr = "HW steps: " + hwsteps;
                    tv_HWSteps.setText(hwstr);
                }
            });
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private GroundTruthDevice.GTdeviceCallback gtdeviceCallback = new GroundTruthDevice.GTdeviceCallback() {
        @Override
        public void connected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gtConnectingDialog.cancel();
                    gtsteps = 0;
                    tv_GT.setText("Ground truth: " + gtsteps);
                    sw_connectGT.setChecked(true);
                }
            });
        }

        @Override
        public void disconnected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Device disconnected", Toast.LENGTH_SHORT).show();
                    gtConnectingDialog.cancel();
                    sw_connectGT.setChecked(false);
                }
            });
        }

        @Override
        public void stepDetected(final boolean left, final  boolean right) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(left != right){
                        gtsteps ++;
                        tv_GT.setText("Ground truth: " + gtsteps);
                    }
                }
            });
        }
    };

}
