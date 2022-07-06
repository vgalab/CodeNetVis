package ca.usask.vga.layout.magnetic;

/* Adapted from:
 * %%
 * Copyright (C) 2006 - 2021 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */


import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableValidator;


import java.io.IOException;

public class ForceDirectedLayoutContext implements TunableValidator {
	
	// @ContainsTunables
	// public EdgeWeighter edgeWeighter = new EdgeWeighter();

	@Tunable(description="Spring Coefficient:", gravity=200.3, groups="Prefuse layout", context="both", longDescription="Default Spring Coefficient, in numeric value", exampleStringValue="1e-4")
	public double defaultSpringCoefficient = 1e-4;

	@Tunable(description="Spring Length:", gravity=200.2, groups="Prefuse layout", context="both", longDescription="Default Spring Length, in numeric value", exampleStringValue="50.0")
	public double defaultSpringLength = 50.0;

	//@Tunable(description="Node Mass:", context="both", longDescription="Default Node Mass, in numeric value", exampleStringValue="3.0")
	public double defaultNodeMass = 3.0;

	@Tunable(description="Repulsion Coefficient:", gravity=200.1, groups="Prefuse layout", context="both", longDescription="Repulsion coefficient, in numeric value", exampleStringValue="1.0")
	public double repulsionCoefficient = 1.0;

	//@Tunable(description="Force deterministic layouts (slower):", context="both", longDescription="Force deterministic layouts (slower); boolean values only, ```true``` or ```false```; defaults to ```false```", exampleStringValue="false")
	public boolean isDeterministic;


	// @Tunable(description="Don't partition graph before layout:", gravity=800.2, /*(groups="Standard Settings",*/ context="both", longDescription="Don't partition graph before layout; boolean values only, ```true``` or ```false```; defaults to ```false```", exampleStringValue="false")
	public boolean singlePartition;

	@Tunable(description="Number of Iterations:", gravity=800.9, context="both", longDescription="Number of Iterations, in numeric value", exampleStringValue="100")
	public int numIterations = 100;

	@Override
	public ValidationState getValidationState(final Appendable errMsg) {
		try {
		if (!isPositive(numIterations))
			errMsg.append("Number of iterations must be > 0; current value = "+numIterations);
		if (!isPositive(defaultSpringCoefficient))
			errMsg.append("Spring coefficient must be > 0; current value = "+defaultSpringCoefficient);
		if (!isPositive(defaultSpringLength))
			errMsg.append("Spring length must be > 0; current value = "+defaultSpringLength);
		if (!isPositive(defaultNodeMass))
			errMsg.append("Node mass must be > 0; current value = "+defaultNodeMass);
		} catch (IOException e) {}
		return isPositive(numIterations) && isPositive(defaultSpringCoefficient)
		       && isPositive(defaultSpringLength) && isPositive(defaultNodeMass)
			? ValidationState.OK : ValidationState.INVALID;
	}

	private static boolean isPositive(final int n) {
		return n > 0;
	}

	private static boolean isPositive(final double n) {
		return n > 0.0;
	}
}
