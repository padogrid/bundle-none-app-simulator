package padogrid.simulator.config;

import padogrid.simulator.Equation;

public class SimulatorConfig {
	private Equation[] equations;
	private Publisher[] publishers;

	public Equation[] getEquations() {
		return equations;
	}

	public void setEquations(Equation[] equations) {
		this.equations = equations;
	}

	public Publisher[] getPublishers() {
		return publishers;
	}

	public void setPublishers(Publisher[] publishers) {
		this.publishers = publishers;
	}

	public static class Publisher {
		private String product;
		private String name;
		private String equationName;
		private DataStructure dataStructure;
		private int initialDelay = 0;
		private int interval = 500;
		private long count = Long.MAX_VALUE;

		public String getProduct() {
			return product;
		}

		public void setProduct(String product) {
			this.product = product;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getEquationName() {
			return equationName;
		}

		public void setEquation(String equationName) {
			this.equationName = equationName;
		}

		public int getInitialDelay() {
			return initialDelay;
		}

		public void setInitialDelay(int initialDelay) {
			this.initialDelay = initialDelay;
		}

		public int getInterval() {
			return interval;
		}

		public void setInterval(int interval) {
			this.interval = interval;
		}

		public long getCount() {
			return count;
		}

		public void setCount(long count) {
			this.count = count;
		}

		public DataStructure getDataStructure() {
			return dataStructure;
		}

		public void setDataStructure(DataStructure dataStructure) {
			this.dataStructure = dataStructure;
		}

		public void setEquationName(String equationName) {
			this.equationName = equationName;
		}

	}
	
	public static class DataStructure {
		private String type;
		private String name;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}

	}
}
