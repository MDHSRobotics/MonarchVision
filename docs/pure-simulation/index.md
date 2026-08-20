# Simulating Without Vision Hardware

This section describes how to use AdvantageScope to simulate the behavior of the entire robot, including vision sensors. This "pure simulation" mode is very beneficial in exercising robot code without any of the robot hardware. Even though there are no physical vision devices, the simulation can detect april tags and objects on the virtual field based on the simulated position and orientation of the robot.

## Topics

- [Setup](sim-setup.md): This describes how to set up the software environment to enable pure simulation of vision devices.
- [Field vision](sim-field-vision.md): This describes how the odometry of the simulated robot is updated in AdvantageScope in response to the simulated vision devices detecting apriltags in the virtual environment.
- [Object detection](sim-object-detection.md): This describes how virtual game pieces are displayed in AdvantageScope in response to the simulated vision devices detecting real game pieces in the virtual environment.
- [Drive to object](sim-drive-to-object.md): This descibes an example command (Drive to Object) to illustrate the benefit of detecting objects on the playing field.
