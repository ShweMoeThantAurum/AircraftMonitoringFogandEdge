# AircraftMonitoringFogandEdge

## Overview

This repository contains the source code, documentation, and supporting files for a proof-of-concept implementation of an edge-fog-cloud IoT architecture for environmental monitoring in aircraft wing components, based on the research paper by Dogea et al. (2023), “Implementation of an edge-fog-cloud computing IoT architecture in aircraft components” (MRS Communications, Volume 13, Issue 3, Pages 416–424, DOI: 10.1557/s43579-023-00364-z, available at https://link.springer.com/article/10.1557/s43579-023-00364-z). The project simulates a five-layer architecture using iFogSim 4.0 and integrates real-time cloud processing with Azure IoT Hub. The implementation simplifies the original framework.

## Repository Structure

- **`iFogSim/`**: Contains the `AircraftMonitoringPoC.java` script for iFogSim simulation.
- **`AzureIoT/`**: Includes `AircraftMonitoringPoC.py` and `.env` files for Azure IoT Hub integration.
- **`Research Paper/`**: Stores the chosen research paper (https://github.com/ShweMoeThantAurum/AircraftMonitoringFogandEdge/tree/main/Research%20Paper).
- **`iFogSim/Results/`**: Holds `Results.xlsx` for simulation metrics and `Comparison Visualization of Metrics of Edge-only and Cloud-only Modes.png` for visual analysis.
- **`Architecture Diagrams/`**: Contains diagrams representing the architecture, application loop, and topology (https://github.com/ShweMoeThantAurum/AircraftMonitoringFogandEdge/tree/main/Architecture%20Diagrams).

## Setup Instructions

### iFogSim Simulation Setup

1. **Install Eclipse IDE**:
   - Download and install Eclipse from https://www.eclipse.org/downloads/, ensuring compatibility with Java SE 1.8.

2. **Download and Configure iFogSim**:
   - Download iFogSim 4.0 from https://github.com/Cloudslab/iFogSim/archive/refs/heads/main.zip.
   - Unzip the downloaded file to a local directory.

3. **Create a New Project in Eclipse**:
   - Launch Eclipse and select “File” > “New” > “Java Project”.
   - Enter a project name (e.g., `AircraftMonitoringPoC`).
   - Choose the unzipped iFogSim folder as the project location.
   - Click “Finish” to complete the setup.

4. **Import and Run `AircraftMonitoringPoC.java`**:
   - Navigate to the `org.fog.test.perfeval` package within the project.
   - Create a new class named `AircraftMonitoringPoC`.
   - Copy the code from https://github.com/ShweMoeThantAurum/AircraftMonitoringFogandEdge/blob/main/iFogSim/AircraftMonitoringPoC.java and paste it into the new class.
   - Ensure the Java System Library is set to Java SE 1.8 under “Project” > “Properties” > “Java Build Path”.
   - Run the simulation by clicking the “Run” button.

5. **Configure Simulation Modes**:
   - To simulate edge/fog mode, set `private static boolean CLOUD = false;` on line 49.
   - To simulate cloud-only mode, set `private static boolean CLOUD = true;` on line 49.
   - Review results in the Eclipse console or export metrics to `Results.xlsx` (https://github.com/ShweMoeThantAurum/AircraftMonitoringFogandEdge/blob/main/iFogSim/Results/Results.xlsx).

### Azure IoT Hub Setup

1. **Create an Azure IoT Hub**:
   - Log in to the Azure portal and create an IoT Hub named `AircraftMonitoring` under the `AircraftMonitoringRG` resource group in the UK South region.

2. **Register Devices**:
   - Add three devices (e.g., `Sensor_1`, `Sensor_2`, `Sensor_3`) in the IoT Hub and note their primary connection strings.

3. **Configure Local Environment**:
   - Download `AircraftMonitoringPoC.py` (https://github.com/ShweMoeThantAurum/AircraftMonitoringFogandEdge/blob/main/AzureIoT/AircraftMonitoringPoC.py) and the `.env` file (https://github.com/ShweMoeThantAurum/AircraftMonitoringFogandEdge/blob/main/AzureIoT/.env).
   - Update the `.env` file on your local machine with the primary connection strings for the three devices, replacing the placeholder values.

4. **Install Dependencies**:
   - Ensure Python 3.12 is installed.
   - Install required packages using the provided `requirements.txt` (see below) with the command: `pip install -r requirements.txt`.

5. **Run the Script**:
   - Execute `AircraftMonitoringPoC.py` using `python AircraftMonitoringPoC.py`.
   - Monitor log outputs to verify sensor data transmission to the IoT Hub, accessible via the Azure portal’s monitoring metrics.

## Requirements

### iFogSim Requirements
- Eclipse IDE with Java SE 1.8
- iFogSim 4.0 (https://github.com/Cloudslab/iFogSim/archive/refs/heads/main.zip)

### Azure IoT Hub Requirements
- `requirements.txt` content:
  ```
  azure-iot-device>=2.0.0
  python-dotenv>=1.0.0
  ```

## Results and Visualization

- **Metrics**: Simulation results are stored in `Results.xlsx` (https://github.com/ShweMoeThantAurum/AircraftMonitoringFogandEdge/blob/main/iFogSim/Results/Results.xlsx), detailing latency, energy consumption, and other performance metrics.
- **Visualization**: A comparison of edge-only and cloud-only modes is available in `Comparison Visualization of Metrics of Edge-only and Cloud-only Modes.png` (https://github.com/ShweMoeThantAurum/AircraftMonitoringFogandEdge/blob/main/iFogSim/Results/Comparison%20Visualization%20of%20Metrics%20of%20Edge-only%20and%20Cloud-only%20Modes%20.png).

## Architecture Diagrams

The `Architecture Diagrams` folder (https://github.com/ShweMoeThantAurum/AircraftMonitoringFogandEdge/tree/main/Architecture%20Diagrams) contains:
- **Proof-of-Concept Architecture**: Illustrates the five-layer structure.
- **Application Data Flow**: Depicts the data flow simulated in iFogSim.
- **Physical Topology**: Represents the topology used in the iFogSim simulation.

## Research Paper

The chosen research paper, available at https://github.com/ShweMoeThantAurum/AircraftMonitoringFogandEdge/tree/main/Research%20Paper, serves as the foundation for this proof-of-concept. The iFogSim and Azure IoT Hub implementations are simplified adaptations of the paper’s framework.

## Notes

- Ensure all code files (`AircraftMonitoringPoC.java` and `AircraftMonitoringPoC.py`) are fully commented as per the assessment requirements.
- The simulations assume idealized conditions; real-world testing with RSL10 sensors is recommended for future enhancements.
- For support, refer to the video presentation linked in the CA project report.

---

### Improvements Made
1. **Clarity and Structure**:
   - Organized content into clear sections (Overview, Repository Structure, Setup Instructions, Requirements, Results, Diagrams, Research Paper, Notes).
   - Provided step-by-step instructions with numbered lists for ease of replication.

2. **Professional Tone**:
   - Replaced informal phrases (e.g., “you must,” “then, you can run it”) with formal language (e.g., “Install Eclipse IDE,” “Execute the script”).
   - Added technical precision (e.g., specifying iFogSim 4.0, Python 3.12).

3. **Completeness**:
   - Included detailed setup for both iFogSim and Azure IoT Hub, addressing all required components.
   - Added a `requirements.txt` section with specific package versions.
   - Referenced all files and links provided, ensuring alignment with the GitHub structure.

4. **Corrections and Enhancements**:
   - Fixed typos (e.g., “teste” to “tested,” “Hun” to “Hub,” “MointoringPoC” to “MonitoringPoC”).
   - Clarified configuration steps (e.g., specifying Java SE 1.8, Azure region).
   - Added a note on idealized conditions to align with the report’s limitations.

5. **Documentation**:
   - Ensured instructions meet the “well-documented” requirement by including file paths, version details, and troubleshooting hints (e.g., checking Java library).

### Next Steps
- **Create `requirements.txt`**: Save the listed dependencies in a file named `requirements.txt` in the `AzureIoT/` directory.
- **Update Repository**: Commit the revised `README.md` and `requirements.txt` to https://github.com/ShweMoeThantAurum/AircraftMonitoringFogandEdge.
- **Insert Links**: Update the CA report’s **Deliverables** section with the repository link (https://github.com/ShweMoeThantAurum/AircraftMonitoringFogandEdge) and the video link once available.
- **Submission**: Upload the report, code, and video to Moodle immediately, as the deadline is 11:59 PM IST, with approximately 2 hours and 37 minutes remaining as of 09:22 PM IST.

If you need further refinements (e.g., additional setup details, diagram integration in `README.md`), please specify within the next hour to meet the deadline.
