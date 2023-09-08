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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.topic.ITopic;

import padogrid.mqtt.client.cluster.ClusterService;
import padogrid.mqtt.client.cluster.HaClusters;
import padogrid.mqtt.client.cluster.HaMqttClient;
import padogrid.mqtt.client.cluster.IHaMqttPlugin;
import padogrid.simulator.config.SimulatorConfig;
import padogrid.simulator.config.SimulatorConfig.DataStructure;
import padogrid.simulator.config.SimulatorConfig.DsType;
import padogrid.simulator.config.SimulatorConfig.Product;
import padogrid.simulator.config.SimulatorConfig.Publisher;
import padogrid.simulator.config.SimulatorConfig.PublisherEquation;

/**
 * {@linkplain DataFeedSimulatorPlugin} is the simulator plugin that publishes
 * equation generated mock data.
 * 
 * @author dpark
 *
 */
public class DataFeedSimulatorPlugin implements IHaMqttPlugin, Constants {

	private String pluginName;
	private String description;
	private SimulatorConfig simulatorConfig;

	// <equationName, Equation>
	private HashMap<String, Equation> equationMap = new HashMap<String, Equation>(10);

	// <publisherName, Equation[]>
	private HashMap<Publisher, Equation[]> publisherMap = new HashMap<Publisher, Equation[]>(10);

	private HaMqttClient haclient;

	private HazelcastInstance hzInstance;

	private String productName;
	private String clusterName;
	private String configFilePath;
	private String simulatorConfigFilePath;
	private boolean isQuiet;

	public DataFeedSimulatorPlugin() {

	}

	@Override
	public boolean prelude(String pluginName, String description, Properties props, String... args) {
		String arg;
		for (int i = 0; i < args.length; i++) {
			arg = args[i];
			if (arg.equalsIgnoreCase("-?")) {
				usage();
				System.exit(0);
			} else if (arg.equals("-product")) {
				if (i < args.length - 1) {
					productName = args[++i].trim();
				}
			} else if (arg.equals("-cluster")) {
				if (i < args.length - 1) {
					clusterName = args[++i].trim();
				}
			} else if (arg.equals("-config")) {
				if (i < args.length - 1) {
					configFilePath = args[++i].trim();
				}
			} else if (arg.equals("-simulator-config")) {
				if (i < args.length - 1) {
					simulatorConfigFilePath = args[++i].trim();
				}
			} else if (arg.equals("-quiet")) {
				isQuiet = true;
			}
		}

		if (productName != null) {
			if (!productName.equalsIgnoreCase("mqtt") && !productName.equalsIgnoreCase("hazelcast")) {
				System.err.printf("ERROR: Unsupported product [%s]. Command aborted.%n", productName);
				System.exit(-1);
			}
			productName = productName.toLowerCase();
		}
		if (configFilePath != null) {
			File file = new File(configFilePath);
			if (file.exists() == false) {
				System.err.printf("ERROR: Specified configuration file does not exist [%s]. Command aborted.%n",
						configFilePath);
				System.exit(-2);
			}
		}
		if (simulatorConfigFilePath != null) {
			File file = new File(simulatorConfigFilePath);
			if (file.exists() == false) {
				System.err.printf(
						"ERROR: Specified simulator configuration file does not exist [%s]. Command aborted.%n",
						simulatorConfigFilePath);
				System.exit(-2);
			}
		}
		return true;
	}

