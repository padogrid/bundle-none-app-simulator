package padogrid.simulator.eq;

/**
 * Standard equations provided by PadoGrid.
 * 
 * @author dpark
 *
 */
public class Equations {
	/**
	 * Circle with radius 1.
	 * <p>
	 * y=sqrt(1-x*x)
	 */
	public final static double circle(double x) {
		return Math.sqrt(1 - x * x);
	}

	/**
	 * y=x+1
	 */
	public final static double linear(double x) {
		return x + 1;
	}
	
	/**
	 * y=x^2+1
	 */
	public final static double quadratic(double x) {
		return Math.pow(x, 2) + x + 1;
	}

	/**
	 * y=x^3+x^2+x+1
	 */
	public final static double cubic(double x) {
		return Math.pow(x, 3) + Math.pow(x, 2) + x + 1;
	}
	
	/**
	 * y=x^3+x^2+1
	 */
	public final static double cubic2(double x) {
		return Math.pow(x, 3) + Math.pow(x, 2) + 1;
	}
	
	/**
	 * y=|x^3+x^2|
	 */
	public final static double cubic3(double x) {
		return Math.abs(Math.pow(x, 3) + Math.pow(x, 2));
	}

	/**
	 * y=x^4+x^3+x^2+x+1
	 */
	public final static double quartic(double x) {
		return Math.pow(x, 4) + Math.pow(x, 3) + Math.pow(x, 2) + x + 1;
	}
	
	/**
	 * y=x^5+x^4+x^3+x^2+x+1
	 */
	public final static double quintic(double x) {
		return Math.pow(x, 5) + Math.pow(x, 4) + Math.pow(x, 3) + Math.pow(x, 2) + x + 1;
	}
	
	/**
	 * y=x^6+x^5+x^4+x^3+x^2+x+1
	 */
	public final static double sextic(double x) {
		return Math.pow(x, 6) + Math.pow(x, 5) + Math.pow(x, 4) + Math.pow(x, 3) + Math.pow(x, 2) + x + 1;
	}
	
	/**
	 * y=x^7+x^6+x^5+x^4+x^3+x^2+x+1
	 */
	public final static double septic(double x) {
		return Math.pow(x, 7) + Math.pow(x, 6) + Math.pow(x, 5) + Math.pow(x, 4) + Math.pow(x, 3) + Math.pow(x, 2) + x + 1;
	}
	
	/**
	 * y=x^8+x^7+x^6+x^5+x^4+x^3+x^2+x+1
	 */
	public final static double octic(double x) {
		return Math.pow(x, 8) + Math.pow(x, 7) + Math.pow(x, 6) + Math.pow(x, 5) + Math.pow(x, 4) + Math.pow(x, 3) + Math.pow(x, 2) + x + 1;
	}
	
	/**
	 * y=x^9+x^8+x^7+x^6+x^5+x^4+x^3+x^2+x+1
	 */
	public final static double nontic(double x) {
		return Math.pow(x, 9) + Math.pow(x, 8) + Math.pow(x, 7) + Math.pow(x, 6) + Math.pow(x, 5) + Math.pow(x, 4) + Math.pow(x, 3) + Math.pow(x, 2) + x + 1;
	}
	
	/**
	 * y=x^10+x^9+x^8+x^7+x^6+x^5+x^4+x^3+x^2+x+1
	 */
	public final static double decic(double x) {
		return  Math.pow(x, 10) + Math.pow(x, 9) + Math.pow(x, 8) + Math.pow(x, 7) + Math.pow(x, 6) + Math.pow(x, 5) + Math.pow(x, 4) + Math.pow(x, 3) + Math.pow(x, 2) + x + 1;
	}
}
