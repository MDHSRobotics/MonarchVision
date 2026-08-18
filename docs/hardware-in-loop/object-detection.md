# Hardware-in-the-Loop Object Detection

## MonarchVision Settings

Turn on hardware-in-the-loop for object detection in `ObjectVisionConstants.java`:
```java
  /** Are we running a physics simulator, but with a real camera connected. */
  public static boolean hardwareInTheLoop = true;
```

If necessary, change  `hardwareInTheLoopCameras` in `ObjectVisionConstants.java` which defines what cameras are on the test board to be used for detecting objects. Each `CameraSpec`  specifies the type of camera, its name, and its position and orientation relative to the center of the robot.

```java
  // The following is a definition of all cameras on a test harness used to test
  // vision hardware-in-the-loop object detection
  public static CameraSpec[] hardwareInLoopCameras = {
    new CameraSpec(
        VisionType.PHOTONVISION,
        "rubik_camera1",
        new Transform3d(
            -Constants.robotLengthInMeters / 2.0, // On back of robot
            0.0,
            Units.inchesToMeters(20.5),
            new Rotation3d(
                0.0,
                Math.toRadians(35.), // Positive points down!!! See above.
                Math.PI))) // Facing backwards
  };
```

## Objective

Describe how real object detections are used by the simulated robot program.

## Topics to cover

- Limelight or PhotonVision object-detection pipeline
- Confidence thresholds
- Camera-to-target measurements
- Multiple targets
- Translating observations into robot or field coordinates
- Testing target selection and drive commands
- Comparing real detections with simulated detections

## Demonstration videos

Add a video showing real objects being detected and displayed in the simulation.

## Troubleshooting notes

Document lighting, background, confidence, latency, and dropped-result issues.
