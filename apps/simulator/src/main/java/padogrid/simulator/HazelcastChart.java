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

import java.text.SimpleDateFormat;
import java.util.Arrays;

import org.json.JSONObject;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ItemEvent;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;
import com.hazelcast.map.MapEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;

import javafx.stage.Stage;
import padogrid.simulator.config.SimulatorConfig;

/**
 * Plots Hazelcast data structure updates.
 * 
 * @author dpark
 *
 */
public class HazelcastChart extends AbstractChart {

	static IQueue<HazelcastJsonValue> hzQueue = null;

	@Override
	public void start(Stage primaryStage) throws Exception {
		super.start(primaryStage);

		// If queue, let's drain it first.
		if (hzQueue != null) {
			HazelcastJsonValue value;
			do {
				value = hzQueue.poll();
				if (value != null) {
					updateChart(new JSONObject(value.getValue()));
				}
			} while (value != null);

			// Add listener
			hzQueue.addItemListener(new ItemListener<HazelcastJsonValue>() {

				@Override
				public void itemAdded(ItemEvent<HazelcastJsonValue> item) {
					HazelcastJsonValue value;
					do {
						value = hzQueue.poll();
						if (value != null) {
							updateChart(new JSONObject(value.getValue()));
						}
					} while (value != null);
				}

				@Override
				public void itemRemoved(ItemEvent<HazelcastJsonValue> item) {
					// ignore
				}
			}, false);
		}
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		HazelcastClient.shutdownAll();
	}

	private static void usage() {
		String executable = System.getProperty(PROPERTY_executableName, HazelcastChart.class.getName());
		writeLine();
		writeLine("NAME");
		writeLine("   " + executable + " - Chart the Hazelcast data published by the simulator");
		writeLine();
		writeLine("SNOPSIS");
		writeLine("   " + executable + " -name ds_name [-ds map|rmap|queue|topic|rtopic]");
		writeLine(
				"                   [-features feature_list] [-time-format time_format] [-window-size window_size] [-?]");
		writeLine();
		writeLine("DESCRIPTION");
		writeLine("   Charts the Hazelcast data published by the simulator.");
		writeLine();
		writeLine("OPTIONS");
		writeLine("   -name ds_name");
		writeLine("             Data structure name, i.e., topic name, map name, queue name, etc.");
		writeLine();
		writeLine("   -ds map|rmap|queue|topic|rtopic");
		writeLine("             Data structure type. Default: topic");
		writeLine();
		writeLine("   -key key_value");
		writeLine("             Key value to listen on. This option applies to map and rmap only. If");
		writeLine("             unspecified, it plots updates for all key values. Specify this option for");
		writeLine("             data structures configured with 'keyType: FIXED'.");
		writeLine();
		writeLine("   -features feature_list");
		writeLine("             Optional comma separated list of features (attributes) to plot. If unspecified,");
		writeLine("             it plots all numerical features.");
		writeLine();
		writeLine("   -time-format time_format");
		writeLine("             Optional time format. The time format must match the 'time' attibute in the payload.");
		writeLine("             Default: \"" + SimulatorConfig.TIME_FORMAT + "\"");
		writeLine();
		writeLine("   -window-size");
		writeLine("             Optional chart window size. The maximum number of data points to display before start");
		writeLine("             trending the chart.");
		writeLine("             Default: " + WINDOW_SIZE);
		writeLine();
		writeLine("SEE ALSO");
		writeLine("   simulator(1)");
		writeLine("   etc/hazelcast-client.xml");
		writeLine("   etc/simulator-edge.yaml");
		writeLine("   etc/simulator-hazelcast.yaml");
		writeLine("   etc/simulator-misc.yaml");
		writeLine("   etc/simulator-padogrid.yaml");
		writeLine("   etc/simulator-padogrid-all.yaml");
		writeLine("   etc/simulator-stocks.yaml");
		writeLine("   etc/template-simulator-padogrid.yaml");
		writeLine();
	}

