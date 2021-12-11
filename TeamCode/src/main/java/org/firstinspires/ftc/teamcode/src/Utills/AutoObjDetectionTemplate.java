package org.firstinspires.ftc.teamcode.src.Utills;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;

import java.util.List;

@Disabled
public abstract class AutoObjDetectionTemplate extends AutonomousTemplate {


    private static final String TFOD_MODEL_ASSET = "Trained Pink Team Marker Finder Mk2.tflite";
    private static final String[] LABELS = {"Pink Team Marker"};


    private static final String VUFORIA_KEY =
            "AWVWPbH/////AAABmbzQF0cF/EvRnE4ykZKAXvpbnJrPQs1aBJ2i7u5ADGzYU+x0dxqGlB/G8yCrcY4FP8cPEA1w+xTXCpbFDmlYcKMG6VL/6v+H0Es3H/1f8xpQG86nSCXKPLxEbYGHkBxAYSlxB0gueBpnxMYsURezlq2Q9e5Br5OIhY7gmZZNa3VPHupscQkrCrVdRMI9mPAbEjMBhVBWjVJEL0+u2tyvEQuK4tllgi8C7AKq5V5lFoKEQG0VD89xlgUfRZsDq89HToRXBOUE2mubPHUcplKiX+1EfB+801eEt+k7lLJ1VyfrXr2tjwyWPjafvTpnaf3C35ox0/TOPdak5pq2gXLpXzAxXc6+RH28m2572tYB58AN";

    public volatile VuforiaLocalizer vuforia;
    public volatile TFObjectDetector tfod;

    private volatile boolean threadedObjReturn = true;

    public void initVuforia() throws InterruptedException {
        telemetry.addData("Vuforia initialization:", "Started");
        telemetry.update();

        Thread t = new Thread(this::_initVuforia);
        t.start();
        while (t.getState() != Thread.State.TERMINATED) {
            if (isStopRequested()) {
                threadedObjReturn = false;
                throw new InterruptedException();
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            continue;
        }
        t.join();

        telemetry.addData("Vuforia initialization:", "Complete");
        telemetry.update();

    }

    public void _initVuforia() {
        //Waits for mutex to be available
        if (globalInitThreadMutex.initThreadRunning) {
            while (globalInitThreadMutex.initThreadRunning) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        //Claims mutex
        globalInitThreadMutex.initThreadRunning = true;

        //does the initialization
        VuforiaLocalizer Vuforia;
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = VUFORIA_KEY;
        parameters.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;

        //  Instantiate the Vuforia engine
        Vuforia = ClassFactory.getInstance().createVuforia(parameters);

        //Passes initialized obj back to caller class
        if (threadedObjReturn) {
            this.vuforia = Vuforia;
        }

        //frees mutex
        globalInitThreadMutex.initThreadRunning = false;


    }

    public void initTfod() throws InterruptedException {
        telemetry.addData("TFOD initialization:", "Started");
        telemetry.update();


        Thread t = new Thread(this::_initTfod);
        t.start();
        while (t.getState() != Thread.State.TERMINATED) {
            if (isStopRequested()) {
                threadedObjReturn = false;
                throw new InterruptedException();
            }
        }

        t.join();


        telemetry.addData("TFOD initialization:", "Complete");
        telemetry.update();

    }

    private void _initTfod() {
        //Waits for mutex to be available
        if (globalInitThreadMutex.initThreadRunning) {
            while (globalInitThreadMutex.initThreadRunning) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        //Claims mutex
        globalInitThreadMutex.initThreadRunning = true;

        //Runs initialization Code
        int tfodMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "tfodMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        TFObjectDetector Tfod;
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfodParameters.minResultConfidence = 0.8f;
        tfodParameters.isModelTensorFlow2 = true;
        tfodParameters.inputSize = 320;
        Tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);
        Tfod.loadModelFromAsset(TFOD_MODEL_ASSET, LABELS);


        //Passes initialized obj back to caller class
        if (threadedObjReturn) {
            this.tfod = Tfod;
        }

        //frees mutex
        globalInitThreadMutex.initThreadRunning = false;
    }


    private static class globalInitThreadMutex {
        private static volatile boolean initThreadRunning;
    }

    public void activate() {
        if (tfod != null) {
            tfod.activate();

            // The TensorFlow software will scale the input images from the camera to a lower resolution.
            // This can result in lower detection accuracy at longer distances (> 55cm or 22").
            // If your target is at distance greater than 50 cm (20") you can adjust the magnification value
            // to artificially zoom in to the center of image.  For best results, the "aspectRatio" argument
            // should be set to the value of the images used to create the TensorFlow Object Detection model
            // (typically 16/9).
            tfod.setZoom(2, 16.0 / 9.0);
        }
    }

    public MarkerPosition findPositionOfMarker() {
        if (tfod != null) {

            // getUpdatedRecognitions() will return null if no new information is available since
            // the last time that call was made.
            List<Recognition> updatedRecognitions = tfod.getUpdatedRecognitions();

            if (updatedRecognitions != null && (updatedRecognitions.size() > 0)) {
                if (updatedRecognitions.get(0).getLeft() > 615) {
                    return MarkerPosition.Right;
                }
                if (updatedRecognitions.get(0).getLeft() <= 615) {
                    return MarkerPosition.Left;
                }
            }
        }
        return MarkerPosition.NotSeen;
    }


    public MarkerPosition getAverageOfMarker(int arraySize, int sleepTime) throws InterruptedException {

        MarkerPosition[] markerPositions = new MarkerPosition[arraySize];

        for (int i = 0; i < arraySize; i++) {
            markerPositions[i] = this.findPositionOfMarker();
            Thread.sleep(sleepTime);
        }

        int sum = 0;
        for (int i = 0; i < arraySize; i++) {
            switch (markerPositions[i]) {
                case NotSeen:
                    sum = sum;
                    break;
                case Right:
                    sum++;
                    break;
                case Left:
                    sum = sum + 2;
                    break;
            }
        }

        int result = (int) Math.round(sum / (double) arraySize);


        switch (result) {
            case 0:
                return MarkerPosition.NotSeen;
            case 1:
                return MarkerPosition.Right;
            case 2:
                return MarkerPosition.Left;
        }
        return MarkerPosition.Left; //It never reaches this line
    }

    public enum MarkerPosition {
        Right,
        Left,
        NotSeen
    }
}


