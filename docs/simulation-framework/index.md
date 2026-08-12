# Simulation Framework

## Overview
The simulation framework used by MonarchVision consists of [AdvantageScope](https://docs.wpilib.org/en/stable/docs/software/dashboards/advantagescope.html) and [AdvantageKit](https://docs.advantagekit.org/) .

## AdvantageScope
[AdvantageScope](https://docs.wpilib.org/en/stable/docs/software/dashboards/advantagescope.html) is a graphical tool for simulating and debugging FRC applications. It can be connected live to a running application so that it responds to updates via NetworkTables. Or, it can replay a log file generated from a previous session. This is particularly useful for debugging a scenario from a prior competition or practice match. AdvantageScope can graphically represent the robot as it moves around the field and highlight vision behavior, such as detecting april tags and game pieces. AdvantageScope is one of the standard dashboards contained in the WPILib installation.

## AdvantageKit
[AdvantageKit](https://docs.advantagekit.org/) is a framework for logging the state of the robot (input and output values) so that the session can be easily replayed and debugged. AdvantageKit also provides a set of [template projects](https://docs.advantagekit.org/getting-started/template-projects) that were used as the basis for architecting the MonarchVision reference application.
