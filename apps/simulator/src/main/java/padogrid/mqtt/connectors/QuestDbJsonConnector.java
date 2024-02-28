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
package padogrid.mqtt.connectors;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.json.JSONObject;

import io.questdb.client.Sender;
import io.questdb.cutlass.line.LineSenderException;
import padogrid.mqtt.client.cluster.HaMqttClient;

/**
 * {@linkplain QuestDbJsonConnector} writes JSON string representation to
 * QuestDB via ILP (InfluxDB Line Protocol). It supports both published and
 * subscribed topics. The topic names are converted to table names by replacing
 * all illegal characters to '_' (underscore).
 * <p>
 * JSON values are stored as follows.
 * <ul>
 * <li>number - double or long</li>
 * <li>string - string (If key is "time" then the value is assumed in the date
 * format of "yyyy-MM-dd'T'HH:mm:ss.SSSZ" and converted to QuestDB timestamp. If
 * the date format does not match then it is stored as string.)</li>
 * <li>boolean - boolean</li>
 * </ul>
 * All other types, i.e., arrays and nested JSON objects are ignored.
 * <p>
 * {@linkplain QuestDbJsonConnector} maintains a QuestDB connection per
 * publisher/subscriber thread.
 * <p>
 * The following properties are supported.
 * <ul>
 * <li>publisherEnabled - "true" to enable the publisher to write to QuestDB,
 * "false" to disable. Case-insensitive. Default: "true"</li>
 * <li>endpoint - QuestDB endpoint URI in the format of host:port</li>
 * <li>topic.regex - Regex for renaming topics to Hazelcast data structure
 * names. By default, replaces '/', with '_'. Default: "[\n\r?, '\"/:)(+*%~]"</li>
 * <li>topic.regexReplacement - The string to be substituted for each match of
 * topic.regex. Default: "_"</li>
 * </ul>
 * <p>
 * @author dpark
 *
 */
public class QuestDbJsonConnector extends AbstractConnector {
	protected HaMqttClient haclient;
	protected String connectorName;
	protected String endpoint;
	private Logger logger = LogManager.getLogger(QuestDbJsonConnector.class);

	/**
	 * {@linkplain Sender} is not thread safe. Use ThreadLocal to handle threads
	 * launched by the simulator.
	 */
	private ThreadLocal<ConnectorArtifact> threadLocal = new ThreadLocal<ConnectorArtifact>();

	/**
	 * senderPool contains all Sender objects created by this connector. It is used
	 * to close Sender objects when the {@linkplain #stop(HaMqttClient)} method is
	 * invoked.
	 */
	private Set<Sender> senderPool = ConcurrentHashMap.newKeySet();

	public QuestDbJsonConnector() {
	}

	@Override
	public boolean init(String pluginName, String description, Properties props, String... args) {
		super.init(pluginName, description, props, args);
		this.endpoint = props.getProperty("endpoint", "localhost:9009");
		logger.info(String.format("QuestDbConnector initialized: [pluginName=%s, description=%s, publisherEnabled=%s, endpoint=%s]%n",
				pluginName, description, this.isPublisherEnabled, this.endpoint));
		return true;
	}

	private Sender createSender() {
		return Sender.builder().address(endpoint).build();
	}

	@Override
	public void start(HaMqttClient haclient) {
		this.haclient = haclient;
		logger.info(String.format("QuestDbConnector started [%s, %s]", connectorName, endpoint));
	}

	/**
	 * Saves the specified payload to QuestDB.
     * @param topic MQTT topic.
     * @param payload MQTT payload in JSON string representation.
	 */
	private void savePayload(String topic, byte[] payload) {
		String str = new String(payload, StandardCharsets.UTF_8);
		try {
			JSONObject json = new JSONObject(str);
			saveJson(topic, json);
		} catch (LineSenderException e) {
			threadLocal.remove();
			logger.error(String.format("Unable to connect to QuestDB [%s, %s]. Message not saved. %s", connectorName,
					endpoint, e.getMessage()));
		} catch (Exception e) {
			logger.error(String.format("Exception raised while parsing data [%s, %s, %s]. Message not saved. %s",
					connectorName, endpoint, str, e.getMessage()));
		}
	}

	/**
	 * Saves the specified JSON object to QuestDB. The table name is constructed
	 * based on the specified topic by replacing unsupported characters with '_'
	 * (underscore).
	 * 
	 * @param topic MQTT topic.
	 * @param json  JSON object to store. JSON object attributes are flattened to
	 *              table columns.
	 */
	private void saveJson(String topic, JSONObject json) {
		ConnectorArtifact artifact = getConnectorArtifact();
		Sender sender = artifact.sender;
		String table = renameTopic(topic);
		sender.table(table);
		json.keys().forEachRemaining(key -> {
			Object value = json.get(key);
			if (value instanceof String) {
				if (key.equals("time")) {
					try {
						Date date = artifact.simpleDateFormat.parse(value.toString());
						sender.timestampColumn(key.toString(), date.getTime() * 1000);
					} catch (ParseException e) {
						sender.stringColumn(key.toString(), value.toString());
					}
				} else {
					sender.stringColumn(key.toString(), value.toString());
				}
			} else if (value instanceof Double) {
				sender.doubleColumn(key, (double) value);
			} else if (value instanceof Float) {
				sender.doubleColumn(key, (float) value);
			} else if (value instanceof Long) {
				sender.longColumn(key, (long) value);
			} else if (value instanceof Integer) {
				sender.longColumn(key, (int) value);
			} else if (value instanceof Boolean) {
				sender.boolColumn(key, (Boolean) value);
			}
		});
		sender.atNow();
		sender.flush();
	}

	@Override
	public void stop() {
		for (Sender sender : senderPool) {
			sender.close();
		}
	}

	/**
	 * Saves the published messages to QuestDB.
	 */
	@Override
	public byte[] beforeMessagePublished(MqttClient[] clients, String topic, byte[] payload) {
		if (isPublisherEnabled) {
			savePayload(topic, payload);
		}
		return payload;
	}

	@Override
	public void afterMessagePublished(MqttClient[] clients, String topic, byte[] payload) {
		// do nothing
	}

	private ConnectorArtifact getConnectorArtifact() {
		ConnectorArtifact artifact = threadLocal.get();
		if (artifact == null) {
			artifact = new ConnectorArtifact(createSender());
			threadLocal.set(artifact);
			senderPool.add(artifact.sender);
		}
		return artifact;
	}

	class ConnectorArtifact {
		Sender sender;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

		ConnectorArtifact(Sender sender) {
			this.sender = sender;
		}
	}

	/**
	 * Saves the subscribed data to QuestDB.
	 */
	@Override
	public void messageArrived(MqttClient client, String topic, byte[] payload) {
		savePayload(topic, payload);
	}
}
