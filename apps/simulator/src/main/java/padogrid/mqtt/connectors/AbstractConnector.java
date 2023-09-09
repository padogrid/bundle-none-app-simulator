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

import java.util.Properties;

import padogrid.mqtt.client.cluster.IHaMqttConnectorPublisher;
import padogrid.mqtt.client.cluster.IHaMqttConnectorSubscriber;

/**
 * {@linkplain AbstractConnector} provides common fields and methods for MQTT
 * plugin connector classes.
 */
public abstract class AbstractConnector implements IHaMqttConnectorPublisher, IHaMqttConnectorSubscriber {
    protected final static String DEFAULT_REGEX = "[\n\r?, '\"/:)(+*%~]";
    protected final static String DEFAULT_REGEX_REPLACEMENT = "_";

    protected String connectorName;
    protected String description;
    protected boolean isPublisherEnabled = true;
    protected String topicRegex = DEFAULT_REGEX;
    protected String topicRegexReplacement = DEFAULT_REGEX_REPLACEMENT;

    /**
     * Initializes the connector by caching the passed-in arguments.
     * @return Always true
     */
    public boolean init(String pluginName, String description, Properties props, String... args) {
        this.connectorName = pluginName;
        this.description = description;
        String val = props.getProperty("publisherEnabled", "true");
        this.isPublisherEnabled = Boolean.parseBoolean(val);
        return true;
    }

    /**
     * Converts the specified topic name to a valid name for the underlying product
     * by replacing unsupported characters with '_' (underscore).
     * 
     * @param topic MQTT topic
     */
    protected String renameTopic(String topic) {
        // Default: Replace unsupported characters to '_'
        return topic.replaceAll(this.topicRegex, this.topicRegexReplacement);
    }
}