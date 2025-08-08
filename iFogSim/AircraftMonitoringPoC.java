package org.fog.test.perfeval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * Simulation setup for IoT-based environmental monitoring in aircraft wing components.
 * @author Shwe Moe Thant
 */
public class AircraftMonitoringPoC {
    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();
    static int numOfSensors = 3;
    
    /** Flag to determine if simulation is cloud-only or edge/fog. */
    private static boolean CLOUD = false; // Set to true for cloud-only, false for edge/fog

    /**
     * Main method to start the simulation.
     * @param args
     */
    public static void main(String[] args) {
        Log.printLine("Starting Aircraft Monitoring PoC...");

        try {
            Log.disable();
            int num_user = 1; 
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            String appId = "aircraft_monitoring";
            FogBroker broker = new FogBroker("broker");
            Application application = createApplication(appId, broker.getId());
            application.setUserId(broker.getId());

            createFogDevices(broker.getId(), appId, application);

            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            if (CLOUD) {
                // Fixing all modules to the cloud for cloud-only deployment
                moduleMapping.addModuleToDevice("data_collection", "cloud");
                moduleMapping.addModuleToDevice("data_selection", "cloud");
                moduleMapping.addModuleToDevice("aggregation_thresholding", "cloud");
                moduleMapping.addModuleToDevice("storage_visualization", "cloud");
                moduleMapping.addModuleToDevice("client", "cloud");
            } else {
                // Placing storage_visualization in cloud, others in edge/fog
                moduleMapping.addModuleToDevice("storage_visualization", "cloud");
                for (FogDevice device : fogDevices) {
                    if (device.getName().startsWith("edge")) {
                        moduleMapping.addModuleToDevice("data_collection", device.getName());
                        moduleMapping.addModuleToDevice("data_selection", device.getName());
                        moduleMapping.addModuleToDevice("client", device.getName());
                    }
                    if (device.getName().startsWith("fog")) {
                        moduleMapping.addModuleToDevice("aggregation_thresholding", device.getName());
                    }
                }
            }

            Controller controller = new Controller("master-controller", fogDevices, sensors, actuators); // Creating the controller
            controller.submitApplication(application, 0, 
                (CLOUD) ? (new ModulePlacementMapping(fogDevices, application, moduleMapping)) 
                : (new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping)));

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            Log.printLine("Aircraft Monitoring PoC finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    /**
     * Creates the fog devices in the physical topology of the simulation.
     * @param userId The user ID.
     * @param appId The application ID.
     * @param application The application object.
     */
    private static void createFogDevices(int userId, String appId, Application application) {
        FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25); // Creates the fog device Cloud at the apex of the hierarchy
        cloud.setParentId(-1);
        fogDevices.add(cloud);

        FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333); // Creates the fog device Proxy Server for virtual sink layer
        proxy.setParentId(cloud.getId());
        proxy.setUplinkLatency(100); // Latency of connection from Proxy Server to the Cloud is 100 ms
        fogDevices.add(proxy);

        FogDevice fogDevice = createFogDevice("fog_device", 2800, 4000, 10000, 10000, 2, 0.0, 107.339, 83.4333); // Creates the fog device for fog computing layer
        fogDevice.setParentId(proxy.getId());
        fogDevice.setUplinkLatency(4); // Latency of connection between fog device and proxy is 4 ms
        fogDevices.add(fogDevice);

        FogDevice edgeDevice = createFogDevice("edge_device", 2800, 4000, 10000, 10000, 3, 0.0, 107.339, 83.4333); // Creates the fog device for edge computing layer
        edgeDevice.setParentId(fogDevice.getId());
        edgeDevice.setUplinkLatency(2); // Latency of connection between edge device and fog device is 2 ms
        fogDevices.add(edgeDevice);

        // Attach sensors and actuators to edge_device for edge mode, or cloud for cloud mode
        int attachId = CLOUD ? cloud.getId() : edgeDevice.getId();
        for (int i = 0; i < numOfSensors; i++) {
            addSensorAndActuator("sensor_" + i, userId, appId, attachId, application);
        }
    }

    /**
     * Adds a sensor and actuator to the simulation.
     * @param id The ID suffix for the sensor and actuator.
     * @param userId The user ID.
     * @param appId The application ID.
     * @param gatewayId The gateway device ID.
     * @param application The application object.
     */
    private static void addSensorAndActuator(String id, int userId, String appId, int gatewayId, Application application) {
        double sensorLatency = CLOUD ? 100.0 : 6.0; // Higher latency in cloud mode to simulate remote access
        double displayLatency = CLOUD ? 100.0 : 1.0; // Higher latency in cloud mode for actuators

        Sensor envSensor = new Sensor("env-" + id, "ENV_SENSOR", userId, appId, new DeterministicDistribution(5)); // Inter-transmission time follows a deterministic distribution
        sensors.add(envSensor);
        envSensor.setGatewayDeviceId(gatewayId);
        envSensor.setLatency(sensorLatency);
        envSensor.setApp(application);

        Actuator display = new Actuator("display-" + id, userId, appId, "DISPLAY");
        actuators.add(display);
        display.setGatewayDeviceId(gatewayId);
        display.setLatency(displayLatency);
        display.setApp(application);
    }

