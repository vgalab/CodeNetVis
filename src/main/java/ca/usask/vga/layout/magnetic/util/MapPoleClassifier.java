package ca.usask.vga.layout.magnetic.util;

import ca.usask.vga.layout.magnetic.poles.PoleManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.LayoutEdge;
import org.cytoscape.view.layout.LayoutNode;
import prefuse.util.force.ForceItem;
import prefuse.util.force.Spring;

import java.util.*;

public class MapPoleClassifier implements PoleClassifier {

    private final CyNetwork network;
    private final PoleManager poleManager;
    private final Map<ForceItem, CyNode> nodeMap;
    private final Map<Spring, CyEdge> edgeMap;

    private final Collection<ForceItem> poleList;
    private final Map<CyNode, ForceItem> poleMap;

    public MapPoleClassifier(CyNetwork network, PoleManager poleManager) {
        this.network = network;
        this.poleManager = poleManager;
        nodeMap = new HashMap<>();
        edgeMap = new HashMap<>();
        poleList = new ArrayList<>();
        poleMap = new HashMap<>();
    }

    public void mapNode(ForceItem item, LayoutNode node) {
        nodeMap.put(item, node.getNode());
        if (poleManager.isPole(network, node.getNode())) {
            poleList.add(item);
            poleMap.put(node.getNode(), item);
        }
    }

    public void mapEdge(Spring spring, LayoutEdge edge) {
        edgeMap.put(spring, edge.getEdge());
    }

    @Override
    public Iterable<ForceItem> getPoleList() {
        return poleList;
    }

    @Override
    public boolean isPole(ForceItem item) {
        return poleList.contains(item);
    }

    @Override
    public ForceItem closestPole(ForceItem item) {
        return poleMap.get(poleManager.getClosestPole(network, nodeMap.get(item)));
    }

    @Override
    public int distanceToPole(ForceItem item) {
        Integer result = poleManager.getClosestPoleDistance(network, nodeMap.get(item));
        if (result == null) return poleManager.UNREACHABLE_NODE;
        return result;
    }

    @Override
    public boolean isDisconnected(ForceItem item) {
        return poleManager.isDisconnected(network, nodeMap.get(item));
    }

    @Override
    public boolean isClosestToMultiple(ForceItem item) {
        return poleManager.isClosestToMultiple(network, nodeMap.get(item));
    }

    @Override
    public boolean isClosestToOne(ForceItem item) {
        return poleManager.isClosestToOne(network, nodeMap.get(item));
    }

    @Override
    public ForceItem poleOf(Spring spring) {
        return poleMap.get(poleManager.getAssignedPole(network, edgeMap.get(spring)));
    }

    @Override
    public boolean isDisconnected(Spring spring) {
        return poleManager.isDisconnected(network, edgeMap.get(spring));
    }

    @Override
    public boolean isClosestToMultiple(Spring spring) {
        return poleManager.isClosestToMultiple(network, edgeMap.get(spring));
    }

    @Override
    public boolean isClosestToOne(Spring spring) {
        return poleManager.isClosestToOne(network, edgeMap.get(spring));
    }

    @Override
    public boolean isPoleOutwards(ForceItem pole) {
        if (!isPole(pole)) return false;
        return poleManager.isPoleOutwards(network, nodeMap.get(pole));
    }


}
