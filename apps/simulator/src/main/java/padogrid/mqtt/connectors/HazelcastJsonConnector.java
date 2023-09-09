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
package padogrid.mqtt.connectors;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.mqttv5.client.MqttClient;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.shaded.org.json.JSONException;
import com.hazelcast.shaded.org.json.JSONObject;
import com.hazelcast.topic.ITopic;

import padogrid.mqtt.client.cluster.HaMqttClient;

/**
 * {@linkplain HazelcastJsonConnector} writes JSON string representation to
 * Hazelcast. It supports both published and subscribed topics. The topic names
 * are converted to Hazelcast data structure names by replacing
 * all illegal characters including '/' to '_' (underscore).
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
 * The following properties are supported.
 * <ul>
 * <li>publisherEnabled - "true" to enable the publisher to write to Hazelcast,
 * "false" to disable. Case-insensitive. Default: "true"</li>
 * <li>clusterName - Hazelcast cluster name. Default: "dev"</li>
 * <li>instanceName - Hazelcast client instance name. Default:
 * "{@linkplain HazelcastJsonConnector}-timestamp"</li>
 * <li>endpoints - Hazelcast endpoints URIs in the format of
 * host1:port1,host2:port2. Default: "localhost:5701"</li>
 * <li>topic.regex - Regex for renaming topics to Hazelcast data structure
 * names. By default, replaces '/', with '_'. Default: "[\n\r?, '\"/:)(+*%~]"</li>
 * <li>topic.regexReplacement - The string to be substituted for each match of
 * topic.regex. Default: "_"</li>
 * </ul>
 * <p>
 * 
 * @author dpark
 *
 */
public class HazelcastJsonConnector extends AbstractConnector {
    private Logger logger = LogManager.getLogger(HazelcastJsonConnector.class);
    private HazelcastInstance hzInstance;
    private HazelcastConnectorConfig.DsType dsType;
    private HazelcastConnectorConfig.KeyType keyType;

    private ThreadLocal<ConnectorArtifact> threadLocal = new ThreadLocal<ConnectorArtifact>();

    // <mapName, keySeq>>
    private HashMap<String, Long> keySeqMap = new HashMap<String, Long>();
    private String keyValue = "key";

    @Override
    public boolean init(String pluginName, String description, Properties props, String... args) {
        super.init(pluginName, description, props, args);
        String clusterName = props.getProperty("clusterName", "dev");
        String endpoints = props.getProperty("endpoints", "localhost:5701");
        String[] split = endpoints.split(",");
        ClientConfig config = new ClientConfig();
        config.setClusterName(clusterName);
        for (String address : split) {
            config.getNetworkConfig().addAddress(address);
        }
        String instanceName = props.getProperty("instanceName",
                HazelcastJsonConnector.class.getSimpleName() + "-" + System.currentTimeMillis());
        config.setInstanceName(instanceName);
        hzInstance = HazelcastClient.getOrCreateHazelcastClient(config);

        String val = props.getProperty("dsType", "MAP").toUpperCase();
        dsType = HazelcastConnectorConfig.DsType.valueOf(val);
        val = props.getProperty("keyType", "SEQUENCE");
        keyType = HazelcastConnectorConfig.KeyType.valueOf(val);

        this.topicRegex = props.getProperty("topic.regex", DEFAULT_REGEX);
        this.topicRegexReplacement = props.getProperty("topic.regexReplacement", DEFAULT_REGEX_REPLACEMENT);

        logger.info(String.format("%s initialized: [pluginName=%s, description=%s, publisherEnabled=%s, endpoint=%s]%n",
                HazelcastJsonConnector.class.getSimpleName(), pluginName, description,
                this.isPublisherEnabled, endpoints));
        return true;
    }

    @Override
    public void stop() {
    }

    @Override
    public void start(HaMqttClient haclient) {
    }

    /**
     * Saves the specified payload to Hazelcast.
     * 
     * @param topic   MQTT topic.
     * @param payload MQTT payload in JSON string representation.
     */
    private void savePayload(String topic, byte[] payload) {
        String str = new String(payload, StandardCharsets.UTF_8);
        HazelcastJsonValue hzJson = new HazelcastJsonValue(str);
        saveJson(topic, hzJson);
    }

    /**
     * Saves the specified JSON object to Hazelcast. The data structure name is
     * constructed based on the specified topic by replacing unsupported characters
     * with '_' (underscore).
     * 
     * @param topic  MQTT topic.
     * @param hzJson Hazelcast JSON object.
     */
    private void saveJson(String topic, HazelcastJsonValue hzJson) {
        // Replace unsupported characters to '_'
        String dsName = renameTopic(topic);
        switch (dsType) {
            case RMAP:
            case MAP:
                String key;
                switch (keyType) {
                    case FIXED:
                        key = keyValue;
                        break;
                    case TIME:
                        ConnectorArtifact artifact = getConnectorArtifact();
                        key = artifact.simpleDateFormat.format(new Date());
                        break;
                    case UUID:
                        key = UUID.randomUUID().toString();
                        break;
                    case KEY:
                        JSONObject json = new JSONObject(hzJson.getValue());
                        try {
                            key = json.get(keyValue).toString();
                        } catch (JSONException ex) {
                            key = keyValue;
                        }
                        break;
                    case SEQUENCE:
                    default:
                        key = getNextKeySeq(dsName);
                        break;
                }
                switch (dsType) {
                    case RMAP:
                        ReplicatedMap<String, HazelcastJsonValue> rmap = hzInstance.getReplicatedMap(dsName);
                        rmap.put(key, hzJson);
                        break;

                    case MAP:
                    default:
                        IMap<String, HazelcastJsonValue> map = hzInstance.getMap(dsName);
                        map.set(key, hzJson);
                        break;
                }
                break;

            case QUEUE:
                IQueue<HazelcastJsonValue> queue = hzInstance.getQueue(dsName);
                queue.offer(hzJson);
                break;

            case RTOPIC:
                ITopic<HazelcastJsonValue> rtopic = hzInstance.getReliableTopic(dsName);
                rtopic.publish(hzJson);
                break;

            case TOPIC:
            default:
                ITopic<HazelcastJsonValue> hztopic = hzInstance.getTopic(dsName);
                hztopic.publish(hzJson);
                break;
        }
    }

    @Override
    public byte[] beforeMessagePublished(MqttClient[] clients, String topic, byte[] payload) {
        if (isPublisherEnabled) {
            savePayload(topic, payload);
        }
        return payload;
    }

    @Override
    public void afterMessagePublished(MqttClient[] clients, String topic, byte[] payload) {
        // Do nothing
    }

    /**
     * Returns the next key sequence for the specified data structure name.
     * 
     * @param dsName Data structure name.
     */
    private String getNextKeySeq(String dsName) {
        long keySeq = keySeqMap.getOrDefault(dsName, 1L);
        String key = Long.toString(keySeq);
        keySeq++;
        keySeqMap.put(dsName, keySeq);
        return key;
    }

    @Override
    public void messageArrived(MqttClient client, String topic, byte[] payload) {
        savePayload(topic, payload);
    }

    private ConnectorArtifact getConnectorArtifact() {
        ConnectorArtifact artifact = threadLocal.get();
        if (artifact == null) {
            artifact = new ConnectorArtifact();
            threadLocal.set(artifact);
        }
        return artifact;
    }

    class ConnectorArtifact {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    }
}