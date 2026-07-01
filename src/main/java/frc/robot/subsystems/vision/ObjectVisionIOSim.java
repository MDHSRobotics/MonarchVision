package frc.robot.subsystems.vision;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.wpilibj.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/***
 * Simulated PhotonVision for ObjectVision IO. This class maintains a list of fuel objects
 * and their locations on the field. When the simulated PhotonVision camera is in a
 * position to detect one or more of them, this class will compute the yaw and pitch.
 */
public class ObjectVisionIOSim extends ObjectVisionIOBase {

  private final List<GamePiece> gamePieces = new ArrayList<>();

  class GamePiece {
    public int classID; // ID of this class of game piece (0=fuel)
    Pose3d fieldPose; // Pose of this game piece on the field

    GamePiece(int classID, Pose3d fieldPose) {
      this.classID = classID;
      this.fieldPose = fieldPose;
    }
  }

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

    // Load locations of balls
    GamePiece gp1 =
        new GamePiece(
            0,
            new Pose3d(
                new Translation3d(1., 1., ObjectVisionConstants.fuelDiameterInMeters / 2.),
                new Rotation3d(0., 0., 0.)));
    gamePieces.add((gp1));
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

    for (GamePiece gamePiece : gamePieces) {

      double area = .1;
      Rotation2d ty = new Rotation2d(Math.toRadians(10.)); // Pitch
      Rotation2d tx = new Rotation2d(Math.toRadians(20.)); // Yaw
      int classID = 0;
      double confidence = 1.0;

      Pose3d robotRelativePose = estimateRobotRelativeObjectPose(tx, ty, objectHeightMeters);

      Pose3d fieldPose = fieldToRobot.transformBy(new Transform3d(Pose3d.kZero, robotRelativePose));

      ObjectObservation observation =
          new ObjectObservation(
              Timer.getFPGATimestamp(),
              classID,
              confidence,
              tx,
              ty,
              area,
              robotRelativePose,
              fieldPose);

      observations.add(observation);
    }

    return observations.toArray(ObjectObservation[]::new);
  }
}
