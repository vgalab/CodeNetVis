package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.util.ErrorCalculator;
import ca.usask.vga.layout.magnetic.force.MagneticForce;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.LayoutPartition;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.undo.UndoSupport;
import prefuse.util.force.DragForce;
import prefuse.util.force.ForceSimulator;
import prefuse.util.force.NBodyForce;
import prefuse.util.force.SpringForce;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * This class extends force directed layout functionality to add a simple magnetic force.
 */
public class SimpleMagneticLayoutTask extends ForceDirectedLayoutTask {

    private Map<LayoutPartition, ErrorCalculator> errorCalc;

    public SimpleMagneticLayoutTask(String displayName, CyNetworkView networkView, Set<View<CyNode>> nodesToLayOut, ForceDirectedLayoutContext context, ForceDirectedLayout.Integrators integrator, String attrName, UndoSupport undo) {
        super(displayName, networkView, nodesToLayOut, context, integrator, attrName, undo);
        errorCalc = new HashMap<>();
    }

    @Override
    protected void addSimulatorForces(ForceSimulator m_fsim, LayoutPartition part) {

        // REGISTERING FORCES

        // Default prefuse layout forces
        m_fsim.addForce(new NBodyForce((float) -context.repulsionCoefficient, NBodyForce.DEFAULT_DISTANCE, NBodyForce.DEFAULT_THETA, monitor));  // Repulsion
        m_fsim.addForce(new SpringForce());  // Attraction (ideal dist)
        m_fsim.addForce(new DragForce());  // Dampening

        SimpleMagneticLayoutContext context = (SimpleMagneticLayoutContext) this.context;

        // Magnetic force (simplified version)
        if (context.magnetEnabled) {
            MagneticForce mf = new MagneticForce(context.fieldType,  (float) context.magneticFieldStrength,
                    (float) context.magneticAlpha,  (float) context.magneticBeta);
            m_fsim.addForce(mf);
            errorCalc.put(part, new ErrorCalculator(m_fsim, mf));
        }

    }

    @Override
    public void layoutPartition(LayoutPartition part) {
        super.layoutPartition(part);
        if (part.edgeCount() > 1 && errorCalc.get(part) != null) {
            var calc = errorCalc.get(part);
            calc.recalculate();
            calc.displayResults(taskMonitor);
        }
    }

    public ErrorCalculator getErrorCalculator(LayoutPartition part) {
        return errorCalc.get(part);
    }

}
