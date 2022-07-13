package ca.usask.vga.layout.magnetic.force;

import ca.usask.vga.layout.magnetic.util.PoleClassifier;
import ca.usask.vga.layout.magnetic.util.Vector;
import org.cytoscape.model.CyEdge;
import org.cytoscape.view.layout.LayoutPartition;
import prefuse.util.force.AbstractForce;
import prefuse.util.force.ForceItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PinForce extends AbstractForce {

    private final PoleClassifier classifier;
    private final Collection<ForceItem> pinnedItems;

    public float soft_pin_factor = 100f;
    public float shape_size;

    protected boolean circlePin, polygonPin;
    protected Vector shapeCenter;

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

    public void setPinAroundCircle(LayoutPartition part, float radius) {
        shape_size = radius;
        circlePin = true;
        shapeCenter = Vector.convert(part.getAverageLocation());
    }

    public void setPinAroundPolygon(LayoutPartition part, float side) {
        shape_size = side;
        polygonPin = true;
        shapeCenter = Vector.convert(part.getAverageLocation());
    }

    protected void pinAroundCircle(Vector center, float radius) {
        Iterator<ForceItem> items = getPinned().iterator();
        int totalNum = getPinnedCount();
        for (int i = 0; i < totalNum && items.hasNext(); i++) {
            ForceItem item = items.next();
            if (totalNum == 1) radius = 0;
            float x = (float) Math.cos(2*Math.PI*i/totalNum)*radius;
            float y = (float) -Math.sin(2*Math.PI*i/totalNum)*radius;
            savedPolePos.put(item, new Vector(x, y).add(center));
        }
    }

    protected void pinAroundPolygon(Vector center, float side) {
        float radius = side / (2*(float) Math.sin(Math.PI/getPinnedCount()));
        pinAroundCircle(center, radius);
    }

    public float getSuggestedRadius(LayoutPartition part) {
        return (float) Math.max(part.getWidth(), part.getHeight()) / 2;
    }

    public boolean isPinned(ForceItem item) {
        return (classifier != null && classifier.isPole(item)) ||
                (pinnedItems != null && pinnedItems.contains(item));
    }

    public Iterable<ForceItem> getPinned() {
        if (classifier != null) return classifier.getPoleListSorted(getSortType());
        return pinnedItems;
    }

    public int getPinnedCount() {
        if (classifier != null) return classifier.getPoleListSize();
        if (pinnedItems != null) return pinnedItems.size();
        return 0;
    }

    protected CyEdge.Type getSortType() {
        if (classifier == null) return CyEdge.Type.ANY;
        Iterator<ForceItem> poles = classifier.getPoleList().iterator();
        boolean isOut = false, isIn = false;
        while (poles.hasNext()) {
            if (classifier.isPoleOutwards(poles.next()))
                isOut = true;
            else
                isIn = true;
        }
        if (isOut == isIn)
            return CyEdge.Type.ANY;
        else if (isOut)
            return CyEdge.Type.OUTGOING;
        else
            return CyEdge.Type.INCOMING;
    }

    @Override
    public void getForce(ForceItem item) {

        if(isPinned(item)) {

            Vector currentPos = new Vector(item.location[0], item.location[1]);

            if (!savedPolePos.containsKey(item)) {
                savedPolePos.put(item, currentPos);
                if (circlePin)
                    pinAroundCircle(shapeCenter, shape_size);
                if (polygonPin)
                    pinAroundPolygon(shapeCenter, shape_size);
                return;
            }

            Vector originalPos = savedPolePos.get(item);
            Vector disp = originalPos.subtract(currentPos).times(soft_pin_factor);

            item.force[0] += disp.x;
            item.force[1] += disp.y;

        }

    }
}
