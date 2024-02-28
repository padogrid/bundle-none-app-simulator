/*
 * Copyright (c) 2023-2024 Netcrest Technologies, LLC. All rights reserved.
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

import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.InterestResultPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionFactory;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.util.CacheListenerAdapter;
import org.apache.geode.pdx.JSONFormatter;
import org.apache.geode.pdx.PdxInstance;
import org.json.JSONObject;

import javafx.stage.Stage;
import padogrid.geode.util.GeodeUtil;
import padogrid.simulator.config.SimulatorConfig;

/**
 * Plots Geode/GemFire data structure updates.
 * 
 * @author dpark
 *
 */
public class GeodeChart extends AbstractChart {

	private static ClientCache clientCache;

	@Override
	public void start(Stage primaryStage) throws Exception {
		super.start(primaryStage);
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		clientCache.close();
	}

	private static void usage() {
		String executable = System.getProperty(PROPERTY_executableName, GeodeChart.class.getName());
		writeLine();
		writeLine("NAME");
		writeLine("   " + executable + " - Chart the Geode/GemFire data published by the simulator");
		writeLine();
		writeLine("SYNOPSIS");
		writeLine("   " + executable
				+ " [-features feature_list] [-time-format time_format] [-window-size window_size] [-?]");
		writeLine();
		writeLine("DESCRIPTION");
		writeLine("   Charts the Geode/GemFire data published by the simulator.");
		writeLine();
		writeLine("OPTIONS");
		writeLine("   -name ds_name");
		writeLine("             Data structure name, i.e., fully-qualified region path. Region paths must begin with '/'.");
		writeLine();
		writeLine("   -key key_value");
		writeLine("             Key value to listen on. If unspecified, it plots updates for all");
		writeLine("             key values. Specify this option for data structures configured");
		writeLine("             'keyType: FIXED'.");
		writeLine();
		writeLine("   -features feature_list");
		writeLine("             Optional comma separated list of features (attributes) to plot. If unspecified,");
		writeLine("             it plots all numerical features.");
		writeLine();
		writeLine("   -time-format time_format");
		writeLine("             Optional time format. The time format must match the 'time' attribute in the payload.");
		writeLine("             Default: \"" + SimulatorConfig.TIME_FORMAT + "\"");
		writeLine();
		writeLine("   -window-size");
		writeLine("             Optional chart window size. The maximum number of data points to display before start");
		writeLine("             trending the chart.");
		writeLine("             Default: " + WINDOW_SIZE);
		writeLine();
		writeLine("SEE ALSO");
		writeLine("   simulator(1)");
		writeLine("   etc/client-gemfire.properties");
		writeLine("   etc/client-cache.xml");
		writeLine("   etc/simulator-edge.yaml");
		writeLine("   etc/simulator-geode.yaml");
		writeLine("   etc/simulator-misc.yaml");
		writeLine("   etc/simulator-padogrid.yaml");
		writeLine("   etc/simulator-padogrid-all.yaml");
		writeLine("   etc/simulator-stocks.yaml");
		writeLine("   etc/template-simulator-padogrid.yaml");
		writeLine();
	}

	public static void main(String[] args) {
		String dsName = null;
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
		// dsName (region path) must start with '/'.
		if (dsName.startsWith("/") == false) {
			dsName = "/" + dsName;
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

		SimulatorConfig.DsType ds = SimulatorConfig.DsType.REGION;
		writeLine("     data structure: " + ds);
		writeLine("data structure name: " + dsName);

		try {
			clientCache = new ClientCacheFactory().create();
		} catch (Exception ex) {
			System.out.printf(
					"Unable to create Geode/GemFire client instance [%s]%nGeode/GemFire client is configured in 'etc/client-cache.xml'.%nSkipping Geode/GemFire...%n",
					ex.getMessage());
			System.exit(-11);
		}

		Region<String, PdxInstance> region;

		switch (ds) {
			case MAP:
			case map:
			case REGION:
			case region:
			default:
				String regionName = null;
				region = clientCache.getRegion(dsName);
				if (region == null) {
					String split[] = dsName.split("/");
					Region parentRegion = null;
					String regionPath = "";
					ClientRegionFactory<String, PdxInstance> clientRegionFactory = clientCache
							.createClientRegionFactory(ClientRegionShortcut.PROXY);
					for (int i = 1; i < split.length; i++) {
						regionName = split[i];	
						if (i == 1) {
							regionPath = "/" + regionName;
						} else {
							regionPath = regionPath + "/" + regionName;
						}
						parentRegion = region;
						region = clientCache.getRegion(regionPath);
						if (region == null) {
							if (i == split.length-1) {
								clientRegionFactory.addCacheListener(new CacheListenerImpl());
							}
							if (parentRegion == null) {
								region = clientRegionFactory.create(regionName);
							} else {
								region = clientRegionFactory.createSubregion(parentRegion, regionName);
							}
						}
					}
				} else {
					region.getAttributesMutator().addCacheListener(new CacheListenerImpl());
				}
				if (key == null) {
					chartTitle = "Region: " + dsName;
					region.registerInterestForAllKeys(InterestResultPolicy.KEYS_VALUES); 
				} else {
					chartTitle = "Region: " + dsName + " (key=" + key + ")";
					region.registerInterest(key, InterestResultPolicy.KEYS_VALUES); 
				}
				break;
		}

		stageTitle = "Geode/GemFire: " + GeodeUtil.getLocators(clientCache);
		launch(args);
	}

	static class CacheListenerImpl extends CacheListenerAdapter<String, PdxInstance> {
		public void afterCreate(EntryEvent<String, PdxInstance> event) {
			afterUpdate(event);
		}

		public void afterUpdate(EntryEvent<String, PdxInstance> event) {
			PdxInstance pdxObj = event.getNewValue();
			JSONObject value = new JSONObject(JSONFormatter.toJSON(pdxObj));
			updateChart(value);
		}	
	}
}
