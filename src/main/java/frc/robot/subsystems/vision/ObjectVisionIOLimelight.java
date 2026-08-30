package frc.robot.subsystems.vision;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.networktables.DoubleSubscriber;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.Timer;
import frc.robot.LimelightHelpers;
import java.util.Arrays;
import java.util.function.Supplier;

/** IO implementation for a real Limelight camera with neural object detection pipeline. */
public class ObjectVisionIOLimelight extends ObjectVisionIOBase {

  private final DoubleSubscriber latencySubscriber;

  /**
   * Constructor.
   *
   * @param cameraName The configured name of the Limelight.
   * @param robotToCamera The 3D position and orientation of the camera relative to the robot.
   * @param robotPoseSupplier Function to return the current pose of the robot.
   * @param objectHeightMeters Height of the object relative to the floor of the field.
   */
  public ObjectVisionIOLimelight(
      String cameraName,
      Transform3d robotToCamera,
      Supplier<Pose2d> robotPoseSupplier,
      double objectHeightMeters) {

    super(cameraName, robotToCamera, robotPoseSupplier, objectHeightMeters);

    // Subscribe to pipeline latency so we can determine whether the Limelight
    // is actively publishing NetworkTables data.
    var table = NetworkTableInstance.getDefault().getTable(cameraName);
    latencySubscriber = table.getDoubleTopic("tl").subscribe(0.0);
  }

  /**
   * Get connection status of camera.
   *
   * <p>The Limelight continually publishes its pipeline latency ("tl"). Consider the camera
   * connected if this value has been updated within the last 250 ms.
   *
   * @return Connection status of camera.
   */
  @Override
  protected boolean isConnected() {
    return ((RobotController.getFPGATime() - latencySubscriber.getLastChange()) / 1000) < 250;
  }

  /**
   * Get all object observations currently reported by the Limelight neural detector.
   *
   * @param fieldToRobot Pose of the robot on the field.
   * @return Array of object observations.
   */
  @Override
  protected ObjectObservation[] getObservations(Pose3d fieldToRobot) {

    var detections = LimelightHelpers.getRawDetections(cameraName);

    return Arrays.stream(detections)
        .map(
            target -> {
              Rotation2d tx = Rotation2d.fromDegrees(target.txnc);

              Rotation2d ty = Rotation2d.fromDegrees(target.tync);

              Pose3d robotRelativePose =
                  estimateRobotRelativeObjectPose(tx, ty, objectHeightMeters);

              Pose3d fieldPose =
                  fieldToRobot.transformBy(new Transform3d(Pose3d.kZero, robotRelativePose));

              return new ObjectObservation(
                  Timer.getFPGATimestamp(),
                  target.classId,
                  1.0,
                  tx,
                  ty,
                  target.ta,
                  robotRelativePose,
                  fieldPose);
            })
        .toArray(ObjectObservation[]::new);
  }
}
