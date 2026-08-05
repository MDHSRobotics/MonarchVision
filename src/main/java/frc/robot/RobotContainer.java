// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package frc.robot;

import static frc.robot.subsystems.vision.FieldVisionConstants.*;

import com.pathplanner.lib.auto.AutoBuilder;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandPS4Controller;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine;
import frc.robot.Constants.Mode;
import frc.robot.commands.DriveCommands;
import frc.robot.commands.DriveRobotFeatureToTarget;
import frc.robot.generated.TunerConstants;
import frc.robot.subsystems.drive.Drive;
import frc.robot.subsystems.drive.GyroIO;
import frc.robot.subsystems.drive.GyroIOPigeon2;
import frc.robot.subsystems.drive.ModuleIO;
import frc.robot.subsystems.drive.ModuleIOSim;
import frc.robot.subsystems.drive.ModuleIOTalonFX;
import frc.robot.subsystems.vision.CameraSpec;
import frc.robot.subsystems.vision.FieldVision;
import frc.robot.subsystems.vision.FieldVisionConstants;
import frc.robot.subsystems.vision.FieldVisionIO;
import frc.robot.subsystems.vision.FieldVisionIOLimelight;
import frc.robot.subsystems.vision.FieldVisionIOPhotonVision;
import frc.robot.subsystems.vision.FieldVisionIOPhotonVisionSim;
import frc.robot.subsystems.vision.FieldVisionIOReplayable;
import frc.robot.subsystems.vision.ObjectVision;
import frc.robot.subsystems.vision.ObjectVisionConstants;
import frc.robot.subsystems.vision.ObjectVisionIO;
import frc.robot.subsystems.vision.ObjectVisionIOPhotonVision;
import frc.robot.subsystems.vision.ObjectVisionIOReplayable;
import frc.robot.subsystems.vision.ObjectVisionIOSim;
import frc.robot.subsystems.vision.TargetLocatorSimpleBallCluster;
import java.util.ArrayList;
import java.util.List;
import org.littletonrobotics.junction.networktables.LoggedDashboardChooser;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and button mappings) should be declared here.
 */
public class RobotContainer {
  // Subsystems
  private final Drive drive;
  private final FieldVision fieldVision;
  private final ObjectVision objectVision;

  // Controller
  private final CommandPS4Controller controller = new CommandPS4Controller(0);
  // private final CommandXboxController controller = new CommandXboxController(0);

  // Dashboard inputs
  private final LoggedDashboardChooser<Command> autoChooser;