    /**
     * Creates a vanilla fog device.
     * @param nodeName Name of the device to be used in simulation.
     * @param mips MIPS.
     * @param ram RAM.
     * @param upBw Uplink bandwidth.
     * @param downBw Downlink bandwidth.
     * @param level Hierarchy level of the device.
     * @param ratePerMips Cost rate per MIPS used.
     * @param busyPower Busy power consumption.
     * @param idlePower Idle power consumption.
     * @return The created fog device.
     */
    private static FogDevice createFogDevice(String nodeName, long mips, int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
        List<Pe> peList = new ArrayList<Pe>();
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000; // Host storage
        int bw = 10000;

        PowerHost host = new PowerHost(hostId, new RamProvisionerSimple(ram), new BwProvisionerOverbooking(bw), storage, peList, new StreamOperatorScheduler(peList), new FogLinearPowerModel(busyPower, idlePower));

        List<Host> hostList = new ArrayList<Host>();
        hostList.add(host);

        String arch = "x86"; 
        String os = "Linux"; 
        String vmm = "Xen";
        double time_zone = 10.0; 
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0; 
        LinkedList<Storage> storageList = new LinkedList<Storage>(); 

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        FogDevice fogdevice = null;
        try {
            fogdevice = new FogDevice(nodeName, characteristics, new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fogdevice.setLevel(level);
        return fogdevice;
    }

    /**
     * Function to create the application in the DDF model.
     * @param appId Unique identifier of the application.
     * @param userId Identifier of the user of the application.
     * @return The created application.
     */
    @SuppressWarnings({"serial"})
    private static Application createApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);

        // Adding modules to the application model
        application.addAppModule("data_collection", 10);
        application.addAppModule("data_selection", 10);
        application.addAppModule("aggregation_thresholding", 10);
        application.addAppModule("storage_visualization", 10);
        application.addAppModule("client", 10);

        // Connecting the application modules with edges
        application.addAppEdge("ENV_SENSOR", "data_collection", 2000, 500, "ENV_SENSOR", Tuple.UP, AppEdge.SENSOR); // Edge from sensor to data_collection
        application.addAppEdge("data_collection", "data_selection", 3500, 500, "RAW_DATA", Tuple.UP, AppEdge.MODULE); // Edge from data_collection to data_selection
        application.addAppEdge("data_selection", "aggregation_thresholding", 1000, 1000, 1000, "FILTERED_DATA", Tuple.UP, AppEdge.MODULE); // Edge from data_selection to aggregation_thresholding
        application.addAppEdge("aggregation_thresholding", "storage_visualization", 14, 500, "AGG_DATA", Tuple.UP, AppEdge.MODULE); // Edge from aggregation_thresholding to storage_visualization
        application.addAppEdge("aggregation_thresholding", "client", 100, 28, 1000, "AGG_DATA_DOWN", Tuple.DOWN, AppEdge.MODULE); // Direct edge for loop completion
        application.addAppEdge("storage_visualization", "client", 100, 28, 1000, "VISUALIZED_DATA", Tuple.DOWN, AppEdge.MODULE); // Edge for storage
        application.addAppEdge("client", "DISPLAY", 1000, 500, "DISPLAY_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR); // Edge from client to actuator

        // Defining the input-output relationships of modules
        application.addTupleMapping("data_collection", "ENV_SENSOR", "RAW_DATA", new FractionalSelectivity(1.0));
        application.addTupleMapping("data_selection", "RAW_DATA", "FILTERED_DATA", new FractionalSelectivity(0.5));
        application.addTupleMapping("aggregation_thresholding", "FILTERED_DATA", "AGG_DATA", new FractionalSelectivity(1.0));
        application.addTupleMapping("aggregation_thresholding", "FILTERED_DATA", "AGG_DATA_DOWN", new FractionalSelectivity(1.0));
        application.addTupleMapping("storage_visualization", "AGG_DATA", "VISUALIZED_DATA", new FractionalSelectivity(1.0));
        application.addTupleMapping("client", "AGG_DATA_DOWN", "DISPLAY_UPDATE", new FractionalSelectivity(1.0));

        // Defining application loops to monitor latency
        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {{
            add("ENV_SENSOR");
            add("data_collection");
            add("data_selection");
            add("aggregation_thresholding");
            add("storage_visualization");
            add("client");
            add("DISPLAY");
        }});
        List<AppLoop> loops = new ArrayList<AppLoop>() {{
            add(loop1);
        }};
        application.setLoops(loops);

        return application;
    }
}