package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.force.FieldType;
import ca.usask.vga.layout.magnetic.force.HierarchyForce;
import ca.usask.vga.layout.magnetic.highlight.CreateSubnetworkTask;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskObserver;

import java.util.HashSet;

/**
 * Used to run the magnetic layout with pre-defined optimal parameters.
 * Overrides the default parameters in {@link PoleMagneticLayoutContext}.
 */
public class SoftwareLayout {

    private final PoleMagneticLayout pml;
    private final TaskManager tm;
    private final CyApplicationManager am;
    private final CreateSubnetworkTask subnetTask;

    private int maxRings;
    private float pinRadius;

    public SoftwareLayout(PoleMagneticLayout pml, TaskManager tm, CyApplicationManager am, CreateSubnetworkTask subnetTask) {
        this.pml = pml;
        this.tm = tm;
        this.am = am;
        this.subnetTask = subnetTask;
    }

    public void createSubnetworkFromVisible() {
        subnetTask.copyCurrentVisible();
    }

    public void cutCommonConnections() {
        subnetTask.copyAndCutCommonEdges();
    }

    public void runLayout() {
        runLayout(null);
    }

    public void runLayout(Runnable onFinished) {

        var context = getContext();

        var netView = am.getCurrentNetworkView();

        var task = pml.createTaskIterator(netView, context, new HashSet<>(netView.getNodeViews()), null);

        tm.execute(task, new TaskObserver() {
            public void taskFinished(ObservableTask task) {}
            public void allFinished(FinishStatus finishStatus) {if (onFinished!=null) onFinished.run();}
        });
    }

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

    public void setPinRadius(float newValue) {
        this.pinRadius = newValue;
    }

    public void setMaxRings(int newValue) {
        this.maxRings = newValue;
    }

    public void layoutOnLoad() {

        var context = getLinearContext();
        var netView = am.getCurrentNetworkView();
        var task = pml.createTaskIterator(netView, context, new HashSet<>(netView.getNodeViews()), null);

        tm.execute(task);
    }

}
