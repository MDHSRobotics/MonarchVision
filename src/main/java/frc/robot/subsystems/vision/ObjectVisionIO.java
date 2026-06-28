package frc.robot.subsystems.vision;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import org.littletonrobotics.junction.AutoLog;

/*
 * This interface is for object detection (as opposed to April tag detection)
 */

public interface ObjectVisionIO {
  @AutoLog
  public static class ObjectVisionIOInputs {
    public boolean connected = false;
    public ObjectObservation[] observations = new ObjectObservation[0];
  }

  /** Represents one detected object from PhotonVision object detection. */
  public static record ObjectObservation(
      double timestamp, // Timestamp of the observation
      int classId, // ID of the detected object
      String className, // Class name of the detected object (e.g., "fuel")
      double confidence, // Confidence level: 0=not confident; 1=certain
      Rotation2d tx, // Yaw
      Rotation2d ty, // Pitch
      double area, // % Area of the frame occupied by the object
      Pose3d robotRelativePose, // Pose of the object relative to the robot
      Pose3d fieldPose) // Pose of the object relative to the field
  {}

  /**
   * Return all of the object observations
   *
   * @param inputs Object of type ObjectVisionIOInputs that will be populated with the connected
   *     status of the camera plus an array of object observations
   */
  public default void updateInputs(ObjectVisionIOInputs inputs) {}
}
