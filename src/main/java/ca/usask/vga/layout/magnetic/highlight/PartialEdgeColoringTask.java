package ca.usask.vga.layout.magnetic.highlight;

import org.cytoscape.model.*;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.work.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

/**
 * Used to create a subnetwork of nodes/edges, then add
 * a midpoint node to each edge. It is then used to
 * apply partial edge styles, where half the edge is
 * colored differently than the other half.
 * The resulting network is made immutable.
 */
public class PartialEdgeColoringTask extends AbstractTask {

    public static final String NETWORK_NAME = "Partial coloring view";

    private final NetworkCyAccess cy;

    public static TaskFactory newTaskFactory(NetworkCyAccess cy) {
        return new TaskFactory() {
            @Override
            public TaskIterator createTaskIterator() {
                return new TaskIterator(new PartialEdgeColoringTask(cy));
            }
            @Override
            public boolean isReady() {
                return true;
            }
        };
    }

    public PartialEdgeColoringTask(NetworkCyAccess cy) {
        this.cy = cy;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {

        // Copy network
        CyNetwork network = new CreateSubnetworkTask(cy).copyCurrentVisible();
        CyNetworkView view = cy.vm.getNetworkViews(network).iterator().next();

        CyTable nodeTable = network.getDefaultNodeTable();
        CyTable edgeTable = network.getDefaultEdgeTable();

        // Replace all edges with a node and 2 new edges
        // Apply style overrides

        List<CyNode> oldNodes = network.getNodeList();
        List<CyEdge> oldEdges = network.getEdgeList();

        class MidpointData {
            final CyNode node;
            final CyEdge edgeSource;
            final CyEdge edgeTarget;
            final double x;
            final double y;

            MidpointData(CyNode node, CyEdge edgeSource, CyEdge edgeTarget, double x, double y) {
                this.node = node;
                this.edgeSource = edgeSource;
                this.edgeTarget = edgeTarget;
                this.x = x;
                this.y = y;
            }
        }

        Set<MidpointData> midpoints = new HashSet<>();

        for (CyEdge edge : oldEdges) {

            CyRow data = edgeTable.getRow(edge.getSUID());

            CyNode source = edge.getSource();
            double sourceX = view.getNodeView(source).getVisualProperty(NODE_X_LOCATION);
            double sourceY = view.getNodeView(source).getVisualProperty(NODE_Y_LOCATION);

            CyNode target = edge.getTarget();
            double targetX = view.getNodeView(target).getVisualProperty(NODE_X_LOCATION);
            double targetY = view.getNodeView(target).getVisualProperty(NODE_Y_LOCATION);

            double x = (sourceX + targetX) / 2;
            double y = (sourceY + targetY) / 2;

            CyNode midpointNode = network.addNode();
            CyRow midpointData = nodeTable.getRow(midpointNode.getSUID());

            CyEdge edge1 = network.addEdge(source, midpointNode, true);
            CyEdge edge2 = network.addEdge(midpointNode, target, true);

            midpoints.add(new MidpointData(midpointNode, edge1, edge2, x, y));
        }

        network.removeEdges(oldEdges);

        cy.eh.flushPayloadEvents();

        for (MidpointData midpoint : midpoints) {
            if (view.getNodeView(midpoint.node) == null) {
                throw new RuntimeException("Midpoint node view not found");
            }

            view.getNodeView(midpoint.node).setVisualProperty(NODE_X_LOCATION, midpoint.x);
            view.getNodeView(midpoint.node).setVisualProperty(NODE_Y_LOCATION, midpoint.y);

            view.getNodeView(midpoint.node).setLockedValue(NODE_SIZE, 0.0);
            //view.getEdgeView(midpoint.edgeSource).setLockedValue(EDGE_VISIBLE, false);
            view.getEdgeView(midpoint.edgeSource).setLockedValue(EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.NONE);
            view.getEdgeView(midpoint.edgeTarget).setLockedValue(EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.NONE);
        }

        String newName = cy.cnn.getSuggestedNetworkTitle(NETWORK_NAME);

        network.getDefaultNetworkTable().getRow(network.getSUID()).set(CyNetwork.NAME, newName);
        network.getDefaultNetworkTable().getRow(network.getSUID()).set(CyRootNetwork.SHARED_NAME, newName);

        cy.am.setCurrentNetworkView(null);

        // This is a way to force the view to update.
        // Otherwise, the new nodes will not be visible.
        Thread.sleep(200);

        cy.am.setCurrentNetworkView(view);

    }
}
