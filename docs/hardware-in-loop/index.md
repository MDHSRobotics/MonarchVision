# Simulating With Vision Hardware

This section describes how to use AdvantageScope to simulate the robot while using real cameras and coprocessors for vision input. This is often referred to as "hardware in the loop" and is very beneficial in exercising robot code without a complete robot. This will be illustrated with a simple test board on which are installed a Limelight as well as USB cameras attached to a Rubik Pi 3. These vision devices detect apriltags and objects in their surroundings and communicate the results to AdvantageScope via NetworkTables to update the state of the simulated robot and playing field.

## Topics

- [Setup](setup.md): This describes a simple test board containing several vision devices. It also explains how to set up the hardware and software environment to enable vision hardware-in-the-loop.
- [Field vision](field-vision.md): This describes how the odometry of the simulated robot is updated in AdvantageScope in response to the vision devices (on the test board) detecting real apriltags in their surroundings.
- [Object detection](object-detection.md): This describes how virtual game pieces are displayed in AdvantageScope in response to the vision devices (on the test board) detecting real game pieces in their surroundings.
