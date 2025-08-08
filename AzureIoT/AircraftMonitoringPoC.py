# Copyright (c) Microsoft. All rights reserved.
# Licensed under the MIT license. See LICENSE file in the project root for full license information.

import random
import time
import json
import certifi
import ssl
import os
from azure.iot.device import IoTHubDeviceClient, Message

# Set SSL certificate file for Azure IoT Hub connection
os.environ["SSL_CERT_FILE"] = certifi.where()

# Device connection strings for three RSL10 sensors
# Replace with your Azure IoT Hub device connection strings from the Azure Portal
CONNECTION_STRINGS = [
    "HostName=AircraftMonitoring.azure-devices.net;DeviceId=RSL10-Sensor1;SharedAccessKey=ht93ZB+qa8dg+v3nrM5un0pRIP3LkTTXniwtbBtXl0U=",
    "HostName=AircraftMonitoring.azure-devices.net;DeviceId=RSL10-Sensor2;SharedAccessKey=ixpEpnX61uQITgexTqVwCtmwwjimRLgIDir49P9uvlk=",
    "HostName=AircraftMonitoring.azure-devices.net;DeviceId=RSL10-Sensor3;SharedAccessKey=ocTwPMEzZ4ehCLAocOvuT60rkLbIi2X+Fpfs+VmbiGU="
]

# Message template for telemetry data
MSG_TXT = '{"temperature": {temperature}, "humidity": {humidity}, "air_quality": {air_quality}}'

# Simulation parameters based on Dogea et al. (2023)
SENSOR_INTERVAL = 0.25  # 250 ms interval for data generation
TEMPERATURE_RANGE = (-40, 85)  # Temperature range (°C)
HUMIDITY_RANGE = (0, 100)  # Humidity range (% RH)
AIR_QUALITY_RANGE = (0, 500)  # Air quality index range
BLE_LATENCY = 0.006  # Simulated Bluetooth Low Energy latency (6 ms)
FILTER_THRESHOLD = 20  # Temperature threshold for filtering (°C)
AGGREGATION_WINDOW = 10  # Number of samples for aggregation
data_buffer = []  # Global buffer for fog layer aggregation
latencies = []  # Global list to track IoT Hub send latencies
message_count = 0  # Track total messages processed
filtered_count = 0  # Track filtered messages
sent_count = 0  # Track messages successfully sent to IoT Hub

def iothub_client_init():
    """
    Initialize IoT Hub clients for three RSL10 sensors.
    :return: List of IoTHubDeviceClient instances.
    """
    clients = []
    for cs in CONNECTION_STRINGS:
        try:
            client = IoTHubDeviceClient.create_from_connection_string(cs)
            clients.append(client)
            print(f"Initialized client for {cs.split(';')[1]}")
        except Exception as e:
            print(f"Failed to initialize client for {cs.split(';')[1]}: {e}")
    return clients

def simulate_sensor_data(sensor_id):
    """
    Simulate RSL10 sensor data for temperature, humidity, and air quality.
    :param sensor_id: Sensor identifier (e.g., Sensor_1).
    :return: Dictionary with simulated telemetry data.
    """
    temperature = random.uniform(TEMPERATURE_RANGE[0], TEMPERATURE_RANGE[1])
    humidity = random.uniform(HUMIDITY_RANGE[0], HUMIDITY_RANGE[1])
    air_quality = random.uniform(AIR_QUALITY_RANGE[0], AIR_QUALITY_RANGE[1])
    return {"sensor_id": sensor_id, "temperature": temperature, "humidity": humidity, "air_quality": air_quality}

def edge_collection(data):
    """
    Simulate edge layer: Collect raw data with BLE latency.
    :param data: Sensor data dictionary.
    :return: Collected data.
    """
    time.sleep(BLE_LATENCY)  # Simulate BLE latency (6 ms)
    print(f"Edge: Collected data from {data['sensor_id']}: {data}")
    return data

def virtual_sink_filter(data):
    """
    Simulate virtual sink layer: Filter data based on temperature threshold.
    :param data: Raw data from edge layer.
    :return: Filtered data or None if discarded.
    """
    global message_count, filtered_count
    message_count += 1
    if data["temperature"] > FILTER_THRESHOLD:
        filtered_count += 1
        print(f"Virtual Sink: Filtered data from {data['sensor_id']}: {data}")
        return data
    print(f"Virtual Sink: Discarded data from {data['sensor_id']} (temperature <= {FILTER_THRESHOLD}°C)")
    return None

