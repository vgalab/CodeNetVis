package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.util.MagneticForce;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.undo.UndoSupport;
import prefuse.util.force.DragForce;
import prefuse.util.force.ForceSimulator;
import prefuse.util.force.NBodyForce;
import prefuse.util.force.SpringForce;

import java.util.Set;

public class SimpleMagneticLayoutTask extends ForceDirectedLayoutTask {

    public SimpleMagneticLayoutTask(String displayName, CyNetworkView networkView, Set<View<CyNode>> nodesToLayOut, ForceDirectedLayoutContext context, ForceDirectedLayout.Integrators integrator, String attrName, UndoSupport undo) {
        super(displayName, networkView, nodesToLayOut, context, integrator, attrName, undo);
    }

    @Override
    protected void addSimulatorForces(ForceSimulator m_fsim) {

        // REGISTERING FORCES
        m_fsim.addForce(new NBodyForce(monitor));  // Repulsion
        m_fsim.addForce(new SpringForce());  // Attraction (ideal dist)
        m_fsim.addForce(new DragForce());  // Dampening

        SimpleMagneticLayoutContext context = (SimpleMagneticLayoutContext) this.context;

        if (context.magnetEnabled)
            // Magnetic force
            m_fsim.addForce(new MagneticForce(context.fieldType, context.magneticFieldStrength, context.magneticAlpha, context.magneticBeta));

    }


}
