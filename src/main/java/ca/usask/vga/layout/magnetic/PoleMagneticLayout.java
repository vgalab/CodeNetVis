package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.poles.PoleManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.AbstractLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.undo.UndoSupport;

import java.util.HashSet;
import java.util.Set;

/**
 * Standard Cytoscape adapter for the {@link PoleMagneticLayoutTask}.
 */
public class PoleMagneticLayout extends AbstractLayoutAlgorithm {

    protected static final String ALGORITHM_ID = "magnetic-layout-poles";
    static final String ALGORITHM_DISPLAY_NAME = "Magnetic Layout - Poles";
    private final PoleManager poleManager;

    public PoleMagneticLayout(PoleManager poleManager, UndoSupport undo) {
        super(ALGORITHM_ID, ALGORITHM_DISPLAY_NAME, undo);
        this.poleManager = poleManager;
    }

    @Override
    public TaskIterator createTaskIterator(CyNetworkView networkView, Object context, Set<View<CyNode>> nodesToLayOut,
                                           String attrName) {
        return new TaskIterator(new PoleMagneticLayoutTask(toString(), networkView, nodesToLayOut,
                (ForceDirectedLayoutContext) context, ForceDirectedLayout.Integrators.RUNGEKUTTA, attrName, undoSupport, poleManager));
    }

    @Override
    public Object createLayoutContext() {
        return new PoleMagneticLayoutContext();
    }

    @Override
    public Set<Class<?>> getSupportedEdgeAttributeTypes() {
        return new HashSet<>();
    }

    @Override
    public boolean getSupportsSelectedOnly() {
        return false;
    }



}
