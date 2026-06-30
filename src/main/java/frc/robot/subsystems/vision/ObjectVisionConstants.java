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
  public static boolean objectCameraInTheLoop = true;

  public static String objectCameraName = "rubik-camera1";

  // Robot to camera transforms
  // (Not used by Limelight, configure in web UI instead)
  // For testing Rubik Pi3 in the loop, the camera is on the test board 20.5 inches from floor
  // This camera goes on the back of the robot, facing backwards.
  public static Transform3d robotToObjectCamera =
      new Transform3d(
          -Constants.robotLengthInMeters / 2.0,
          0.0,
          Units.inchesToMeters(20.5),
          new Rotation3d(0.0, Math.toRadians(35.), Math.PI));

  public static double fuelDiameterInMeters = 0.15; // Fuel ball is 15 cm in diameter

  // Tuning constants for tracking objects
  public static final double MATCH_DISTANCE_METERS =
      1.5 * ObjectVisionConstants.fuelDiameterInMeters;
  public static final double MAX_STALENESS_SECONDS = 0.5;
  public static final double BLEND_ALPHA = 0.3;
  public static final int MIN_HITS_TO_CONFIRM = 3;
}