def fog_aggregation(data):
    """
    Simulate fog layer: Aggregate data over 10 samples.
    :param data: Filtered data from virtual sink.
    :return: Aggregated data or None if not ready.
    """
    global data_buffer
    data_buffer.append(data)
    if len(data_buffer) >= AGGREGATION_WINDOW:
        # Calculate averages for temperature, humidity, and air quality
        avg_temp = sum(d["temperature"] for d in data_buffer) / len(data_buffer)
        avg_humidity = sum(d["humidity"] for d in data_buffer) / len(data_buffer)
        avg_air_quality = sum(d["air_quality"] for d in data_buffer) / len(data_buffer)
        aggregated_data = {
            "sensor_id": "aggregated",
            "temperature": avg_temp,
            "humidity": avg_humidity,
            "air_quality": avg_air_quality
        }
        data_buffer.clear()  # Reset buffer
        print(f"Fog: Aggregated data: {aggregated_data}")
        return aggregated_data
    return None

def send_to_iothub(client, data):
    """
    Send aggregated data to Azure IoT Hub and log latency.
    :param client: IoTHubDeviceClient instance.
    :param data: Aggregated data dictionary.
    """
    global sent_count, latencies
    try:
        msg_txt_formatted = MSG_TXT.format(
            temperature=data["temperature"],
            humidity=data["humidity"],
            air_quality=data["air_quality"]
        )
        message = Message(msg_txt_formatted)
        message.custom_properties["sensor_id"] = data["sensor_id"]
        message.custom_properties["temperatureAlert"] = "true" if data["temperature"] > 30 else "false"

        start_time = time.time()
        client.send_message(message)
        send_latency = (time.time() - start_time) * 1000  # Convert to ms
        latencies.append(send_latency)
        sent_count += 1
        print(f"Cloud: Sent message to IoT Hub: {message}, Latency: {send_latency:.2f} ms")
    except Exception as e:
        print(f"Cloud: Failed to send message for {data['sensor_id']}: {str(e)}")
        if "ThrottlingException" in str(e):
            print("Warning: IoT Hub throttling detected. Consider increasing SENSOR_INTERVAL.")

def log_metrics():
    """
    Log performance metrics for evaluation.
    """
    if latencies:
        avg_latency = sum(latencies) / len(latencies)
        print(f"Metrics: Average IoT Hub Latency: {avg_latency:.2f} ms")
    print(f"Metrics: Total Messages Processed: {message_count}")
    print(f"Metrics: Filtered Messages: {filtered_count}")
    print(f"Metrics: Messages Sent to IoT Hub: {sent_count}")
    print(f"Metrics: Data Reduction (Filtering): {(filtered_count / message_count * 100):.2f}%")
    print(f"Metrics: Data Reduction (Aggregation): {((filtered_count - sent_count) / filtered_count * 100):.2f}% if filtered_count > 0 else 0")

def iothub_client_telemetry_run():
    """
    Run the PoC simulation with edge/fog processing and Azure IoT Hub integration.
    """
    print("Starting Aircraft Monitoring PoC with Azure IoT Hub...")
    clients = iothub_client_init()
    try:
        while True:
            for i, client in enumerate(clients):
                # Simulate sensor data
                start_time = time.time()
                sensor_data = simulate_sensor_data(f"Sensor_{i + 1}")

                # Edge collection
                raw_data = edge_collection(sensor_data)

                # Virtual sink filtering
                filtered_data = virtual_sink_filter(raw_data)

                # Fog aggregation
                if filtered_data:
                    aggregated_data = fog_aggregation(filtered_data)

                    # Send to IoT Hub if aggregated
                    if aggregated_data:
                        send_to_iothub(client, aggregated_data)

                # Simulate 250 ms interval (adjusted for processing time)
                elapsed = (time.time() - start_time)
                sleep_time = max(0, SENSOR_INTERVAL - elapsed)
                time.sleep(sleep_time)

                # Log metrics every 30 messages (~10 aggregation cycles)
                if message_count % 30 == 0 and message_count > 0:
                    log_metrics()

    except KeyboardInterrupt:
        print("Aircraft Monitoring PoC stopped.")
        log_metrics()
    finally:
        for client in clients:
            client.disconnect()

if __name__ == '__main__':
    print("IoT Hub - Aircraft Monitoring PoC")
    print("Press Ctrl-C to exit")
    iothub_client_telemetry_run()