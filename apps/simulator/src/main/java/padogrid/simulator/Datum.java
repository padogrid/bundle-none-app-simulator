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
package padogrid.simulator;

import java.lang.reflect.InvocationTargetException;

public class Datum {
	private long startTimestamp = System.currentTimeMillis();
	private long timestamp = startTimestamp;
	private double value;
	private double baseValue;
	private boolean isUpTick = true;

	public Datum() {
	}

	/**
	 * Initializes a new Datum object with the specified equation.
	 * 
	 * @param equation Equation
	 */
	public Datum(Equation equation) {
		baseValue = equation.getMinBase();
		if (equation.getCalculation() != null) {
			value = (double) equation.getCalculation().calculate(baseValue);
		} else if (equation.getCalculationMethod() != null) {
			try {
				value = (double) equation.getCalculationMethod().invoke(equation, baseValue);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// ignore
			}
		}
	}

	public Datum(long timestamp, double value, double baseValue, boolean isUpTick) {
		this.startTimestamp = timestamp;
		this.timestamp = timestamp;
		this.value = value;
		this.baseValue = baseValue;
		this.isUpTick = isUpTick;
	}

	public long getStartTimestamp() {
		return startTimestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public double getBaseValue() {
		return baseValue;
	}

	public void setBaseValue(double baseValue) {
		this.baseValue = baseValue;
	}

	public boolean isUpTick() {
		return isUpTick;
	}

	public void setUpTick(boolean isUpTick) {
		this.isUpTick = isUpTick;
	}

	@Override
	public String toString() {
		return "Datum [timestamp=" + timestamp + ", value=" + value + ", baseValue=" + baseValue + "]";
	}

}
