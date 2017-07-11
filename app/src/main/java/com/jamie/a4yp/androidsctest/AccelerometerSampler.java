package com.jamie.a4yp.androidsctest;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.jamie.fourthYearProject.stepCounterModule.StepCounter;

/**
 * Created by Jamie Brynes on 7/10/2017.
 */

public class AccelerometerSampler {

    // Sensor management
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener accelerometerEventListener;


    public AccelerometerSampler(Context ctx, final StepCounter stepCounter) {
        sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelerometerEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                stepCounter.processSample(event.timestamp, event.values);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    public void start() {
        sensorManager.registerListener(accelerometerEventListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stop() {
        sensorManager.unregisterListener(accelerometerEventListener);
    }
}
