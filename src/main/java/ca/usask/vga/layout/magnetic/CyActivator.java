package ca.usask.vga.layout.magnetic;

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

