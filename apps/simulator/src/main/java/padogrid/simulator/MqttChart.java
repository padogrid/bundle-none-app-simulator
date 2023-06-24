/*
 * Copyright (c) 2023 Netcrest Technologies, LLC. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package padogrid.simulator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import padogrid.mqtt.client.cluster.HaClusters;
import padogrid.mqtt.client.cluster.HaMqttClient;
import padogrid.mqtt.client.cluster.HaMqttConnectionOptions;
import padogrid.mqtt.client.cluster.IClusterConfig;
import padogrid.mqtt.client.cluster.IHaMqttCallback;
import padogrid.mqtt.client.cluster.config.ClusterConfig;

/**
 * Plots MQTT topic updates.
 * 
 * @author dpark
 *
 */
public class MqttChart extends Application implements Constants {

	static int WINDOW_SIZE = 200;
	static XYChart.Series<String, Number> series;

	static String topicFilter = null;
	static HaMqttClient client = null;

	@Override
	public void start(Stage primaryStage) throws Exception {
		String[] endpoints = client.getServerURIs();
		Arrays.sort(endpoints);
		String endpointsStr = "";
		for (int i = 0; i < endpoints.length; i++) {
			if (i > 0) {
				endpointsStr += ",";
			}
			endpointsStr += endpoints[i];
		}
		String stageTitle = "MQTT: " + endpointsStr;
		String chartTitle = "Topic: " + topicFilter;
		primaryStage.setTitle(stageTitle);

		// defining the axes
		final CategoryAxis xAxis = new CategoryAxis(); // we are gonna plot against time
		final NumberAxis yAxis = new NumberAxis();
		xAxis.setLabel("Time");
		xAxis.setAnimated(false); // axis animations are removed
		yAxis.setLabel("Value");
		yAxis.setAnimated(false); // axis animations are removed

		// creating the line chart with two axis created above
		final LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
		lineChart.setTitle(chartTitle);
		lineChart.setAnimated(false); // disable animations

		// defining a series to display data
		series = new XYChart.Series<>();
		series.setName("Data Series");

		// add series to chart
		lineChart.getData().add(series);

		// setup scene
		Scene scene = new Scene(lineChart, 1000, 600);
		primaryStage.setScene(scene);

		// show the stage
		primaryStage.show();
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		HaClusters.stop();
	}

	private static void writeLine() {
		System.out.println();
	}

	private static void writeLine(String line) {
		System.out.println(line);
	}

	@SuppressWarnings("unused")
	private static void write(String str) {
		System.out.print(str);
	}

	private static void usage() {
		String executable = System.getProperty(PROPERTY_executableName, MqttChart.class.getName());
		writeLine();
		writeLine("NAME");
		writeLine("   " + executable + " - Chart the MQTT data published by the simulator");
		writeLine();
		writeLine("SNOPSIS");
		writeLine("   " + executable
				+ " [[-cluster cluster_name] [-config config_file] | [-endpoints serverURIs]] [-fos fos] [-qos qos] -t topic_filter [-?]");
		writeLine();
		writeLine("DESCRIPTION");
		writeLine("   Charts the MQTT data published by the simulator.");
		writeLine();
		writeLine("   - If '-cluster' is specified and -config is not specified, then '-cluster'");
		writeLine("     represents a PadoGrid cluster and maps it to a unique virtual cluster name.");
		writeLine();
		writeLine("   - If '-config' is specified, then '-cluster' represents a virtual cluster");
		writeLine("     defined in the configuration file.");
		writeLine();
		writeLine("   - If '-config' is specified and '-cluster' is not specified, then the default");
		writeLine("     virtual cluster defined in the configuration file is used.");
		writeLine();
		writeLine("   - If '-endpoints' is specified, then '-cluster' and '-config' are not allowed.");
		writeLine();
		writeLine("   - If '-cluster', '-config', and '-endpoints' are not specified, then the PadoGrid's");
		writeLine("     current context cluster is used.");
		writeLine();
		writeLine("   - If PadoGrid cluster is not an MQTT cluster, then it defaults to endpoints,");
		writeLine("     'tcp://localhost:1883-1885'.");
		writeLine();
		writeLine("OPTIONS");
		writeLine("   -cluster cluster_name");
		writeLine("             Connects to the specified PadoGrid cluster. Exits if it does not exist in the");
		writeLine("             current workspace.");
		writeLine();
		writeLine("   -endpoints serverURIs");
		writeLine("             Connects to the specified endpoints. Exits if none of the endpoints exist.");
		writeLine("             Default: tcp://localhost:1883-1885");
		writeLine();
		writeLine("   -config config_file");
		writeLine("             Optional HaMqttClient configuration file.");
		writeLine();
		writeLine("   -fos fos");
		writeLine("             Optional FoS value. Valid values are 0, 1, 2, 3. Default: 0.");
		writeLine();
		writeLine("   -qos qos");
		writeLine("             Optional QoS value. Valid values are 0, 1, 2. Default: 0.");
		writeLine();
		writeLine("   -t topic_filter");
		writeLine("             Topic filter.");
		writeLine();
		writeLine("SEE ALSO");
		writeLine("   simulator(1)");
		writeLine("   etc/mqttv5-client.yaml");
		writeLine("   etc/simulator-edge.yaml");
		writeLine("   etc/simulator-misc.yaml");
		writeLine("   etc/simulator-padogrid.yaml");
		writeLine("   etc/simulator-stocks.yaml");
		writeLine("   etc/template-simulator-padogrid.yaml");
		writeLine();
	}

