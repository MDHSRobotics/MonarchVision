# MonarchVision Application

## Overview
[MonarchVision](https://github.com/MDHSRobotics/MonarchVision.git) is a Java reference application developed by [FRC Team 4141](https://www.materdei.org/apps/pages/robotics) from Mater Dei High School in Santa Ana, California for exploring and implementing vision-related hardware and software. It is a reference application in the sense that it controls a hypothetical robot based on the FIRST KitBot but its architecture and concepts can be adapted to real-world applications and robots. This site documents how to set up vision hardware and simulate its behavior with or without vision hardware physically connected to the computer running the simulation.

The overriding goal of MonarchVision is to illustrate techniques that improve the productivity of FRC programming by minimizing the need for a fully assembled robot - or any robot at all, for that matter. Utilizing the virtual environment provided the FRC AdvantageScope tool teams can prototype and perfect designs (hardware and software) much earlier in the season.

MonarchVision employs Limelight as well as Rubik Pi3 vision hardware to accomplish tasks, such as:

- Accurate field odometry based on april tag detection by Rubik Pi3 and/or Limelight
- Object detection (2026 fuel balls) by Rubik Pi3
- Graphical simulation using AdvantageScope with or without vision hardware-in-the-loop
- Command to automatically drive to detected objects (2026 fuel balls)
- Comprehensive logging of all input and output data enabling playback in AdavantageScope

The MonarchVision code and associated documentation can be found in the Github at https://github.com/MDHSRobotics/MonarchVision.git.

## Main topics

- AdvantageKit and AdvantageScope
- Drive simulation
- Replay
- Simulating field vision
- Simulating object detection
- Driving to a ball cluster
- Hardware-in-the-loop testing
- Vision hardware setup
- Software architecture

## Acknowledgements

MonarchVision relies heavily on technology developed by [FRC Team 6328](https://www.littletonrobotics.org/) Mechanical Advantage from Littleton, Massachusetts. The AdvantageScope tool provides graphical simulation and replay while the AdvantageKit framework and associated templates provide logging and hardware abstraction.

Portions of the Java code and documentation were drafted with the assistance of generative AI tools. All code was reviewed, tested, and refined by the author.

## Feedback and Suggestions

This documentation is based on our experience developing and testing vision, simulation, and hardware-in-the-loop systems for FRC. If you find an error,
have a suggestion for improving the documentation, or have experience that would add to the discussion, we welcome your feedback.

Please [open an issue on GitHub](https://github.com/MDHSRobotics/MonarchVision/issues).

When reporting a technical issue, please include relevant information such as your WPILib, AdvantageKit, PhotonVision, or Limelight versions when applicable.
