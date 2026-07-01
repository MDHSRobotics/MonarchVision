package frc.robot.subsystems.vision;

import edu.wpi.first.wpilibj.Alert;
import edu.wpi.first.wpilibj.Alert.AlertType;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import java.util.LinkedList;
import java.util.List;
import org.littletonrobotics.junction.Logger;

/** Subsystem to process object observations */
public class ObjectVision extends SubsystemBase {
  private final ObjectVisionIO[] io;
  private final ObjectVisionIOInputsAutoLogged[] inputs;
  private final Alert[] disconnectedAlerts;
  private final Alert[] noResultsAlerts;
  private final Alert[] noTargetsAlerts;
  private final TrackedObjects trackedObjects;

  /**
   * Constructor for ObjectVision class
   *
   * @param io One or more camera objects that comply with the ObjectVisionIO interface.
   */
  public ObjectVision(ObjectVisionIO... io) {
    this.io = io;

    // Initialize the array of inputs to be logged by this class
    inputs = new ObjectVisionIOInputsAutoLogged[io.length];
    for (int i = 0; i < inputs.length; i++) {
      inputs[i] = new ObjectVisionIOInputsAutoLogged();
    }

    // Initialize alerts
    disconnectedAlerts = new Alert[io.length];
    noResultsAlerts = new Alert[io.length];
    noTargetsAlerts = new Alert[io.length];
    for (int i = 0; i < inputs.length; i++) {
      disconnectedAlerts[i] =
          new Alert("Object vision camera " + i + " is disconnected.", AlertType.kWarning);
      noResultsAlerts[i] =
          new Alert("Object vision camera " + i + " is disconnected.", AlertType.kWarning);
      noTargetsAlerts[i] =
          new Alert("Object vision camera " + i + " is disconnected.", AlertType.kWarning);
    }

    // Initialize the object tracker
    trackedObjects = new TrackedObjects();
  }

  /**
   * The Periodic method finds all object observations, passes them to the tracker, and logs them
   */
  @Override
  public void periodic() {
    List<ObjectVisionIO.ObjectObservation> allObservations = new LinkedList<>();

    for (int cameraIndex = 0; cameraIndex < io.length; cameraIndex++) {
      io[cameraIndex].updateInputs(inputs[cameraIndex]);
      Logger.processInputs("ObjectVision/Camera" + cameraIndex, inputs[cameraIndex]);

      disconnectedAlerts[cameraIndex].set(!inputs[cameraIndex].connected);

      noResultsAlerts[cameraIndex].set(
          inputs[cameraIndex].connected && !inputs[cameraIndex].hasResults);

      noTargetsAlerts[cameraIndex].set(
          inputs[cameraIndex].connected
              && inputs[cameraIndex].hasResults
              && !inputs[cameraIndex].hasTargets);

      for (var observation : inputs[cameraIndex].observations) {
        allObservations.add(observation);
      }
    }

    if (allObservations.isEmpty()) {
      return;
    }

    // Feed all the observations from this cycle into the tracker
    trackedObjects.update(
        allObservations.toArray(new ObjectVisionIO.ObjectObservation[0]), Timer.getFPGATimestamp());

    // Log confirmed poses to AdvantageScope
    Logger.recordOutput(
        "ObjectVision/ConfirmedTrackedObjects/FieldPoses", trackedObjects.getConfirmedFieldPoses());
    Logger.recordOutput(
        "ObjectVision/ConfirmedTrackedObjects/RobotRelativePoses",
        trackedObjects.getConfirmedRobotRelativePoses());

    // Log unconfirmed poses to AdvantageScope - Useful for debugging; might want to disable
    Logger.recordOutput(
        "ObjectVision/UnconfirmedTrackedObjects/FieldPoses",
        trackedObjects.getUnconfirmedFieldPoses());
    Logger.recordOutput(
        "ObjectVision/UnconfirmedTrackedObjects/RobotRelativePoses",
        trackedObjects.getUnconfirmedRobotRelativePoses());
  }

  /**
   * Get the observations from a specific camera
   *
   * @param cameraIndex
   * @return Array of observations
   */
  public ObjectVisionIO.ObjectObservation[] getObservations(int cameraIndex) {
    return inputs[cameraIndex].observations;
  }

  /**
   * Get all observations from all cameras
   *
   * @return Array of observations
   */
  public ObjectVisionIO.ObjectObservation[] getAllObservations() {
    List<ObjectVisionIO.ObjectObservation> allObservations = new LinkedList<>();

    for (var input : inputs) {
      for (var observation : input.observations) {
        allObservations.add(observation);
      }
    }

    return allObservations.toArray(new ObjectVisionIO.ObjectObservation[0]);
  }
}
