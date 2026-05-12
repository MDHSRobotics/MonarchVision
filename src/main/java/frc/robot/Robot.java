/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package frc.robot;

import edu.wpi.first.wpilibj.Timer;

import static frc.robot.Constants.Vision.*;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.simulation.BatterySim;
import edu.wpi.first.wpilibj.simulation.RoboRioSim;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.subsystems.drivetrain.SwerveDrive;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonUtils;
import org.photonvision.targeting.PhotonTrackedTarget;

import com.pathplanner.lib.auto.AutoBuilder;

public class Robot extends TimedRobot {
    private SwerveDrive drivetrain;
    private VisionSim visionSim;
    private PhotonCamera camera;

    private final double VISION_TURN_kP = 0.01;
    private final double VISION_DES_ANGLE_deg = 0.0;
    private final double VISION_STRAFE_kP = 0.5;
    private final double VISION_DES_RANGE_m = 3.125;

    private XboxController controller;

    private int lastSeenTagID = -1;
    private double lastSeenRange = 0.0;
    private double lastSeenYaw = 0.0;
    private boolean lastSeenIsTarget = false;
    private double lastSeenTime = 0.0;
    private static final double TARGET_LOST_TIMEOUT_SEC = 0.25;

    private Command autonomousCommand;
    private SendableChooser<Command> autoChooser = null;

    @Override
    public void robotInit() {
        drivetrain = new SwerveDrive();
        camera = new PhotonCamera(kCameraName);

        visionSim = new VisionSim(camera);

        controller = new XboxController(0);

        autoChooser = AutoBuilder.buildAutoChooser();
        SmartDashboard.putData("Auto Chooser", autoChooser);
    }

    public Command getAutonomousCommand() {
        return autoChooser.getSelected();
    }

    @Override
    public void robotPeriodic() {

        CommandScheduler.getInstance().run();
        
        // Update drivetrain subsystem
        drivetrain.periodic();

        // Log values to the dashboard
        drivetrain.log();
    }

    @Override
    public void disabledPeriodic() {
        drivetrain.stop();
    }

    @Override
    public void teleopInit() {
        if (autonomousCommand != null) {
            autonomousCommand.cancel();
        }
        resetPose();
    }