  /** The container for the robot. Contains subsystems, OI devices, and commands. */
  public RobotContainer() {
    switch (Constants.currentMode) {
      case REAL:
        // Real robot, instantiate hardware IO implementations

        // ModuleIOTalonFX is intended for modules with TalonFX drive, TalonFX turn, and
        // a CANcoder
        drive =
            new Drive(
                new GyroIOPigeon2(),
                new ModuleIOTalonFX(TunerConstants.FrontLeft),
                new ModuleIOTalonFX(TunerConstants.FrontRight),
                new ModuleIOTalonFX(TunerConstants.BackLeft),
                new ModuleIOTalonFX(TunerConstants.BackRight));

        // The ModuleIOTalonFXS implementation provides an example implementation for
        // TalonFXS controller connected to a CANdi with a PWM encoder. The
        // implementations
        // of ModuleIOTalonFX, ModuleIOTalonFXS, and ModuleIOSpark (from the Spark
        // swerve
        // template) can be freely intermixed to support alternative hardware
        // arrangements.
        // Please see the AdvantageKit template documentation for more information:
        // https://docs.advantagekit.org/getting-started/template-projects/talonfx-swerve-template#custom-module-implementations
        //
        // drive =
        // new Drive(
        // new GyroIOPigeon2(),
        // new ModuleIOTalonFXS(TunerConstants.FrontLeft),
        // new ModuleIOTalonFXS(TunerConstants.FrontRight),
        // new ModuleIOTalonFXS(TunerConstants.BackLeft),
        // new ModuleIOTalonFXS(TunerConstants.BackRight));

        // Get the list of cameras on the robot for field orientation
        List<FieldVisionIO> robotFieldVisionIoList =
            createFieldVisionIOList(false, FieldVisionConstants.robotCameras);

        fieldVision =
            new FieldVision(
                drive::addVisionMeasurement, robotFieldVisionIoList.toArray(FieldVisionIO[]::new));

        // Get the list of cameras on the robot for object detection
        List<ObjectVisionIO> objectVisionIoList =
            createObjectVisionIOList(false, ObjectVisionConstants.robotCameras);

        objectVision = new ObjectVision(objectVisionIoList.toArray(ObjectVisionIO[]::new));
        break;

      case SIM:
        // Sim robot, instantiate physics sim IO implementations

        drive =
            new Drive(
                new GyroIO() {},
                new ModuleIOSim(TunerConstants.FrontLeft),
                new ModuleIOSim(TunerConstants.FrontRight),
                new ModuleIOSim(TunerConstants.BackLeft),
                new ModuleIOSim(TunerConstants.BackRight));

        //       ----------- VISION FOR FIELD ORIENTATION ----------------

        // This is the list of "cameras" to use in simulation for field orientation
        // It will either be simulation cameras based on PhotonVision - or
        // some combination of real Limelight and/or PhotonVision cameras acting as hw-in-the-loop
        List<FieldVisionIO> simFieldVisionIoList;
        if (FieldVisionConstants.hardwareInTheLoop) {
          // We are running simulation but with a real vision hardware to detect apriltags
          simFieldVisionIoList =
              createFieldVisionIOList(false, FieldVisionConstants.hardwareInLoopCameras);
        } else {
          // There is no camera hardware in the loop so this is pure simulation of April tag
          // detection
          simFieldVisionIoList = createFieldVisionIOList(true, FieldVisionConstants.robotCameras);
        }
        // Create the FieldVision object using the "cameras" selected above
        fieldVision =
            new FieldVision(
                drive::addVisionMeasurement, simFieldVisionIoList.toArray(FieldVisionIO[]::new));

        //       ----------- VISION FOR OBJECT DETECTION ----------------

        // This is the list of "cameras" to use in simulation for object detection
        List<ObjectVisionIO> simObjectVisionIoList;
        if (ObjectVisionConstants.hardwareInTheLoop) {
          // We are running simulation but with real vision hardware in the loop for object
          // detection
          simObjectVisionIoList =
              createObjectVisionIOList(false, ObjectVisionConstants.hardwareInLoopCameras);
        } else {
          // There is no camera hardware in the loop so this is pure simulation of object detection
          // using predefined positions of balls
          simObjectVisionIoList =
              createObjectVisionIOList(true, ObjectVisionConstants.robotCameras);
        }
        // Create the ObjectVision object using the camera above
        objectVision = new ObjectVision(simObjectVisionIoList.toArray(ObjectVisionIO[]::new));
        break;

      default:
        // Replayed robot, disable IO implementations
        // (Use same number of dummy implementations as the real robot)

        drive =
            new Drive(
                new GyroIO() {},
                new ModuleIO() {},
                new ModuleIO() {},
                new ModuleIO() {},
                new ModuleIO() {});

        // Get a list of dummy field cameras for replay
        List<FieldVisionIO> replayableFieldVisionIoList;
        if (FieldVisionConstants.hardwareInTheLoop) {
          replayableFieldVisionIoList =
              createFieldVisionIOList(false, FieldVisionConstants.hardwareInLoopCameras);
        } else {
          replayableFieldVisionIoList =
              createFieldVisionIOList(true, FieldVisionConstants.robotCameras);
        }
        // Create the FieldVision object using the "cameras" selected above
        fieldVision =
            new FieldVision(
                drive::addVisionMeasurement,
                replayableFieldVisionIoList.toArray(FieldVisionIO[]::new));

        // Get list of dummy object cameras for replay
        List<ObjectVisionIO> replayableObjectVisionIoList;
        if (ObjectVisionConstants.hardwareInTheLoop) {
          replayableObjectVisionIoList =
              createObjectVisionIOList(false, ObjectVisionConstants.hardwareInLoopCameras);
        } else {
          replayableObjectVisionIoList =
              createObjectVisionIOList(true, ObjectVisionConstants.robotCameras);
        }
        // Create the ObjectVision object using the camera above
        objectVision =
            new ObjectVision(replayableObjectVisionIoList.toArray(ObjectVisionIO[]::new));
        break;
    }

    // Set up auto routines
    autoChooser = new LoggedDashboardChooser<>("Auto Choices", AutoBuilder.buildAutoChooser());

    // Set up SysId routines
    autoChooser.addOption(
        "Drive Wheel Radius Characterization", DriveCommands.wheelRadiusCharacterization(drive));
    autoChooser.addOption(
        "Drive Simple FF Characterization", DriveCommands.feedforwardCharacterization(drive));
    autoChooser.addOption(
        "Drive SysId (Quasistatic Forward)",
        drive.sysIdQuasistatic(SysIdRoutine.Direction.kForward));
    autoChooser.addOption(
        "Drive SysId (Quasistatic Reverse)",
        drive.sysIdQuasistatic(SysIdRoutine.Direction.kReverse));
    autoChooser.addOption(
        "Drive SysId (Dynamic Forward)", drive.sysIdDynamic(SysIdRoutine.Direction.kForward));
    autoChooser.addOption(
        "Drive SysId (Dynamic Reverse)", drive.sysIdDynamic(SysIdRoutine.Direction.kReverse));

    // Configure the button bindings
    configureButtonBindings();
  }

