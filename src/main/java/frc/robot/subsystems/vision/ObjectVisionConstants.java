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
  public static boolean objectCameraInTheLoop = false;

  public static String objectCameraName = "rubik-camera1";

  // Robot to camera transforms
  // (Not used by Limelight, configure in web UI instead)
  // For testing Rubik Pi3 in the loop, the camera is on the test board 20.5 inches from floor
  // This camera goes on the back of the robot, facing backwards.
  //
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
  public static Transform3d robotToObjectCamera =
      new Transform3d(
          -Constants.robotLengthInMeters / 2.0,
          0.0,
          Units.inchesToMeters(20.5),
          new Rotation3d(
              0.0,
              Math.toRadians(35.), // Positive points down!!! See above.
              Math.PI));

  public static double fuelDiameterInMeters = 0.15; // Fuel ball is 15 cm in diameter

  // Tuning constants for tracking objects
  public static final double MATCH_DISTANCE_METERS =
      1.5 * ObjectVisionConstants.fuelDiameterInMeters;
  public static final double MAX_STALENESS_SECONDS = 0.5;
  public static final double BLEND_ALPHA = 0.3;
  public static final int MIN_HITS_TO_CONFIRM = 3;
}
