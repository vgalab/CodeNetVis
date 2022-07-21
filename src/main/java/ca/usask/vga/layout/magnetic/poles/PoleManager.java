package ca.usask.vga.layout.magnetic.poles;

import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.*;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.session.events.SessionAboutToBeLoadedEvent;
import org.cytoscape.session.events.SessionAboutToBeLoadedListener;
import org.cytoscape.work.undo.UndoSupport;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PoleManager implements NetworkAddedListener, SetCurrentNetworkListener, SessionAboutToBeLoadedListener {

    private final UndoSupport undoSupport;

    protected Map<CyNetwork, List<CyNode>> poleList;
    protected Set<CyNode> poleIsOutwards;

    protected Map<CyNetwork, Map<CyNode, Map<CyNode, Byte>>> cachedPoleDistances;

    // Table column names
    public static final String NAMESPACE = "Magnetic Poles", IS_POLE = "Is pole?", CLOSEST_POLE = "Closest pole",
        IS_OUTWARDS = "Is pole outwards?", DISTANCE_TO_POLE = "Distance to pole",
        EDGE_ASSIGNED_POLE = "Assigned pole", EDGE_TARGET_NODE_POLE = "Target node pole",
        IS_DISCONNECTED = "Not connected", IN_POLE_LIST = "Inward Pole List", OUT_POLE_LIST = "Outward Pole List";

    public static final int UNREACHABLE_NODE = 999;

    public static final String DISCONNECTED_NAME = "none", MULTIPLE_POLES_NAME = "multiple";

    protected boolean tableInitialized;

    protected PoleManagerEdit lastEdit;

    public PoleManager(CyNetworkManager networkManager, UndoSupport undoSupport) {
        this.undoSupport = undoSupport;
        poleList = new HashMap<>();
        poleIsOutwards = new HashSet<>();
        cachedPoleDistances = new HashMap<>();
        for (CyNetwork net : networkManager.getNetworkSet()) {
            readPoleListFromTable(net);
        }
    }

    protected void initializePoleList(CyNetwork network) {
        if (!poleList.containsKey(network)) {
            poleList.put(network, new ArrayList<CyNode>());
        }
    }

    protected void readPoleListFromTable(CyNetwork network) {
        readPoleListFromColumn(network, IN_POLE_LIST, false);
        readPoleListFromColumn(network, OUT_POLE_LIST, true);
    }

    protected void readPoleListFromColumn(CyNetwork network, String columnName, boolean isOutwards) {
        CyTable table = network.getDefaultNetworkTable();
        if (table.getColumn(NAMESPACE, columnName) != null) {
            List<String> poleNameList = table.getRow(network.getSUID()).getList(NAMESPACE, columnName, String.class);
            if (poleNameList != null) {
                for (String name : poleNameList) {
                    Iterator<Long> iterator = network.getDefaultNodeTable().getMatchingKeys("name", name, Long.class).iterator();
                    if (!iterator.hasNext()) continue;
                    long matchSUID = iterator.next();
                    CyNode node = network.getNode(matchSUID);
                    if (node != null) {
                        addPole(network, node);
                        setPoleDirection(network, node, isOutwards);
                    }
                }
            }
        }
    }

    public List<CyNode> getPoleList(CyNetwork network) {
        initializePoleList(network);
        return poleList.get(network);
    }

    public List<CyNode> getPoleListSorted(CyNetwork network, Comparator<CyNode> comparator) {
        List<CyNode> list = getPoleList(network);
        list = new ArrayList<>(list);
        Collections.sort(list, comparator);
        Collections.reverse(list);
        return list;
    }

    public List<String> getPoleNameList(CyNetwork network) {
        List<CyNode> list = getPoleList(network);
        List<String> newList = new ArrayList<>(list.size());
        for (CyNode n : list) {
            newList.add(getPoleName(network, n));
        }
        return newList;
    }

    public List<String> getInPoleNameList(CyNetwork network) {
        List<CyNode> list = getPoleList(network);
        List<String> newList = new ArrayList<>(list.size());
        for (CyNode n : list) {
            if (!isPoleOutwards(network, n))
                newList.add(getPoleName(network, n));
        }
        return newList;
    }

    public List<String> getOutPoleNameList(CyNetwork network) {
        List<CyNode> list = getPoleList(network);
        List<String> newList = new ArrayList<>(list.size());
        for (CyNode n : list) {
            if (isPoleOutwards(network, n))
                newList.add(getPoleName(network, n));
        }
        return newList;
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

    public void removeAllPoles(CyNetwork network) {
        getPoleList(network).clear();
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

    protected void invalidateNetworkCache(CyNetwork network) {
        if (cachedPoleDistances != null && cachedPoleDistances.containsKey(network))
            cachedPoleDistances.get(network).clear();
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
                if (toExplore.contains(n2)) continue;
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

    public Collection<CyNode> getClosestPoles(CyNetwork network, CyNode from) {

        List<CyNode> closestPoles = new ArrayList<>();
        int closestDist = UNREACHABLE_NODE;
        boolean equalDistance = false;

        for (CyNode pole : getPoleList(network)) {

            int dist = getDistanceToPole(network, pole, from);
            if (dist <= closestDist) {
                equalDistance = dist == closestDist;
                if (!equalDistance)
                    closestPoles.clear();
                closestDist = dist;
                closestPoles.add(pole);
            }

        }

        if (closestDist == UNREACHABLE_NODE)
            closestPoles.clear();
        return closestPoles;
    }

    public CyNode getClosestPole(CyNetwork network, CyNode from) {
        Collection<CyNode> closest = getClosestPoles(network, from);
        if (closest.size() == 1)
            return closest.iterator().next();
        return null;
    }

    @Nullable
    public Integer getClosestPoleDistance(CyNetwork network, CyNode from) {
        Collection<CyNode> closest = getClosestPoles(network, from);
        if (closest.size() > 0)
            return getDistanceToPole(network, closest.iterator().next(), from);
        return null;
    }

    public boolean isDisconnected(CyNetwork network, CyNode from) {
        if (from == null) return true;
        return getClosestPoles(network, from).size() == 0;
    }

    public boolean isClosestToMultiple(CyNetwork network, CyNode from) {
        if (from == null) return false;
        return getClosestPoles(network, from).size() > 1;
    }

    public boolean isClosestToOne(CyNetwork network, CyNode from) {
        if (from == null) return false;
        return getClosestPoles(network, from).size() == 1;
    }

    public Collection<CyNode> getAssignedPoles(CyNetwork network, CyEdge edge) {
        Collection<CyNode> p1 = getClosestPoles(network, edge.getSource());
        Collection<CyNode> p2 = getClosestPoles(network, edge.getTarget());
        p1.addAll(p2);
        return new HashSet<>(p1);
    }

    public CyNode getAssignedPole(CyNetwork network, CyEdge edge) {
        Collection<CyNode> closest = getAssignedPoles(network, edge);
        if (isClosestToOne(network, edge))
            return closest.iterator().next();
        return null;
    }

    public CyNode getTargetPole(CyNetwork network, CyEdge edge) {
        if (isClosestToOne(network, edge.getTarget()))
            return getClosestPole(network, edge.getTarget());
        return null;
    }

    public boolean isDisconnected(CyNetwork network, CyEdge edge) {
        if (edge == null) return true;
        return isDisconnected(network, edge.getTarget()) || isDisconnected(network, edge.getSource());
    }

    public boolean isClosestToMultiple(CyNetwork network, CyEdge edge) {
        if (edge == null) return false;
        if (isDisconnected(network, edge))
            return false;
        if (isClosestToMultiple(network, edge.getSource()) || isClosestToMultiple(network, edge.getTarget()))
            return true;
        return getClosestPole(network, edge.getSource()) != getClosestPole(network, edge.getTarget());
    }

    public boolean isClosestToOne(CyNetwork network, CyEdge edge) {
        if (edge == null) return false;
        if (isDisconnected(network, edge))
            return false;
        return !isClosestToMultiple(network, edge);
    }

    protected String getPoleName(CyNetwork network, CyNode pole) {
        return network.getDefaultNodeTable().getRow(pole.getSUID()).get("name", String.class);
    }

    public void updateTables(CyNetwork network) {

        // Network pole lists
        CyTable networkTable = network.getDefaultNetworkTable();
        if (networkTable.getColumn(NAMESPACE, IN_POLE_LIST) == null) {
            networkTable.createListColumn(NAMESPACE, IN_POLE_LIST, String.class, false);
        }
        networkTable.getRow(network.getSUID()).set(NAMESPACE, IN_POLE_LIST, getInPoleNameList(network));

        if (networkTable.getColumn(NAMESPACE, OUT_POLE_LIST) == null) {
            networkTable.createListColumn(NAMESPACE, OUT_POLE_LIST, String.class, false);
        }
        networkTable.getRow(network.getSUID()).set(NAMESPACE, OUT_POLE_LIST, getOutPoleNameList(network));

        CyTable nodeTable = network.getDefaultNodeTable();

        // IS_POLE column
        if (nodeTable.getColumn(NAMESPACE, IS_POLE) == null) {
            nodeTable.createColumn(NAMESPACE, IS_POLE, Boolean.class, false);
        }
        for (CyNode node : network.getNodeList()) {
            nodeTable.getRow(node.getSUID()).set(NAMESPACE, IS_POLE, isPole(network, node));
        }

        // IS_OUTWARDS column
        /*if (nodeTable.getColumn(NAMESPACE, IS_OUTWARDS) == null) {
            nodeTable.createColumn(NAMESPACE, IS_OUTWARDS, Boolean.class, false);
        }
        for (CyNode node : network.getNodeList()) {
            nodeTable.getRow(node.getSUID()).set(NAMESPACE, IS_OUTWARDS, isPoleOutwards(network, node));
        }*/

        // CLOSEST_POLE and DISTANCE_TO_POLE columns
        if (nodeTable.getColumn(NAMESPACE, CLOSEST_POLE) == null) {
            nodeTable.createColumn(NAMESPACE, CLOSEST_POLE, String.class, false);
        }
        if (nodeTable.getColumn(NAMESPACE, DISTANCE_TO_POLE) == null) {
            nodeTable.createColumn(NAMESPACE, DISTANCE_TO_POLE, Integer.class, false);
        }
        if (nodeTable.getColumn(NAMESPACE, IS_DISCONNECTED) == null) {
            nodeTable.createColumn(NAMESPACE, IS_DISCONNECTED, Boolean.class, false);
        }

        for (CyNode node : network.getNodeList()) {
            CyNode closestPole = getClosestPole(network, node);
            boolean isDisconnected = isDisconnected(network, node);
            if (closestPole != null) {
                nodeTable.getRow(node.getSUID()).set(NAMESPACE, CLOSEST_POLE, getPoleName(network, closestPole));
            } else {
                if (isDisconnected)
                    nodeTable.getRow(node.getSUID()).set(NAMESPACE, CLOSEST_POLE, DISCONNECTED_NAME);
                else
                    nodeTable.getRow(node.getSUID()).set(NAMESPACE, CLOSEST_POLE, MULTIPLE_POLES_NAME);
            }
            nodeTable.getRow(node.getSUID()).set(NAMESPACE, DISTANCE_TO_POLE, getClosestPoleDistance(network, node));
            nodeTable.getRow(node.getSUID()).set(NAMESPACE, IS_DISCONNECTED, isDisconnected);
        }

        // EDGE_POLE_INFLUENCE
        CyTable edgeTable = network.getDefaultEdgeTable();

        if (edgeTable.getColumn(NAMESPACE, EDGE_ASSIGNED_POLE) == null) {
            edgeTable.createColumn(NAMESPACE, EDGE_ASSIGNED_POLE, String.class, false);
        }
        for (CyEdge edge : network.getEdgeList()) {
            CyNode pole = getAssignedPole(network, edge);
            if (pole != null)
                edgeTable.getRow(edge.getSUID()).set(NAMESPACE, EDGE_ASSIGNED_POLE, getPoleName(network, pole));
            else {
                if (isDisconnected(network, edge))
                    edgeTable.getRow(edge.getSUID()).set(NAMESPACE, EDGE_ASSIGNED_POLE, DISCONNECTED_NAME);
                else
                    edgeTable.getRow(edge.getSUID()).set(NAMESPACE, EDGE_ASSIGNED_POLE, MULTIPLE_POLES_NAME);
            }
        }

        // EDGE_POLE_TARGET

        if (edgeTable.getColumn(NAMESPACE, EDGE_TARGET_NODE_POLE) == null) {
            edgeTable.createColumn(NAMESPACE, EDGE_TARGET_NODE_POLE, String.class, false);
        }
        for (CyEdge edge : network.getEdgeList()) {
            CyNode pole = getTargetPole(network, edge);
            if (pole != null)
                edgeTable.getRow(edge.getSUID()).set(NAMESPACE, EDGE_TARGET_NODE_POLE, getPoleName(network, pole));
            else {
                if (isDisconnected(network, edge))
                    edgeTable.getRow(edge.getSUID()).set(NAMESPACE, EDGE_TARGET_NODE_POLE, DISCONNECTED_NAME);
                else
                    edgeTable.getRow(edge.getSUID()).set(NAMESPACE, EDGE_TARGET_NODE_POLE, MULTIPLE_POLES_NAME);
            }
        }

        tableInitialized = true;
    }

    @Override
    public void handleEvent(NetworkAddedEvent e) {
        if (e.getNetwork() == null || e.getNetwork().getDefaultNetworkTable() == null) {
            return;
        }
        readPoleListFromTable(e.getNetwork());
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        if (e.getNetwork() == null || e.getNetwork().getDefaultNetworkTable() == null) {
            // Network could be set to null
            return;
        }
        if (getPoleList(e.getNetwork()).size() != 0 || tableInitialized)
            updateTables(e.getNetwork());
    }

    @Override
    public void handleEvent(SessionAboutToBeLoadedEvent e) {
        // This ensures that the user has to prompt adding poles before any new tables are added
        tableInitialized = false;
    }

    public void beginEdit(String operation, CyNetwork network) {
        if (lastEdit != null) completeEdit();
        lastEdit = new PoleManagerEdit(operation, this, network);
        lastEdit.setBefore();
    }

    public void completeEdit() {
        lastEdit.setAfter();
        if (lastEdit.changesPresent())
            undoSupport.postEdit(lastEdit);
        lastEdit = null;
    }

}
