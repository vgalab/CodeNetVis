package ca.usask.vga.layout.magnetic;

/*
 * #%L
 * Cytoscape Prefuse Layout Impl (layout-prefuse-impl)
 * $Id:$
 * $HeadURL:$
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

import ca.usask.vga.layout.magnetic.highlight.ChangeHopDistanceAction;
import ca.usask.vga.layout.magnetic.highlight.CopyHighlightedAction;
import ca.usask.vga.layout.magnetic.highlight.EdgeHighlighting;
import ca.usask.vga.layout.magnetic.highlight.ToggleHighlightAction;
import ca.usask.vga.layout.magnetic.poles.AddNorthPoleAction;
import ca.usask.vga.layout.magnetic.poles.AddSouthPoleAction;
import ca.usask.vga.layout.magnetic.poles.PoleManager;
import ca.usask.vga.layout.magnetic.poles.RemovePoleAction;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.undo.UndoSupport;
import org.osgi.framework.BundleContext;

import java.util.Properties;

import static org.cytoscape.work.ServiceProperties.*;

public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {
		
		UndoSupport undo = getService(bc,UndoSupport.class);

		// Editor Edge Highlighting
		final EdgeHighlighting edgeHighlighting = new EdgeHighlighting(new EdgeHighlighting.CyAccess(
				getService(bc, CyNetworkFactory.class),
				getService(bc, CyNetworkManager.class),
				getService(bc, CyNetworkViewFactory.class),
				getService(bc, CyNetworkViewManager.class),
				getService(bc, CyNetworkNaming.class),
				getService(bc, VisualMappingManager.class),
				getService(bc, CyRootNetworkManager.class)
		));
		registerService(bc, edgeHighlighting, SelectedNodesAndEdgesListener.class);

		registerService(bc, new ToggleHighlightAction(edgeHighlighting), CyAction.class, new Properties());
		registerService(bc, new CopyHighlightedAction(edgeHighlighting), CyAction.class, new Properties());
		registerService(bc, new ChangeHopDistanceAction(edgeHighlighting), CyAction.class, new Properties());

		// Magnetic Layout
		ForceDirectedLayout forceDirectedLayout = new ForceDirectedLayout(undo);

        Properties forceDirectedLayoutProps = new Properties();
        forceDirectedLayoutProps.setProperty(PREFERRED_MENU,"Layout.Magnetic Layouts");
        forceDirectedLayoutProps.setProperty("preferredTaskManager","menu");  // Purpose: unknown
        forceDirectedLayoutProps.setProperty(TITLE,forceDirectedLayout.toString());
        forceDirectedLayoutProps.setProperty(MENU_GRAVITY,"10.5");
		registerService(bc,forceDirectedLayout,CyLayoutAlgorithm.class, forceDirectedLayoutProps);

		// Magnetic Poles
		PoleManager poleManager = new PoleManager();
		registerService(bc, poleManager, PoleManager.class);

		AddNorthPoleAction addNPole = new AddNorthPoleAction(poleManager);
		registerService(bc, addNPole, CyAction.class);
		registerService(bc, addNPole, SelectedNodesAndEdgesListener.class);

		AddSouthPoleAction addSPole = new AddSouthPoleAction(poleManager);
		registerService(bc, addSPole, CyAction.class);
		registerService(bc, addSPole, SelectedNodesAndEdgesListener.class);

		RemovePoleAction removePole = new RemovePoleAction(poleManager);
		registerService(bc, removePole, CyAction.class);
		registerService(bc, removePole, SelectedNodesAndEdgesListener.class);



	}
}

