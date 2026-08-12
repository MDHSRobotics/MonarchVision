# MonarchVision Demonstrations

The following videos demonstrate key capabilities provided by MonarchVision.

## Hardware in the Loop

[Click here for video demonstrating hardware in the loop](https://youtu.be/3Co8RURMXEQ)

A Rubik Pi3 and three Arducam cameras, along with a Limelight 4, are installed on a wooden mockup of a robot. They are connected to a laptop running a simulation of a swerve drive robot. The Rubik and Limelight vision systems detect real world april tags and fuel objects and communicate with the simulation environment of AdvantageScope. A setup such as this will be beneficial in learning how to use and tune various vision hardware and in optimizing camera placement relative to field elements and game pieces

## Simulating April Tag Detection
[Click here for the video demonstrating how to simulate april tag detection](https://youtu.be/x-fdh5pTHro)

Virtual cameras detect april tags in the simulation environment of AdvantageScope as the robot moves around the field. This is useful for making design decisions about the placement and orientation of cameras to ensure uninterrupted detection of april tags.

## Simulating Object Detection
[Click here for the video demonstrating how to simulate object detection](https://youtu.be/uMEvP_zmIEU)

Using AdvantageScope, a virtual camera detects the locations of yellow balls (fuel from the 2026 competition) as the robot moves around the field. A sample command illustrates a simple algorithm for automatically driving the robot to a cluster of balls detected nearby. This will be useful for making design decisions about placement and orientation of a camera for object detection. It's also useful for developing algorithms to drive to, or interact with, detected objects.

## Advantage Scope Replay
[Click here for the video demonstrating how to use AdvantageScope to replay session](https://youtu.be/PWvU9gglgmc)

Using AdvantageScope, a simulation session is recorded and played back in its entirety, allowing the user to inspect all input and output data at any point during the session. This capability is not limited to the vision system and can be used for recoding simulation sessions or real sessions with a physical robot. It will be invaluable, for example, when debugging any subsystem error that occurs during a competition match, improving the chances of fixing it before the next match starts.