    @Override
    public void teleopPeriodic() {
        // Calculate drivetrain commands from Joystick values
        double forward = -controller.getLeftY() * Constants.Swerve.kMaxLinearSpeed;
        double strafe = -controller.getLeftX() * Constants.Swerve.kMaxLinearSpeed;
        double turn = -controller.getRightX() * Constants.Swerve.kMaxAngularSpeed;

        // Read in relevant data from the Camera
        boolean targetVisible = false;

        var results = camera.getAllUnreadResults();
        if (!results.isEmpty()) {
            // Camera processed a new frame since last
            // Get the last one in the list.
            var result = results.get(results.size() - 1);
            PhotonTrackedTarget bestTarget = null;
            if (result.hasTargets()) {
                // At least one AprilTag was seen by the camera
                for (var target : result.getTargets()) {
                    int id = target.getFiducialId();
                    if (id == 7) {
                        // Found Tag 7, record its information
                        bestTarget = target;
                        targetVisible = true;
                        break;
                    }
                }
                if (!targetVisible) {
                    // Found some April tags but not the target of interest
                    // So just report the best one found
                    bestTarget = result.getBestTarget();
                    targetVisible = false;
                }
                lastSeenTagID = bestTarget.getFiducialId();
                lastSeenIsTarget = targetVisible;
                lastSeenYaw = bestTarget.getYaw();
                lastSeenRange = PhotonUtils.calculateDistanceToTargetMeters(
                                    0.5, // Measured with a tape measure, or in CAD.
                                    1.435, // From 2024 game manual for ID 7
                                    Units.degreesToRadians(30.0), // Measured with a protractor, or in CAD.
                                    Units.degreesToRadians(bestTarget.getPitch()));
                lastSeenYaw = bestTarget.getYaw();

                lastSeenTime = Timer.getFPGATimestamp();
            }
            
            boolean recentlySeen = Timer.getFPGATimestamp() - lastSeenTime < TARGET_LOST_TIMEOUT_SEC;
            if (recentlySeen) {
                // Put debug information to the dashboard
                SmartDashboard.putBoolean("Target Visible", lastSeenIsTarget);
                SmartDashboard.putNumber("Detected Tag ID", lastSeenTagID);
                SmartDashboard.putNumber("Detected Range (m)", lastSeenRange);
                SmartDashboard.putNumber("Detected Yaw", lastSeenYaw);

            }
            else {
                SmartDashboard.putBoolean("Target Visible", false);
                SmartDashboard.putNumber("Detected Tag ID", -1);
                SmartDashboard.putNumber("Detected Range (m)", 0.0);
                SmartDashboard.putNumber("Detected Yaw", 0.0);

            }  
        }

        // Auto-align when requested
        if (controller.getAButton() && targetVisible) {
            // Driver wants auto-alignment to tag 7
            // And, tag 7 is in sight, so we can turn toward it.
            // Override the driver's turn and fwd/rev command with an automatic one
            // That turns toward the tag, and gets the range right.
            turn =
                    (VISION_DES_ANGLE_deg - lastSeenYaw) * VISION_TURN_kP * Constants.Swerve.kMaxAngularSpeed;
            forward =
                    (VISION_DES_RANGE_m - lastSeenRange) * VISION_STRAFE_kP * Constants.Swerve.kMaxLinearSpeed;
        }

        // Orient robot based on POV buttons
        int pov = controller.getPOV();
        if (pov != -1) {
            double targetHeadingDeg = pov;
            double currentHeadingDeg = drivetrain.getPose().getRotation().getDegrees();

            double errorDeg = MathUtil.inputModulus(
                targetHeadingDeg - currentHeadingDeg,
                -180.0,
                180.0
            );

            turn = errorDeg * VISION_TURN_kP * Constants.Swerve.kMaxAngularSpeed;
            forward = 0.0;
            strafe = 0.0;
        }

        // Command drivetrain motors based on target speeds
        //if (forward > 0.01) System.out.println("Forward = "+forward);
        //if (turn > 0.01) System.out.println("Turn = "+turn);
        //if (strafe > 0.01) System.out.println("Strafe = "+strafe);
        drivetrain.drive(forward, strafe, turn);

    }

    @Override
    public void autonomousInit() {
        autonomousCommand = getAutonomousCommand();

        if (autonomousCommand != null) {
            autonomousCommand.schedule();
        }
    }

    @Override
    public void simulationPeriodic() {
        // Update drivetrain simulation
        drivetrain.simulationPeriodic();

        // Update camera simulation
        visionSim.simulationPeriodic(drivetrain.getSimPose());

        var debugField = visionSim.getSimDebugField();
        debugField.getObject("EstimatedRobot").setPose(drivetrain.getPose());
        debugField.getObject("EstimatedRobotModules").setPoses(drivetrain.getModulePoses());

        // Calculate battery voltage sag due to current draw
        var batteryVoltage =
                BatterySim.calculateDefaultBatteryLoadedVoltage(drivetrain.getCurrentDraw());

        // Using max(0.1, voltage) here isn't a *physically correct* solution,
        // but it avoids problems with battery voltage measuring 0.
        RoboRioSim.setVInVoltage(Math.max(0.1, batteryVoltage));
    }

    public void resetPose() {
        // Example Only - startPose should be derived from some assumption
        // of where your robot was placed on the field.
        // The first pose in an autonomous path is often a good choice.
        var startPose = new Pose2d(1, 1, new Rotation2d());
        drivetrain.resetPose(startPose, true);
        visionSim.resetSimPose(startPose);
    }
}
