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

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.pdx.JSONFormatter;
import org.apache.geode.pdx.PdxInstance;
import org.apache.geode.pdx.PdxSerializationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.mqttv5.client.MqttClient;

import padogrid.geode.util.GeodeUtil;
import padogrid.mqtt.client.cluster.HaMqttClient;

/**
 * {@linkplain GeodeJsonConnector} writes JSON string representation to
 * Geode. It supports both published and subscribed topics. The topic names
 * are converted to Geode data structure names by replacing
 * all illegal characters including '/' to '_' (underscore).
 * <p>
 * JSON values are stored as follows.
 * <ul>
 * <li>number - double or long</li>
 * <li>string - string (If key is "time" then the value is assumed in the date
 * format of "yyyy-MM-dd'T'HH:mm:ss.SSSZ" and converted to Geode timestamp. If
 * the date format does not match then it is stored as string.)</li>
 * <li>boolean - boolean</li>
 * </ul>
 * All other types, i.e., arrays and nested JSON objects are ignored.
 * <p>
 * The following properties are supported.
 * <ul>
 * <li>publisherEnabled - "true" to enable the publisher to write to Geode,
 * "false" to disable. Case-insensitive. Default: "true"</li>
 * <li>clusterName - Geode cluster name. Default: "dev"</li>
 * <li>instanceName - Geode client instance name. Default:
 * "{@linkplain GeodeJsonConnector}-timestamp"</li>
 * <li>endpoints - Geode endpoints URIs in the format of
 * host1:port1,host2:port2. Default: "localhost:5701"</li>
 * <li>topic.regex - Regex for renaming topics to Geode data structure
 * names. By default, replaces '/', with '_'. Default: "[\n\r?,
 * '\"/:)(+*%~]"</li>
 * <li>topic.regexReplacement - The string to be substituted for each match of
 * topic.regex. Default: "_"</li>
 * </ul>
 * <p>
 * 
 * @author dpark
 *
 */
public class GeodeJsonConnector extends AbstractConnector {
    private Logger logger = LogManager.getLogger(GeodeJsonConnector.class);
    private ClientCache clientCache;
    private GeodeConnectorConfig.DsType dsType;
    private GeodeConnectorConfig.KeyType keyType;

    private ThreadLocal<ConnectorArtifact> threadLocal = new ThreadLocal<ConnectorArtifact>();

    // <mapName, keySeq>>
    private HashMap<String, Long> keySeqMap = new HashMap<String, Long>();
    private String keyValue = "key";

    @Override
    public boolean init(String pluginName, String description, Properties props, String... args) {
        super.init(pluginName, description, props, args);

        clientCache = new ClientCacheFactory().create();

        String val = props.getProperty("dsType", "MAP").toUpperCase();
        dsType = GeodeConnectorConfig.DsType.valueOf(val);
        val = props.getProperty("keyType", "SEQUENCE");
        keyType = GeodeConnectorConfig.KeyType.valueOf(val);

        this.topicRegex = props.getProperty("topic.regex", DEFAULT_REGEX);
        this.topicRegexReplacement = props.getProperty("topic.regexReplacement", DEFAULT_REGEX_REPLACEMENT);

        String locators = GeodeUtil.getLocators(clientCache);
        logger.info(String.format("%s initialized: [pluginName=%s, description=%s, publisherEnabled=%s, endpoint=%s]%n",
                GeodeJsonConnector.class.getSimpleName(), pluginName, description,
                this.isPublisherEnabled, locators));
        return true;
    }

    @Override
    public void stop() {
    }

    @Override
    public void start(HaMqttClient haclient) {
    }

    /**
     * Saves the specified payload to Geode.
     * 
     * @param topic   MQTT topic.
     * @param payload MQTT payload in JSON string representation.
     */
    private void savePayload(String topic, byte[] payload) {
        String jsonStr = new String(payload, StandardCharsets.UTF_8);
        saveJson(topic, jsonStr);
    }

    /**
     * Saves the specified JSON object to Geode. The data structure name is
     * constructed based on the specified topic by replacing unsupported characters
     * with '_' (underscore).
     * 
     * @param topic   MQTT topic.
     * @param jsonStr JSON string value.
     */
    private void saveJson(String topic, String jsonStr) {
        // Replace unsupported characters to '_'
        String dsName = renameTopic(topic);
        PdxInstance pdxObj = JSONFormatter.fromJSON(jsonStr);
        switch (dsType) {
            case MAP:
            case REGION:
            default:
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
                        try {
                            key = pdxObj.getField(keyValue).toString();
                        } catch (PdxSerializationException ex) {
                            key = keyValue;
                        }
                        break;
                    case SEQUENCE:
                    default:
                        key = getNextKeySeq(dsName);
                        break;
                }
                switch (dsType) {
                    case MAP:
                    case REGION:
                    default:
                        Region<String, PdxInstance> region = clientCache.getRegion(dsName);
                        region.put(key, pdxObj);
                        break;
                }
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