	public static void main(String[] args) {
		String clusterName = null;
		String endpoints = null;
		String configFilePath = null;
		int qos = 0;
		int fos = 0;
		topicFilter = null;

		String arg;
		for (int i = 0; i < args.length; i++) {
			arg = args[i];
			if (arg.equalsIgnoreCase("-?")) {
				usage();
				System.exit(0);
			} else if (arg.equals("-cluster")) {
				if (i < args.length - 1) {
					clusterName = args[++i].trim();
				}
			} else if (arg.equals("-endpoints")) {
				if (i < args.length - 1) {
					endpoints = args[++i].trim();
				}
			} else if (arg.equals("-config")) {
				if (i < args.length - 1) {
					configFilePath = args[++i].trim();
				}
			} else if (arg.equals("-t")) {
				if (i < args.length - 1) {
					topicFilter = args[++i].trim();
				}
			} else if (arg.equals("-qos")) {
				if (i < args.length - 1) {
					String qosStr = args[++i].trim();
					try {
						qos = Integer.parseInt(qosStr);
					} catch (NumberFormatException ex) {
						System.err.printf("ERROR: Invalid qos: [%s]. Valid values are 0, 1, or 2. Command aborted.%n",
								qosStr);
						System.exit(1);
					}
				}
			} else if (arg.equals("-fos")) {
				if (i < args.length - 1) {
					String fosStr = args[++i].trim();
					try {
						qos = Integer.parseInt(fosStr);
					} catch (NumberFormatException ex) {
						System.err.printf("ERROR: Invalid fos: [%s]. Valid values are 0, 1, 2, 3. Command aborted.%n",
								fosStr);
						System.exit(1);
					}
				}
			}
		}

		// Validate inputs
		if (clusterName != null && endpoints != null) {
			System.err.printf("ERROR: -cluster, -endpoints are not allowed together. Command aborted.%n");
			System.exit(2);
		}
		if (configFilePath != null && endpoints != null) {
			System.err.printf("ERROR: -config, -endpoints are not allowed together. Command aborted.%n");
			System.exit(2);
		}
		if (topicFilter == null) {
			System.err.printf("ERROR: Topic filter not specified: [-t].%n");
			System.exit(3);
		}

		// Collect system properties - passed in by the invoking script.
		if (configFilePath == null && clusterName == null) {
			clusterName = System.getProperty("cluster.name");
		}
		if (endpoints == null) {
			endpoints = System.getProperty("cluster.endpoints");
		}

		// Display all options
		if (clusterName != null) {
			writeLine("PadoGrid Cluster: " + clusterName);
		}
		String virtualClusterName = clusterName;
		if (configFilePath != null) {
			try {
				// We need to do this here in order to get the default
				// cluster name.
				HaClusters.initialize(new File(configFilePath));
				if (virtualClusterName == null) {
					virtualClusterName = HaClusters.getDefaultClusterName();
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.err.printf(
						"ERROR: Exception occurred while initializing virtual clusters: [file=%s]. Command aborted.%n",
						configFilePath);
				System.exit(-1);
			}
		}
		if (virtualClusterName == null) {
			virtualClusterName = "subscriber";
		}

		writeLine("cluster: " + virtualClusterName + " (virtual)");

		// If endpoints is not set, then default to
		// IClusterConfig.DEFAULT_CLIENT_SERVER_URIS.
		if (configFilePath == null && endpoints == null) {
			endpoints = IClusterConfig.DEFAULT_CLIENT_SERVER_URIS;
		}

		if (endpoints != null) {
			writeLine("endpoints: " + endpoints);
		}
		writeLine("fos: " + fos);
		writeLine("qos: " + qos);
		if (configFilePath != null) {
			writeLine("config: " + configFilePath);
		}
		writeLine("topicFilter: " + topicFilter);

		// Create cluster
		if (configFilePath == null) {
			ClusterConfig clusterConfig = new ClusterConfig();
			clusterConfig.setDefaultCluster(virtualClusterName);
			HaMqttConnectionOptions options = new HaMqttConnectionOptions();
			endpoints = endpoints.replaceAll(" ", "");
			options.getConnection().setServerURIs(endpoints.split(","));
			ClusterConfig.Cluster cluster = new ClusterConfig.Cluster();
			cluster.setName(virtualClusterName);
			cluster.setFos(fos);
			cluster.setConnections(options);
			clusterConfig.setClusters(new ClusterConfig.Cluster[] { cluster });
			try {
				HaClusters.initialize(clusterConfig);
				client = HaClusters.getOrCreateHaMqttClient(cluster);
			} catch (IOException e) {
				e.printStackTrace();
				System.err.printf(
						"ERROR: Exception occurred while creating a virtual cluster: [%s]. Command aborted.%n",
						virtualClusterName);
				System.exit(-1);
			}
		} else {
			try {
				client = HaClusters.getOrCreateHaMqttClient(virtualClusterName);
			} catch (IOException e) {
				e.printStackTrace();
				System.err.printf(
						"ERROR: Exception occurred while creating a virtual cluster: [file=%s]. Command aborted.%n",
						configFilePath);
				System.exit(-1);
			}
		}

		// This should never occur
		if (client == null) {
			System.err.printf("ERROR: Unable to create the virtual cluster: [%s]. Command aborted.%n",
					virtualClusterName);
			System.exit(-1);
		}

		// Register callback to display received messages
		client.addCallbackCluster(new IHaMqttCallback() {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

			@Override
			public void mqttErrorOccurred(MqttClient client, MqttException exception) {
				// do nothing
			}

			@Override
			public void messageArrived(MqttClient client, String topic, MqttMessage message) throws Exception {
				try {
					JSONObject json = new JSONObject(new String(message.getPayload(), StandardCharsets.UTF_8));
					String time = json.getString("time");

					double value = json.getDouble("value");
					Platform.runLater(() -> {
						try {
							Date date = simpleDateFormat.parse(time);
							date.getTime();
							series.getData().add(new XYChart.Data<>(simpleDateFormat.format(date), value));
							if (series.getData().size() > WINDOW_SIZE)
								series.getData().remove(0);
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					});
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			@Override
			public void disconnected(MqttClient client, MqttDisconnectResponse disconnectResponse) {
				// do nothing
			}

			@Override
			public void deliveryComplete(MqttClient client, IMqttToken token) {
				// do nothing
			}

			@Override
			public void connectComplete(MqttClient client, boolean reconnect, String serverURI) {
				// do nothing
			}

			@Override
			public void authPacketArrived(MqttClient client, int reasonCode, MqttProperties properties) {
				// do nothing
			}
		});

		// Connect
		try {
			client.connect();
			if (client.isConnected() == false) {
				System.err
						.printf("ERROR: Unable to connect to any of the endpoints in the cluster. Command aborted.%n");
				HaClusters.stop();
				System.exit(-1);
			}
			client.subscribe(topicFilter, qos);

			launch(args);

		} catch (Exception e) {
			System.err.printf("ERROR: Error occured while subscribing to the topic filter. Command aborted.%n");
			e.printStackTrace();
			HaClusters.stop();
			System.exit(-3);
		}
	}
}
