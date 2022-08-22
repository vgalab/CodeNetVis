package ca.usask.vga.layout.magnetic;

import prefuse.util.force.EulerIntegrator;
import prefuse.util.force.Integrator;
import prefuse.util.force.RungeKuttaIntegrator;
import prefuse.util.force.StateMonitor;

/* Adapted from:
 * #%L
 * Cytoscape Prefuse Layout Impl (layout-prefuse-impl)
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

/**
 * Contains {@link Integrators} enum used for implementations of the layout.
 * Previously part of the original Prefuse layout implementation.
 */
public abstract class ForceDirectedLayout {

	public enum Integrators {
		RUNGEKUTTA("Runge-Kutta"), EULER("Euler");

		private String name;

		Integrators(String str) {
			name = str;
		}

		@Override
		public String toString() {
			return name;
		}

		public Integrator getNewIntegrator(StateMonitor monitor) {
			if (this == EULER)
				return new EulerIntegrator(monitor);
			else
				return new RungeKuttaIntegrator(monitor);
		}
	}
}
