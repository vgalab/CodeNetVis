package ca.usask.vga.layout.magnetic.force;

import ca.usask.vga.layout.magnetic.util.PoleClassifier;
import ca.usask.vga.layout.magnetic.util.Vector;
import org.jetbrains.annotations.NotNull;
import prefuse.util.force.AbstractForce;
import prefuse.util.force.ForceItem;

/**
 * Pole gravity force attracts nodes to their corresponding poles.
 * The force gets stronger as the distance from the center increases.
 * Requires a {@link PoleClassifier} to be provided.
 * @see GravityForce
 */
public class PoleGravityForce extends AbstractForce {

    private final PoleClassifier classifier;

    public float gravityConstant = 0.1f;

    public PoleGravityForce(@NotNull PoleClassifier classifier, float gravityConstant) {
        this.classifier = classifier;
        this.gravityConstant = gravityConstant;
    }

    @Override
    protected String[] getParameterNames() {
        return new String[0];
    }

    @Override
    public boolean isItemForce() {
        return true;
    }

    @Override
    public void getForce(ForceItem item) {

        Vector nodePos = new Vector(item.location[0], item.location[1]);

        ForceItem pole = classifier.closestPole(item);

        if (pole == null) return;

        int distanceToPole = classifier.closestPoleDistance(item);

        if (distanceToPole == 0) return;

        Vector polePos = new Vector(pole.location[0], pole.location[1]);

        Vector disp = polePos.subtract(nodePos).times(gravityConstant).divide(distanceToPole);

        item.force[0] += disp.x;
        item.force[1] += disp.y;

    }
}

