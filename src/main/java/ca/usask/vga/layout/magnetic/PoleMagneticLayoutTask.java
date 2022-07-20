package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.force.*;
import ca.usask.vga.layout.magnetic.poles.PoleManager;
import ca.usask.vga.layout.magnetic.util.*;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.LayoutEdge;
import org.cytoscape.view.layout.LayoutNode;
import org.cytoscape.view.layout.LayoutPartition;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.work.undo.UndoSupport;
import prefuse.util.force.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PoleMagneticLayoutTask extends ForceDirectedLayoutTask {

    private final PoleManager poleManager;
    protected MapPoleClassifier poleClassifier;
    private Map<LayoutPartition, ErrorCalculator> errorCalc;

    public PoleMagneticLayoutTask(String displayName, CyNetworkView networkView, Set<View<CyNode>> nodesToLayOut, ForceDirectedLayoutContext context, ForceDirectedLayout.Integrators integrator, String attrName, UndoSupport undo, PoleManager poleManager) {
        super(displayName, networkView, nodesToLayOut, context, integrator, attrName, undo);
        this.poleManager = poleManager;
        poleClassifier = new MapPoleClassifier(networkView.getModel(), poleManager);
        errorCalc = new HashMap<>();
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
    protected void clearMaps() {
        poleClassifier = new MapPoleClassifier(networkView.getModel(), poleManager);
    }

    @Override
    protected void addSimulatorForces(ForceSimulator m_fsim, LayoutPartition part) {

        // REGISTERING FORCES

        // Default prefuse layout forces
        m_fsim.addForce(new NBodyForce((float) -context.repulsionCoefficient, NBodyForce.DEFAULT_DISTANCE, NBodyForce.DEFAULT_THETA, monitor));  // Repulsion
        m_fsim.addForce(new SpringForce());  // Spring Attraction (ideal dist)
        m_fsim.addForce(new DragForce());  // Dampening

        PoleMagneticLayoutContext context = (PoleMagneticLayoutContext) this.context;

        // Magnetic force
        MagneticForce mf = null;
        if (context.magnetEnabled) {
            if (context.useMagneticPoles) {
                // Using magnetic pole classification
                mf = new MagneticForce(poleClassifier, (float) context.magneticFieldStrength,
                        (float) context.magneticAlpha,  (float) context.magneticBeta);
            } else {
                // Simpler version without poles
                mf = new MagneticForce(context.fieldType,  (float) context.magneticFieldStrength,
                        (float) context.magneticAlpha,  (float) context.magneticBeta);
            }
            m_fsim.addForce(mf);
        }
        errorCalc.put(part, new ErrorCalculator(m_fsim, mf));

        // Pole pin force
        if (context.pinPoles) {
            PinForce pf = new PinForce(poleClassifier);
            m_fsim.addForce(pf);
            // TODO: More options for pins
            if (context.useCirclePin)
                pf.setPinAroundCircle(part, pf.getSuggestedRadius(part));
        }

        // Pole gravity force
        if (context.usePoleAttraction)
            m_fsim.addForce(new PoleGravityForce(poleClassifier, (float) context.poleGravity));

        // Central gravity force
        if (context.useCentralGravity)
            m_fsim.addForce(new GravityForce(part.getAverageLocation(), (float) context.centralGravity));

        // Hierarchy force
        if (context.useHierarchyForce)
            m_fsim.addForce(new HierarchyForce(poleClassifier, context.hierarchyType,
                    (float) context.hierarchyForce, HierarchyForce.getSuggestedRadius(part)));

    }

    @Override
    public void layoutPartition(LayoutPartition part) {
        PoleMagneticLayoutContext context = (PoleMagneticLayoutContext) this.context;
        if (context.useAutoLayout)
            new AutoLayout(this, part).run(taskMonitor);

        super.layoutPartition(part);
        if (part.edgeCount() > 1 && errorCalc.get(part) != null)
            errorCalc.get(part).displayResults(taskMonitor);
    }

    public ErrorCalculator getErrorCalculator(LayoutPartition part) {
        return errorCalc.get(part);
    }

}
