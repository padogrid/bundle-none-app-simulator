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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Random;

import padogrid.mqtt.client.cluster.internal.ConfigUtil;

public class Equation {
	private String name;
	private String formula;
	private String description;
	private String timeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	private long st = System.currentTimeMillis();
	private String startTime;
	private int timeInterval = 500;
	private double minBase = -1;
	private double maxBase = 1;
	private double baseSpread = 0.1;
	private double jitter = 0.1;
	private double multiplier = 1.0;
	private double constant = 0;
	private double baseAverage;
	private String calculationFunction;
	private String calculationClass;
	private EquationType type = EquationType.REVERSE;
	private long resetBaseTime = 0;

	private Method calculationMethod;

	private ICalculation calculation;

	private Random random = new Random();

	public Equation() {
		init();
	}

	public Equation(long startTime, int timeDelta, double baseSpread) {
		this(startTime, timeDelta, baseSpread, 0.1);
	}

	public Equation(long startTime, int timeInterval, double baseSpread, double jitter) {
		this.st = startTime;
		this.timeInterval = timeInterval;
		this.baseSpread = baseSpread;
		this.jitter = jitter;
		init();
	}

	private void init() {
		baseAverage = (maxBase - minBase) / 2;
	}

	public String getName() {
		return ConfigUtil.parseStringValue(name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFormula() {
		return ConfigUtil.parseStringValue(formula);
	}

	public void setFormula(String formula) {
		this.formula = formula;
	}

	public String getDescription() {
		return ConfigUtil.parseStringValue(description);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTimeFormat() {
		if (timeFormat == null || timeFormat.trim().length() == 0) {
			timeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
		} else {
			ConfigUtil.parseStringValue(timeFormat);
		}
		return timeFormat;
	}

	public void setTimeFormat(String timeFormat) {
		this.timeFormat = timeFormat;
	}

	public String getStartTime() {
		return ConfigUtil.parseStringValue(startTime);
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public int getTimeInterval() {
		return timeInterval;
	}

	public void setTimeInterval(int timeInterval) {
		this.timeInterval = timeInterval;
	}

	public double getBaseSpread() {
		return baseSpread;
	}

	public void setBaseSpread(double baseSpread) {
		this.baseSpread = baseSpread;
	}

	public double getJitter() {
		return jitter;
	}

	public void setJitter(double jitter) {
		this.jitter = jitter;
	}

	public double getBaseAverage() {
		return baseAverage;
	}

	public void setBaseAverage(int baseAverage) {
		this.baseAverage = baseAverage;
	}

	public String getCalculationFunction() {
		return ConfigUtil.parseStringValue(calculationFunction);
	}

	public void setCalculationFunction(String calculationFunction) {
		this.calculationFunction = calculationFunction;
	}

	public double getMinBase() {
		return minBase;
	}

	public double getMaxBase() {
		return maxBase;
	}

	public void setCalculationMethod(Method calculationMethod) {
		this.calculationMethod = calculationMethod;
	}

	public void setMinBase(int minBase) {
		this.minBase = minBase;
		init();
	}

	public void setMaxBase(int maxBase) {
		this.maxBase = maxBase;
		init();
	}

	public double getMultiplier() {
		return multiplier;
	}

	public void setMultiplier(double multiplier) {
		this.multiplier = multiplier;
	}

	public String getCalculationClass() {
		return ConfigUtil.parseStringValue(calculationClass);
	}

	public void setCalculationClass(String calculationClass) {
		this.calculationClass = calculationClass;
	}

	public void setMinBase(double minBase) {
		this.minBase = minBase;
	}

	public void setMaxBase(double maxBase) {
		this.maxBase = maxBase;
	}

	public double getConstant() {
		return constant;
	}

	public void setConstant(double constant) {
		this.constant = constant;
	}

	public EquationType getType() {
		return type;
	}

	public void setType(EquationType type) {
		this.type = type;
	}

	public long getResetBaseTime() {
		return resetBaseTime;
	}

	public void setResetBaseTime(long resetBaseTime) {
		this.resetBaseTime = resetBaseTime;
	}

	public ICalculation getCalculation() {
		if (calculation == null) {
			if (calculationClass != null) {
				int index = calculationClass.lastIndexOf('.');
				String className = calculationClass.substring(0, index);
				Class<?> clazz = null;
				try {
					clazz = Class.forName(className);
				} catch (ClassNotFoundException e) {
					System.err.printf(
							"ERROR: Invalid class name. [calculationClass=%s, error=%s] Equation discarded.%n",
							calculationClass, e.getMessage());
				}
				if (clazz != null) {
					try {
						calculation = (ICalculation) clazz.newInstance();
					} catch (Exception e) {
						System.err.printf("ERROR: Invalid class. [calculationClass=%s, error=%s] Equation discarded.%n",
								calculationClass, e.getMessage());
					}
				}
			}
		}
		return calculation;
	}

	public void setCalculation(ICalculation calculationImpl) {
		this.calculation = calculationImpl;
	}

	public Method getCalculationMethod() {
		if (calculationMethod == null) {
			if (calculationFunction != null) {
				int index = calculationFunction.lastIndexOf('.');
				String className = calculationFunction.substring(0, index);
				String methodName = calculationFunction.substring(index + 1);
				Class<?> clazz = null;
				try {
					clazz = Class.forName(className);
				} catch (ClassNotFoundException e) {
					System.err.printf("ERROR: Invalid class name. [functionNmae=%s, error=%s] Equation discarded.%n",
							calculationFunction, e.getMessage());
				}

				if (clazz != null) {
					try {
						calculationMethod = clazz.getMethod(methodName, double.class);
					} catch (NoSuchMethodException | SecurityException e) {
						try {
							calculationMethod = clazz.getMethod(methodName, Double.class);
						} catch (NoSuchMethodException | SecurityException e1) {
							System.err.printf(
									"ERROR: Invalid method name. [functionName=%s, error=%s] Equation discarded.%n",
									calculationFunction, e.getMessage());

						}
					}
				}
			}
		}
		return calculationMethod;
	}

	public Datum updateDatum(Datum previousDatum) {
		double baseValue;
		long timestamp;
		boolean isUpTick;
		boolean isResetBaseTime = false;
		if (previousDatum == null) {
			if (maxBase > minBase) {
				baseValue = minBase;
				timestamp = st;
				isUpTick = true;
			} else {
				baseValue = maxBase;
				timestamp = st;
				isUpTick = true;
			}
		} else {
			baseValue = previousDatum.getBaseValue();
			switch (type) {
			case REPEAT:
				// positive uptick
				if (baseValue >= maxBase) {
					baseValue = minBase;
					isResetBaseTime = resetBaseTime != 0 ? true : false;
				}
				isUpTick = true;
				break;
				
			case REVERSE:
			default:
				if (baseValue >= maxBase) {
					isUpTick = false;
					isResetBaseTime = resetBaseTime != 0 ? true : false;
				} else if (baseValue <= minBase) {
					isUpTick = true;
					isResetBaseTime = resetBaseTime != 0 ? true : false;
				} else {
					isUpTick = previousDatum.isUpTick();
				}
				break;
			}
			if (isUpTick) {
				baseValue += baseSpread;
			} else {
				baseValue -= baseSpread;
			}
			timestamp = previousDatum.getTimestamp();
		}
		double value = 0;
		if (calculationMethod != null) {
			try {
				value = (double) calculationMethod.invoke(null, baseValue) + constant;
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// TODO: Throw for now
				e.printStackTrace();
			}
		} else if (calculation != null) {
			value = calculation.calculate(baseValue) + constant;
		}
		if (value != value) {
			value = 0;
		}

		value += jitter * random.nextDouble();
		value *= baseAverage * multiplier;

		if (isResetBaseTime) {
			previousDatum = resetBaseTime(previousDatum);
		} else {
			timestamp += timeInterval;
			previousDatum.setTimestamp(timestamp);
		}
		previousDatum.setValue(value);
		previousDatum.setBaseValue(baseValue);
		previousDatum.setUpTick(isUpTick);

		return previousDatum;
	}

	/**
	 * Resets the base time. There is no effect if resetBaseTime = 0.
	 * 
	 * Example:
	 * <ul>
	 * <li>resetBaseTime: 86_400_000 (1 day)</li>
	 * <li>startTime: "2022-10-10T09:00:00.000-0400"</li>
	 * <li>base time: "2024-11-11T11:12:34.565-0400"</li>
	 * <li>new base time: "2024-11-11T09:00:00.000-0400" + resetStartTime</li>
	 * <li>new base time: "2024-11-12T09:00:00.000-0400"</li>
	 * </ul>
	 * 
	 * @param previousDatum Previous datum
	 */
	private Datum resetBaseTime(Datum previousDatum) {
		if (resetBaseTime == 0) {
			return previousDatum;
		}
		// Get time portion of startTime
		long startTime = previousDatum.getStartTimestamp();
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(startTime);
		int hour = calendar.get(Calendar.HOUR); // 12 hour clock
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);
		int millisecond = calendar.get(Calendar.MILLISECOND);

		// Set the base time with the time portion of startTime
		long baseTime = previousDatum.getTimestamp();
		calendar.setTimeInMillis(baseTime);
		calendar.set(Calendar.HOUR, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, second);
		calendar.set(Calendar.MILLISECOND, millisecond);

		// Add resetBaseTime
		long newBaseTime = calendar.getTimeInMillis() + resetBaseTime;
		previousDatum.setTimestamp(newBaseTime);
		return previousDatum;
	}

	@Override
	public String toString() {
		return "Equation [name=" + name + ", formula=" + formula + ", description=" + description + ", timeFormat="
				+ timeFormat + ", startTime=" + startTime + ", timeInterval=" + timeInterval + ", minBase=" + minBase
				+ ", maxBase=" + maxBase + ", baseSpread=" + baseSpread + ", jitter=" + jitter + ", baseAverage="
				+ baseAverage + ", resetBaseTime=" + resetBaseTime + ", calculationFunction=" + calculationFunction
				+ ", calculationMethod=" + calculationMethod + ", calculation=" + calculation + ", getCalculation()="
				+ getCalculation() + ", getCalculationMethod()=" + getCalculationMethod() + ", getClass()=" + getClass()
				+ ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + "]";
	}

	public static enum EquationType {
		REPEAT, REVERSE
	}

}
