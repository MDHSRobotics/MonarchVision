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

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.PS4Controller;
import edu.wpi.first.wpilibj.simulation.BatterySim;
import edu.wpi.first.wpilibj.simulation.RoboRioSim;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.subsystems.drivetrain.SwerveDrive;


import com.pathplanner.lib.auto.AutoBuilder;

public class Robot extends TimedRobot {
    private SwerveDrive drivetrain;
    private Vision vision;

    private final double TURN_kP = 0.01;
    private static final double TURN_SPEED_SCALE = 0.25;

    private PS4Controller controller;

    private Command autonomousCommand;
    private SendableChooser<Command> autoChooser = null;

    @Override
    public void robotInit() {
        drivetrain = new SwerveDrive();

        vision = new Vision(drivetrain::addVisionMeasurement);

        controller = new PS4Controller(0);

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

        // Update vision
        vision.periodic();

        // Test/Example only!
        // Apply an offset to pose estimator to test vision correction
        // You probably don't want this on a real robot, just delete it.
        if (controller.getCrossButtonPressed()) {
            var disturbance =
                    new Transform2d(new Translation2d(1.0, 1.0), new Rotation2d(0.17 * 2 * Math.PI));
            drivetrain.resetPose(drivetrain.getPose().plus(disturbance), false);
        }

        // Reset pose
        if (controller.getCircleButtonPressed()) {
            resetPose();
        }

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
    }

    @Override
    public void teleopPeriodic() {

        // Calculate drivetrain commands from Joystick values

        double forward = MathUtil.applyDeadband(-controller.getLeftY(), 0.08); // Down/UP
        double strafe  = MathUtil.applyDeadband(-controller.getLeftX(), 0.08); // Left/Right
        double turn    = MathUtil.applyDeadband(-controller.getRightX(), 0.08);  // Rotate

        // smooth response
        forward = forward * forward * forward;
        strafe  = strafe * strafe * strafe;
        turn    = turn * turn * turn;

        // scale to robot max speeds
        forward *= Constants.Swerve.kMaxLinearSpeed;
        strafe  *= Constants.Swerve.kMaxLinearSpeed;
        turn    *= Constants.Swerve.kMaxAngularSpeed * TURN_SPEED_SCALE;

        // Calculate whether close to target based on our global pose estimate.
        var curPose = drivetrain.getPose();
        var closeToTarget = (curPose.getY() > 2.0 && curPose.getX() < 4.0); // Close enough to blue speaker
        SmartDashboard.putBoolean("Close to Target", closeToTarget);

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

            turn = errorDeg * TURN_kP * Constants.Swerve.kMaxAngularSpeed;
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

    }

    @Override
    public void simulationPeriodic() {
        // Update drivetrain simulation
        drivetrain.simulationPeriodic();

        // Update camera simulation
        vision.simulationPeriodic(drivetrain.getSimPose());

        var debugField = vision.getSimDebugField();
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
        vision.resetSimPose(startPose);
    }

}
