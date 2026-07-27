// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package frc.robot.subsystems.vision;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.util.Units;
import frc.robot.Constants;

public class FieldVisionConstants {

  /** Are we running a physics simulator, but with a real camera connected. */
  public static boolean rubikInTheLoop = true;

  public static boolean limelightInTheLoop = true;

  public static String rubikBackCameraInLoopName = "rubik_camera2";
  public static String rubikLeftCameraInLoopName = "rubik_camera3";
  public static String limelightInLoopCameraName = "limelight-one";

  // AprilTag layout
  public static AprilTagFieldLayout aprilTagLayout =
      AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField);

  // Camera names, must match names configured on coprocessor
  public static String camera0Name = "camera_0";
  public static String camera1Name = "camera_1";

  // Robot to camera transforms
  // (Not used by Limelight, configure in web UI instead)

  // NOTE: Pay attention to the signs of the camera rotation because they are not intuitive,
  //       especially the pitch. You would think that a camera pointed down to the floor
  //       would have a negative pitch but actually it should be positive. All the anlges use
  //       the right-hand rule: point the thumb on your right hand in the positive direction
  //       of the axis you are going to rotate about. Then your finger curl in the positive
  //       direction of rotation. The convention is that the x-axis is positive toward the
  //       front of the robot, y-axis is positive toward the right of the robot; z-axis is
  //       positive vertically up. So pitch is a rotation about y-axis so point your right
  //       thumb toward the right of the robot and you will see that curling your fingers
  //       will tilt the front of the robot down.

  // First camera goes on the front of the robot, tilted down
  public static Transform3d robotToCamera0 =
      new Transform3d(
          Constants.robotLengthInMeters / 2.0, 0.0, 0.2, new Rotation3d(0.0, -0.4, 0.0));
  // Second camera goes on the back of the robot, tilted down
  public static Transform3d robotToCamera1 =
      new Transform3d(
          -Constants.robotLengthInMeters / 2.0, 0.0, 0.2, new Rotation3d(0.0, -0.4, Math.PI));

  // Camera positions on test harness
  // One camera is simulated to be on back of robot, pointing up
  public static Transform3d robotToBackRubikCameraInLoop =
      new Transform3d(
          -Constants.robotLengthInMeters / 2.0,
          0.0,
          Units.inchesToMeters(7),
          new Rotation3d(
              0.0,
              -Math.toRadians(26), // Negative points up!!! See above.
              Math.PI));

  // The other camera is  simulated to be on left side of robot, facing straight ahead
  public static Transform3d robotToLeftRubikCameraInLoop =
      new Transform3d(
          0.0,
          Constants.robotWidthInMeters / 2.0,
          Units.inchesToMeters(22),
          new Rotation3d(0.0, 0.0, Math.PI / 2));

  // Basic filtering thresholds
  public static double maxAmbiguity = 0.3;
  public static double maxZError = 0.15;

  // Standard deviation baselines, for 1 meter distance and 1 tag
  // (Adjusted automatically based on distance and # of tags)
  public static double linearStdDevBaseline = 0.02; // Meters
  public static double angularStdDevBaseline = 0.06; // Radians

  // Standard deviation multipliers for each camera
  // (Adjust to trust some cameras more than others)
  public static double[] cameraStdDevFactors =
      new double[] {
        1.0, // Camera 0
        1.0 // Camera 1
      };

  // Multipliers to apply for MegaTag 2 observations
  public static double linearStdDevMegatag2Factor = 0.5; // More stable than full 3D solve
  public static double angularStdDevMegatag2Factor =
      Double.POSITIVE_INFINITY; // No rotation data available
}
