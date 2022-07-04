package ca.usask.vga.layout.magnetic;

import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.AbstractLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.undo.UndoSupport;

import java.util.HashSet;
import java.util.Set;

public class SimpleMagneticLayout extends AbstractLayoutAlgorithm {

    protected static final String ALGORITHM_ID = "magnetic-layout-simple";
    static final String ALGORITHM_DISPLAY_NAME = "Magnetic Layout - Simple";

    public SimpleMagneticLayout(UndoSupport undo) {
        super(ALGORITHM_ID, ALGORITHM_DISPLAY_NAME, undo);
    }

    @Override
    public TaskIterator createTaskIterator(CyNetworkView networkView, Object context, Set<View<CyNode>> nodesToLayOut,
                                           String attrName) {
        return new TaskIterator(new SimpleMagneticLayoutTask(toString(), networkView, nodesToLayOut,
                (ForceDirectedLayoutContext) context, ForceDirectedLayout.Integrators.RUNGEKUTTA, attrName, undoSupport));
    }

    @Override
    public Object createLayoutContext() {
        return new SimpleMagneticLayoutContext();
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
