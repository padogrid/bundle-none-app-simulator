package padogrid.simulator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

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
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFormula() {
		return formula;
	}

	public void setFormula(String formula) {
		this.formula = formula;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTimeFormat() {
		if (timeFormat == null || timeFormat.trim().length() == 0) {
			timeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
		}
		return timeFormat;
	}

	public void setTimeFormat(String timeFormat) {
		this.timeFormat = timeFormat;
	}

	public String getStartTime() {
		return startTime;
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
		return calculationFunction;
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
		return calculationClass;
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

	public Datum createDatum(Datum previousDatum) {
		double baseValue;
		long timestamp;
		boolean isUpTick;
		if (previousDatum == null) {
			baseValue = minBase;
			timestamp = st;
			isUpTick = true;
		} else {
			baseValue = previousDatum.getBaseValue();
			switch (type) {
			case REPEAT:
				if (baseValue >= maxBase) {
					baseValue = minBase;
				}
				isUpTick = true;
				break;
			case REVERSE:
			default:
				if (baseValue >= maxBase) {
					isUpTick = false;
				} else if (baseValue <= minBase) {
					isUpTick = true;
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

		double value = calculation.calculate(baseValue) + constant;

		value += jitter * random.nextDouble();
		value *= baseAverage * multiplier;

		timestamp += timeInterval;

		return new Datum(timestamp, value, baseValue, isUpTick);
	}

	public Datum updateDatum(Datum previousDatum) {
		double baseValue;
		long timestamp;
		boolean isUpTick;
		if (previousDatum == null) {
			baseValue = minBase;
			timestamp = st;
			isUpTick = true;
		} else {
			baseValue = previousDatum.getBaseValue();
			switch (type) {
			case REPEAT:
				if (baseValue >= maxBase) {
					baseValue = minBase;
				}
				isUpTick = true;
				break;
			case REVERSE:
			default:
				if (baseValue >= maxBase) {
					isUpTick = false;
				} else if (baseValue <= minBase) {
					isUpTick = true;
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
				// TODO Auto-generated catch block
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

		timestamp += timeInterval;
		previousDatum.setTimestamp(timestamp);
		previousDatum.setValue(value);
		previousDatum.setBaseValue(baseValue);
		previousDatum.setUpTick(isUpTick);

		return previousDatum;
	}

	@Override
	public String toString() {
		return "Equation [name=" + name + ", formula=" + formula + ", description=" + description + ", timeFormat="
				+ timeFormat + ", startTime=" + startTime + ", timeInterval=" + timeInterval + ", minBase=" + minBase
				+ ", maxBase=" + maxBase + ", baseSpread=" + baseSpread + ", jitter=" + jitter + ", baseAverage="
				+ baseAverage + ", calculationFunction=" + calculationFunction + ", calculationMethod="
				+ calculationMethod + ", calculation=" + calculation + ", getCalculation()=" + getCalculation()
				+ ", getCalculationMethod()=" + getCalculationMethod() + ", getClass()=" + getClass() + ", hashCode()="
				+ hashCode() + ", toString()=" + super.toString() + "]";
	}

	public static enum EquationType {
		REPEAT, REVERSE
	}

}
