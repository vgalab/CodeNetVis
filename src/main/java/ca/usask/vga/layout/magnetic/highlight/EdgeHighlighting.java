package ca.usask.vga.layout.magnetic.highlight;

import ca.usask.vga.layout.magnetic.AppPreferences;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.*;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.model.*;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;

import java.awt.*;
import java.util.*;
import java.util.List;

public class EdgeHighlighting implements SelectedNodesAndEdgesListener, SetCurrentNetworkListener {

    private final NetworkCyAccess cy;
    private final AppPreferences preferences;
    private SelectedNodesAndEdgesEvent lastEvent;

    private final String ENABLED_PROPERTY = "magnetic-layout.edgeHighlightingEnabled";

    private boolean enabled = false;
    private int desiredHopDistance = 1;

    public EdgeHighlighting(NetworkCyAccess cy, AppPreferences preferences) {
        this.cy = cy;
        this.preferences = preferences;
        // Load preference from file
        boolean enabledOnLoad = Boolean.parseBoolean(preferences.getProperties().getProperty(ENABLED_PROPERTY));
        setEventToCurrentSelection();
        setEnabled(enabledOnLoad);
    }

    public void toggleFeature() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled)
            applyHighlighting(lastEvent);
        else
            clearHighlighting(lastEvent);
        preferences.getProperties().setProperty(ENABLED_PROPERTY, ""+enabled);
    }

    public void setEventToCurrentSelection() {
        CyNetwork currentNet = cy.am.getCurrentNetwork();
        if (currentNet == null) {
            lastEvent = null;
            return;
        }
        lastEvent = new SelectedNodesAndEdgesEvent(currentNet, true, true, true);
    }

    public void setEventToNetwork(CyNetwork network) {
        if (network == null) {
            lastEvent = null;
            return;
        }
        lastEvent = new SelectedNodesAndEdgesEvent(network, true, true, true);
    }

    public void setDesiredHopDistance(int desiredHopDistance) {
        this.desiredHopDistance = desiredHopDistance;
        if (enabled) {
            clearHighlighting(lastEvent);
            applyHighlighting(lastEvent);
        }
    }

    protected void clearHighlighting(SelectedNodesAndEdgesEvent event) {
        if (event == null) return;
        Collection<CyNetworkView> views = cy.vm.getNetworkViews(event.getNetwork());
        for (CyNetworkView v : views) {
            if (v == null) continue;
            for (CyEdge e : event.getUnselectedEdges()) {
                clearStyle(e, v);
            }
        }
    }

    protected void applyHighlighting(SelectedNodesAndEdgesEvent event) {
        if (event == null) return;
        Collection<CyNetworkView> views = cy.vm.getNetworkViews(event.getNetwork());
        for (CyNetworkView v : views) {
            for (CyNode n : event.getSelectedNodes()) {
                exploreEdges(n, event.getNetwork(), v, desiredHopDistance);
            }
        }
    }

    protected void exploreEdges(CyNode start, CyNetwork net, CyNetworkView view, int depth) {
        exploreEdges(start, net, view, depth, null, null);
    }

    protected void exploreEdges(CyNode start, CyNetwork net, CyNetworkView view, int depth, Collection<CyNode> nodes, Collection<CyEdge> edges) {

        class Point {
            final CyNode node;
            final boolean isStart, isInbound;
            final byte distance;
            final CyEdge edge;
            public Point(CyNode node, boolean isStart, boolean isInbound, byte distance, CyEdge edge) {
                this.node = node;
                this.isStart = isStart;
                this.isInbound = isInbound;
                this.distance = distance;
                this.edge = edge;
            }
        }

        Queue<Point> toExplore = new ArrayDeque<>();

        toExplore.add(new Point(start, true, false, (byte) 0, null));

        Set<CyNode> visited = new HashSet<>();

        while (!toExplore.isEmpty()) {

            Point n = toExplore.remove();

            if (n.edge != null) {
                if (edges != null) edges.add(n.edge);
                if (view != null && n.isInbound)  {
                    // TODO: Potentially add a check for edges that are both in and out, or remove them
                    applyIncomingStyle(n.edge, view);
                }
                if (view != null && !n.isInbound) {
                    applyOutgoingStyle(n.edge, view);
                }
            }

            if (visited.contains(n.node)) continue;
            visited.add(n.node);
            if (nodes != null) nodes.add(n.node);

            if (n.distance >= depth) continue;

            if (n.isStart || n.isInbound)
                for (CyEdge e : net.getAdjacentEdgeIterable(n.node, CyEdge.Type.INCOMING)) {
                    CyNode n2 = e.getSource();
                    //if (visited.contains(n2)) continue;
                    toExplore.add(new Point(n2, false, true, (byte) (1 + n.distance), e));
                }

            if (n.isStart || !n.isInbound)
                for (CyEdge e : net.getAdjacentEdgeIterable(n.node, CyEdge.Type.OUTGOING)) {
                    CyNode n2 = e.getTarget();
                    //if (visited.contains(n2)) continue;
                    toExplore.add(new Point(n2, false, false, (byte) (1 + n.distance), e));
                }

        }

    }

    /*protected void exploreIncomingEdges(CyNode n, CyNetwork net, CyNetworkView view, int depth, Collection<CyNode> nodes, Collection<CyEdge> edges) {
        if (nodes != null) nodes.add(n);
        if (depth == 0) return;

        for (CyEdge e : net.getAdjacentEdgeIterable(n, CyEdge.Type.INCOMING)) {
            if (edges != null) edges.add(e);
            if (view != null)
                applyIncomingStyle(e, view);
            exploreIncomingEdges(e.getSource(), net, view, depth-1, nodes, edges);
        }
    }

    protected void exploreOutgoingEdges(CyNode n, CyNetwork net, CyNetworkView view, int depth, Collection<CyNode> nodes, Collection<CyEdge> edges) {
        if (nodes != null) nodes.add(n);
        if (depth == 0) return;

        for (CyEdge e : net.getAdjacentEdgeIterable(n, CyEdge.Type.OUTGOING)) {
            if (edges != null) edges.add(e);
            if (view != null)
                applyOutgoingStyle(e, view);
            exploreOutgoingEdges(e.getTarget(), net, view, depth-1, nodes, edges);
        }
    }

    protected void exploreIncomingEdges(CyNode n, CyNetwork net, CyNetworkView view, int depth) {
        exploreIncomingEdges(n, net, view, depth, null, null);
    }

    protected void exploreOutgoingEdges(CyNode n, CyNetwork net, CyNetworkView view, int depth) {
        exploreOutgoingEdges(n, net, view, depth, null, null);
    }*/

    protected void applyIncomingStyle(CyEdge edge, CyNetworkView view) {
        View<CyEdge> edgeView = view.getEdgeView(edge);
        edgeView.setLockedValue(BasicVisualLexicon.EDGE_UNSELECTED_PAINT, Color.BLUE);
        edgeView.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, 255);
        View<CyNode> nodeView = view.getNodeView(edge.getSource());
        nodeView.setLockedValue(BasicVisualLexicon.NODE_TRANSPARENCY, 255);
    }

    protected void applyOutgoingStyle(CyEdge edge, CyNetworkView view) {
        View<CyEdge> edgeView = view.getEdgeView(edge);
        edgeView.setLockedValue(BasicVisualLexicon.EDGE_UNSELECTED_PAINT, Color.RED);
        edgeView.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, 255);
        View<CyNode> nodeView = view.getNodeView(edge.getTarget());
        nodeView.setLockedValue(BasicVisualLexicon.NODE_TRANSPARENCY, 255);
    }

    protected void clearStyle(CyEdge edge, CyNetworkView view) {
        View<CyEdge> edgeView = view.getEdgeView(edge);
        edgeView.clearValueLock(BasicVisualLexicon.EDGE_UNSELECTED_PAINT);
        edgeView.clearValueLock(BasicVisualLexicon.EDGE_TRANSPARENCY);
        View<CyNode> nodeView = view.getNodeView(edge.getSource());
        nodeView.clearValueLock(BasicVisualLexicon.NODE_TRANSPARENCY);
        nodeView = view.getNodeView(edge.getTarget());
        nodeView.clearValueLock(BasicVisualLexicon.NODE_TRANSPARENCY);
    }

    @Override
    public void handleEvent(SelectedNodesAndEdgesEvent event) {
        lastEvent = event;

        if (event != null && event.nodesChanged()) {
            clearHighlighting(event);
            if (enabled)
                applyHighlighting(event);
        }
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        setEventToNetwork(e.getNetwork());
        handleEvent(lastEvent);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void selectionToNetwork() {
        if (lastEvent == null) {
            System.out.println("Last event is null");
            return;
        }
        ArrayList<CyNode> selectedNodes = new ArrayList<>();
        ArrayList<CyEdge> selectedEdges = new ArrayList<>();

        for (CyNode n : lastEvent.getSelectedNodes()) {
            exploreEdges(n, lastEvent.getNetwork(), null, desiredHopDistance, selectedNodes, selectedEdges);
        }
        new CreateSubnetworkTask(cy).copyNetwork(lastEvent.getNetwork(), selectedNodes, selectedEdges);
    }


}
