package ca.usask.vga.layout.magnetic.util;

public enum FieldType {

    HORIZONTAL("Linear (horizontal)", new FieldFunction() {
        public Vector getFieldAt(Vector pos) {
            return new Vector(1, 0);
        }
    }),

    VERTICAL("Linear (vertical)", new FieldFunction() {
        public Vector getFieldAt(Vector pos) {
            return new Vector(0, 1);
        }
    }),

    POLAR("Polar (outwards)", new FieldFunction() {
        public Vector getFieldAt(Vector pos) {
            return pos.normalized();
        }
    }),

    CONCENTRIC("Concentric (anticlockwise)", new FieldFunction() {
        public Vector getFieldAt(Vector pos) {
            return pos.normalized().rotate90clockwise();
        }
    });

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