package frc.robot.commands;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.drive.Drive;
import frc.robot.subsystems.vision.ObjectVision;
import frc.robot.subsystems.vision.TargetLocator;
import org.littletonrobotics.junction.Logger;

public class DriveRobotFeatureToTarget extends Command {
  private static final double MIN_SPEED_METERS_PER_SECOND = 0.35;
  private static final double MAX_SPEED_METERS_PER_SECOND = 1.5;
  private static final double TRANSLATION_KP = 0.8;

  private static final double HEADING_KP = 3.0;
  private static final double MAX_OMEGA_RADIANS_PER_SECOND = 3.0;

  /*
   * Continue this far beyond the target before ending.
   *
   * Set to 0.0 to finish as soon as the feature crosses the target plane.
   * A small positive value helps ensure the pickup passes fully through it.
   */
  private static final double DRIVE_THROUGH_DISTANCE_METERS = 0.05;

  private final Drive drive;
  private final ObjectVision objectVision;
  private final TargetLocator targetLocator;

  /*
   * Transform expressed in robot coordinates:
   *
   * Translation:
   *   +X = toward the robot front
   *   +Y = toward the robot left
   *
   * Rotation:
   *   Direction the positioning feature should face.
   */
  private final Transform2d robotToPositioningFeature;

  private Translation2d target;

  /*
   * Fixed unit vector from the feature toward the target when the command
   * starts. This is used to determine when the feature has passed the target.
   */
  private Translation2d initialApproachDirection;

  private double remainingDistanceAlongApproach;
  private boolean targetPassed;

  public DriveRobotFeatureToTarget(
      Drive drive,
      ObjectVision objectVision,
      TargetLocator targetLocator,
      Transform2d robotToPositioningFeature) {

    this.drive = drive;
    this.objectVision = objectVision;
    this.targetLocator = targetLocator;
    this.robotToPositioningFeature = robotToPositioningFeature;

    addRequirements(drive);
  }

  @Override
  public void initialize() {
    target = objectVision.selectTarget(targetLocator, drive.getPose()).orElse(null);

    initialApproachDirection = null;
    remainingDistanceAlongApproach = Double.POSITIVE_INFINITY;
    targetPassed = false;

    if (target == null) {
      return;
    }

    Pose2d initialRobotPose = drive.getPose();

    /*
     * transformBy()/plus() applies the robot-relative transform to the
     * field-relative robot pose, producing the feature's field pose.
     */
    Pose2d initialFeaturePose = initialRobotPose.transformBy(robotToPositioningFeature);

    Translation2d initialFeatureToTarget = target.minus(initialFeaturePose.getTranslation());

    double initialDistance = initialFeatureToTarget.getNorm();

    if (initialDistance < 1e-6) {
      /*
       * The feature already starts at the target. There is no meaningful
       * approach direction.
       */
      targetPassed = true;
      return;
    }

    initialApproachDirection = initialFeatureToTarget.div(initialDistance);

    remainingDistanceAlongApproach = initialDistance;

    Logger.recordOutput("ObjectVision/DriveAssist/InitialFeaturePose", initialFeaturePose);
    Logger.recordOutput(
        "ObjectVision/DriveAssist/InitialApproachDirection", initialApproachDirection);
  }

