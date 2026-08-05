# Software Architecture

## Objective

Explain how simulation, real hardware, logging, and robot commands share a common architecture.

## Suggested architecture diagram

```mermaid
flowchart TD
    Robot[Robot Program]
    Vision[Vision Subsystem]
    IO[Vision IO Interfaces]
    Sim[Simulation Implementations]
    Real[Real Hardware Implementations]
    Log[AdvantageKit Logging]
    Commands[Drive and Targeting Commands]

    Robot --> Vision
    Vision --> IO
    IO --> Sim
    IO --> Real
    Vision --> Log
    Vision --> Commands
```

## Topics to cover

- Subsystem organization
- IO interfaces
- Real, simulated, and replay implementations
- AdvantageKit-generated input classes
- Camera-specific adapters
- Shared observation records
- Field vision versus object vision
- Target-selection services
- Drive commands
- Logging boundaries
- Configuration constants
- Data flow and update timing

## Design goals

- Run the same high-level code in simulation and on the robot
- Keep hardware-specific APIs out of commands
- Make observations easy to log and replay
- Support multiple cameras
- Reject invalid or stale observations consistently
