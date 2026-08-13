# Replay

## Overview
MonarchVision logs all input and output data for all subsystems using the [AdvantageKit](https://docs.advantagekit.org/) framework. This enables efficient debugging during a simulation session as well as the ability to easily replay the log file from a previous session (whether it was a simulation session or one for a real robot).

This section describes how to log data while the following section explains how to replay a log file.


[AdvantageScope](https://docs.wpilib.org/en/stable/docs/software/dashboards/advantagescope.html) is a graphical tool for simulating and debugging FRC applications. It can be connected live to a running application so that it responds to updates via NetworkTables. Or, it can replay a log file generated from a previous session. This is particularly useful for debugging a scenario from a prior competition or practice match. AdvantageScope can graphically represent the robot as it moves around the field and highlight vision behavior, such as detecting april tags and game pieces.

This section describes only the vision-related capabilities of AdvantageScope. See the [WPILib AdvantageScope documentation](https://docs.wpilib.org/en/stable/docs/software/dashboards/advantagescope.html) for more information.

## Installation
See the [AdvantageKit Installation documentation](https://docs.advantagekit.org/getting-started/installation/) for information on how to install its vendordep and related topics.