	public static void main(String[] args) {
		String dsName = null;
		String dsStr = null;
		String key = null;
		int windowSize = WINDOW_SIZE;

		String arg;
		for (int i = 0; i < args.length; i++) {
			arg = args[i];
			if (arg.equalsIgnoreCase("-?")) {
				usage();
				System.exit(0);
			} else if (arg.equals("-name")) {
				if (i < args.length - 1) {
					dsName = args[++i].trim();
				}
			} else if (arg.equals("-ds")) {
				if (i < args.length - 1) {
					dsStr = args[++i].trim();
				}
			} else if (arg.equals("-key")) {
				if (i < args.length - 1) {
					key = args[++i].trim();
				}
			} else if (arg.equals("-features")) {
				if (i < args.length - 1) {
					String val = args[++i].trim();
					val = val.replaceAll("\\s", "");
					features = val.split(",");
					Arrays.sort(features);
				}
			} else if (arg.equals("-time-format")) {
				if (i < args.length - 1) {
					timeFormat = args[++i].trim();
				}
			} else if (arg.equals("-window-size")) {
				if (i < args.length - 1) {
					String val = args[++i].trim();
					try {
						windowSize = Integer.parseInt(val);
					} catch (Exception ex) {
						System.err.printf("ERROR: Invalid window-size [%s]%n. Command aborted.", val);
						System.exit(1);
					}
				}
			}
		}

		// Validate inputs
		if (dsName == null) {
			System.err.printf("ERROR: Data structure name not specified: [-name]. Command aborted.%n");
			System.exit(1);
		}

		if (windowSize < MIN_WINDOW_SIZE) {
			System.out.printf("Window size too small [%d]. Setting to the minimum value of %d...%n", windowSize,
					MIN_WINDOW_SIZE);
		} else if (windowSize > 10000) {
			System.out.printf("Window size too large [%d]. Setting to the maximum value of %d...%n", windowSize,
					MAX_WINDOW_SIZE);
			WINDOW_SIZE = MAX_WINDOW_SIZE;
		} else {
			WINDOW_SIZE = windowSize;
		}

		// Set time format
		simpleDateFormat = new SimpleDateFormat(timeFormat);

		SimulatorConfig.DsType ds = SimulatorConfig.DsType.TOPIC;
		if (dsStr != null) {
			ds = SimulatorConfig.DsType.valueOf(dsStr.toUpperCase());
			if (ds == null) {
				ds = SimulatorConfig.DsType.TOPIC;
			}
		}

		writeLine("     data structure: " + ds);
		writeLine("data structure name: " + dsName);

		HazelcastInstance hzInstance = HazelcastClient.getOrCreateHazelcastClient();
		final IMap<String, HazelcastJsonValue> hzMap;
		final ReplicatedMap<String, HazelcastJsonValue> hzRMap;
		final ITopic<HazelcastJsonValue> hzTopic;
		final ITopic<HazelcastJsonValue> hzRTopic;

		switch (ds) {
		case MAP:
		case map:
			hzMap = hzInstance.getMap(dsName);
			if (key == null) {
				hzMap.addEntryListener(new EntryAddedListener<String, HazelcastJsonValue>() {
					@Override
					public void entryAdded(EntryEvent<String, HazelcastJsonValue> event) {
						HazelcastJsonValue value = event.getValue();
						updateChart(new JSONObject(value.getValue()));
					}
				}, true);
				hzMap.addEntryListener(new EntryUpdatedListener<String, HazelcastJsonValue>() {

					@Override
					public void entryUpdated(EntryEvent<String, HazelcastJsonValue> event) {
						HazelcastJsonValue value = event.getValue();
						updateChart(new JSONObject(value.getValue()));
					}
				}, true);
				chartTitle = "Map: " + dsName;
			} else {
				hzMap.addEntryListener(new EntryAddedListener<String, HazelcastJsonValue>() {
					@Override
					public void entryAdded(EntryEvent<String, HazelcastJsonValue> event) {
						HazelcastJsonValue value = event.getValue();
						updateChart(new JSONObject(value.getValue()));
					}
				}, key, true);
				hzMap.addEntryListener(new EntryUpdatedListener<String, HazelcastJsonValue>() {
					@Override
					public void entryUpdated(EntryEvent<String, HazelcastJsonValue> event) {
						HazelcastJsonValue value = event.getValue();
						updateChart(new JSONObject(value.getValue()));
					}
				}, key, true);
				chartTitle = "Map: " + dsName + " (key=" + key + ")";
			}

			break;

		case RMAP:
		case rmap:
			hzRMap = hzInstance.getReplicatedMap(dsName);
			hzRMap.addEntryListener(new EntryListener<String, HazelcastJsonValue>() {

				@Override
				public void mapEvicted(MapEvent event) {
					// ignore
				}

				@Override
				public void mapCleared(MapEvent event) {
					// ignore
				}

				@Override
				public void entryExpired(EntryEvent<String, HazelcastJsonValue> event) {
					// ignore
				}

				@Override
				public void entryEvicted(EntryEvent<String, HazelcastJsonValue> event) {
					// ignore
				}

				@Override
				public void entryRemoved(EntryEvent<String, HazelcastJsonValue> event) {
					// ignore
				}

				@Override
				public void entryUpdated(EntryEvent<String, HazelcastJsonValue> event) {
					HazelcastJsonValue value = event.getValue();
					updateChart(new JSONObject(value.getValue()));
				}

				@Override
				public void entryAdded(EntryEvent<String, HazelcastJsonValue> event) {
					HazelcastJsonValue value = event.getValue();
					updateChart(new JSONObject(value.getValue()));
				}
			});
			chartTitle = "ReplicatedMap: " + dsName;
			break;

		case QUEUE:
		case queue:
			hzQueue = hzInstance.getQueue(dsName);
			chartTitle = "Qeuue: " + dsName;
			break;

		case RTOPIC:
		case rtopic:
			hzRTopic = hzInstance.getReliableTopic(dsName);
			hzRTopic.addMessageListener(new MessageListener<HazelcastJsonValue>() {

				@Override
				public void onMessage(Message<HazelcastJsonValue> message) {
					HazelcastJsonValue value = message.getMessageObject();
					updateChart(new JSONObject(value.getValue()));
				}
			});
			chartTitle = "Reliable Topic: " + dsName;
			break;

		case TOPIC:
		case topic:
		default:
			hzTopic = hzInstance.getTopic(dsName);
			hzTopic.addMessageListener(new MessageListener<HazelcastJsonValue>() {

				@Override
				public void onMessage(Message<HazelcastJsonValue> message) {
					HazelcastJsonValue value = message.getMessageObject();
					updateChart(new JSONObject(value.getValue()));
				}

			});
			chartTitle = "Topic: " + dsName;
			break;
		}

		stageTitle = "Hazelcast: " + hzInstance.getCluster().getMembers();

		launch(args);
	}
}