  private List<FieldVisionIO> createFieldVisionIOList(
      Boolean inPureSimulation, CameraSpec[] cameraSpecArray) {

    List<FieldVisionIO> fieldVisionIoList = new ArrayList<>();
    for (int i = 0; i < cameraSpecArray.length; i++) {

      if (Constants.currentMode == Mode.REPLAY) {
        // Replaying a log file so create a dummy camera with the proper name
        fieldVisionIoList.add(new FieldVisionIOReplayable(cameraSpecArray[i].cameraName));
      } else if (inPureSimulation) {
        // In pure simulation mode (i.e., without any hardware in the loop), always use
        // PhotonVisionSim
        // even if this is a Limelight because we cannot simulate Limelights
        fieldVisionIoList.add(
            new FieldVisionIOPhotonVisionSim(
                cameraSpecArray[i].cameraName, cameraSpecArray[i].robotToCamera, drive::getPose));
      } else {
        // We are either running with a real robot or in simulation mode with vision hardware in the
        // loop
        switch (cameraSpecArray[i].visionType) {
          case LIMELIGHT:
            fieldVisionIoList.add(
                new FieldVisionIOLimelight(cameraSpecArray[i].cameraName, drive::getRotation));
            break;

          case PHOTONVISION:
            fieldVisionIoList.add(
                new FieldVisionIOPhotonVision(
                    cameraSpecArray[i].cameraName, cameraSpecArray[i].robotToCamera));
            break;

          default:
            throw new IllegalArgumentException(
                "Unknown vision type "
                    + cameraSpecArray[i].visionType
                    + " for camera "
                    + cameraSpecArray[i].cameraName);
        }
      }
    }

    return fieldVisionIoList;
  }