	@Override
	public boolean init(String pluginName, String description, Properties props, String... args) {
		this.pluginName = pluginName;
		this.description = description;
		try {
			init(args);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-3);
		}
		return true;
	}

	@Override
	public void stop() {

	}

	private void init(String... args) throws FileNotFoundException {
		if (simulatorConfigFilePath == null) {
			simulatorConfigFilePath = System.getProperty(ISimulatorConfig.PROPERTY_SIMULATOR_CONFIG_FILE);
		}
		if (simulatorConfigFilePath != null && simulatorConfigFilePath.length() > 0) {
			File file = new File(simulatorConfigFilePath);
			Yaml yaml = new Yaml(new Constructor(SimulatorConfig.class));
			yaml.setBeanAccess(BeanAccess.FIELD);
			FileReader reader = new FileReader(file);
			simulatorConfig = yaml.load(reader);
		} else {
			InputStream inputStream = ClusterService.class.getClassLoader()
					.getResourceAsStream(ISimulatorConfig.DEFAULT_SIMULATOR_CONFIG_FILE);
			if (inputStream != null) {
				Yaml yaml = new Yaml(new Constructor(SimulatorConfig.class));
				yaml.setBeanAccess(BeanAccess.FIELD);
				simulatorConfig = yaml.load(inputStream);
			}
		}

		// Initialize equations
		Equation[] equations = simulatorConfig.getEquations();
		for (Equation equation : equations) {
			Method method = equation.getCalculationMethod();
			if (method == null) {
				System.out.printf("WARNING: Calculation function undefined. %s. Equation discarded.%n", equation);
			} else {
				equationMap.put(equation.getName(), equation);
			}
		}

		// Find if mqtt defined in the config
		Publisher[] publishers = simulatorConfig.getPublishers();
		boolean isMqtt = false;
		boolean isHazelcast = false;
		for (Publisher publisher : publishers) {
			isMqtt = publisher.isEnabled()
					&& (publisher.getProduct() == Product.MQTT || publisher.getProduct() == Product.mqtt);
			if (isMqtt) {
				break;
			}
		}
		for (Publisher publisher : publishers) {
			isHazelcast = publisher.isEnabled()
					&& (publisher.getProduct() == Product.HAZELCAST || publisher.getProduct() == Product.hazelcast);
			if (isHazelcast) {
				break;
			}
		}

		// Initialize HaMqttClient
		if (isMqtt) {
			if (productName == null || productName.equals("mqtt")) {
				try {
					File configFile = null;
					if (configFilePath != null) {
						configFile = new File(configFilePath);
					}
					try {
						HaClusters.initialize(configFile, args);
					} catch (IOException e) {
						e.printStackTrace();
						System.err.printf(
								"ERROR: Exception occurred while initializing virtual clusters: [file=%s]. Command aborted.%n",
								configFilePath);
						System.exit(-1);
					}
					if (clusterName == null) {
						haclient = HaClusters.getHaMqttClient();
					} else {
						haclient = HaClusters.getOrCreateHaMqttClient(clusterName);
					}

					if (haclient == null) {
						System.err.printf("ERROR: Specified cluster not found [%s]. Command aborted.%n", clusterName);
						System.exit(-10);
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// Create hzInstance
		if (isHazelcast) {
			if (productName == null || productName.equals("hazelcast")) {
				try {
					hzInstance = HazelcastClient.getOrCreateHazelcastClient();
				} catch (Exception ex) {
					System.out.printf(
							"Unable to create Hazelcast client instance [%s]%nHazelcast client is configured in 'etc/hazelcast-client.xml'.%nSkipping Hazelcast...%n",
							ex.getMessage());
				}
			}
		}

		// Must have at least one connected product
		if ((haclient == null || haclient.isConnected() == false) && hzInstance == null) {
			if (isMqtt) {
				System.err.printf("ERROR: Unable to connect to virtual cluster [%s]. Endpoints unreachable %s.%n",
						haclient.getClusterName(), Arrays.toString(haclient.getServerURIs()));
			}
			if (isHazelcast) {
				System.err.printf("ERROR: Unable to connect to Hazelcast.%n");

			}
			System.err.printf("       Command aborted.%n");
			System.exit(-11);
		} else {
			if (isMqtt) {
				if (haclient == null || haclient.isConnected() == false) {
					System.out.printf("MQTT: unable to connect to MQTT virtual cluster.%n");
				} else {
					System.out.printf("MQTT: MQTT virtual cluster connected. %s%n",
							Arrays.toString(haclient.getServerURIs()));
				}
			}
			if (isHazelcast) {
				if (hzInstance == null) {
					System.out.printf("Hazelcast: unable to connect to Hazelcast cluster.%n");
				} else {
					System.out.printf("Hazelcast: Hazelcast cluster connected. %s%n",
							hzInstance.getCluster().getMembers());
				}
			}
		}

	}

	@Override
	public void run() {
		// Initialize publishers
		Publisher[] publishers = simulatorConfig.getPublishers();
		if (publishers == null) {
			System.err.printf("ERROR: Publishers undefined in the configuration file. Command aborted.%n");
			System.exit(-3);
		}

		for (Publisher publisher : publishers) {
			if (publisher.isEnabled()) {
				if (productName == null || productName.equalsIgnoreCase(publisher.getProduct().name())) {
					// If Hazelcast is not connected then skip
					if ((publisher.getProduct() == Product.HAZELCAST || publisher.getProduct() == Product.hazelcast)
							&& hzInstance == null) {
						continue;
					}
					PublisherEquation publisherEquations = publisher.getEquations();
					if (publisherEquations != null) {
						String[] equationNames = publisherEquations.getEquationNames();
						ArrayList<Equation> equationList = new ArrayList<Equation>(equationNames.length);
						for (String equationName : equationNames) {
							Equation equation = getEquation(equationName);
							if (equation == null) {
								System.err.printf(
										"ERROR: Equation undefined for the publisher [product=%s, publisher=%s, equationName=%s]. Equation ignored.%n",
										productName, publisher.getName(), equationName);
							} else {
								equationList.add(equation);
							}
						}
						if (equationList.size() > 0) {
							publisherMap.put(publisher, equationList.toArray(new Equation[0]));
						}
					}
				}
			}
		}

		// Launch publisher threads
		final ScheduledExecutorService ses = Executors.newScheduledThreadPool(publisherMap.size());

		Iterator<Map.Entry<Publisher, Equation[]>> iterator = publisherMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Publisher, Equation[]> entry = iterator.next();
			Publisher publisher = entry.getKey();
			Equation[] equations = entry.getValue();

			ses.scheduleAtFixedRate(new Runnable() {

				PublisherDatum publisherDatum = new PublisherDatum(publisher, equations);
				long keySeq = 1;

				IMap<String, HazelcastJsonValue> hzMap = hzInstance != null
						&& (publisher.getDataStructure().getType() == DsType.MAP
								|| publisher.getDataStructure().getType() == DsType.map)
										? hzInstance.getMap(publisher.getDataStructure().getName())
										: null;
				ReplicatedMap<String, HazelcastJsonValue> hzRMap = hzInstance != null
						&& (publisher.getDataStructure().getType() == DsType.RMAP
								|| publisher.getDataStructure().getType() == DsType.rmap)
										? hzInstance.getReplicatedMap(publisher.getDataStructure().getName())
										: null;
				ITopic<HazelcastJsonValue> hzTopic = hzInstance != null
						&& (publisher.getDataStructure().getType() == DsType.TOPIC
								|| publisher.getDataStructure().getType() == DsType.topic)
										? hzInstance.getTopic(publisher.getDataStructure().getName())
										: null;
				ITopic<HazelcastJsonValue> hzRTopic = hzInstance != null
						&& (publisher.getDataStructure().getType() == DsType.RTOPIC
								|| publisher.getDataStructure().getType() == DsType.rtopic)
										? hzInstance.getReliableTopic(publisher.getDataStructure().getName())
										: null;
				IQueue<HazelcastJsonValue> hzQueue = hzInstance != null
						&& (publisher.getDataStructure().getType() == DsType.QUEUE
								|| publisher.getDataStructure().getType() == DsType.queue)
										? hzInstance.getQueue(publisher.getDataStructure().getName())
										: null;

				@Override
				public void run() {
					// TODO: The scheduler thread cannot be stopped individually. We let it fall
					// through for now. We'll need to replace the scheduler with another mechanism
					// in order to fix this.
					if (publisher.isEnabled() == false) {
						return;
					}
					DataStructure ds = publisher.getDataStructure();
					JSONObject json = publisherDatum.generateData();

					try {
						if (publisher.getProduct() == Product.MQTT || publisher.getProduct() == Product.mqtt) {
							String topic = ds.getName();
							haclient.publish(topic, json.toString().getBytes(), 0, false);
							if (isQuiet == false) {
								System.out.printf("product=%s, topic=%s: %s%n", publisher.getProduct(), topic, json);
							}
						} else {
							HazelcastJsonValue value = new HazelcastJsonValue(json.toString());
							switch (ds.getType()) {
							case MAP:
							case RMAP:
							case map:
							case rmap:
								String key;
								switch (ds.getKeyType()) {
								case FIXED:
									key = ds.getKeyValue();
									break;

								case TIME:
									key = json.getString("time");
									break;

								case UUID:
									key = UUID.randomUUID().toString();
									break;

								case SEQUENCE:
								default:
									key = Long.toString(keySeq);
									keySeq++;
									break;
								}
								if (hzMap != null) {
									hzMap.set(key, value);
									if (isQuiet == false) {
										System.out.printf("product=%s, map=%s: %s, %s%n", publisher.getProduct(),
												hzMap.getName(), key, json);
									}
								} else if (hzRMap != null) {
									hzRMap.put(key, value);
									if (isQuiet == false) {
										System.out.printf("product=%s, rmap=%s: %s, %s%n", publisher.getProduct(),
												hzRMap.getName(), key, json);
									}
								}
								break;

							case QUEUE:
							case queue:
								hzQueue.offer(value);
								if (isQuiet == false) {
									System.out.printf("product=%s, queue=%s: %s%n", publisher.getProduct(),
											hzQueue.getName(), json);
								}
								break;

							case RTOPIC:
							case rtopic:
								hzRTopic.publish(value);
								if (isQuiet == false) {
									System.out.printf("product=%s, rtopic=%s: %s%n", publisher.getProduct(),
											hzRTopic.getName(), json);
								}
								break;

							case TOPIC:
							case topic:
							default:
								hzTopic.publish(value);
								if (isQuiet == false) {
									System.out.printf("product=%s, topic=%s: %s%n", publisher.getProduct(),
											hzTopic.getName(), json);
								}
								break;
							}
						}
					} catch (Exception ex) {
						// TODO: Ignore for now
//							System.err.printf("ERROR: Exception occurred while invoking data structure [%s]%n",
//									ex.getMessage());
					}

					if (publisherDatum.getMaxCount() >= 0
							&& publisherDatum.getCount() >= publisherDatum.getMaxCount()) {
						System.out.printf("Publisher max count reached [publisher=%s, count=%d]. Publisher stopped.%n",
								publisherDatum.getName(), publisherDatum.getCount());
						publisher.setEnabled(false);
					} else if (publisherDatum.getIterations() >= 0
							&& publisherDatum.getIterationCount() >= publisherDatum.getIterations()) {
						System.out.printf(
								"Publisher max iterations reached [publisher=%s, iterationCount=%d]. Publisher stopped.%n",
								publisherDatum.getName(), publisherDatum.getIterationCount());
						publisher.setEnabled(false);
					}

					// Stop the simulator if all publishers are terminated (disabled).
					if (publisher.isEnabled() == false) {
						stopSimulator(ses);
					}
				}

			}, publisher.getInitialDelay(), publisher.getEquations().getEquationDelay(), TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * Stop the simulator if all publishers are terminated (disabled).
	 * 
	 * @param ses executor service
	 */
	private void stopSimulator(final ScheduledExecutorService ses) {
		boolean isAllTerminated = true;
		Iterator<Map.Entry<Publisher, Equation[]>> iterator = publisherMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Publisher p = iterator.next().getKey();
			if (p.isEnabled()) {
				isAllTerminated = false;
				break;
			}
		}
		if (isAllTerminated) {
			ses.shutdown();

			if (haclient != null) {
				haclient.disconnect();
			}
			if (hzInstance != null) {
				hzInstance.shutdown();
			}
		}
	}

	private Equation getEquation(String equationName) {
		return equationMap.get(equationName);
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

	private void usage() {
		String executable = System.getProperty(PROPERTY_executableName, DataFeedSimulatorPlugin.class.getName());
		if (pluginName != null) {
			writeLine();
			writeLine("PLUGIN");
			writeLine("   " + pluginName + " - " + description);
		}
		writeLine();
		writeLine("NAME");
		writeLine("   " + executable + " - Publish simulated data generated by equations");
		writeLine();
		writeLine("SNOPSIS");
		writeLine("   " + executable + " [-product mqtt|hazelcast] [-cluster cluster_name] [-config config_file]");
		writeLine("            [-simulator-config simulator_config_file] [-quiet] [-?]");
		writeLine();
		writeLine("DESCRIPTION");
		writeLine("   Publishes simulated data generated by the equations defined in the following configuration");
		writeLine("   file.");
		writeLine();
		writeLine("      etc/simulator-padogrid.yaml");
		writeLine();
		writeLine("   You can supply additional equations by creating static methods with the following");
		writeLine("   signature.");
		writeLine();
		writeLine("      public final static double your_equation(double x);");
		writeLine();
		writeLine("   The static method takes the input x, and returns y of your equation. For example,");
		writeLine("   the following method defines y = x^2.");
		writeLine();
		writeLine("      public final static double xsquared(double x) {");
		writeLine("         return x*x;");
		writeLine("      }");
		writeLine();
		writeLine("   Alternatively, you can also supply classes that implement the following interface.");
		writeLine();
		writeLine("      padgrid.simulator.ICalculation.");
		writeLine();
		writeLine("   See etc/template-simulator-padogrid.yaml for details.");
		writeLine();
		writeLine("OPTIONS");
		writeLine("   -product mqtt|hazelcast");
		writeLine("             Publishes data to the specified product. If this option is unspecified,");
		writeLine("             then by default, the simulator publishes to all of the products defined in the");
		writeLine("             configuration file.");
		writeLine();
		writeLine("   -cluster cluster_name");
		writeLine("             Connects to the specified cluster defined in the MQTT configuration file.");
		writeLine("             This option applies to MQTT only. It is ignored for Hazelcast.");
		writeLine();
		writeLine("   -config config_file");
		writeLine("             Optional MQTT configuration file.");
		writeLine("             Deault: etc/mqttv5-client.yaml");
		writeLine();
		writeLine("   -simulator-config simulator_config_file");
		writeLine("             Optional simulator configuration file.");
		writeLine("             Default: etc/simulator-padogrid.yaml");
		writeLine();
		writeLine("   -quiet");
		writeLine("             Stops printing simulated data.");
		writeLine();
		writeLine("SEE ALSO");
		writeLine("   chart_mqtt(1), chart_hazelcast(1)");
		writeLine("   etc/simulator-edge.yaml");
		writeLine("   etc/simulator-misc.yaml");
		writeLine("   etc/simulator-padogrid.yaml");
		writeLine("   etc/simulator-stocks.yaml");
		writeLine("   etc/template-simulator-padogrid.yaml");
		writeLine();
	}

	class PublisherDatum {
		String name;
		Equation[] equations;
		Datum[] data;
		Equation resetEquation;
		Datum resetDatum;
		long timeInterval;
		long startTime;
		long timestamp;
		SimpleDateFormat simpleDateFormat;
		private long resetBaseTime = 0;
		private long maxCount = -1;
		private long iterations = -1;
		private long count = 0;
		private long iterationCount = 0;

		PublisherDatum(Publisher publisher, Equation[] equations) {
			this.name = publisher.getName();
			this.equations = equations;
			this.data = new Datum[equations.length];
			for (int i = 0; i < data.length; i++) {
				data[i] = new Datum();
			}
			this.timeInterval = publisher.getTimeInterval();
			this.simpleDateFormat = new SimpleDateFormat(
					simulatorConfig.getTimeFormat() == null ? "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
							: simulatorConfig.getTimeFormat());

			if (publisher.getStartTime() == null) {
				this.startTime = System.currentTimeMillis();
			} else {
				try {
					this.startTime = simpleDateFormat.parse(publisher.getStartTime()).getTime();
				} catch (ParseException e) {
					System.err.printf(
							"ERROR: Data parser error [startTime=%s, timeFormat=%s]. Using current time instead.%n",
							publisher.getStartTime(), simulatorConfig.getTimeFormat());
					this.startTime = System.currentTimeMillis();
				}
			}
			this.timestamp = startTime;

			// Determine reset equation if defined
			if (publisher.getReset() != null && publisher.getReset().getEquationName() != null) {
				if (publisher.getReset().getResetBaseTime() >= 0) {
					String equationName = publisher.getReset().getEquationName();
					resetBaseTime = publisher.getReset().getResetBaseTime();
					for (int i = 0; i < equations.length; i++) {
						if (equations[i].getName().equals(equationName)) {
							resetEquation = equations[i];
							resetDatum = data[i];
							break;
						}
					}
				}
				this.iterations = publisher.getReset().getIterations();
			}

			this.maxCount = publisher.getMaxCount();
		}

		/**
		 * Generates data by invoking all equations.
		 * 
		 * @return JSON object containing generated data
		 */
		JSONObject generateData() {
			// Generate data
			JSONObject json = new JSONObject();
			for (int i = 0; i < data.length; i++) {
				data[i] = equations[i].updateDatum(data[i]);
				json.put(equations[i].getName(), data[i].getValue());
			}
			String time = simpleDateFormat.format(new Date(timestamp));
			json.put("time", time);

			// If reset then update timestamp accordingly
			if (resetEquation != null) {
				boolean isResetBaseTime = false;
				double baseValue = resetDatum.getBaseValue();
				switch (resetEquation.getType()) {
				case REPEAT:
					if (baseValue >= resetEquation.getMaxBase()) {
						isResetBaseTime = resetBaseTime != 0 ? true : false;
						iterationCount++;
					}
					break;
				case REVERSE:
				default:
					if (baseValue >= resetEquation.getMaxBase()) {
						isResetBaseTime = resetBaseTime != 0 ? true : false;
						iterationCount++;
					} else if (baseValue <= resetEquation.getMinBase()) {
						isResetBaseTime = resetBaseTime != 0 ? true : false;
						iterationCount++;
					}
					break;
				}
				if (isResetBaseTime) {
					timestamp = resetBaseTime();
				} else {
					timestamp += timeInterval;
				}
			} else {
				timestamp += timeInterval;
			}
			if (maxCount > 0) {
				count++;
			}
			return json;
		}

		/**
		 * Resets the base time.
		 * 
		 * Example:
		 * <ul>
		 * <li>resetBaseTime: 86_400_000 (1 day)</li>
		 * <li>startTime: "2022-10-10T09:00:00.000-0400"</li>
		 * <li>base time: "2024-11-11T11:12:34.565-0400"</li>
		 * <li>new base time: "2024-11-11T09:00:00.000-0400" + resetStartTime</li>
		 * <li>new base time: "2024-11-12T09:00:00.000-0400"</li>
		 * </ul>
		 * 
		 * @param previousDatum Previous datum
		 */
		private long resetBaseTime() {
			// Get time portion of startTime
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(startTime);
			int hour = calendar.get(Calendar.HOUR); // 12 hour clock
			int minute = calendar.get(Calendar.MINUTE);
			int second = calendar.get(Calendar.SECOND);
			int millisecond = calendar.get(Calendar.MILLISECOND);

			// Set the base time with the time portion of startTime
			long baseTime = timestamp;
			calendar.setTimeInMillis(baseTime);
			calendar.set(Calendar.HOUR, hour);
			calendar.set(Calendar.MINUTE, minute);
			calendar.set(Calendar.SECOND, second);
			calendar.set(Calendar.MILLISECOND, millisecond);

			// Add resetBaseTime
			long newBaseTime = calendar.getTimeInMillis() + resetBaseTime;
			return newBaseTime;
		}

		public String getName() {
			return name;
		}

		public long getMaxCount() {
			return maxCount;
		}

		public long getIterations() {
			return iterations;
		}

		public long getCount() {
			return count;
		}

		public long getIterationCount() {
			return iterationCount;
		}
	}
}
