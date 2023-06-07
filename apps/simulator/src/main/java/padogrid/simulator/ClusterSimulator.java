package padogrid.simulator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;

import padogrid.mqtt.client.cluster.ClusterService;
import padogrid.mqtt.client.cluster.HaClusters;
import padogrid.mqtt.client.cluster.HaMqttClient;
import padogrid.mqtt.client.cluster.config.ClusterConfig;
import padogrid.simulator.config.SimulatorConfig;
import padogrid.simulator.config.SimulatorConfig.DataStructure;
import padogrid.simulator.config.SimulatorConfig.Publisher;

public class ClusterSimulator implements Constants {

	private SimulatorConfig simulatorConfig;
	private HashMap<String, Equation> equationMap = new HashMap<String, Equation>(10);
	private HaMqttClient haclient;

	private String product;
	private String clusterName;
	private String configFilePath;
	private boolean isQuiet;

	public ClusterSimulator(String product, String clusterName, String configFilePath, boolean isQuiet) throws FileNotFoundException {
		this.product = product;
		this.clusterName = clusterName;
		this.configFilePath = configFilePath;
		this.isQuiet = isQuiet;
		init();
	}

	private void init() throws FileNotFoundException {

		if (configFilePath == null) {
			configFilePath = System.getProperty(ISimulatorConfig.PROPERTY_SIMULATOR_CONFIG_FILE);
		}
		if (configFilePath != null && configFilePath.length() > 0) {
			File file = new File(configFilePath);
			Yaml yaml = new Yaml(new Constructor(SimulatorConfig.class));
			yaml.setBeanAccess(BeanAccess.FIELD);
			FileReader reader = new FileReader(file);
			simulatorConfig = yaml.load(reader);
		} else {
			InputStream inputStream = ClusterService.class.getClassLoader()
					.getResourceAsStream(ISimulatorConfig.DEFAULT_SIMULATOR_CONFIG_FILE);
			if (inputStream != null) {
				Yaml yaml = new Yaml(new Constructor(ClusterConfig.class));
				yaml.setBeanAccess(BeanAccess.FIELD);
				simulatorConfig = yaml.load(inputStream);
			}
		}

		// Initialize equations
		Equation[] equations = simulatorConfig.getEquations();
		for (Equation equation : equations) {
			Method method = equation.getCalculationMethod();
			if (method == null) {
				System.out.printf("WARNING: Calculation function undefined. %s. Equation discarded.%n", equation);
			} else {
				equationMap.put(equation.getName(), equation);
			}
		}

		// Initialize HaMqttClient
		if (product == null || product.equals("mqtt")) {
			try {
				if (clusterName == null) {
					haclient = HaClusters.getHaMqttClient();
				} else {
					haclient = HaClusters.getOrCreateHaMqttClient(clusterName);
				}

				if (haclient == null) {
					System.err.printf("ERROR: Specified cluster not found [%s]. Command aborted.%n", clusterName);
					System.exit(-10);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sets the specified environment variable.
	 * 
	 * @param envvar Environment variable
	 * @param value  Environment variable value
	 */
	@SuppressWarnings("unchecked")
	public static void setEnv(String envvar, String value) {
		try {
			Map<String, String> env = System.getenv();
			Class<?> cl = env.getClass();
			Field field = cl.getDeclaredField("m");
			field.setAccessible(true);
			Map<String, String> writableEnv = (Map<String, String>) field.get(env);
			writableEnv.put(envvar, value);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to set environment variable", e);
		}
	}

	public void start() {
		// Initialize publishers
		Publisher[] publishers = simulatorConfig.getPublishers();
		if (publishers == null) {
			System.err.printf("ERROR: Publishers undefined in the configuration file. Command aborted.%n");
			System.exit(-3);
		}
		int count;
		if (product != null) {
			count = 0;
			for (Publisher publisher : publishers) {
				if (product.equalsIgnoreCase(publisher.getProduct())) {
					count++;
				}
			}
		} else {
			count = publishers.length;
		}

		// Launch publisher threads
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(count);
		for (Publisher publisher : publishers) {
			if (product == null || (product != null && product.equalsIgnoreCase(publisher.getProduct()))) {
				ses.scheduleAtFixedRate(new Runnable() {
					
					Equation equation = getEquation(publisher.getEquationName());
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
							equation.getTimeFormat() == null ? "yyyy-MM-dd'T'HH:mm:ss.SSSZ" : equation.getTimeFormat());
					Datum datum = new Datum(equation);

					@Override
					public void run() {
						DataStructure ds = publisher.getDataStructure();
						String topic = ds.getName();
						datum = equation.updateDatum(datum);
						JSONObject json = new JSONObject();
						json.put("time", simpleDateFormat.format(datum.getTimestamp()));
						json.put("value", datum.getValue());
						try {
							haclient.publish(topic, json.toString().getBytes(), 0, false);
						} catch (MqttException e) {
							// ignore
						}
						if (isQuiet == false) {
							System.out.printf("product=%s, topic=%s: %s%n", publisher.getProduct(), topic, json);
						}
					}

				}, publisher.getInitialDelay(), publisher.getInterval(), TimeUnit.MILLISECONDS);
			}
		}
	}

	private Equation getEquation(String equationName) {
		return equationMap.get(equationName);
	}

	private static void writeLine() {
		System.out.println();
	}

	private static void writeLine(String line) {
		System.out.println(line);
	}

	@SuppressWarnings("unused")
	private static void write(String str) {
		System.out.print(str);
	}

	private static void usage() {
		String executable = System.getProperty(PROPERTY_executableName, MqttChart.class.getName());
		writeLine();
		writeLine("NAME");
		writeLine("   " + executable + " - Publish simulated data generated by equations");
		writeLine();
		writeLine("SNOPSIS");
		writeLine("   " + executable
				+ " [-product mqtt|hazelcast] [-cluster cluster_name] [-config config_file] [-quiet] [-?]");
		writeLine();
		writeLine("DESCRIPTION");
		writeLine("   Publishes simulated data generated by the equations defined in the following configuration");
		writeLine("   file.");
		writeLine();
		writeLine("   etc/padogrid-simulator.yaml");
		writeLine();
		writeLine("   You can supply additional equations by creating static methods with the follwing");
		writeLine("   signature.");
		writeLine();
		writeLine("   public final static double your_equation(double x);");
		writeLine();
		writeLine("   The static method takes the input x, and returns y of your equation. For example,");
		writeLine("   the following method defines y = x^2.");
		writeLine();
		writeLine("   public final static double xsquared(double x) {");
		writeLine("      return x*x;");
		writeLine("   }");
		writeLine();
		writeLine("   Alternatively, you can also supply classes that implement the following interaface.");
		writeLine();
		writeLine("      padgrid.simulator.ICalculation.");
		writeLine();
		writeLine("   See etc/template-padogrid-simulator.yaml for details.");
		writeLine();
		writeLine("OPTIONS");
		writeLine("   -product mqtt|hazelcast");
		writeLine("             Publishes data to the specified product. If this option is unspecified,");
		writeLine("             then by default, publishes to all of the products defined in the");
		writeLine("             configuration file.");
		writeLine();
		writeLine("   -cluster cluster_name");
		writeLine("             Connects to the specified cluster defined in the MQTT configuration file.");
		writeLine();
		writeLine("   -config config_file");
		writeLine("             Optional configuration file. If option is unspecified, then the default");
		writeLine("             configuration file (etc/padogrid-simulator.yaml) is used.");
		writeLine();
		writeLine("   -quiet");
		writeLine("             If specified, then simulated data is not printed.");
		writeLine();
		writeLine("SEE ALSO");
		writeLine("   chart(1)");
		writeLine("   etc/padogrid-simulator.yaml");
		writeLine("   etc/stocks.yaml");
		writeLine("   etc/template-padogrid-simulator.yaml");
		writeLine();
	}

	public static void main(String[] args) throws InterruptedException {

		String product = null;
		String clusterName = null;
		String configFilePath = null;
		boolean isQuiet = false;

		String arg;
		for (int i = 0; i < args.length; i++) {
			arg = args[i];
			if (arg.equalsIgnoreCase("-?")) {
				usage();
				System.exit(0);
			} else if (arg.equals("-product")) {
				if (i < args.length - 1) {
					product = args[++i].trim();
				}
			} else if (arg.equals("-cluster")) {
				if (i < args.length - 1) {
					clusterName = args[++i].trim();
				}
			} else if (arg.equals("-config")) {
				if (i < args.length - 1) {
					configFilePath = args[++i].trim();
				}
			} else if (arg.equals("-quiet")) {
				isQuiet = true;
			}
		}

		if (product != null) {
			if (!product.equalsIgnoreCase("mqtt") && !product.equalsIgnoreCase("hazelcast")) {
				System.err.printf("ERROR: Unsupported product [%s]. Command aborted.%n", product);
				System.exit(-1);
			}
			product = product.toLowerCase();
		}
		if (configFilePath != null) {
			File file = new File(configFilePath);
			if (file.exists() == false) {
				System.err.printf("ERROR: Specified configuration file does not exist [%s]. Command aborted.%n",
						configFilePath);
				System.exit(-2);
			}
		}

		try {
			ClusterSimulator simulator = new ClusterSimulator(product, clusterName, configFilePath, isQuiet);
			simulator.start();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
