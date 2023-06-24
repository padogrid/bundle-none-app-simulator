package padogrid.simulator.config;

import padogrid.mqtt.client.cluster.internal.ConfigUtil;
import padogrid.simulator.Equation;

public class SimulatorConfig {
	private Equation[] equations;
	private Publisher[] publishers;

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
		private String equationName;
		private DataStructure dataStructure;
		private int initialDelay = 0;
		private int timeInterval = 500;
		private long count = Long.MAX_VALUE;
		private boolean enabled = true;

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

		public String getEquationName() {
			return ConfigUtil.parseStringValue(equationName);
		}

		public void setEquationName(String equationName) {
			this.equationName = equationName;
		}

		public void setEquation(String equationName) {
			this.equationName = equationName;
		}

		public int getInitialDelay() {
			return initialDelay;
		}

		public void setInitialDelay(int initialDelay) {
			this.initialDelay = initialDelay;
		}

		public int getTimeInterval() {
			return timeInterval;
		}

		public void setTimeInterval(int timeInterval) {
			this.timeInterval = timeInterval;
		}

		public long getCount() {
			return count;
		}

		public void setCount(long count) {
			this.count = count;
		}

		public DataStructure getDataStructure() {
			return dataStructure;
		}

		public void setDataStructure(DataStructure dataStructure) {
			this.dataStructure = dataStructure;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}

	public static class DataStructure {
		private DsType type = DsType.TOPIC;
		private String name;
		private KeyType keyType = KeyType.SEQUENCE;
		private String keyPrefix = "";
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

		public String getKeyPrefix() {
			return keyPrefix;
		}

		public void setKeyPrefix(String keyPrefix) {
			this.keyPrefix = keyPrefix;
		}

		public int getKeySequenceStart() {
			return keySequenceStart;
		}

		public void setKeySequenceStart(int keySequenceStart) {
			this.keySequenceStart = keySequenceStart;
		}
	}

	public enum Product {
		MQTT, mqtt, HAZELCAST, hazelcast
	}

	public static enum DsType {
		MAP, RMAP, QUEUE, TOPIC, RTOPIC, map, rmap, queue, topic, rtopic
	}

	public static enum KeyType {
		SEQUENCE, TIME, UUID, sequence, time, uuid
	}
}
