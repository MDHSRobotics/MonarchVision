package frc.robot.commands;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.drive.Drive;
import frc.robot.subsystems.vision.ObjectVision;
import frc.robot.subsystems.vision.TargetLocatorSimpleBallCluster;
import org.littletonrobotics.junction.Logger;

public class DriveToBallCluster extends Command {

  private final Drive drive;
  private final ObjectVision objectVision;
  private final TargetLocatorSimpleBallCluster targetLocator;

  private Translation2d target;

  public DriveToBallCluster(Drive drive, ObjectVision objectVision) {

    this.drive = drive;
    this.objectVision = objectVision;
    this.targetLocator = new TargetLocatorSimpleBallCluster();

    addRequirements(drive);
  }

  @Override
  public void initialize() {
    target = objectVision.selectTarget(targetLocator, drive.getPose()).orElse(null);
  }

  @Override
  public void execute() {
    if (target == null) {
      drive.runVelocity(new ChassisSpeeds());
      return;
    }

    Pose2d robotPose = drive.getPose();

    Translation2d robotToTarget = target.minus(robotPose.getTranslation());

    double distance = robotToTarget.getNorm();

    if (distance < 0.10) {
      drive.runVelocity(new ChassisSpeeds());
      return;
    }

    /*
     * Direction from the robot toward the target in field coordinates.
     */
    Rotation2d travelDirection = robotToTarget.getAngle();

    /*
     * Continue moving through the cluster rather than gradually slowing
     * to zero too early.
     */
    double speedMetersPerSecond = MathUtil.clamp(0.5 + distance * 0.8, 0.5, 1.5);

    /*
     * Translate toward the target.
     */
    double vxField = speedMetersPerSecond * travelDirection.getCos();

    double vyField = speedMetersPerSecond * travelDirection.getSin();

    /*
     * Robot pose rotation represents the direction the front faces.
     *
     * Since the pickup is on the front, the front should face the target.
     */
    Rotation2d desiredRobotHeading = travelDirection.plus(Rotation2d.kZero);

    double headingError =
        MathUtil.angleModulus(
            desiredRobotHeading.getRadians() - robotPose.getRotation().getRadians());

    double omega = MathUtil.clamp(headingError * 3.0, -3.0, 3.0);

    ChassisSpeeds robotRelativeSpeeds =
        ChassisSpeeds.fromFieldRelativeSpeeds(vxField, vyField, omega, robotPose.getRotation());

    drive.runVelocity(robotRelativeSpeeds);

    Logger.recordOutput("ObjectVision/DriveAssist/Target", target);
    Logger.recordOutput("ObjectVision/DriveAssist/DistanceMeters", distance);
    Logger.recordOutput("ObjectVision/DriveAssist/TravelDirection", travelDirection);
    Logger.recordOutput("ObjectVision/DriveAssist/DesiredRobotHeading", desiredRobotHeading);
    Logger.recordOutput("ObjectVision/DriveAssist/HeadingErrorRadians", headingError);
  }

  @Override
  public void end(boolean interrupted) {
    drive.runVelocity(new ChassisSpeeds());
    targetLocator.clearTarget();
  }

  @Override
  public boolean isFinished() {
    return false;
  }
}
