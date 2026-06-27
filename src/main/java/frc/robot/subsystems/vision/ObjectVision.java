package frc.robot.subsystems.vision;

import edu.wpi.first.wpilibj.Alert;
import edu.wpi.first.wpilibj.Alert.AlertType;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import java.util.LinkedList;
import java.util.List;
import org.littletonrobotics.junction.Logger;

/** Subsystem to process object observations */
public class ObjectVision extends SubsystemBase {
  private final ObjectVisionIO[] io;
  private final ObjectVisionIOInputsAutoLogged[] inputs;
  private final Alert[] disconnectedAlerts;

  /**
   * Constructor for ObjectVision class
   *
   * @param io One or more camera objects that comply with the ObjectVisionIO interface.
   */
  public ObjectVision(ObjectVisionIO... io) {
    this.io = io;

    // Initialize the arrary of inputs to be logged by this class
    inputs = new ObjectVisionIOInputsAutoLogged[io.length];
    for (int i = 0; i < inputs.length; i++) {
      inputs[i] = new ObjectVisionIOInputsAutoLogged();
    }

    // Initialize disconnected alerts
    disconnectedAlerts = new Alert[io.length];
    for (int i = 0; i < inputs.length; i++) {
      disconnectedAlerts[i] =
          new Alert("Object vision camera " + i + " is disconnected.", AlertType.kWarning);
    }
  }

  /**
   * The Periodic method finds all object observations, passes them to the consumer, and logs them
   */
  @Override
  public void periodic() {
    List<ObjectVisionIO.ObjectObservation> allObservations = new LinkedList<>();

    for (int cameraIndex = 0; cameraIndex < io.length; cameraIndex++) {
      io[cameraIndex].updateInputs(inputs[cameraIndex]);
      Logger.processInputs("ObjectVision/Camera" + cameraIndex, inputs[cameraIndex]);

      disconnectedAlerts[cameraIndex].set(!inputs[cameraIndex].connected);

      for (var observation : inputs[cameraIndex].observations) {
        allObservations.add(observation);
      }
    }

    // Log all the raw observations
    Logger.recordOutput(
        "ObjectVision/Summary/RawObservations",
        allObservations.toArray(new ObjectVisionIO.ObjectObservation[0]));
  }

  public ObjectVisionIO.ObjectObservation[] getObservations(int cameraIndex) {
    return inputs[cameraIndex].observations;
  }

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
