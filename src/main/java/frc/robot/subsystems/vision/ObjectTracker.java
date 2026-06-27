package frc.robot.subsystems.vision;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.subsystems.vision.ObjectVisionIO.ObjectObservation;
import java.util.LinkedList;
import java.util.List;
import org.littletonrobotics.junction.Logger;

public class ObjectTracker extends SubsystemBase {
  private final ObjectVision objectVision;
  private final List<ObjectVisionIO.ObjectObservation> trackedObservations;

  public ObjectTracker(ObjectVision objectVision) {
    this.objectVision = objectVision;
    this.trackedObservations = new LinkedList<>();
  }

  @Override
  public void periodic() {
    updateTrackedObservations(objectVision.getAllObservations());

    // Log all the tracked observations
    Logger.recordOutput(
        "ObjectVision/Summary/TrackedObservations",
        trackedObservations.toArray(new ObjectVisionIO.ObjectObservation[0]));
  }

  public void updateTrackedObservations(ObjectVisionIO.ObjectObservation[] observations) {
    // TODO: Modify the list of tracked objects rather than just blindly using them all
    trackedObservations.clear();
    for (ObjectObservation observation : observations) {
      trackedObservations.add(observation);
    }
  }
}
