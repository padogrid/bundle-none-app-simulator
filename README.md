![PadoGrid](https://github.com/padogrid/padogrid/raw/develop/images/padogrid-3d-16x16.png) [*PadoGrid*](https://github.com/padogrid) | [*Catalogs*](https://github.com/padogrid/catalog-bundles/blob/master/all-catalog.md) | [*Manual*](https://github.com/padogrid/padogrid/wiki) | [*FAQ*](https://github.com/padogrid/padogrid/wiki/faq) | [*Releases*](https://github.com/padogrid/padogrid/releases) | [*Templates*](https://github.com/padogrid/padogrid/wiki/Using-Bundle-Templates) | [*Pods*](https://github.com/padogrid/padogrid/wiki/Understanding-Padogrid-Pods) | [*Kubernetes*](https://github.com/padogrid/padogrid/wiki/Kubernetes) | [*Docker*](https://github.com/padogrid/padogrid/wiki/Docker) | [*Apps*](https://github.com/padogrid/padogrid/wiki/Apps) | [*Quick Start*](https://github.com/padogrid/padogrid/wiki/Quick-Start)

---

# Data Feed Simulator

This bundle includes a data feed simulator for generating continous numerical data for MQTT and Hazelcast.

## Installing Bundle

```bash
install_bundle -download bundle-none-app-simulator
```

❗ This bundle is no longer compatible with PadoGrid 0.9.26 due to the recent changes made in the `HaMqttClient` API. Please use PadoGrid 0.9.27-SNAPSHOT or a later version.

## Use Case

The Data Feed Simulator publishes numerical data computed by a set of equations. By adding noise (jitter) to the computed values, you can produce simulated data for real-world applications. There are a number of useful data feeds included in this bundle. You can customize them or introduce your own equations to generate based on your application requirements.

![Igloo](images/igloo.png)

## Required Software

- PadoGrid 0.9.27-SNAPSHOT+ (07/01/23)
- Mosquitto 2.x
- Hazelcast 5.x

## Bundle Contents

```console
simulator
├── bin_sh
│   ├── build_app
│   ├── chart_hazelcast
│   ├── chart_mqtt
│   ├── setenv.sh
│   └── simulator
├── etc
│   ├── hazelcast-client.xml
│   ├── log4j2.properties
│   ├── mqttv5-client.yaml
│   ├── simulator-edge.yaml
│   ├── simulator-logging.properties
│   ├── simulator-misc.yaml
│   ├── simulator-padogrid.yaml
│   ├── simulator-stocks.yaml
│   └── template-simulator-padogrid.yaml
└── src
    └── main
        └── java
```

## Configuring Bundle Environment

Build the simulator application as follows. The `build_app` script compiles the provided simulator source code.

```bash
cd_app simulator/bin_sh
./build_app
```

## Startup Sequence

### 1. Start Mosquitto and/or Hazelcast

```bash
# Mosquitto
make_cluster -product mosquitto
switch_cluster mymosquitto
start_cluster 

# Hazelcast
make_cluster -product hazelcast
switch_cluster myhz
start_cluster 
```

### 2. Build simulator

```bash
# Build the simulator if you haven't done so already
cd_app simulator/bin_sh
./build_app
```

### 3. Start simulator

```bash
cd_app simulator/bin_sh
./simulator
```

### 4. Display data in trending chart

By default, the `simulator` command loads the `etc/simulator-padogrid.yaml` file, which defines numerous equations. Each equation is invoked by the paired publisher which defines the topic to publish the data. Take a look at the configuration file and select the topics that you want to view in charts. 

```bash
cd_app simulator
cat etc/simulator-padogrid.yaml
```

#### 4.1. Display MQTT data in trending chart

Display the MQTT data by running the `chart_mqtt` command which takes a single topic as an argument.

```bash
cd_app simulator/bin_sh
./chart_mqtt -?
```

Output:

```console
NAME
   chart_mqtt - Chart the MQTT data published by the simulator

SNOPSIS
   chart_mqtt [[-cluster cluster_name] [-config config_file] | [-endpoints serverURIs]]
              [-fos fos] [-qos qos] -t topic_filter [-?]

DESCRIPTION
   Charts the MQTT data published by the simulator.

   - If '-cluster' is specified and -config is not specified, then '-cluster'
     represents a PadoGrid cluster and maps it to a unique virtual cluster name.

   - If '-config' is specified, then '-cluster' represents a virtual cluster
     defined in the configuration file.

   - If '-config' is specified and '-cluster' is not specified, then the default
     virtual cluster defined in the configuration file is used.

   - If '-endpoints' is specified, then '-cluster' and '-config' are not allowed.

   - If '-cluster', '-config', and '-endpoints' are not specified, then the PadoGrid's
     current context cluster is used.

   - If PadoGrid cluster is not an MQTT cluster, then it defaults to endpoints,
     'tcp://localhost:1883-1885'.

OPTIONS
   -cluster cluster_name
             Connects to the specified PadoGrid cluster. Exits if it does not exist in the
             current workspace.

   -endpoints serverURIs
             Connects to the specified endpoints. Exits if none of the endpoints exist.
             Default: tcp://localhost:1883-1885

   -config config_file
             Optional HaMqttClient configuration file.

   -fos fos
             Optional FoS value. Valid values are 0, 1, 2, 3. Default: 0.

   -qos qos
             Optional QoS value. Valid values are 0, 1, 2. Default: 0.

   -t topic_filter
             Topic filter.
```

Try running the following examples.

```bash
# Display sine wave
./chart_mqtt -t test/sine

# Display damped sine wave
./chart_mqtt -t test/dampedSineWave
```

#### 4.2. `chart_hazelcast`

Display the Hazelcast data by running the `chart_hazelcast` command which takes a data structure type and name as arguments.

```bash
./chart_hazelcast -?
```

Output:

```console
NAME
   chart_hazelcast - Chart the Hazelcast data published by the simulator

SNOPSIS
   chart_hazelcast -name ds_name [-ds map|rmap|queue|topic|rtopic] [-?]

DESCRIPTION
   Charts the Hazelcast data published by the simulator.

OPTIONS
   -name ds_name
             Data structure name, i.e., topic name, map name, queue name, etc.

   -ds map|rmap|queue|topic|rtopic
             Data structure type. Default: topic

   -key key_value
             Key value to listen on. This option applies to map and rmap only. If
             unspecified, it plots updates for all key values. Specify this option for
             data structures configured with 'keyType: FIXED'.

SEE ALSO
   simulator(1)
   etc/hazelcast-client.xml
   etc/simulator-edge.yaml
   etc/simulator-misc.yaml
   etc/simulator-padogrid.yaml
   etc/simulator-stocks.yaml
   etc/template-simulator-padogrid.yaml
```

#### 4.2. Display MQTT data in trending chart

Try running the following examples.

```bash
# Display sine wave steamed to topic data structure
./chart_hazelcast -name test/sine -ds topic

# Display damped sine wave streamed to topic data structure
./chart_hazelcast -name test/dampedSineWave -ds topic
```

#### 4.3. Simulator configuration files

Application specific data feeds are defined in `etc/simulator-stock.yaml` and `etc/simulator-misc.yaml`. Try running them.

First start the simulator:

```bash
# Stock prices
./simulator -simulator-config ../etc/simulator-stocks.yaml

# Miscellaneous data. For Hazelcast, publishes data to map, rmap, queue, topic, and rtopic.
./simulator -simulator-config ../etc/simulator-misc.yaml
```

Run MQTT charts:

```bash
# Display simulator-stocks.yaml
./chart_mqtt -t test/stock1
./chart_mqtt -t test/stock2

# Display simulator-misc.yaml
./chart_mqtt -t test/igloo
./chart_mqtt -t test/temperature
./chart_mqtt -t test/carcost
./chart_mqtt -t test/heartbeat
```

Run Hazelcast charts:

```bash
# Display simulator-stocks.yaml
./chart_hazelcast -name stock1 -ds topic
./chart_hazelcast -name stock2 -ds topic

# Display simulator-misc.yaml (publishes data to map, rmap, queue, topic, rtopic)
./chart_hazelcast -name igloo -ds topic
./chart_hazelcast -name temperature -ds topic
./chart_hazelcast -name carcost -ds topic
./chart_hazelcast -name heartbeat -ds topic
```

The provided configuration files also include Hazelcast data structures other than topics.

```bash
cd_app simulator
vi etc/simulator-misc.yaml
```

For Hazelcast `Map` and `Replicated Map`, these files have been configured with `keyType: FIXED` to update the same key. You can optionally, specify the '-key' option to plot updates on the specified key.

```bash
cd_app simulator/bin_sh
./chart_hazelcast -name carcost -ds map -key key
```

To configure key types other than 'FIXED', please see  `etc/template-simulator-padogrid.yaml` for details. For example, the following sets `keyType: SEQUENCE` to increment the key value starting from 1000.

```yaml
publishers:
...
  - product: hazelcast
    enabled: true
    name: carcost-publisher
    equationName: carcost
    dataStructure:
      type: map
      name: carcost
      keyType: SEQUENCE
      keySequenceStart: 1000
    timeInterval: 500
```

Run Hazelcast chart:

```bash
cd_app simulator/bin_sh
./chart_hazelcast -name carcost -ds map
```

✏️  Note that `chart_hazelcast` first drains and plots existing entries in the Hazelcast `Queue` data structure before plotting updates.

## Tuning Data Feeds

Each equation defined in the configuration file can be tuned to fit your needs. The [`etc/template-simulator-padogrid.yaml`](apps/simulator/etc/template-simulator-padogrid.yaml) provides detailed parameter descriptions.

```bash
cd_app simulator
cat etc/template-simulator-padogrid.yaml
```

Output:

```yaml
# Define one or more equations
equations:
    # Required unique equation name. Required for configuring publisher
  - name: null

    # Optional equation formula (for documentation only)
    # Example: y=sin(x)
    formula: null

    # Optional equation description (for documentation only)
    description: null

    # Optional time format.
    # Default: "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    timeFormat: "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

    # Optional start time. Must conform to timeFormat. Ex: "2022-10-10T09:00:00.000-0400"
    # Default: current time.
    startTime: "2022-10-10T09:00:00.000-0400"

    # Optional time interval in milliseconds
    # Default: 500
    timeInterval: 500

    # Optional miniumn base (x) value. The base is equivalent to x in y=x.
    # Default: -1
    minBase: -1

    # Optional maximum base (x) value. The base is equivalent to x in y=x.
    # Default: 1
    maxBase: 1

    # Optional base value spread (delta). x is incremented by this value before each calculation.
    # Default: 0.1
    baseSpread: 0.1

    # Optional percentage of the random number to add to the equation result before
    # the base average, (maxBase-minBase)/2, is added to the computed value.
    # Default: 0.1
    jitter: 0.1

    # Optional multiplier. The jitter computed value is multiplied by this value. Set this
    # attribute to amplify or deamplify the final value.
    # Default: 1.0
    multiplier: 1.0

    # Calculation function. Must be a static method with the following signature.
    #    public final double my_function(double x);
    # The following classes contain useful functions.
    #   java.lang.Math
    #    padogrid.simulator.eq.Equations
    # Example: java.lang.Math.sin
    #          padogrid.simulator.eq.circle
    # Must specify one of calculationFunction and calculationClass. calculationFunction overrides
    # calculationClass.
    calculationFunction: null

    # Calculation class. Must implement padogrid.simulator.ICalculation.
    # Must specify one of calculationFunction and calculationClass. calculationFunction overrides
    # calculationClass.
    calculationClass: null

    # Optional constant value. This value is added to the value returned by the calculation
    # function. Set this attribute to move the curve up or down along the y-axis.
    # Default: 0
    constant: 0

    # Optional cycle type. Valid values are REPEAT, REVERSE (case sensitive). If REPEAT, each
    # caculation cycle increases the base value starting from minBase. If REVERSE, upon reaching
    # the maxBase value, the caculation cycle is reversed by decrementing the base value.
    # Default: REVERSE
    type: REVERSE

    # Optional base time reset. This value is added to the 'startTime' adjusted base time when the
    # base (x) value reaches 'minBase' or 'maxBase'. The current base time is first reset to the
    # date portion and then this value is added to it as shown in the following example.
    #
    #   resetBaseTime: 86_400_000 (1 day)
    #   startTime: "2022-10-10T09:00:00.000-0400"
    #   base time: "2024-11-11T11:12:34.565-0400"
    #   new base time: "2024-11-11T09:00:00.000-0400" + resetStartTime
    #   new base time: "2024-11-12T09:00:00.000-0400"
    #
    # The default value of 0 does nothing and proceeds with the current base time.
    #
    # By resetting the base time, you can simulate a realistic time capsule on a window of curve
    # catured by minBase and maxBase.
    #
    # Default: 0
    resetBaseTime: 0

publishers:        
    # Product name. Valid values are MQTT|HAZELCAST
    # Required product name.
    # Default: MQTT
  - product: MQTT
    
    # Optional parameter to enable (true) or disable (false) the publisher.
    # Default: true
    enabled: true

    # Required unique publisher name.
    name: null

    # Required equation name. This must be one of the equation names defined in
    # in the equations element.
    equationName: null

    # Data structure. Each product has one or more data structure types as follows.
    #
    # - mqtt
    #     dataStructure:
    #       type: TOPIC
    #       name: <topic_name>
    #
    # - hazelcast
    #     # Hazelcast dataStructure default is TOPIC
    #     dataStructure:
    #       type: MAP|RMAP|QUEUE|TOPIC|RTOPIC
    #       name: <map_name>
    #       keyType: FIXED|SEQUENCE|TIME|UUID
    #       keyPrefix: null
    #       keySquenceStart: 1
    #     dataStructure:
    #       type: RMAP
    #       name: <replicated_map_name>
    #       keyType: FIXED|SEQUENCE|TIME|UUID
    #       keyPrefix: null
    #       keySquenceStart: 1
    #     dataStructure:
    #       type: QUEUE
    #       name: <queue_name>
    #     dataStructure:
    #       type: TOPIC
    #       name: <topic_name>
    #     dataStructure:
    #       type: RTOPIC
    #       name: <reliable_topic_name>
    dataStructure:
      # Required data structure type.
      #   MQTT valid valued: TOPIC)
      #   Hazelcast valid values: MAP|RMAP|QUEUE|TOPIC|RTOPIC
      # Default: TOPIC
      type: TOPIC
      
      # Required data structure name.
      #   MQTT: Topic name
      #   Hazelcast: Name of map, replicated map, queue, topic, or reliable topic
      # Default: null <undefined>
      name: null
      
      # Key type. Applies to Hazelcast data structures only. All keys are string.
      # Valid values are SEQUENCE|TIME|UUID.
      #    FIXED - Key value is a single fixed value. Set keyValue as the key value.
      #            If keyPrefix is not defined then the key value is set to 'key'.
      #    SEQUENCE - Key values are sequenced starting from keySequenceStart.
      #    TIME - Key values are time stamps
      #    UUID - Key values are UUID.
      # Default: SEQUENCE
      keyType: FIXED
      
      # Key value for the FIXED key type.
      # Default: key
      keyValue: key
      
      # Key squence start number. Key sequence is incremented starting from this number.
      # Default: 1
      keySquenceStart: 1
    
    # Initial delay in milliseconds. The publisher waits this amount of time before start
    # publishing data.
    # Default: 0
    initialDelay: 0

    # Time interval in milliseconds. The publisher periodically publishes data at this
    # timeInterval.
    timeInterval: 500
```

## Adding New Equations

You can add your own equations by creating Java static functions. All equation functions take the following form.

```java
public final static double function(x) {
   return <computed_value>;
}
```

For example, the following defines linear and quadratic equations.

```java
package mydatafeed;

publid class MyEquations {
   public final static double linear(double x) {
      return x + 1;
   }

   public final static double quadratic(double x) {
      return Math.pow(x, 2) + x + 1;
   }
}
```

You can place your source code in the `src/java/main/` directory and compile it by running the `build_app` command.

```bash
cd_app simulator/bin_sh
./build_app
```

To use your equations, you define them in the configuration file.

```bash
cd_app simulator
vi etc/simulator-mydatafeed.yaml
```

Enter the following `simulator-mydatafeed.yaml`:

```yaml
equations:
  - name: linear
    formula: y=x+1
    description: linear
    timeInterval: 500
    baseSpread: 0.1
    jitter: 0.1
    calculationFunction: mydatafeed.MyQueations.linear
  - name: quadratic
    formula: y=x^2+x+1
    description: quadratic
    timeInterval: 500
    baseSpread: 0.1
    jitter: 0.1
    calculationFunction: mydatafeed.MyQueations.quadratic

publishers:
  - product: mqtt
    name: linear-publisher
    equationName: linear
    dataStructure:
      type: topic
      name: mydatafeed/linear
    interval: 500
  - product: mqtt
    name: quadratic-publisher
    equationName: quadratic
    dataStructure:
      type: topic
      name: mydatafeed/quadratic
    interval: 500
```

Now, run simulator and chart.

```bash
cd_app simulator/bin_sh
./simulator -simulator-config ../etc/simulator-mydatafeed.yaml
./chart_mqtt -t mydatafeed/linear
./chart_mqtt -t mydatafeed/quadratic
```

## QuestDB

QuestDB is an open source columnar time-series database ideal for storing data generated by IoT devices. This bundle includes the [`QuestDbJsonConnector`](apps/simulator/src/main/java/padogrid/mqtt/connectors/QuestDbJsonConnector.java)  plugin that automatcially inserts the MQTT streamed data into QuestDB. 

1. Run QuestDB container

```bash
docker run -p 9000:9000 -p 9009:9009 -p 8812:8812 questdb/questdb
```

2. Run simulator

```bash
cd_app simulator/bin_sh
./simulator -config ../etc/mqttv5-questdb.yaml -simulator ../etc/simulator-stocks.yaml
```

3. Open QuestDB console

URL: <http://127.0.0.1:9000/>


## Simulator Plugin

This bundle also includes a simulator plugin that can be embedded in virtual clusters. The MQTT configuration file, `etc/mqttv5-simulator.yaml`, defines the plugin as follows.

```bash
cd_app simulator
cat etc/mqttv5-simulator.yaml
```

Output:

```yaml
...
plugins:
  # Data feed plugin APP that generates simulated data
  - name: datafeed
    description: A data feed that publishes simulated numerical data for MQTT and Hazelcast with QuestDB enabled
    enabled: true
    context: APP
    className: padogrid.simulator.DataFeedSimulatorPlugin
...
clusters:
  - name: edge
    enabled: true
    autoConnect: true
    fos: 0
    publisherType: ROUND_ROBIN

    # QuestDB connector. Enable or disable in the 'plugins' element.
    pluginName: questdb
...
```

Take a look at [`DataFeedSimulatorPlugin`](apps/simulator/src/main/java/padogrid/simulator/DataFeedSimulatorPlugin.java). It implements `IMqttPlugin` which extends `Runnable`. The virtual cluster service bootstraps the plugin in a daemon thread during the application startup time. Typically, plugins are highly configurable and resuable such that you can create embedded virtual clusters without needing to code.

You can use the `HaMqttClient` API to embed plugins in your application or simply use the `vc_start` command as follows.

1. Display the simulator plugin usage.

```bash
cd_app simulator
vc_start -config mqtt5-simulator.yaml -?
```

2. Start the simulator plugin.

```bash
vc_start -config etc/mqttv5-simulator.yaml -simulator-config etc/simulator-stocks.yaml
```

✏️  Note that the [`DataFeedSimulator`](apps/simulator/src/main/java/padogrid/simulator/DataFeedSimulator.java) class launched by the `simulator` command also invokes `padogrid.simulator.DataFeedSimulatorPlugin`.

## Teardown

```bash
# Ctrl-C simulator and exit charts

# Stop cluster
stop_cluster
```

## References

1. Graph Plotter, <https://www.transum.org/Maths/Activity/Graph/Desmos.asp>
1. *Mosquitto/MQTT Virtual Cluster Tutorial*, PadoGrid Bundles, <https://github.com/padogrid/bundle-mosquitto-tutorial-virtual-clusters>.
1. *Installing Mosquitto*, PadoGrid Manual, <https://github.com/padogrid/padogrid/wiki/Installing-Building-Mosquitto>
1. *MQTT Addon Library*, PadoGrid, <https://github.com/padogrid/padogrid/blob/develop/mqtt-addon-core/README.md>
1. *Mosquitto Overview*, PadoGrid Manual, <https://github.com/padogrid/padogrid/wiki/Mosquitto-Overview>
1. *Clustering MQTT*, PadoGrid Manual, <https://github.com/padogrid/padogrid/wiki/Clustering-MQTT>
1. *Eclipse Mosquitto*, <https://mosquitto.org/>
1. *Paho*, Eclipse Foundation, <https://www.eclipse.org/paho/>
1. *Fast SQL for time-series*, QuestDB, <https://questdb.io/>

---

![PadoGrid](https://github.com/padogrid/padogrid/raw/develop/images/padogrid-3d-16x16.png) [*PadoGrid*](https://github.com/padogrid) | [*Catalogs*](https://github.com/padogrid/catalog-bundles/blob/master/all-catalog.md) | [*Manual*](https://github.com/padogrid/padogrid/wiki) | [*FAQ*](https://github.com/padogrid/padogrid/wiki/faq) | [*Releases*](https://github.com/padogrid/padogrid/releases) | [*Templates*](https://github.com/padogrid/padogrid/wiki/Using-Bundle-Templates) | [*Pods*](https://github.com/padogrid/padogrid/wiki/Understanding-Padogrid-Pods) | [*Kubernetes*](https://github.com/padogrid/padogrid/wiki/Kubernetes) | [*Docker*](https://github.com/padogrid/padogrid/wiki/Docker) | [*Apps*](https://github.com/padogrid/padogrid/wiki/Apps) | [*Quick Start*](https://github.com/padogrid/padogrid/wiki/Quick-Start)
