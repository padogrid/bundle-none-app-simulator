package padogrid.mqtt.connectors;

public class HazelcastConnectorConfig {
	public static enum DsType {
		MAP, RMAP, QUEUE, TOPIC, RTOPIC
	}

	public static enum KeyType {
		FIXED, SEQUENCE, TIME, UUID, KEY
	}
}
