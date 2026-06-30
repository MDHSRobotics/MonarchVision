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
  private final PhotonCamera camera; // Name of camera defined in PhotonVision dashboard
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
    inputs.hasResults = false;
    inputs.hasTargets = false;
    inputs.unreadResultCount = 0;
    inputs.targetCount = 0;
    inputs.latestTimestampSeconds = 0.0;
    inputs.status = inputs.connected ? "Connected, no unread results" : "Disconnected";
    inputs.observations = new ObjectObservation[0];

    if (!inputs.connected) {
      return;
    }

    var results = camera.getAllUnreadResults();
    inputs.unreadResultCount = results.size();

    if (results.isEmpty()) {
      return;
    }

    inputs.hasResults = true;

    // Get the pose of the robot once for this update cycle
    var fieldToRobot = new Pose3d(robotPoseSupplier.get());

    inputs.observations =
        results.stream()
            .peek(result -> inputs.latestTimestampSeconds = result.getTimestampSeconds())
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

    inputs.targetCount = inputs.observations.length;
    inputs.hasTargets = inputs.targetCount > 0;

    if (inputs.hasTargets) {
      inputs.status = "Connected, targets visible";
    } else {
      inputs.status = "Connected, results received, no targets";
    }
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

    // Camera's 3d position and orientation relative to the robot
    Pose3d cameraPose = Pose3d.kZero.transformBy(robotToCamera);

    /*
     * Record the 3d rotation from the camera to the target
     *
     * PhotonVision yaw is positive to the right in image/target terms (i.e., increasing
     * pixel x values), while WPILib +z is positive counterclockwise when viewed from above
     * (i.e, left). Probably need to negate the tx (yaw) value before sending to WPILIB.
     * Test by placing a ball to the robot's right and verifying the estimated y
     * coordinate is positive.
     */
    Rotation3d targetRotation =
        new Rotation3d(
            0.0,
            -ty.getRadians(), // pitch down should point toward floor
            -tx.getRadians()); // yaw

    // Unit vector from the camera to the target relative to the camera
    Translation3d rayCameraFrame = new Translation3d(1.0, targetRotation);

    // Unit vector from the camera to the target relative to the robot
    Translation3d rayRobotFrame = rayCameraFrame.rotateBy(cameraPose.getRotation());

    // Z component of unit vector relative to robot
    double dz = rayRobotFrame.getZ();
    
    // Algorithm doesn't work if the centers of the target and camera have same z value
    if (Math.abs(dz) < 1e-6) {
      return Pose3d.kZero;
    }

    /*
     * We know the distance of the vertical drop from the camera height to the target
     * (which in the case of a ball is the horizontal plane thru the center of ball).
     * Use that distance normalized by the z component of the unit vector in robot
     * space to compute a scale factor. This scale factor can be applied to the x and y
     * components of the vector to determine the corresponding x and y distances to
     * the target.
     */
    double scale = (objectHeightMeters - cameraPose.getZ()) / dz;

    if (scale < 0.0) {
      return Pose3d.kZero;
    }

    // Use the scale factor to get a vector from the camera to the target, then add
    // that vector to the position of camera to compute the position of the target
    // relative to the robot
    Translation3d robotToObjectTranslation =
        cameraPose.getTranslation().plus(rayRobotFrame.times(scale));

    // Return a pose for the target relative to the robot
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
