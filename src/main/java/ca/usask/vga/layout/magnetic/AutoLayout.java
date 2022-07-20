package ca.usask.vga.layout.magnetic;

import org.cytoscape.view.layout.*;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import prefuse.util.force.ForceItem;
import prefuse.util.force.ForceSimulator;
import prefuse.util.force.Spring;

import java.util.*;

public class AutoLayout extends AbstractTask {

    public final static int TRIAL_ITERATIONS = 5;

    private final PoleMagneticLayoutTask layout;
    private final LayoutPartition part;

    public AutoLayout(PoleMagneticLayoutTask layout, LayoutPartition part) {
        this.layout = layout;
        this.part = part;
    }

    public PoleMagneticLayoutContext getContext() {
        return (PoleMagneticLayoutContext) layout.context;
    }

    @Override
    public void run(TaskMonitor taskMonitor) {

        // TODO: Consider multiple threads/executions
        taskMonitor.showMessage(TaskMonitor.Level.INFO, "Finding parameters...");

        AutoLayoutVariables auto = new AutoLayoutVariables(getContext());
        Iterable<int[]> combinations = auto.getAllCombinations();

        int[] bestComb = new int[auto.getVarCount()];
        auto.setAll(bestComb);
        float maxScore = runTrial();

        float newScore;

        int progress = 1, totalCombinations = auto.getCombinationCount();

        for (int[] combination : combinations) {

            taskMonitor.setProgress((float) progress / totalCombinations);
            progress++;

            auto.setAll(combination);
            newScore = runTrial();
            //taskMonitor.showMessage(TaskMonitor.Level.INFO, "Comb: " + Arrays.toString(combination) + " Score: " + newScore);

            if (newScore > maxScore) {
                maxScore = newScore;
                bestComb = combination;
            }

        }

        taskMonitor.showMessage(TaskMonitor.Level.INFO, "Chosen combination: " + Arrays.toString(bestComb) + " Score: " + maxScore);
        auto.setAll(bestComb);
    }

    public float runTrial() {
        runNewSimulation(TRIAL_ITERATIONS);
        return new AutoLayoutQuality(getContext()).calculateScore(layout.getErrorCalculator(part));
    }

    protected ForceSimulator runNewSimulation(int iterations) {

        layout.clearMaps();

        // Calculate our edge weights
        part.calculateEdgeWeights();

        ForceSimulator m_fsim = new ForceSimulator(layout.integrator.getNewIntegrator(layout.monitor), layout.monitor);
        layout.addSimulatorForces(m_fsim, part);

        List<LayoutNode> nodeList = part.getNodeList();
        List<LayoutEdge> edgeList = part.getEdgeList();

        if (layout.context.isDeterministic) {
            Collections.sort(nodeList);
            Collections.sort(edgeList);
        }

        Map<LayoutNode, ForceItem> forceItems = new HashMap<>();

        // initialize nodes
        for (LayoutNode ln : nodeList) {

            ForceItem fitem = forceItems.get(ln);

            if (fitem == null) {
                fitem = new ForceItem();
                forceItems.put(ln, fitem);
            }

            fitem.mass = layout.getMassValue(ln);
            fitem.location[0] = (float) ln.getX();
            fitem.location[1] = (float) ln.getY();
            m_fsim.addItem(fitem);

            layout.mapForceItem(ln, fitem);
        }

        // initialize edges
        for (LayoutEdge e : edgeList) {

            LayoutNode n1 = e.getSource();
            ForceItem f1 = forceItems.get(n1);
            LayoutNode n2 = e.getTarget();
            ForceItem f2 = forceItems.get(n2);

            if (f1 == null || f2 == null)
                continue;

            Spring s = m_fsim.addSpring(f1, f2, layout.getSpringCoefficient(e), layout.getSpringLength(e));
            layout.mapSpring(e, s);
        }

        // perform layout
        // TODO: Match full time steps

        long timestep = 1000L;

        for (int i = 0; i < iterations; i++) {

            timestep *= (1.0 - i / (double) iterations);
            long step = timestep + 50;
            m_fsim.runSimulator(step);

        }

        return m_fsim;
    }
}
