# Hardware-in-the-Loop Field Vision


## Monarch Vision Settings
Turn on hardware-in-the-loop mode for apriltag detection in `FieldVisionConstants.java`:
```java
  /** Are we running a physics simulator, but with a real camera connected. */
  public static boolean hardwareInTheLoop = true;
```

If necessary, change  `hardwareInTheLoopCameras` in `FieldVisionConstants.java` which defines what cameras are on the test board to be used for detecting apriltags. Each `CameraSpec`  specifies the type of camera, its name, and its position and orientation relative to the center of the robot.
```java
  // The following is a definition of all cameras on a test harness used to testing
  // vision hardware-in-the-loop in simulation mode.
  public static CameraSpec[] hardwareInLoopCameras = {
    new CameraSpec(
        VisionType.LIMELIGHT,
        "limelight-one",
        new Transform3d(
            Constants.robotLengthInMeters / 2.0,
            0.0,
            Units.inchesToMeters(26.),
            new Rotation3d(0.0, 0.0, 0.0))),
    new CameraSpec(
        VisionType.PHOTONVISION,
        "rubik_camera2",
        new Transform3d(
            -Constants.robotLengthInMeters / 2.0,
            0.0,
            Units.inchesToMeters(7),
            new Rotation3d(
                0.0,
                -Math.toRadians(26), // Negative points up!!! See above.
                Math.PI))),
    new CameraSpec(
        VisionType.PHOTONVISION,
        "rubik_camera3",
        new Transform3d(
            0.0,
            Constants.robotWidthInMeters / 2.0,
            Units.inchesToMeters(22),
            new Rotation3d(0.0, 0.0, Math.PI / 2)))
  };
```


## Objective

Describe how real Limelight or PhotonVision AprilTag observations are supplied to a simulated robot program.

## Topics to cover

- Network configuration
- NetworkTables server selection
- Camera naming
- Camera pose configuration
- Receiving real observations in simulation
- Applying observations to simulated odometry
- Comparing physical measurements with reported measurements
- Viewing results in AdvantageScope

## Demonstration videos

Add a video showing a real camera moving while the simulated robot pose updates.

## Troubleshooting notes

Document intermittent observations, stale targets, incorrect camera transforms, and pose jumps.
