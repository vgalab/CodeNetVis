package ca.usask.vga.layout.magnetic.force;

import ca.usask.vga.layout.magnetic.util.Vector;
import org.cytoscape.view.layout.LayoutPoint;
import prefuse.util.force.AbstractForce;
import prefuse.util.force.ForceItem;

/**
 * Simple gravity force that attracts nodes to the center of the layout.
 * The force gets stronger as the distance from the center increases.
 */
public class GravityForce extends AbstractForce {

    public float gravityConstant = 0.1f;
    public Vector centerPos;

    public GravityForce(LayoutPoint center, float gravityConstant) {
        this.gravityConstant = gravityConstant;
        centerPos = Vector.convert(center);
    }

    public GravityForce(Vector centerPos, float gravityConstant) {
        this.gravityConstant = gravityConstant;
        this.centerPos = centerPos;
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

        Vector disp = centerPos.subtract(nodePos).times(gravityConstant);

        item.force[0] += disp.x;
        item.force[1] += disp.y;

    }
}
