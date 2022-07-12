package ca.usask.vga.layout.magnetic.util;

import org.cytoscape.view.layout.LayoutPartition;
import org.jetbrains.annotations.NotNull;
import prefuse.util.force.AbstractForce;
import prefuse.util.force.ForceItem;

public class HierarchyForce extends AbstractForce {

    public enum Type {
        NONE("None"),
        BASED_ON_HOP_DISTANCE("Based on hop distance"),
        SINE_FUNCTION("Sine function rings");

        private final String name;
        Type(String str) {
            this.name = str;
        }
        public String toString() {
            return this.name;
        }
    }

    private final PoleClassifier classifier;

    final Type type;
    final float forceStrength;
    final float hierarchyRadius;

    public HierarchyForce(@NotNull PoleClassifier classifier, Type type, float forceStrength, float hierarchyRadius) {
        this.classifier = classifier;
        this.type = type;
        this.forceStrength = forceStrength;
        this.hierarchyRadius = hierarchyRadius;
    }

    @Override
    protected String[] getParameterNames() {
        return new String[0];
    }

    @Override
    public boolean isItemForce() {
        return true;
    }

    public static float getSuggestedRadius(LayoutPartition part) {
        float circle_radius = (float) Math.max(part.getWidth(), part.getHeight()) / 2;
        return circle_radius / 5; // Enough for 5 rings
    }

    @Override
    public void getForce(ForceItem item) {

        if (type == Type.NONE) return;

        ForceItem pole = classifier.closestPole(item);
        if (pole == null) return;

        Vector nodePos = Vector.convert(item.location);
        Vector polePos = Vector.convert(pole.location);
        Vector disp = nodePos.subtract(polePos);
        float dist = disp.magnitude();

        Vector force = new Vector();

        if (type == Type.BASED_ON_HOP_DISTANCE) {
            int hop_distance = classifier.closestPoleDistance(item);
            float desired_dist = hop_distance * hierarchyRadius;
            force = disp.normalized().times((desired_dist - dist) * forceStrength);
        } else if (type == Type.SINE_FUNCTION) {
            if (dist < hierarchyRadius * 0.75) {
                // Do not attract to the pole
                force = disp.normalized().times(forceStrength / (0.75f));
            } else {
                force = disp.normalized().times((float) Math.sin(2 * Math.PI * dist / hierarchyRadius)
                        * forceStrength * (-hierarchyRadius / dist));
            }
        }

        item.force[0] += force.x;
        item.force[1] += force.y;
    }
}
