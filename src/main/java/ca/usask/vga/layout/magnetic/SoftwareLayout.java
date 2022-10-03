package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.force.FieldType;
import ca.usask.vga.layout.magnetic.force.HierarchyForce;
import ca.usask.vga.layout.magnetic.highlight.CreateSubnetworkTask;
import ca.usask.vga.layout.magnetic.highlight.NetworkCyAccess;
import ca.usask.vga.layout.magnetic.highlight.PartialEdgeColoringTask;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.*;

import javax.swing.*;
import java.util.HashSet;

/**
 * Used to run the magnetic layout with pre-defined optimal parameters.
 * Overrides the default parameters in {@link PoleMagneticLayoutContext}.
 */
public class SoftwareLayout {

    static final float DEFAULT_RING_CIRCLE_RADIUS = 500f;

    private final PoleMagneticLayout pml;
    private final TaskManager tm;
    private final CyApplicationManager am;
    private final CreateSubnetworkTask subnetTask;
    private final NetworkCyAccess cy;

    private int maxRings;
    private float pinRadius;

    /**
     * Initializes the parameters for the software layout functionality.
     */
    public SoftwareLayout(PoleMagneticLayout pml, TaskManager tm, CyApplicationManager am, CreateSubnetworkTask subnetTask, NetworkCyAccess cy) {
        this.pml = pml;
        this.tm = tm;
        this.am = am;
        this.subnetTask = subnetTask;
        this.cy = cy;
    }

    /**
     * Creates a new subnetwork from visible nodes and edges.
     */
    public void createSubnetworkFromVisible() {
        subnetTask.copyCurrentVisible();
    }

    /**
     * Creates a new subnetwork from visible nodes and edges,
     * with edges between nodes of different colors/poles removed.
     */
    public void cutCommonConnections() {
        subnetTask.copyAndCutCommonEdges();
    }

    /**
     * Creates a new network with partial edge coloring of the current network.
     */
    public void createPartialColoring() {
        tm.execute(new TaskIterator(new PartialEdgeColoringTask(cy)));
        JOptionPane.showMessageDialog(null, "Please note that partial edge coloring has extra nodes " +
                "which can interfere with layout and filtering.\nIt is recommended to treat this subgraph as immutable.",
                "Partial edge coloring view created", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Checks whether a network is considered IMMUTABLE due to it being
     * a partial coloring of another network.
     * Note: This function is no longer used due to partial coloring view changed to mutable.
     */
    @Deprecated
    public boolean isImmutable(CyNetwork net) {
        return net == null || net.getDefaultNetworkTable().getRow(net.getSUID())
                .get("name", String.class)
                .startsWith(PartialEdgeColoringTask.NETWORK_NAME);
    }

    /**
     * Runs the POLE magnetic layout algorithm with the currently saved parameters.
     */
    public void runLayout(Runnable onFinished) {

        var context = getContext();

        var netView = am.getCurrentNetworkView();

        var task = pml.createTaskIterator(netView, context, new HashSet<>(netView.getNodeViews()), null);

        tm.execute(task, new TaskObserver() {
            public void taskFinished(ObservableTask task) {}
            public void allFinished(FinishStatus finishStatus) {if (onFinished!=null) onFinished.run();}
        });
    }

    /**
     * Returns the recommended parameters for the POLE magnetic layout.
     */
    protected PoleMagneticLayoutContext getContext() {

        var c = new PoleMagneticLayoutContext();

        c.repulsionCoefficient = 1;
        c.defaultSpringLength = 50;
        c.defaultSpringCoefficient = 1e-5;

        c.useCentralGravity = true;
        c.centralGravity = 1e-6;

        if (pinRadius == 0) {
            c.pinPoles = false;
        } else {
            c.pinPoles = true;
            c.useCirclePin = true;
            c.pinRadius = pinRadius;
        }

        c.usePoleAttraction = true;
        c.poleGravity = 1e-5;

        if (maxRings == 0) {
            c.useHierarchyForce = false;
        } else {
            c.useHierarchyForce = true;
            c.hierarchyType = HierarchyForce.Type.SINE_FUNCTION;
            c.ringRadius = pinRadius / (maxRings + 1);
            if (pinRadius == 0) c.ringRadius = DEFAULT_RING_CIRCLE_RADIUS / (maxRings + 1);
            c.hierarchyForce = 2e-1;
        }

        c.magnetEnabled = true;
        c.magneticAlpha = 0;
        c.magneticBeta = 1;
        c.useMagneticPoles = true;
        c.magneticFieldStrength = 1e-4;

        c.numIterations = 50;
        c.useAnimation = true;

        c.useAutoLayout = false;

        return c;
    }

    /**
     * Returns the recommended parameters for the LINEAR magnetic layout.
     */
    protected PoleMagneticLayoutContext getLinearContext() {

        var c = getContext();

        c.repulsionCoefficient = 5;
        c.defaultSpringLength = 50;
        c.defaultSpringCoefficient = 1e-5;

        c.useCentralGravity = false;
        c.pinPoles = false;

        c.usePoleAttraction = false;
        c.useHierarchyForce = false;

        c.magnetEnabled = true;
        c.magneticAlpha = 0;
        c.magneticBeta = 2;
        c.useMagneticPoles = false;
        c.fieldType = FieldType.HORIZONTAL;
        c.magneticFieldStrength = 1e-3;

        c.numIterations = 50;
        c.useAnimation = true;

        c.useAutoLayout = false;

        return c;
    }

    /**
     * Sets the radius of the pins for use whenever the software layout is run.
     */
    public void setPinRadius(float newValue) {
        this.pinRadius = newValue;
    }

    /**
     * Sets the maximum number of rings for use whenever the software layout is run.
     */
    public void setMaxRings(int newValue) {
        this.maxRings = newValue;
    }

    /**
     * Runs the default LINEAR magnetic layout.
     * Executed immediately after a new network is loaded.
     */
    public void layoutOnLoad() {
        runLinearLayout(null);
    }

    /**
     * Runs the default LINEAR magnetic layout.
     * Calls back to the runnable after the layout is finished.
     */
    public void runLinearLayout(Runnable onFinished) {

        var context = getLinearContext();
        var netView = am.getCurrentNetworkView();
        var task = pml.createTaskIterator(netView, context, new HashSet<>(netView.getNodeViews()), null);

        tm.execute(task, new TaskObserver() {
            public void taskFinished(ObservableTask task) {}
            public void allFinished(FinishStatus finishStatus) {if (onFinished!=null) onFinished.run();}
        });
    }

}
