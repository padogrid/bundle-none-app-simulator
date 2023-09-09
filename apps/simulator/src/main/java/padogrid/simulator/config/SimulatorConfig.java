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
package padogrid.simulator.config;

import padogrid.mqtt.client.cluster.internal.ConfigUtil;
import padogrid.simulator.Equation;

/**
 * {@linkplain SimulatorConfig} sets the simulator configuration parameters.
 */
public class SimulatorConfig {
	public final static String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	private String timeFormat = TIME_FORMAT;
	private Equation[] equations;
	private Publisher[] publishers;

	public String getTimeFormat() {
		if (timeFormat == null || timeFormat.trim().length() == 0) {
			timeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
		} else {
			ConfigUtil.parseStringValue(timeFormat);
		}
		return timeFormat;
	}

	public void setTimeFormat(String timeFormat) {
		this.timeFormat = timeFormat;
	}

	public Equation[] getEquations() {
		return equations;
	}

	public void setEquations(Equation[] equations) {
		this.equations = equations;
	}

	public Publisher[] getPublishers() {
		return publishers;
	}

	public void setPublishers(Publisher[] publishers) {
		this.publishers = publishers;
	}

	public static class Publisher {
		private Product product = Product.MQTT;
		private String name;
		private int initialDelay = 0;
		private String startTime;
		// timeInterval in msec
		private int timeInterval = 500;
		private boolean enabled = true;
		private DataStructure dataStructure;
		private PublisherEquation equations;
		private Reset reset;
		private long maxCount = -1;

		public Product getProduct() {
			return product;
		}

		public void setProduct(Product product) {
			this.product = product;
		}

		public String getName() {
			return ConfigUtil.parseStringValue(name);
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getInitialDelay() {
			if (initialDelay < 0) {
				return 0;
			}
			return initialDelay;
		}

		public void setInitialDelay(int initialDelay) {
			this.initialDelay = initialDelay;
		}

		public String getStartTime() {
			return ConfigUtil.parseStringValue(startTime);
		}

		public void setStartTime(String startTime) {
			this.startTime = startTime;
		}

		public int getTimeInterval() {
			return timeInterval;
		}

		public void setTimeInterval(int timeInterval) {
			this.timeInterval = timeInterval;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public DataStructure getDataStructure() {
			return dataStructure;
		}

		public void setDataStructure(DataStructure dataStructure) {
			this.dataStructure = dataStructure;
		}

		public PublisherEquation getEquations() {
			return equations;
		}

		public void setEquations(PublisherEquation equations) {
			this.equations = equations;
		}

		public Reset getReset() {
			return reset;
		}

		public void setReset(Reset reset) {
			this.reset = reset;
		}

		/**
		 * Returns the number of values to publish. If a negative value, then publishes
		 * indefinitely. Default: -1.
		 */
		public long getMaxCount() {
			return maxCount;
		}

		/**
		 * Sets the max number of values to publish.
		 * 
		 * @param maxCount If a negative value, then publishes indefinitely. Default:
		 *                 -1.
		 */
		public void setMaxCount(long maxCount) {
			this.maxCount = maxCount;
		}
	}

	public static class DataStructure {
		private DsType type = DsType.TOPIC;
		private String name;
		private KeyType keyType = KeyType.SEQUENCE;
		private String keyValue = "key";
		private int keySequenceStart = 1;

		public DsType getType() {
			return type;
		}

		public void setType(DsType type) {
			this.type = type;
		}

		public String getName() {
			return ConfigUtil.parseStringValue(name);
		}

		public void setName(String name) {
			this.name = name;
		}

		public KeyType getKeyType() {
			return keyType;
		}

		public void setKeyType(KeyType keyType) {
			this.keyType = keyType;
		}

		public String getKeyValue() {
			if (keyValue == null || keyValue.length() == 0) {
				keyValue = "key";
			}
			return ConfigUtil.parseStringValue(keyValue);
		}

		public void setKeyValue(String keyValue) {
			this.keyValue = keyValue;
		}

		public int getKeySequenceStart() {
			return keySequenceStart;
		}

		public void setKeySequenceStart(int keySequenceStart) {
			this.keySequenceStart = keySequenceStart;
		}
	}

	public static class PublisherEquation {
		private String[] equationNames;
		private long equationDelay = 500;

		public String[] getEquationNames() {
			return equationNames;
		}

		public void setEquationNames(String[] equationNames) {
			this.equationNames = equationNames;
		}

		public long getEquationDelay() {
			// The scheduler requires a non-zero positive number
			if (equationDelay <= 0) {
				equationDelay = 1;
			}
			return equationDelay;
		}

		public void setEquationDelay(long equationDelay) {
			this.equationDelay = equationDelay;
		}
	}

	public static class Reset {
		private String equationName;
		private long resetBaseTime;
		private long iterations = -1;

		public String getEquationName() {
			return ConfigUtil.parseStringValue(equationName);
		}

		public void setEquationName(String equationName) {
			this.equationName = equationName;
		}

		public long getResetBaseTime() {
			return resetBaseTime;
		}

		public void setResetBaseTime(long resetBaseTime) {
			this.resetBaseTime = resetBaseTime;
		}

		/**
		 * Returns the number of iterations. If a negative value, then iterates
		 * indefinitely. Default: -1.
		 */
		public long getIterations() {
			return iterations;
		}

		/**
		 * Sets the number of iterations.
		 * 
		 * @param iterations If a negative value, then iterates indefinitely. Default:
		 *                   -1.
		 */
		public void setIterations(long iterations) {
			this.iterations = iterations;
		}
	}

	public enum Product {
		MQTT, mqtt, HAZELCAST, hazelcast
	}

	public static enum DsType {
		MAP, RMAP, QUEUE, TOPIC, RTOPIC, map, rmap, queue, topic, rtopic
	}

	public static enum KeyType {
		FIXED, SEQUENCE, TIME, UUID, sequence, time, uuid
	}
}