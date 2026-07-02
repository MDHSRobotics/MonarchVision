package frc.robot.subsystems.vision;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.Timer;
import frc.robot.subsystems.vision.GamePieceLayout.GamePiecesOfType;
import frc.robot.subsystems.vision.GamePieceLayout.Position;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import org.littletonrobotics.junction.AutoLog;
import org.littletonrobotics.junction.Logger;

/***
 * Simulated PhotonVision for ObjectVision IO. This class maintains a list of fuel objects
 * and their locations on the field. When the simulated PhotonVision camera is in a
 * position to detect one or more of them, this class will compute the yaw and pitch.
 */
public class ObjectVisionIOSim extends ObjectVisionIOBase {
  @AutoLog
  public static class ObjectVisionIOSimGamePieces {
    public boolean gamePiecesLoaded;
    public String className = "";
    public Pose3d[] fieldPoses = new Pose3d[0];
  }

  private final ObjectVisionIOSimGamePiecesAutoLogged simGamePieces =
      new ObjectVisionIOSimGamePiecesAutoLogged();

  private static final double HORIZONTALFOVRAD = Math.toRadians(65.);
  private static final double VERTICALFOVRAD = Math.toRadians(40.);
  private static final double MAXDISTANCEMETERS = 3.5;

  private final Random random = new Random();

  /**
   * Constructor
   *
   * @param name The configured name of the PhotonVision camera.
   * @param robotToCamera The 3D position and orientation of the camera relative to the robot.
   * @param robotPoseSupplier Function to return the current pose of the robot
   * @param objectHeightMeters Height of the object relative to the floor of field
   */
  public ObjectVisionIOSim(
      String cameraName,
      Transform3d robotToCamera,
      Supplier<Pose2d> robotPoseSupplier,
      double objectHeightMeters) {

    super(cameraName, robotToCamera, robotPoseSupplier, objectHeightMeters);

    // Load all game pieces in this layout
    loadGamePieces("fuel_layout.json");
    simGamePieces.gamePiecesLoaded = simGamePieces.fieldPoses.length > 0;

    // Log the game pieces; the poses can be displayed in AdvantageScope
    Logger.processInputs("ObjectVision/SimGamePieces", simGamePieces);
  }

  /**
   * Get connection status of camera
   *
   * @return Connection status of camera
   */
  @Override
  protected boolean isConnected() {
    return true;
  }

  /**
   * @param fieldToRobot Pose of the robot on the field
   * @return array of observations
   */
  @Override
  protected ObjectObservation[] getObservations(Pose3d fieldToRobot) {

    List<ObjectObservation> observations = new ArrayList<>();

    // Pose of where the robot currently is on the field
    Pose3d robotPose3d = new Pose3d(robotPoseSupplier.get());

    // Determine the camera pose on the field based on its offset from center of robot
    Pose3d cameraPose = robotPose3d.transformBy(robotToCamera);

    // Test all of the predefined game pieces on the field
    for (Pose3d gamePiecePose : simGamePieces.fieldPoses) {
      // Get the vector from the camera to the game piece being tested
      Transform3d cameraToObjectXform = new Transform3d(cameraPose, gamePiecePose);
      Translation3d cameraToObjectVector = cameraToObjectXform.getTranslation();

      // Ignore this game piece if it is behind the camera
      if (cameraToObjectVector.getX() <= 0.0) continue;

      // Get the yaw and pitch of the game piece relative to the center of the camera
      double yawRad = Math.atan2(cameraToObjectVector.getY(), cameraToObjectVector.getX());
      double pitchRad =
          Math.atan2(
              cameraToObjectVector.getZ(),
              Math.hypot(cameraToObjectVector.getX(), cameraToObjectVector.getY()));

      // Ignore this game piece if it is outside the camera's field of view
      if (Math.abs(yawRad) > HORIZONTALFOVRAD / 2.0) continue;
      if (Math.abs(pitchRad) > VERTICALFOVRAD / 2.0) continue;

      // Get the distance of the game piece from the camera and ignore it if it is too far away
      double distance = cameraToObjectVector.getNorm();
      if (distance > MAXDISTANCEMETERS) continue;

      // Reduce the confidence as the distance increases
      double confidence = MathUtil.clamp(1.0 - distance / MAXDISTANCEMETERS, 0.15, 0.95);

      // Introduce some realism by slightly and randomly perturbing the yaw, pitch and confidence
      yawRad += random.nextGaussian() * 0.0087; // About .5 degrees = .0087 radians
      pitchRad += random.nextGaussian() * 0.0087; // About .5 degrees = .0087 radians
      confidence += random.nextGaussian() * 0.03;

      Rotation2d ty = new Rotation2d(pitchRad); // Pitch
      Rotation2d tx = new Rotation2d(-yawRad); // Yaw (NOTE opposite sign)

      // Fuel (ball) game piece has a class ID of 0
      int classID = 0;

      // Estimate an area assuming that a ball at 1 meter fills about 10% of the image
      double kAreaAt1Meter = 0.10;
      double area = kAreaAt1Meter / (distance * distance);
      area = MathUtil.clamp(area, 0.0, 1.0); // Clamp to [0, 1]

      // Estimate the pose of the object relative to the robot
      Pose3d robotRelativePose = estimateRobotRelativeObjectPose(tx, ty, objectHeightMeters);

      // Get the pose of the object relative to the field
      Pose3d robotFieldPose =
          fieldToRobot.transformBy(new Transform3d(Pose3d.kZero, robotRelativePose));

      // Create a generic object observation
      ObjectObservation observation =
          new ObjectObservation(
              Timer.getFPGATimestamp(),
              classID,
              confidence,
              tx,
              ty,
              area,
              robotRelativePose,
              robotFieldPose);

      observations.add(observation);
    }

    return observations.toArray(ObjectObservation[]::new);
  }

  private void loadGamePieces(String jsonFileName) {

    try {
      ObjectMapper mapper = new ObjectMapper();

      File file = new File(Filesystem.getDeployDirectory(), "objectvision/" + jsonFileName);

      GamePieceLayout layout = mapper.readValue(file, GamePieceLayout.class);

      for (GamePiecesOfType gamePiecesOfType : layout.gamePiecesArray) {

        // Get the positions of the game pieces of this type
        List<Pose3d> fieldPoseList = new ArrayList<>();
        for (Position position : gamePiecesOfType.fieldPositions) {
          fieldPoseList.add(position.toPose3d());
        }

        simGamePieces.className = gamePiecesOfType.className;
        simGamePieces.fieldPoses = fieldPoseList.toArray(new Pose3d[0]);
      }

    } catch (Throwable t) {
      System.err.println("CRITICAL: " + t.getMessage());
      t.printStackTrace();
    }
  }
}
