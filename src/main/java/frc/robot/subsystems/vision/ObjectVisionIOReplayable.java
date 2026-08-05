package frc.robot.subsystems.vision;

public class ObjectVisionIOReplayable implements ObjectVisionIO {

  private final String cameraName;

  public ObjectVisionIOReplayable(String cameraName) {
    this.cameraName = cameraName;
  }

  // Get a human-friendly name for this interface
  public String getName() {
    return cameraName;
  }
}
