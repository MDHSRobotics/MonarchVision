package frc.robot.subsystems.vision;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Selects a target location by finding the most favorable cluster of balls */
public class TargetLocatorSimpleBallCluster extends TargetLocator {
  // Balls within this distance are considered part of the same cluster.
  private static final double CLUSTER_DISTANCE_METERS = 0.85;

  // Ignore detections farther away than this.
  private static final double MAX_TARGET_DISTANCE_METERS = 4.0;

  // Aim slightly beyond the center so the robot drives through the cluster.
  private static final double DRIVE_THROUGH_DISTANCE_METERS = 0.40;

  public TargetLocatorSimpleBallCluster() {
    super();
  }

  /**
   * Selects and locks a target. Call this when the assist command begins.
   *
   * @param robotPose current field-relative robot pose
   * @param detectedBalls field-relative positions of detected balls
   */
  @Override
  public Optional<Translation2d> selectTarget(Pose2d robotPose, List<Translation2d> detectedBalls) {

    List<Translation2d> validBalls =
        detectedBalls.stream()
            .filter(
                ball -> ball.getDistance(robotPose.getTranslation()) <= MAX_TARGET_DISTANCE_METERS)
            .toList();

    if (validBalls.isEmpty()) {
      lockedTarget = null;
      return Optional.empty();
    }

    List<Cluster> clusters = buildClusters(validBalls);

    /*
     * Prefer:
     *   1. More balls
     *   2. The closer cluster when ball counts are equal
     */
    Cluster selected =
        clusters.stream()
            .max(
                Comparator.comparingInt(Cluster::ballCount)
                    .thenComparingDouble(
                        cluster -> -cluster.center().getDistance(robotPose.getTranslation())))
            .orElse(null);

    if (selected == null) {
      lockedTarget = null;
      return Optional.empty();
    }

    lockedTarget = calculateDriveThroughTarget(robotPose, selected.center());

    return Optional.of(lockedTarget);
  }

  private List<Cluster> buildClusters(List<Translation2d> balls) {
    List<Cluster> clusters = new ArrayList<>();
    boolean[] assigned = new boolean[balls.size()];

    for (int i = 0; i < balls.size(); i++) {
      if (assigned[i]) {
        continue;
      }

      List<Translation2d> members = new ArrayList<>();
      List<Integer> pending = new ArrayList<>();

      assigned[i] = true;
      pending.add(i);

      while (!pending.isEmpty()) {
        int currentIndex = pending.remove(pending.size() - 1);
        Translation2d currentBall = balls.get(currentIndex);

        members.add(currentBall);

        for (int candidateIndex = 0; candidateIndex < balls.size(); candidateIndex++) {

          if (assigned[candidateIndex]) {
            continue;
          }

          Translation2d candidate = balls.get(candidateIndex);

          if (currentBall.getDistance(candidate) <= CLUSTER_DISTANCE_METERS) {
            assigned[candidateIndex] = true;
            pending.add(candidateIndex);
          }
        }
      }

      clusters.add(new Cluster(calculateCenter(members), members));
    }

    return clusters;
  }

  private Translation2d calculateCenter(List<Translation2d> members) {

    double totalX = 0.0;
    double totalY = 0.0;

    for (Translation2d member : members) {
      totalX += member.getX();
      totalY += member.getY();
    }

    return new Translation2d(totalX / members.size(), totalY / members.size());
  }

  private Translation2d calculateDriveThroughTarget(Pose2d robotPose, Translation2d clusterCenter) {

    Translation2d robotToCluster = clusterCenter.minus(robotPose.getTranslation());

    if (robotToCluster.getNorm() < 0.05) {
      return clusterCenter;
    }

    Translation2d driveThroughOffset =
        robotToCluster.div(robotToCluster.getNorm()).times(DRIVE_THROUGH_DISTANCE_METERS);

    return clusterCenter.plus(driveThroughOffset);
  }

  private record Cluster(Translation2d center, List<Translation2d> members) {

    public int ballCount() {
      return members.size();
    }
  }
}
