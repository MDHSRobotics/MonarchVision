// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package frc.robot;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.RobotBase;

/**
 * This class defines the runtime mode used by AdvantageKit. The mode is always "real" when running
 * on a roboRIO. Change the value of "simMode" to switch between "sim" (physics sim) and "replay"
 * (log replay from a file).
 */
public final class Constants {
  public static final Mode simMode = Mode.REPLAY;
  public static final Mode currentMode = RobotBase.isReal() ? Mode.REAL : simMode;
  // public static final Mode currentMode = RobotBase.isReal() ? Mode.REAL : Mode.REPLAY;

  public static enum Mode {
    /** Running on a real robot. */
    REAL,

    /** Running a physics simulator. */
    SIM,

    /** Replaying from a log file. */
    REPLAY
  }

  // By making the robot 24 inches square, it will match one of the predefined sizes
  // for the AdvantageScope robot on the 2D field
  public static final double robotLengthInMeters = Units.inchesToMeters(24.);
  public static final double robotWidthInMeters = Units.inchesToMeters(24.);

  // Looking from above, how to locate the pickup opening relative to the center of the robot
  public static final Transform2d robotToPickupXform =
      new Transform2d(robotLengthInMeters / 2., 0., new Rotation2d(0.));
}
