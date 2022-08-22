package ca.usask.vga.layout.magnetic.poles;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.undo.AbstractCyEdit;

import java.util.*;

/**
 * Implements the undo/redo functionality for changes to the pole list.
 * Use {@link PoleManager#beginEdit pm.beginEdit()} and
 * {@link PoleManager#completeEdit pm.completeEdit()} of the PoleManager
 * to enable this functionality for new actions.
 */
public class PoleManagerEdit extends AbstractCyEdit {

    private final PoleManager poleManager;
    private final CyNetwork network;
    protected Map<CyNetwork, List<CyNode>> poleListBefore;
    protected Set<CyNode> poleIsOutwardsBefore;

    protected Map<CyNetwork, List<CyNode>> poleListAfter;
    protected Set<CyNode> poleIsOutwardsAfter;

    protected PoleManagerEdit(String presentationName, PoleManager poleManager, CyNetwork network) {
        super(presentationName);
        this.poleManager = poleManager;
        this.network = network;
    }

    protected void setBefore() {
        poleListBefore = copyMap(poleManager.poleList);
        poleIsOutwardsBefore = copySet(poleManager.poleIsOutwards);
        poleListBefore.computeIfAbsent(network, k -> new ArrayList<>());
    }

    protected void setAfter() {
        poleListAfter = copyMap(poleManager.poleList);
        poleIsOutwardsAfter = copySet(poleManager.poleIsOutwards);
        poleListAfter.computeIfAbsent(network, k -> new ArrayList<>());
    }

    protected boolean changesPresent() {
        if (!poleListBefore.get(network).equals(poleListAfter.get(network)))
            return true;
        return !poleIsOutwardsBefore.equals(poleIsOutwardsAfter);
    }

    private Map<CyNetwork, List<CyNode>> copyMap(Map<CyNetwork, List<CyNode>> og) {
        Map<CyNetwork, List<CyNode>> map = new HashMap<>();
        for (CyNetwork n : og.keySet()) {
            map.put(n, new ArrayList<>(og.get(n)));
        }
        return map;
    }

    private Set<CyNode> copySet(Set<CyNode> og) {
        return new HashSet<>(og);
    }

    @Override
    public void undo() {
        poleManager.poleList = copyMap(poleListBefore);
        poleManager.poleIsOutwards = copySet(poleIsOutwardsBefore);
        poleManager.invalidateNetworkCache(network);
        poleManager.updateTables(network);
    }

    @Override
    public void redo() {
        poleManager.poleList = copyMap(poleListAfter);
        poleManager.poleIsOutwards = copySet(poleIsOutwardsAfter);
        poleManager.invalidateNetworkCache(network);
        poleManager.updateTables(network);
    }

}
