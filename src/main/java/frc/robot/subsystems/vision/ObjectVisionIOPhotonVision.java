package frc.robot.subsystems.vision;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.function.Supplier;
import org.photonvision.PhotonCamera;

/*
 * IO implementation for the real PhotonVision camera with object detection pipeline
 */
public class ObjectVisionIOPhotonVision implements ObjectVisionIO {
  private final PhotonCamera camera; // Name of camera defined in PhontonVision dashboard
  private final Transform3d robotToCamera; // Position and orientation of camera relative to robot
  private final Supplier<Pose2d>
      robotPoseSupplier; // Function to return the current pose of the robot
  private final double objectHeightMeters; // Height of the object relative to the floor of field

  /**
   * Creates a new ObjectVisionIOPhotonVision.
   *
   * @param name The configured name of the camera.
   * @param robotToCamera The 3D position and orientation of the camera relative to the robot.
   * @param robotPoseSupplier Function to return the current pose of the robot
   * @param objectHeightMeters Height of the object relative to the floor of field
   */
  public ObjectVisionIOPhotonVision(
      String cameraName,
      Transform3d robotToCamera,
      Supplier<Pose2d> robotPoseSupplier,
      double objectHeightMeters) {

    this.camera = new PhotonCamera(cameraName);
    this.robotToCamera = robotToCamera;
    this.robotPoseSupplier = robotPoseSupplier;
    this.objectHeightMeters = objectHeightMeters;
  }

  /**
   * Return all of the object observations detected by PhotonVision
   *
   * @param inputs Object of type ObjectVisionIOInputs that will be populated with the connected
   *     status of the camera plus an array of object observations
   */
  @Override
  public void updateInputs(ObjectVisionIOInputs inputs) {
    inputs.connected = camera.isConnected();

    var results = camera.getAllUnreadResults();
    if (results.isEmpty()) {
      inputs.observations = new ObjectObservation[0];
      return;
    }

    // Just process the most recent result
    var result = results.get(results.size() - 1);

    // Get the pose of the robot
    var fieldToRobot = new Pose3d(robotPoseSupplier.get());

    inputs.observations =
        result.getTargets().stream()
            .map(
                target -> {
                  Rotation2d tx = Rotation2d.fromDegrees(target.getYaw());
                  Rotation2d ty = Rotation2d.fromDegrees(target.getPitch());

                  Pose3d robotRelativePose =
                      estimateRobotRelativeObjectPose(tx, ty, objectHeightMeters);

                  Pose3d fieldPose =
                      fieldToRobot.transformBy(new Transform3d(Pose3d.kZero, robotRelativePose));

                  return new ObjectObservation(
                      result.getTimestampSeconds(),
                      target.getDetectedObjectClassID(),
                      classNameFromId(target.getDetectedObjectClassID()),
                      target.getDetectedObjectConfidence(),
                      tx,
                      ty,
                      target.getArea(),
                      robotRelativePose,
                      fieldPose);
                })
            .toArray(ObjectObservation[]::new);
  }

  /**
   * Estimate the position and orientation of the object relative to the robot
   *
   * @param tx Yaw of the object relative to the camera
   * @param ty Pitch of the object relative to the camera
   * @param objectHeightMeters Height of the object relative to the floor of the field
   * @return Pose of the object relative to the robot
   */
  private Pose3d estimateRobotRelativeObjectPose(
      Rotation2d tx, Rotation2d ty, double objectHeightMeters) {

    Pose3d cameraPose = Pose3d.kZero.transformBy(robotToCamera);

    // PhotonVision yaw is usually positive to the left in image/target terms,
    // while WPILib +Y is left. If this is reversed in testing, flip this sign.
    Rotation3d targetRotation =
        new Rotation3d(
            0.0,
            -ty.getRadians(), // pitch down should point toward floor
            -tx.getRadians());

    Translation3d rayCameraFrame = new Translation3d(1.0, targetRotation);

    Translation3d rayRobotFrame = rayCameraFrame.rotateBy(cameraPose.getRotation());

    double dz = rayRobotFrame.getZ();
    if (Math.abs(dz) < 1e-6) {
      return Pose3d.kZero;
    }

    double scale = (objectHeightMeters - cameraPose.getZ()) / dz;

    if (scale < 0.0) {
      return Pose3d.kZero;
    }

    Translation3d robotToObjectTranslation =
        cameraPose.getTranslation().plus(rayRobotFrame.times(scale));

    return new Pose3d(robotToObjectTranslation, Rotation3d.kZero);
  }

  /**
   * Return the name of the object class based on its id
   *
   * @param id ID of object class
   * @return Name of the object class
   */
  private static String classNameFromId(int id) {

    // NOTE: This needs to be updated whenever PhotonVision is trained on other types of objects
    return switch (id) {
      case 0 -> "fuel";
      default -> "unknown";
    };
  }
}
