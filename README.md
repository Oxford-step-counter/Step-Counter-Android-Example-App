Step counter app for Android
============================

This is a step counter app for Android based on [this algorithm](https://github.com/4YP/Java-Step-Counter).
The algorithm performs a peak detection on the data coming from the accelerometers.

The app runs the algorithm and shows the results.
It also connects to the Arduino-based ground truth device explained [here](https://github.com/Oxford-step-counter/GroundTruthDevice) and dumps all the data on a CSV file.


## Compile

Import the project as a gradle project in Android Studio.
The step counter algorithm is imported directly from its Github repository.

## Log files

The app can log all the data on a file on the external memory (the SD card).
Each file will have the timestamp of when the test started within the name.
The columns are the following:

    timestamp (microsencods since boot), accx, accy, accz, steps as detected by algo, steps as detected by ground truth device, steps as detected by the hardware step counter

