package ca.usask.vga.layout.magnetic.poles;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;

import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * Allows actions to act on the selected nodes/edges in the current view
 */
public abstract class ActionOnSelected extends AbstractCyAction implements SelectedNodesAndEdgesListener {

    private SelectedNodesAndEdgesEvent lastSelectionEvent;

    public ActionOnSelected(String name) {
        super(name);
    }

   protected CyNetwork getNetwork() {
        return lastSelectionEvent.getNetwork();
   }

   protected Collection<CyNode> getSelectedNodes() {
        if (!isSelectionActive()) return null;
        return lastSelectionEvent.getSelectedNodes();
   }

   protected Collection<CyNode> getUnselectedNodes() {
       if (!isSelectionActive()) return null;
       return lastSelectionEvent.getUnselectedNodes();
   }

    protected Collection<CyEdge> getSelectedEdges() {
        if (!isSelectionActive()) return null;
        return lastSelectionEvent.getSelectedEdges();
    }

    protected Collection<CyEdge> getUnselectedEdges() {
        if (!isSelectionActive()) return null;
        return lastSelectionEvent.getUnselectedEdges();
    }

    protected boolean isSelectionActive() {
        return lastSelectionEvent != null && lastSelectionEvent.isCurrentNetwork();
    }

    protected boolean nodesChanged() {
        return isSelectionActive() && lastSelectionEvent.nodesChanged();
    }

    protected boolean edgesChanged() {
        return isSelectionActive() && lastSelectionEvent.edgesChanged();
    }

    @Override
    public void handleEvent(SelectedNodesAndEdgesEvent event) {
        lastSelectionEvent = event;
    }

}
