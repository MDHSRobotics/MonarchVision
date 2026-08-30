// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package frc.robot.subsystems.vision;

import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.util.Units;
import frc.robot.Constants;

public class ObjectVisionConstants {

  /** Are we running a physics simulator, but with a real Rubik Pi3 connected. */
  public static boolean hardwareInTheLoop = true;

  // Notes about robot to camera transforms
  // 1. Limelight does not yet support object detection (at least not well enough)
  //
  // 2. Pay attention to the signs of the camera rotation because they are not intuitive,
  //    especially the pitch. You would think that a camera pointed down to the floor
  //    would have a negative pitch but actually it should be positive. All the angles use
  //    the right-hand rule: point the thumb on your right hand in the positive direction
  //    of the axis you are going to rotate about. Then your finger curl in the positive
  //    direction of rotation. The convention is that the x-axis is positive toward the
  //    front of the robot, y-axis is positive toward the left of the robot; z-axis is
  //    positive vertically up. So pitch is a rotation about y-axis so point your right
  //    thumb toward the left of the robot and you will see that curling your fingers
  //    will tilt the front of the robot down.

  // The following is a definition of all cameras on the real robot used for object detection.
  // These are also used by simulation
  public static CameraSpec[] robotCameras = {
    new CameraSpec(
        VisionType.PHOTONVISION,
        "object_camera",
        new Transform3d(
            Constants.robotLengthInMeters / 2.0, // On front of robot
            0.0,
            Units.inchesToMeters(20.5),
            new Rotation3d(
                0.0,
                Math.toRadians(35.), // Positive points down!!! See above.
                0.))) // Facing forwards
  };

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
                Math.PI))), // Facing backwards
    new CameraSpec(
        VisionType.LIMELIGHT,
        "limelight-one",
        new Transform3d(
            Constants.robotLengthInMeters / 2.0, // On front of robot
            0.0,
            Units.inchesToMeters(26),
            new Rotation3d(
                0.0,
                Math.toRadians(36.), // Positive points down!!! See above.
                0.))) // Facing forward
  };

  public static double fuelDiameterInMeters = 0.15; // Fuel ball is 15 cm in diameter

  // Tuning constants for tracking objects
  public static final double MATCH_DISTANCE_METERS =
      0.8 * ObjectVisionConstants.fuelDiameterInMeters;
  public static final double MAX_STALENESS_SECONDS = 0.5;
  public static final double BLEND_ALPHA = 0.3;
  public static final int MIN_HITS_TO_CONFIRM = 3;
}
