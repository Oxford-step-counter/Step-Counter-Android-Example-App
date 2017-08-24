Step counter app for Android
============================

This is a step counter app for Android based on [this algorithm](https://github.com/4YP/Java-Step-Counter).
The algorithm performs a peak detection on the data coming from the accelerometers.

The app runs the algorithm and shows the results.
It also connects to the Arduino-based ground truth device explained here and dumps all the data on a CSV file.


## Compile

Import the project as a gradle project in Android Studio.
The step counter algorithm is imported directly from its Github repository.

## Log files

The app can log all the data on a file on the external memory (the SD card).
Each file will have the timestamp of when the test started within the name.
The columns are the following:

    timestamp (microsencods since boot), accx, accy, accz, steps as detected by algo, steps as detected by ground truth device, steps as detected by the hardware step counter


## License: MIT

Copyright (c) University of Oxford

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
