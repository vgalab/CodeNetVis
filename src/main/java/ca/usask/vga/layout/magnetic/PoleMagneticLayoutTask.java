package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.poles.PoleManager;
import ca.usask.vga.layout.magnetic.util.MagneticForce;
import ca.usask.vga.layout.magnetic.util.MapPoleClassifier;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.LayoutEdge;
import org.cytoscape.view.layout.LayoutNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.undo.UndoSupport;
import prefuse.util.force.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PoleMagneticLayoutTask extends ForceDirectedLayoutTask {

    protected MapPoleClassifier poleClassifier;

    public PoleMagneticLayoutTask(String displayName, CyNetworkView networkView, Set<View<CyNode>> nodesToLayOut, ForceDirectedLayoutContext context, ForceDirectedLayout.Integrators integrator, String attrName, UndoSupport undo, PoleManager poleManager) {
        super(displayName, networkView, nodesToLayOut, context, integrator, attrName, undo);
        poleClassifier = new MapPoleClassifier(networkView.getModel(), poleManager);
    }

    @Override
    protected void mapForceItem(LayoutNode ln, ForceItem fitem) {
        poleClassifier.mapNode(fitem, ln);
    }

    @Override
    protected void mapSpring(LayoutEdge le, Spring spring) {
        poleClassifier.mapEdge(spring, le);
    }

    @Override
    protected void addSimulatorForces(ForceSimulator m_fsim) {

        // REGISTERING FORCES
        m_fsim.addForce(new NBodyForce(monitor));  // Repulsion
        m_fsim.addForce(new SpringForce());  // Attraction (ideal dist)
        m_fsim.addForce(new DragForce());  // Dampening

        PoleMagneticLayoutContext context = (PoleMagneticLayoutContext) this.context;

        // Magnetic force
        if (context.magnetEnabled) {
            if (context.usePoles) {
                m_fsim.addForce(new MagneticForce(poleClassifier, context.usePoles, context.magneticFieldStrength, context.magneticAlpha, context.magneticBeta));
            } else {
                m_fsim.addForce(new MagneticForce(context.fieldType, context.magneticFieldStrength, context.magneticAlpha, context.magneticBeta));
            }
        }

    }

}
