package frc.robot.subsystems.vision;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform3d;
import java.util.function.Supplier;
import org.photonvision.PhotonCamera;

/** IO implementation for the real PhotonVision camera with object detection pipeline */
public class ObjectVisionIOPhotonVision extends ObjectVisionIOBase {
  private final PhotonCamera camera; // PhotonVision Camera

  /**
   * Constructor
   *
   * @param name The configured name of the PhotonVision camera.
   * @param robotToCamera The 3D position and orientation of the camera relative to the robot.
   * @param robotPoseSupplier Function to return the current pose of the robot
   * @param objectHeightMeters Height of the object relative to the floor of field
   */
  public ObjectVisionIOPhotonVision(
      String cameraName,
      Transform3d robotToCamera,
      Supplier<Pose2d> robotPoseSupplier,
      double objectHeightMeters) {
    super(cameraName, robotToCamera, robotPoseSupplier, objectHeightMeters);

    this.camera = new PhotonCamera(cameraName);
  }

  /**
   * Get connection status of camera
   *
   * @return Connection status of camera
   */
  @Override
  protected boolean isConnected() {
    return camera.isConnected();
  }

  /**
   * @param fieldToRobot Pose of the robot on the field
   * @return array of observations
   */
  @Override
  protected ObjectObservation[] getObservations(Pose3d fieldToRobot) {

    ObjectObservation[] observations = new ObjectObservation[0];

    var results = camera.getAllUnreadResults();

    if (!results.isEmpty()) {

      observations =
          results.stream()
              .flatMap(
                  result ->
                      result.getTargets().stream()
                          .map(
                              target -> {
                                Rotation2d tx = Rotation2d.fromDegrees(target.getYaw());
                                Rotation2d ty = Rotation2d.fromDegrees(target.getPitch());

                                Pose3d robotRelativePose =
                                    estimateRobotRelativeObjectPose(tx, ty, objectHeightMeters);

                                Pose3d fieldPose =
                                    fieldToRobot.transformBy(
                                        new Transform3d(Pose3d.kZero, robotRelativePose));

                                return new ObjectObservation(
                                    result.getTimestampSeconds(),
                                    target.getDetectedObjectClassID(),
                                    target.getDetectedObjectConfidence(),
                                    tx,
                                    ty,
                                    target.getArea(),
                                    robotRelativePose,
                                    fieldPose);
                              }))
              .toArray(ObjectObservation[]::new);
    }

    return observations;
  }
}
