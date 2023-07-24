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
package padogrid.simulator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeSet;

import org.json.JSONObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import padogrid.mqtt.client.cluster.HaClusters;
import padogrid.simulator.config.SimulatorConfig;

/**
 * {@linkplain AbstractChart} creates a trending chart for subclasses. 
 * 
 * @author dpark
 *
 */
public class AbstractChart extends Application implements Constants {
	protected static int WINDOW_SIZE = DEFAULT_WINDOW_SIZE;
	protected static LineChart<String, Number> lineChart;
	protected static HashMap<String, XYChart.Series<String, Number>> seriesMap = new HashMap<String, XYChart.Series<String, Number>>(
			10);

	protected static String[] features;
	protected static String timeFormat = SimulatorConfig.TIME_FORMAT;
	protected static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(timeFormat);

	protected static String stageTitle;
	protected static String chartTitle;

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle(stageTitle);

		// defining the axes
		final CategoryAxis xAxis = new CategoryAxis(); // we are gonna plot against time
		final NumberAxis yAxis = new NumberAxis();
		xAxis.setLabel("Time");
		xAxis.setAnimated(false); // axis animations are removed
		yAxis.setLabel("Value");
		yAxis.setAnimated(false); // axis animations are removed

		// creating the line chart with two axis created above
		lineChart = new LineChart<>(xAxis, yAxis);
		lineChart.setTitle(chartTitle);
		lineChart.setAnimated(false); // disable animations

		// setup scene
		Scene scene = new Scene(lineChart, 1000, 600);
		primaryStage.setScene(scene);

		// show the stage
		primaryStage.show();
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		HaClusters.stop();
	}

	protected static void writeLine() {
		System.out.println();
	}

	protected static void writeLine(String line) {
		System.out.println(line);
	}

	@SuppressWarnings("unused")
	protected static void write(String str) {
		System.out.print(str);
	}

	/**
	 * Updates the chart with the specified value.
	 * 
	 * @param evalue
	 */
	protected static void updateChart(JSONObject json) {
		String time = json.getString("time");
		Platform.runLater(() -> {
			try {
				Date date = simpleDateFormat.parse(time);
				String dateStr = simpleDateFormat.format(date);
				if (features == null) {
					// Sort keys
					TreeSet<String> treeSet = new TreeSet<String>(json.keySet());
					treeSet.forEach((feature) -> {
						updateSeries(json, feature, dateStr);
					});
				} else {
					for (String feature : features) {
						updateSeries(json, feature, dateStr);
					}
				}
			} catch (ParseException e) {
				System.err.printf("Invalid time format[time=%s, timeFormat=%s]%n", time, simpleDateFormat.toPattern());
			}
		});
	}

	/**
	 * Update the line series of the specified feature. It creates a new line series
	 * if it does not exist.
	 * 
	 * @param json    JSON object
	 * @param feature Feature name
	 * @param dateStr String representation of time
	 */
	protected static void updateSeries(JSONObject json, String feature, String dateStr) {
		Object val = json.get(feature);
		if (val instanceof Number) {
			Number value = (Number) json.get(feature);
			XYChart.Series<String, Number> series = seriesMap.get(feature);
			if (series == null) {
				series = new XYChart.Series<String, Number>();
				series.setName(feature);
				lineChart.getData().add(series);
				seriesMap.put(feature, series);
			}
			series.getData().add(new XYChart.Data<>(dateStr, value));
			if (series.getData().size() > WINDOW_SIZE) {
				series.getData().remove(0);
			}
		}
	}
}
