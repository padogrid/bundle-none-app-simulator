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
	 * Decay function
	 * <p>
	 * y=e^(-x/5)
	 * 
	 * @param x
	 * @return
	 */
	public final static double decay(double x) {
		return Math.exp(-x / 5);
	}
	
	/**
	 * Exponential decay function
	 * <p>
	 * y=e^(-x)
	 * 
	 * @param x
	 */
	public final static double expDecay(double x) {
		return Math.exp(-x);
	}

	/**
	 * y=2e^(-2x)sin(2*pi*x/.5) <br>
	 * y=2e^{-2x}\cdot\sin\left(2\cdot3.14\cdot\frac{x}{.5}\right) <br>
	 * Y= Amplitude*exp(-K*X)*sin((2*pi*X/Wavelength)+PhaseShift
	 */
	public final static double dampedSineWave(double x) {
		return 2 * Math.exp(-2 * x) * Math.sin(2 * Math.PI * x / 0.5);
	}

	/**
	 * y=sin(x)^63*sin(x+1.5)*8 <br>
	 * Graph Plotter: \sin(x)^{63}*\sin(x+1.5)*8
	 */
	public final static double heartbeat(double x) {
		return Math.pow(Math.sin(x), 63) * Math.sin(x + 1.5) * 8;
	}

	/**
	 * y=x+1
	 */
	public final static double linear(double x) {
		return x + 1;
	}

	/**
	 * y=x^2+x+1
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
		return Math.pow(x, 7) + Math.pow(x, 6) + Math.pow(x, 5) + Math.pow(x, 4) + Math.pow(x, 3) + Math.pow(x, 2) + x
				+ 1;
	}

	/**
	 * y=x^8+x^7+x^6+x^5+x^4+x^3+x^2+x+1
	 */
	public final static double octic(double x) {
		return Math.pow(x, 8) + Math.pow(x, 7) + Math.pow(x, 6) + Math.pow(x, 5) + Math.pow(x, 4) + Math.pow(x, 3)
				+ Math.pow(x, 2) + x + 1;
	}

	/**
	 * y=x^9+x^8+x^7+x^6+x^5+x^4+x^3+x^2+x+1
	 */
	public final static double nontic(double x) {
		return Math.pow(x, 9) + Math.pow(x, 8) + Math.pow(x, 7) + Math.pow(x, 6) + Math.pow(x, 5) + Math.pow(x, 4)
				+ Math.pow(x, 3) + Math.pow(x, 2) + x + 1;
	}

	/**
	 * y=x^10+x^9+x^8+x^7+x^6+x^5+x^4+x^3+x^2+x+1
	 */
	public final static double decic(double x) {
		return Math.pow(x, 10) + Math.pow(x, 9) + Math.pow(x, 8) + Math.pow(x, 7) + Math.pow(x, 6) + Math.pow(x, 5)
				+ Math.pow(x, 4) + Math.pow(x, 3) + Math.pow(x, 2) + x + 1;
	}
}
