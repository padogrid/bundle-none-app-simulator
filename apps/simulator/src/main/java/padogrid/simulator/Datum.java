package padogrid.simulator;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Datum {
	private long timestamp = System.currentTimeMillis();
	private double value;
	private double baseValue;
	private boolean isUpTick = true;

	public Datum() {
	}

	/**
	 * Initializes a new Datum object with the specified equation.
	 * @param equation Equation
	 */
	public Datum(Equation equation) {
		baseValue = equation.getMinBase();
		SimpleDateFormat dateFormatter = new SimpleDateFormat(equation.getTimeFormat());
		if (equation.getStartTime() != null) {
			try {
				timestamp = dateFormatter.parse(equation.getStartTime()).getTime();
			} catch (ParseException e) {
				// ignore
			}
		}
		
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
		this.timestamp = timestamp;
		this.value = value;
		this.baseValue = baseValue;
		this.isUpTick = isUpTick;
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
