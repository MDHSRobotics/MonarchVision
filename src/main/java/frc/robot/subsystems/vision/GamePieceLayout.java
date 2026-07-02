package frc.robot.subsystems.vision;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import java.util.ArrayList;
import java.util.List;

/* This class defines the field locations of game pieces - is is simple to lend itself to json */

// This is currently used by objectvision simulation but could be useful elsewhere

public class GamePieceLayout {
  public List<GamePiecesOfType> gamePiecesArray = new ArrayList<>();

  // This is to allow the layout to consist of more than one type of game piece
  public static class GamePiecesOfType {
    public String className;      // Name for the game piece
    public List<Position> fieldPositions = new ArrayList<>(); // One (x,y,z) position for each instance of this type of game piece
  }

  public static class Position {
    public double x;
    public double y;
    public double z;

    public Position() {}

    public Position(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }

    public Pose3d toPose3d() {
      return new Pose3d(x, y, z, new Rotation3d());
    }
  }

  public GamePieceLayout() {}

  /**
   * Return the name of the object class based on its id
   *
   * @param id ID of object class
   * @return Name of the object class
   */
  private static String GamePieceNameFromId(int id) {

    // NOTE: This needs to be updated whenever PhotonVision is trained on other types of objects
    return switch (id) {
      case 0 -> "Fuel";
      default -> "unknown";
    };
  }

  /**
   * Return the name of the object class based on its id
   *
   * @param id ID of object class
   * @return Name of the object class
   */
  private static int GamePieceIDFromName(String name) {

    // NOTE: This needs to be updated whenever PhotonVision is trained on other types of objects
    return switch (name) {
      case "Fuel" -> 0;
      default -> 99999;
    };
  }
}
