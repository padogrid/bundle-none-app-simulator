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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import padogrid.simulator.config.SimulatorConfig;

/**
 * Plots Hazelcast data structure updates.
 * 
 * @author dpark
 *
 */
public class HazelcastChart extends Application implements Constants {

	static int WINDOW_SIZE = 200;
	static XYChart.Series<String, Number> series;

	static String stageTitle;
	static String chartTitle;

	static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	
	static IQueue<HazelcastJsonValue> hzQueue = null;

	@Override
	public void start(Stage primaryStage) throws Exception {
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
		
		// If queue, let's drain it first.
		if (hzQueue != null) {
			HazelcastJsonValue value;
			do {
				value = hzQueue.poll();
				if (value != null) {
					updateChart(value);
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
							updateChart(value);
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
		String executable = System.getProperty(PROPERTY_executableName, HazelcastChart.class.getName());
		writeLine();
		writeLine("NAME");
		writeLine("   " + executable + " - Chart the Hazelcast data published by the simulator");
		writeLine();
		writeLine("SNOPSIS");
		writeLine("   " + executable + " -name ds_name [-ds map|rmap|queue|topic|rtopic] [-?]");
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
		writeLine("SEE ALSO");
		writeLine("   simulator(1)");
		writeLine("   etc/hazelcast-client.xml");
		writeLine("   etc/simulator-edge.yaml");
		writeLine("   etc/simulator-misc.yaml");
		writeLine("   etc/simulator-padogrid.yaml");
		writeLine("   etc/simulator-stocks.yaml");
		writeLine("   etc/template-simulator-padogrid.yaml");
		writeLine();
	}

	static void updateChart(HazelcastJsonValue evalue) {
		JSONObject json = new JSONObject(evalue.getValue());
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
	}

	public static void main(String[] args) {
		String dsName = null;
		String dsStr = null;
		String key = null;

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
			}
		}

		// Validate inputs
		if (dsName == null) {
			System.err.printf("ERROR: Data structure name not specified: [-name].%n");
			System.exit(1);
		}
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
						updateChart(value);
					}
				}, true);
				hzMap.addEntryListener(new EntryUpdatedListener<String, HazelcastJsonValue>() {
	
					@Override
					public void entryUpdated(EntryEvent<String, HazelcastJsonValue> event) {
						HazelcastJsonValue value = event.getValue();
						updateChart(value);
					}
				}, true);
				chartTitle = "Map: " + dsName;
			} else {
				hzMap.addEntryListener(new EntryAddedListener<String, HazelcastJsonValue>() {
					@Override
					public void entryAdded(EntryEvent<String, HazelcastJsonValue> event) {
						HazelcastJsonValue value = event.getValue();
						updateChart(value);
					}
				}, key, true);
				hzMap.addEntryListener(new EntryUpdatedListener<String, HazelcastJsonValue>() {
					@Override
					public void entryUpdated(EntryEvent<String, HazelcastJsonValue> event) {
						HazelcastJsonValue value = event.getValue();
						updateChart(value);
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
					String key = event.getKey();
					HazelcastJsonValue value = event.getValue();
					updateChart(value);
				}

				@Override
				public void entryAdded(EntryEvent<String, HazelcastJsonValue> event) {
					String key = event.getKey();
					HazelcastJsonValue value = event.getValue();
					updateChart(value);
				}
			});
			chartTitle = "ReplicatedMap: " + dsName;
			break;

		case QUEUE:
		case queue:
			hzQueue = hzInstance.getQueue(dsName);
//			hzQueue.addItemListener(new ItemListener<HazelcastJsonValue>() {
//
//				@Override
//				public void itemAdded(ItemEvent<HazelcastJsonValue> item) {
//					HazelcastJsonValue value = null;
//					do {
//						value = hzQueue.poll();
//						if (value != null) {
//							updateChart(value);
//						}
//					} while (value != null);
//				}
//
//				@Override
//				public void itemRemoved(ItemEvent<HazelcastJsonValue> item) {
//					// ignore
//				}
//			}, false);
			chartTitle = "Qeuue: " + dsName;
			break;

		case RTOPIC:
		case rtopic:
			hzRTopic = hzInstance.getReliableTopic(dsName);
			hzRTopic.addMessageListener(new MessageListener<HazelcastJsonValue>() {

				@Override
				public void onMessage(Message<HazelcastJsonValue> message) {
					HazelcastJsonValue value = message.getMessageObject();
					updateChart(value);
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
					updateChart(value);
				}

			});
			chartTitle = "Topic: " + dsName;
			break;
		}

		stageTitle = "Hazelcast: " + hzInstance.getCluster().getMembers();

		launch(args);
	}
}
