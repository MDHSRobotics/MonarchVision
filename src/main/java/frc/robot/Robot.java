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

import java.util.List;

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
import org.photonvision.targeting.PhotonPipelineResult;

import org.photonvision.PhotonPoseEstimator;
import org.photonvision.PhotonPoseEstimator.PoseStrategy;
import edu.wpi.first.math.VecBuilder;

import com.pathplanner.lib.auto.AutoBuilder;

public class Robot extends TimedRobot {
    private SwerveDrive drivetrain;
    private VisionSim visionSim;
    private PhotonCamera camera;

    private final double VISION_TURN_kP = 0.01;
    private final double VISION_DES_ANGLE_deg = 0.0;
    private final double VISION_STRAFE_kP = 0.5;
    private final double VISION_DES_RANGE_m = 1.25;

    private XboxController controller;

    private List<PhotonPipelineResult> latestCameraResults = null;
    private int lastSeenTagID = -1;
    private double lastSeenRange = 0.0;
    private double lastSeenYaw = 0.0;
    private boolean lastSeenIsPreferred = false;
    private double lastSeenTime = 0.0;
    private static final double TARGET_LOST_TIMEOUT_SEC = 0.25;

    // Needed to update drivetrain pose based on april tags detected
    private PhotonPoseEstimator photonPoseEstimator;

    private Command autonomousCommand;
    private SendableChooser<Command> autoChooser = null;

    @Override
    public void robotInit() {
        drivetrain = new SwerveDrive();
        camera = new PhotonCamera(kCameraName);

        visionSim = new VisionSim(camera);

        // This photonPoseEstimator will be used to adjust the drivetrain pose
        // based on detected april tags
        photonPoseEstimator = new PhotonPoseEstimator(
            kTagLayout,
            PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR,
            kRobotToCam
        );

        photonPoseEstimator.setMultiTagFallbackStrategy(
            PoseStrategy.LOWEST_AMBIGUITY
        );

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

    private void readCamera(int preferredTagID) {
        // Read in relevant data from the Camera
        boolean preferredTagFound = false;

        latestCameraResults = camera.getAllUnreadResults();
        if (!latestCameraResults.isEmpty()) {
            // Camera processed a new frame since last
            // Get the last one in the list.
            var result = latestCameraResults.get(latestCameraResults.size() - 1);
            PhotonTrackedTarget bestTarget = null;
            if (result.hasTargets()) {
                // At least one AprilTag was seen by the camera
                // If we are looking for a preferred tag, see if it was detected
                if (preferredTagID >= 0 ) {
                    for (var target : result.getTargets()) {
                        int id = target.getFiducialId();
                        if (id == preferredTagID) {
                            // Found preferred tag
                            bestTarget = target;
                            preferredTagFound = true;
                            break;
                        }
                    }
                }
                if (!preferredTagFound) {
                    // Found some April tags but not the preferred tag (if any)
                    // So just report the best one found
                    bestTarget = result.getBestTarget();
                    preferredTagFound = false;
                }
                lastSeenTagID = bestTarget.getFiducialId();
                lastSeenIsPreferred = preferredTagFound;
                lastSeenYaw = bestTarget.getYaw();
                lastSeenRange = PhotonUtils.calculateDistanceToTargetMeters(
                                    0.5, // Measured with a tape measure, or in CAD.
                                    0.889, // From 2026 game manual for ID 7
                                    Constants.Vision.camPitch, // Measured with a protractor, or in CAD.
                                    Units.degreesToRadians(bestTarget.getPitch()));
                lastSeenYaw = bestTarget.getYaw();

                lastSeenTime = Timer.getFPGATimestamp();
            }
            
            boolean recentlySeen = Timer.getFPGATimestamp() - lastSeenTime < TARGET_LOST_TIMEOUT_SEC;
            if (recentlySeen) {
                // Put debug information to the dashboard
                SmartDashboard.putBoolean("Preferred Tag Visible", preferredTagFound);
                SmartDashboard.putNumber("Detected Tag ID", lastSeenTagID);
                SmartDashboard.putNumber("Detected Range (m)", lastSeenRange);
                SmartDashboard.putNumber("Detected Yaw", lastSeenYaw);

            }
            else {
                SmartDashboard.putBoolean("Preferred Tag Visible", false);
                SmartDashboard.putNumber("Detected Tag ID", -1);
                SmartDashboard.putNumber("Detected Range (m)", 0.0);
                SmartDashboard.putNumber("Detected Yaw", 0.0);

            }  
        }
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

        // Read camera, looking for target to align to
        int targetTagID = 7;
        //readCamera(targetTagID);

        // Auto-align when requested
        if (controller.getAButton() && lastSeenIsPreferred) {
            // Driver wants auto-alignment to target tag
            // And, it is in sight, so we can turn toward it.
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
    public void autonomousPeriodic() {
        // Just read camera and report tags detected as we are moving
        int noPreferredTag = -1;
        //readCamera(noPreferredTag);
    }

    @Override
    public void simulationPeriodic() {
        // Update drivetrain simulation
        drivetrain.simulationPeriodic();

        // Update the estimated pose based on detected april tags
        updatePoseEstimatorFromVision();

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

    private void updatePoseEstimatorFromVision() {

        /*
         * A Note about the Vision Pose determined when an april tag is detected:
         * This isn't stored as a field — it's a momentary measurement. 
         * When PhotonVision sees an AprilTag, it can compute the robot's full 
         * field-relative pose using the known tag location (from the field layout)
         * and the camera's transform. That pose estimate gets passed into:
         *    poseEstimator.addVisionMeasurement(visionPose, timestamp);
         * ...and is immediately blended into the estimated pose by the Kalman filter. 
         * It's imprecise (noisy camera, uncertain tag detection) but it's absolute — 
         * it doesn't care how far you've driven or how much the wheels have slipped.
         * The timestamp matters a lot here — WPILib's pose estimator accepts backdated 
         * measurements, so if the camera has 30ms of latency, you pass the timestamp 
         * from when the frame was captured, not when it was processed. The filter 
         * rewinds and replays to correct for that latency.
         */

        readCamera((-1));
        
        // Check to see if the camera has any results
        // If so, estimate a pose based on april tags detected in the last frame
        if (latestCameraResults != null){
            if (latestCameraResults.size() > 0) {
                var result = latestCameraResults.get(latestCameraResults.size() - 1);
                
                var visionEstimate = photonPoseEstimator.update(result);

                if (visionEstimate.isPresent()) {
                    var est = visionEstimate.get();

                    // Update the estimated pose with the pose calculated by vision
                    drivetrain.addVisionMeasurement(
                        est.estimatedPose.toPose2d(),
                        est.timestampSeconds,
                        VecBuilder.fill(0.7, 0.7, 1.5)
                    );
                }
            }
        }
    }
}
