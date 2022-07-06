package ca.usask.vga.layout.magnetic.util;

import prefuse.util.force.AbstractForce;
import prefuse.util.force.ForceItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PinForce extends AbstractForce {

    private final PoleClassifier classifier;
    private final Collection<ForceItem> pinnedItems;

    public float soft_pin_factor = 0.1f;

    private final Map<ForceItem, Vector> savedPolePos = new HashMap<>();

    public PinForce(PoleClassifier classifier) {
        this.classifier = classifier;
        this.pinnedItems = null;
    }

    public PinForce(Collection<ForceItem> pinnedItems) {
        this.pinnedItems = pinnedItems;
        classifier = null;
    }

    @Override
    protected String[] getParameterNames() {
        return new String[0];
    }

    @Override
    public boolean isItemForce() {
        return true;
    }

    public boolean isPinned(ForceItem item) {
        return (classifier != null && classifier.isPole(item)) ||
                (pinnedItems != null && pinnedItems.contains(item));
    }

    @Override
    public void getForce(ForceItem item) {

        if(isPinned(item)) {

            Vector currentPos = new Vector(item.location[0], item.location[1]);

            if (!savedPolePos.containsKey(item)) {
                savedPolePos.put(item, currentPos);
                return;
            }

            Vector originalPos = savedPolePos.get(item);
            Vector disp = originalPos.subtract(currentPos).times(soft_pin_factor);

            item.force[0] += disp.x;
            item.force[1] += disp.y;

        }

    }
}