  private List<ObjectVisionIO> createObjectVisionIOList(
      Boolean inPureSimulation, CameraSpec[] cameraSpecArray) {

    List<ObjectVisionIO> objectVisionIoList = new ArrayList<>();
    for (int i = 0; i < cameraSpecArray.length; i++) {

      if (Constants.currentMode == Mode.REPLAY) {
        // Replaying a log file so create a dummy camera with the proper name
        objectVisionIoList.add(new ObjectVisionIOReplayable(cameraSpecArray[i].cameraName));
      } else {
        // We are either running with a real robot, hardware-in-the-loop, or in simulation mode
        switch (cameraSpecArray[i].visionType) {
          case LIMELIGHT:
            throw new IllegalArgumentException(
                "Limelight object vision not yet implemented - Camera: "
                    + cameraSpecArray[i].cameraName);

          case PHOTONVISION:
            if (inPureSimulation) {
              // In pure simulation mode (i.e., without any hardware in the loop), always use
              // PhotonVisionSim
              // even if this is a Limelight because we cannot simulate Limelights
              objectVisionIoList.add(
                  new ObjectVisionIOSim(
                      cameraSpecArray[i].cameraName,
                      cameraSpecArray[i].robotToCamera,
                      drive::getPose,
                      // Raise the ball by its radius so its bottom is on the floor:
                      ObjectVisionConstants.fuelDiameterInMeters / 2.0)); // (meters)
            } else {
              // Real robot or hardware-in-the-loop
              objectVisionIoList.add(
                  new ObjectVisionIOPhotonVision(
                      cameraSpecArray[i].cameraName,
                      cameraSpecArray[i].robotToCamera,
                      drive::getPose,
                      // Raise the ball by its radius so its bottom is on the floor:
                      ObjectVisionConstants.fuelDiameterInMeters / 2.0)); // (meters)
            }
            break;

          default:
            throw new IllegalArgumentException(
                "Unknown vision type "
                    + cameraSpecArray[i].visionType
                    + " for camera "
                    + cameraSpecArray[i].cameraName);
        }
      }
    }

    return objectVisionIoList;
  }

  /**
   * Use this method to define your button->command mappings. Buttons can be created by
   * instantiating a {@link GenericHID} or one of its subclasses ({@link
   * edu.wpi.first.wpilibj.Joystick} or {@link XboxController}), and then passing it to a {@link
   * edu.wpi.first.wpilibj2.command.button.JoystickButton}.
   */
  private void configureButtonBindings() {
    // Default command, normal field-relative drive
    drive.setDefaultCommand(
        DriveCommands.joystickDrive(
            drive,
            () -> -controller.getLeftY(),
            () -> -controller.getLeftX(),
            () -> -controller.getRightX()));

    // Drive to cluster of balls
    controller
        .triangle()
        .whileTrue(
            new DriveRobotFeatureToTarget(
                drive,
                objectVision,
                new TargetLocatorSimpleBallCluster(),
                Constants.robotToPickupXform));

    // Lock to 0°
    controller
        .square()
        .whileTrue(
            DriveCommands.joystickDriveAtAngle(
                drive,
                () -> -controller.getLeftY(),
                () -> -controller.getLeftX(),
                () -> Rotation2d.kZero));

    // Switch to X pattern
    controller.cross().onTrue(Commands.runOnce(drive::stopWithX, drive));

    // Reset gyro to 0°
    controller
        .circle()
        .whileTrue(
            Commands.runOnce(
                    () ->
                        drive.setPose(
                            new Pose2d(drive.getPose().getTranslation(), Rotation2d.kZero)),
                    drive)
                .ignoringDisable(true));

    // Test/Example for Simulation purposes only!
    // Apply an offset to pose estimator to test vision correction
    // You probably don't want this on a real robot so just enable in Simulation
    if (Constants.currentMode == Constants.Mode.SIM) {
      controller
          .L1()
          .whileTrue(
              Commands.runOnce(
                  () -> {
                    var disturbance =
                        new Transform2d(
                            new Translation2d(1.0, 1.0), new Rotation2d(0.17 * 2 * Math.PI));
                    drive.setPose(drive.getPose().plus(disturbance));
                  }));
    }

    // Reset pose
    controller
        .L2()
        .whileTrue(
            Commands.runOnce(() -> drive.setPose(new Pose2d(1., 1., Rotation2d.kZero)), drive)
                .ignoringDisable(true));
  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand() {
    return autoChooser.get();
  }
}
