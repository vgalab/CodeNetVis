package ca.usask.vga.layout.magnetic.force;

import ca.usask.vga.layout.magnetic.util.Vector;


/**
 * Defines different field types for the simplified {@link MagneticForce} calculation.
 * Each element is a Vector -> Vector function.
 */
public enum FieldType {

    HORIZONTAL("Linear (horizontal)", pos -> new Vector(1, 0)),

    VERTICAL("Linear (vertical)", pos -> new Vector(0, 1)),

    POLAR("Polar (outwards)", pos -> pos.normalized()),

    CONCENTRIC("Concentric (anticlockwise)", pos -> pos.normalized().rotate90clockwise());

    private final String name;
    private final FieldFunction function;

    FieldType(String str, FieldFunction function) {
        this.name = str;
        this.function = function;
    }

    public String toString() {
        return this.name;
    }

    public Vector getFieldAt(Vector pos) {
        return function.getFieldAt(pos);
    }

    public interface FieldFunction {
        Vector getFieldAt(Vector pos);
    }
}