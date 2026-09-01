# MonarchVision Application

## Overview
[MonarchVision](https://github.com/MDHSRobotics/MonarchVision.git) is a Java reference application developed for [FRC Team 4141](https://www.materdei.org/apps/pages/robotics) from Mater Dei High School in Santa Ana, California. It is intended to be used by students to learn, explore, and implement vision-related hardware and software for FRC robotics. MonarchVision is a reference application in the sense that it controls a hypothetical robot based on the FIRST KitBot but its architecture and concepts can be adapted to real-world applications and robots. This site documents how to set up vision hardware and simulate its behavior with or without vision hardware physically connected to the computer running the simulation.

The overriding goal of MonarchVision is to illustrate techniques that improve the productivity of FRC programming by minimizing the need for a fully assembled robot - or any robot at all, for that matter. Utilizing the virtual environment provided the FRC AdvantageScope tool, students can prototype and refine designs (hardware and software) much earlier in the season.

MonarchVision employs Limelight as well as Rubik Pi3 vision hardware to accomplish tasks, such as:

- Maintain accurate field odometry based on april tag detection
- Detect game piece objects (2026 fuel balls)
- Automatically drive to detected objects
- Graphical simulate robot behavior with or without connected vision devices

## Main topics

This site documents the following topics.

- Installation and set up of vision hardware (Limelight and the Rubik Pi3 coprocessor and cameras)
- The simulation environment provided by AdvantageScope to graphically represent the behavior of the robot as it responds to visually detected objects on the field (apriltags and game pieces)
- Comprehensive logging based on the AdvantageKit framework, facilitating debugging and replay
- Software architecture that minimizes the impact of changes to the design of vision sub-systems on the robot 
- Hardware-in-the-loop simulation, enabling Limelight and/or Rubik devices to be physically connected the to computer running the simulation
- Pure simulation of vision sub-systems without any connected hardware

The MonarchVision code and associated documentation can be found in Github at https://github.com/MDHSRobotics/MonarchVision.git.

>MonarchVision has not yet been updated to accommodate the Systemcore architecture which will be used for the 2027 competition.

## Acknowledgements

MonarchVision relies heavily on technology developed by [FRC Team 6328](https://www.littletonrobotics.org/) Mechanical Advantage from Littleton, Massachusetts. The AdvantageScope tool provides graphical simulation and replay while the AdvantageKit framework and associated templates provide logging and hardware abstraction.

Portions of the Java code and documentation were drafted with the assistance of generative AI tools. All code was reviewed, tested, and refined by the author.

See [our Acknowledgments page.](acknowledgments.md)

## Feedback and Suggestions

This documentation is based on our experience developing and testing vision, simulation, and hardware-in-the-loop systems for FRC. If you find an error,
have a suggestion for improving the documentation, or have experience that would add to the discussion, we welcome your feedback.

Please [open an issue on GitHub](https://github.com/MDHSRobotics/MonarchVision/issues).

When reporting a technical issue, please include relevant information such as your WPILib, AdvantageKit, PhotonVision, or Limelight versions when applicable.
