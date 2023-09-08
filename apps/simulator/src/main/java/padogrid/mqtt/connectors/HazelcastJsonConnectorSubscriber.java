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
import padogrid.mqtt.client.cluster.IHaMqttConnectorSubscriber;

public class HazelcastJsonConnectorSubscriber implements IHaMqttConnectorSubscriber {

    protected String connectorName;
    protected String description;
    private Logger logger = LogManager.getLogger(HazelcastJsonConnectorSubscriber.class);
    private HazelcastInstance hzInstance;
    private HazelcastConnectorConfig.DsType dsType;
    private HazelcastConnectorConfig.KeyType keyType;

    private ThreadLocal<ConnectorArtifact> threadLocal = new ThreadLocal<ConnectorArtifact>();

    // <mapName, keySeq>>
    private HashMap<String, Long> keySeqMap = new HashMap<String, Long>();
    private String keyValue = "key";

    @Override
    public boolean init(String pluginName, String description, Properties props, String... args) {
        this.connectorName = pluginName;
        this.description = description;
        String clusterName = props.getProperty("clusterName", "dev");
        String endpoints = props.getProperty("endpoint", "localhost:5701");
        String[] split = endpoints.split(",");
        ClientConfig config = new ClientConfig();
        config.setClusterName(clusterName);
        for (String address : split) {
            config.getNetworkConfig().addAddress(address);
        }
        String instanceName = props.getProperty("instanceName", HazelcastJsonConnectorSubscriber.class.getSimpleName() + "-" + System.currentTimeMillis());
        config.setInstanceName(instanceName);
        hzInstance = HazelcastClient.getOrCreateHazelcastClient(config);

        String val = props.getProperty("dsType", "MAP").toUpperCase();
        dsType = HazelcastConnectorConfig.DsType.valueOf(val);
        val = props.getProperty("keyType", "SEQUENCE");
        keyType = HazelcastConnectorConfig.KeyType.valueOf(val);
        logger.info(String.format("%s initialized: [pluginName=%s, description=%s, endpoint=%s]%n",
                HazelcastJsonConnectorSubscriber.class.getSimpleName(), pluginName, description, endpoints));
        return true;
    }

    @Override
    public void stop() {
    }

    @Override
    public void start(HaMqttClient haclient) {
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
        String str = new String(payload, StandardCharsets.UTF_8);
        HazelcastJsonValue value = new HazelcastJsonValue(str);
        // Replace unsupported characters to '_'
        String dsName = topic.replaceAll("[\n\r?, '\"/:)(+*%~]", "_");
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
                        JSONObject json = new JSONObject(str);
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
                        rmap.put(key, value);
                        break;

                    case MAP:
                    default:
                        IMap<String, HazelcastJsonValue> map = hzInstance.getMap(dsName);
                        map.set(key, value);
                        break;
                }
                break;

            case QUEUE:
                IQueue<HazelcastJsonValue> queue = hzInstance.getQueue(dsName);
                queue.offer(value);
                break;

            case RTOPIC:
                ITopic<HazelcastJsonValue> rtopic = hzInstance.getReliableTopic(dsName);
                rtopic.publish(value);
                break;

            case TOPIC:
            default:
                ITopic<HazelcastJsonValue> hztopic = hzInstance.getTopic(dsName);
                hztopic.publish(value);
                break;
        }
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