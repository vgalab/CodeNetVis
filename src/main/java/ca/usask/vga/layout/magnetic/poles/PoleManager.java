package ca.usask.vga.layout.magnetic.poles;

import org.cytoscape.model.*;
import sun.security.provider.CtrDrbg;

import java.util.*;

public class PoleManager {

    protected Map<CyNetwork, List<CyNode>> poleList;

    protected Set<CyNode> poleIsOutwards;

    protected Map<CyNetwork, Map<CyNode, Map<CyNode, Byte>>> cachedPoleDistances;

    public final String NAMESPACE = "Magnetic Poles", IS_POLE = "Is pole?", CLOSEST_POLE = "Closest pole", IS_OUTWARDS = "Is pole outwards?", DISTANCE_TO_POLE = "Distance to pole";
    public final int UNREACHABLE_NODE = 999;

    public PoleManager() {
        poleList = new HashMap<>();
        poleIsOutwards = new HashSet<>();
        cachedPoleDistances = new HashMap<>();
    }

    protected void initializePoleList(CyNetwork network) {
        if (!poleList.containsKey(network)) {
            poleList.put(network, new ArrayList<CyNode>());
            updateTables(network);
        }
    }

    public List<CyNode> getPoleList(CyNetwork network) {
        initializePoleList(network);
        return poleList.get(network);
    }

    public void addPole(CyNetwork network, CyNode node) {
        if (!getPoleList(network).contains(node)) {
            getPoleList(network).add(node);
        }
    }

    public void addPole(CyNetwork network, Collection<CyNode> nodes) {
        for (CyNode n : nodes)
            addPole(network, n);
    }

    protected void makePoleOutwards(CyNetwork network, CyNode pole) {
        poleIsOutwards.add(pole);
    }

    protected void makePoleInwards(CyNetwork network, CyNode pole) {
        poleIsOutwards.remove(pole);
    }

    public boolean isPoleOutwards(CyNetwork network, CyNode pole) {
        return poleIsOutwards.contains(pole);
    }

    public void setPoleDirection(CyNetwork network, CyNode pole, boolean isOutwards) {
        if (isOutwards != isPoleOutwards(network, pole))
            invalidateCache(network, pole);
        if (isOutwards)
            makePoleOutwards(network, pole);
        else
            makePoleInwards(network, pole);
    }

    public void setPoleDirection(CyNetwork network, Collection<CyNode> nodes, boolean isOutwards) {
        for (CyNode n : nodes)
            setPoleDirection(network, n, isOutwards);
    }

    public void removePole(CyNetwork network, CyNode node) {
        getPoleList(network).remove(node);
    }

    public void removePole(CyNetwork network, Collection<CyNode> nodes) {
        for (CyNode n : nodes)
            removePole(network, n);
    }

    public boolean isPole(CyNetwork network, CyNode node) {
        return getPoleList(network).contains(node);
    }

    protected Map<CyNode, Byte> getCachedShortestDistances(CyNetwork network, CyNode pole) {
        if (cachedPoleDistances != null && cachedPoleDistances.containsKey(network) && cachedPoleDistances.get(network).containsKey(pole))
            return cachedPoleDistances.get(network).get(pole);
        return null;
    }

    protected void setCachedShortestDistances(CyNetwork network, CyNode pole, Map<CyNode, Byte> distances) {
        if (cachedPoleDistances == null)
            cachedPoleDistances = new HashMap<>();
        if (!cachedPoleDistances.containsKey(network))
            cachedPoleDistances.put(network, new HashMap<CyNode, Map<CyNode, Byte>>());
        cachedPoleDistances.get(network).put(pole, distances);
    }

    protected void invalidateCache(CyNetwork network, CyNode pole) {
        if (cachedPoleDistances != null && cachedPoleDistances.containsKey(network))
            cachedPoleDistances.get(network).remove(pole);
    }

    protected Map<CyNode, Byte> getShortestDistancesFrom(CyNetwork network, CyNode pole) {
        // Caching
        Map<CyNode, Byte> cache = getCachedShortestDistances(network, pole);
        if (cache != null)
            return cache;

        // RUN BFS
        boolean isOutwards = isPoleOutwards(network, pole);

        Queue<CyNode> toExplore = new ArrayDeque<>();
        toExplore.add(pole);

        Set<CyNode> visited = new HashSet<>();

        Map<CyNode, Byte> shortestDistances = new HashMap<>();
        shortestDistances.put(pole, (byte) 0);

        while (!toExplore.isEmpty()) {

            CyNode n = toExplore.remove();
            visited.add(n);
            byte dist = shortestDistances.get(n);

            CyEdge.Type edgeDirection = isOutwards ? CyEdge.Type.OUTGOING : CyEdge.Type.INCOMING;

            for (CyEdge e : network.getAdjacentEdgeIterable(n, edgeDirection)) {
                CyNode n2 = e.getSource();
                if (isOutwards) n2 = e.getTarget();
                if (visited.contains(n2)) continue;
                toExplore.add(n2);
                shortestDistances.put(n2, (byte) (dist+1));
            }

        }

        setCachedShortestDistances(network, pole, shortestDistances);
        return shortestDistances;
    }

    public int getDistanceToPole(CyNetwork network, CyNode pole, CyNode from) {
        Map<CyNode, Byte> distances = getShortestDistancesFrom(network, pole);
        if (!distances.containsKey(from))
            return UNREACHABLE_NODE; // FAR AWAY
        return distances.get(from);
    }

    protected CyNode getClosestPole(CyNetwork network, CyNode from) {

        CyNode closestPole = null;
        int closestDist = UNREACHABLE_NODE;
        boolean equalDistance = false;

        for (CyNode pole : getPoleList(network)) {

            int dist = getDistanceToPole(network, pole, from);
            if (dist <= closestDist) {
                equalDistance = dist == closestDist;
                closestDist = dist;
                closestPole = pole;
            }

        }

        if (equalDistance)
            return null; // TODO: Check if this is good
        return closestPole;
    }

    public void updateTables(CyNetwork network) {

        CyTable nodeTable = network.getDefaultNodeTable();

        if (nodeTable.getColumn(NAMESPACE, IS_POLE) == null) {
            nodeTable.createColumn(NAMESPACE, IS_POLE, Boolean.class, false);
        }
        for (CyNode node : network.getNodeList()) {
            nodeTable.getRow(node.getSUID()).set(NAMESPACE, IS_POLE, isPole(network, node));
        }

        if (nodeTable.getColumn(NAMESPACE, IS_OUTWARDS) == null) {
            nodeTable.createColumn(NAMESPACE, IS_OUTWARDS, Boolean.class, false);
        }
        for (CyNode node : network.getNodeList()) {
            nodeTable.getRow(node.getSUID()).set(NAMESPACE, IS_OUTWARDS, isPoleOutwards(network, node));
        }

        if (nodeTable.getColumn(NAMESPACE, CLOSEST_POLE) == null) {
            nodeTable.createColumn(NAMESPACE, CLOSEST_POLE, Long.class, false);
        }
        for (CyNode node : network.getNodeList()) {
            CyNode closestPole = getClosestPole(network, node);
            if (closestPole != null)
                nodeTable.getRow(node.getSUID()).set(NAMESPACE, CLOSEST_POLE, closestPole.getSUID());
            else
                nodeTable.getRow(node.getSUID()).set(NAMESPACE, CLOSEST_POLE, 0L);
        }


    }



}