  @Override
  public void execute() {
    if (target == null || initialApproachDirection == null) {
      drive.runVelocity(new ChassisSpeeds());
      return;
    }

    Pose2d robotPose = drive.getPose();

    /*
     * Current field-relative pose of the pickup or other positioning feature.
     */
    Pose2d positioningFeaturePose = robotPose.transformBy(robotToPositioningFeature);

    /*
     * Field-relative vector from the feature to the target.
     */
    Translation2d featureToTarget = target.minus(positioningFeaturePose.getTranslation());

    double distance = featureToTarget.getNorm();

    /*
     * Dot product with the original approach direction:
     *
     *   positive: feature has not reached the target plane
     *   zero:     feature is on the target plane
     *   negative: feature has passed through the target plane
     */
    remainingDistanceAlongApproach =
        featureToTarget.getX() * initialApproachDirection.getX()
            + featureToTarget.getY() * initialApproachDirection.getY();

    targetPassed = remainingDistanceAlongApproach <= -DRIVE_THROUGH_DISTANCE_METERS;

    if (targetPassed) {
      drive.runVelocity(new ChassisSpeeds());
      logOutputs(positioningFeaturePose, distance, new Rotation2d(), new Rotation2d(), 0.0);
      return;
    }

    /*
     * featureToTarget is field-relative, so its angle is already the
     * field-relative direction toward the target.
     *
     * Do not subtract positioningFeaturePose.getRotation() here.
     */
    Rotation2d travelDirection;

    /*
     * Very close to the target, featureToTarget.getAngle() becomes sensitive
     * to small odometry changes. Continue along the original approach
     * direction through the target instead.
     */
    if (remainingDistanceAlongApproach < 0.20) {
      travelDirection = initialApproachDirection.getAngle();
    } else {
      travelDirection = featureToTarget.getAngle();
    }

    double speedMetersPerSecond =
        MathUtil.clamp(
            TRANSLATION_KP * distance, MIN_SPEED_METERS_PER_SECOND, MAX_SPEED_METERS_PER_SECOND);

    /*
     * These are field-relative velocity components.
     */
    double vxField = speedMetersPerSecond * travelDirection.getCos();

    double vyField = speedMetersPerSecond * travelDirection.getSin();

    /*
     * The feature's field heading equals:
     *
     *   robot heading + feature rotation relative to robot
     *
     * Therefore:
     *
     *   desired robot heading =
     *       desired feature heading - feature-relative rotation
     */
    Rotation2d desiredRobotHeading = travelDirection.minus(robotToPositioningFeature.getRotation());

    double headingError =
        MathUtil.angleModulus(
            desiredRobotHeading.getRadians() - robotPose.getRotation().getRadians());

    double omega =
        MathUtil.clamp(
            HEADING_KP * headingError, -MAX_OMEGA_RADIANS_PER_SECOND, MAX_OMEGA_RADIANS_PER_SECOND);

    /*
     * Reduce translation while the positioning feature is poorly aligned.
     * This prevents the swerve from moving aggressively sideways while
     * rotating into the correct pickup orientation.
     */
    double absoluteHeadingError = Math.abs(headingError);
    double alignmentScale;

    if (absoluteHeadingError >= Math.toRadians(90.0)) {
      alignmentScale = 0.0;
    } else {
      alignmentScale = MathUtil.clamp(Math.cos(absoluteHeadingError), 0.20, 1.0);
    }

    vxField *= alignmentScale;
    vyField *= alignmentScale;

    ChassisSpeeds robotRelativeSpeeds =
        ChassisSpeeds.fromFieldRelativeSpeeds(vxField, vyField, omega, robotPose.getRotation());

    drive.runVelocity(robotRelativeSpeeds);

    logOutputs(
        positioningFeaturePose, distance, travelDirection, desiredRobotHeading, headingError);

    Logger.recordOutput("ObjectVision/DriveAssist/VxField", vxField);
    Logger.recordOutput("ObjectVision/DriveAssist/VyField", vyField);
    Logger.recordOutput("ObjectVision/DriveAssist/Omega", omega);
    Logger.recordOutput("ObjectVision/DriveAssist/AlignmentScale", alignmentScale);
  }

  private void logOutputs(
      Pose2d positioningFeaturePose,
      double distance,
      Rotation2d travelDirection,
      Rotation2d desiredRobotHeading,
      double headingError) {

    Logger.recordOutput("ObjectVision/DriveAssist/Target", target);
    Logger.recordOutput("ObjectVision/DriveAssist/PositioningFeaturePose", positioningFeaturePose);
    Logger.recordOutput("ObjectVision/DriveAssist/DistanceMeters", distance);
    Logger.recordOutput(
        "ObjectVision/DriveAssist/RemainingAlongApproach", remainingDistanceAlongApproach);
    Logger.recordOutput("ObjectVision/DriveAssist/TravelDirection", travelDirection);
    Logger.recordOutput("ObjectVision/DriveAssist/DesiredRobotHeading", desiredRobotHeading);
    Logger.recordOutput("ObjectVision/DriveAssist/HeadingErrorRadians", headingError);
    Logger.recordOutput("ObjectVision/DriveAssist/TargetPassed", targetPassed);
  }

  @Override
  public void end(boolean interrupted) {
    drive.runVelocity(new ChassisSpeeds());
    targetLocator.clearTarget();

    Logger.recordOutput("ObjectVision/DriveAssist/Active", false);
  }

  @Override
  public boolean isFinished() {
    /*
     * End immediately if no target existed when the command started.
     */
    return target == null || targetPassed;
  }
}
