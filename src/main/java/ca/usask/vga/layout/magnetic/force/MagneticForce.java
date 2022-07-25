package ca.usask.vga.layout.magnetic.force;

import ca.usask.vga.layout.magnetic.util.PoleClassifier;
import ca.usask.vga.layout.magnetic.util.Vector;
import prefuse.util.force.*;

import static ca.usask.vga.layout.magnetic.util.Vector.powf;
import static ca.usask.vga.layout.magnetic.util.Vector.sign;


/**
 * Magnetic force aligns directed edges with a magnetic field.
 * Requires a {@link PoleClassifier} if the edges should be aligned with poles.
 */
public class MagneticForce extends AbstractForce {

    float field_strength, alpha, beta;
    boolean bi_directional = false;
    FieldType field_type;

    final PoleClassifier classifier;
    final boolean usePoles;

    /**
     * Constructor for the {@link MagneticForce} class.
     * Uses a simplified version of the Magnetic Force calculation.
     * @param field_type Magnetic field type to use
     * @param field_strength Strength of the magnetic field
     * @param alpha Exponent of the distance term
     * @param beta Exponent of the angle term
     */
    public MagneticForce(FieldType field_type, float field_strength, float alpha, float beta) {
        this.field_type = field_type;
        this.field_strength = field_strength;
        this.alpha = alpha;
        this.beta = beta;
        classifier = null;
        usePoles = false;
    }

    /**
     * Constructor for the {@link MagneticForce} class.
     * Uses multiple poles to calculate the magnetic field.
     * @param classifier Pole classifier to use
     * @param field_strength Strength of the magnetic field
     * @param alpha Exponent of the distance term
     * @param beta Exponent of the angle term
     */
    public MagneticForce(PoleClassifier classifier, float field_strength, float alpha, float beta) {
        this.field_type = null;
        this.field_strength = field_strength;
        this.alpha = alpha;
        this.beta = beta;
        this.classifier = classifier;
        this.usePoles = true;
    }

    @Override
    protected String[] getParameterNames() {
        // TODO: Parameter names
        return new String[0];
    }

    /**
     * Returns true.
     * @see prefuse.util.force.Force#isSpringForce()
     */
    public boolean isSpringForce() {
        return true;
    }

    /**
     * Calculates the force vector acting on the items due to the magnetic force.
     * @param s the Spring (Edge) for which to compute the force
     * @see prefuse.util.force.Force#getForce(prefuse.util.force.Spring)
     */
    public void getForce(Spring s) {

        // Initialize variables
        ForceItem item1 = s.item1;
        ForceItem item2 = s.item2;

        Vector pos_n = new Vector(item1.location[0], item1.location[1]);
        Vector pos_t = new Vector(item2.location[0], item2.location[1]);

        Vector disp = pos_n.displacement(pos_t);
        float dist = disp.magnitude();

        if (dist == 0.0)
            // No force is applied
            return;

        Vector field_direction;
        Vector midpoint = getCenter(pos_n, pos_t);

        if (!usePoles) {
            field_direction = field_type.getFieldAt(midpoint);
        } else {
            field_direction = getMultiPoleFieldFor(midpoint, s);
        }

        int edge_dir = 1; // Edge direction is always the same since item2 is the target
        field_direction = field_direction.times(edge_dir);

        if (field_direction.magnitude() == 0.0)
            return; // Cannot compute the angle when zero

        // CALCULATE FORCE
        Vector force_on_n = magnetic_equation(field_direction, disp, field_strength, alpha, beta);

        Vector force_on_t = force_on_n.times(-1);

        item1.force[0] += force_on_n.x;
        item1.force[1] += force_on_n.y;
        item2.force[0] += force_on_t.x;
        item2.force[1] += force_on_t.y;
    }

    /**
     * Calculates the force vector acting on the nodes due to the magnetic force.
     * @param m Magnetic field vector
     * @param d Vector direction of the edge
     * @param b Strength of the magnetic field
     * @param alpha Exponent of the distance term
     * @param beta Exponent of the angle term
     * @return Vector representing the force on the node
     */
    private Vector magnetic_equation(Vector m, Vector d, float b, float alpha, float beta)
    {
        float dist = (float) Math.sqrt(d.magnitude());
        Vector force_on_n;
        if (!bi_directional)
            force_on_n = d.rotate90clockwise().times(-(b * powf(dist, alpha-1) *
                    powf(m.angleCos(d), beta) * sign(m.cross(d))));
        else
            force_on_n = d.rotate90clockwise().times(-(b * powf(dist, alpha-1) *
                    powf(Math.abs(m.angleSin(d)), beta) * sign(m.cross(d) * m.dot(d))));
        return force_on_n;
    }

    /**
     * Calculates magnetic field direction towards the closest pole.
     * @param pos Position of the center of the spring
     * @param edge Edge that is to be evaluated
     * @return Magnetic field direction
     */
    public Vector getMultiPoleFieldFor(Vector pos, Spring edge) {

        if (classifier == null)
            return new Vector();

        if (!classifier.isClosestToOne(edge))
            return new Vector();

        ForceItem closestPole = classifier.poleOf(edge);
        assert closestPole != null;

        Vector polePos = new Vector(closestPole.location[0], closestPole.location[1]);

        Vector disp = polePos.subtract(pos);
        if (classifier.isPoleOutwards(closestPole))
            disp = disp.times(-1);
        return disp;
    }

    /**
     * Returns the center of the spring given the two nodes.
     * @param from Position of the source node
     * @param to Position of the target node
     * @return Vector representing the center position of the edge
     */
    protected Vector getCenter(Vector from, Vector to) {
        // Midpoint (other center calculations can be added)
        return from.add(to).times(0.5f);
    }

    /**
     * Utility function to calculate edge misalignment.
     * @param s Spring to be evaluated
     * @return Angle between the edge and the magnetic field
     */
    public float getEdgeMisalignment(Spring s) {

        ForceItem item1 = s.item1;
        ForceItem item2 = s.item2;

        Vector pos_n = new Vector(item1.location[0], item1.location[1]);
        Vector pos_t = new Vector(item2.location[0], item2.location[1]);

        Vector disp = pos_n.displacement(pos_t);

        Vector field_direction;
        Vector midpoint = getCenter(pos_n, pos_t);

        if (!usePoles) {
            field_direction = field_type.getFieldAt(midpoint);
        } else {
            field_direction = getMultiPoleFieldFor(midpoint, s);
        }

        float angle = field_direction.angleCos(disp);
        return Math.abs(angle);

    }

}
