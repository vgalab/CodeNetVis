package ca.usask.vga.layout.magnetic.util;

import static java.lang.Math.*;

import org.cytoscape.view.layout.LayoutPoint;
import org.jetbrains.annotations.NotNull;

public class Vector {

    public final float x, y;

    public Vector(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public static Vector convert(LayoutPoint point) {
        return new Vector((float) point.getX(), (float) point.getY());
    }

    public static Vector convert(float[] point) {
        return new Vector(point[0], point[1]);
    }

    public Vector() {
        this(0,0);
    }

    // Basic properties

    float magnitude()
    {
        return (float) sqrt(x*x + y*y);
    }

    Vector displacement(@NotNull Vector to) {
        return new Vector(to.x - x, to.y - y);
    }

    float distance(@NotNull Vector to)
    {
        return displacement(to).magnitude();
    }

    Vector normalized()
    {
        // Prevents division by zero
        if (magnitude() == 0)
            return new Vector(0, 0);
        return new Vector(this.x / magnitude(), this.y / magnitude());
    }

    // Scalar operations

    Vector times(float b)
    {
        return new Vector(this.x * b, this.y * b);
    }

    Vector divide(float b)
    {
        return new Vector(this.x / b, this.y / b);
    }

    // Vector operations

    Vector add(@NotNull Vector b)
    {
        return new Vector(this.x + b.x, this.y + b.y);
    }


    Vector subtract(@NotNull Vector b)
    {
        return new Vector(this.x - b.x, this.y - b.y);
    }

    float dot(Vector b)
    {
        return this.x * b.x + this.y * b.y;
    }

    float cross(Vector b)
    {
        return this.x * b.y - this.y * b.x;
    }

    float angleCos(Vector b)
    {
        if (this.magnitude() == 0 || b.magnitude() == 0)
            return 0;
        float dot_product = this.dot(b) / (this.magnitude() * b.magnitude());
        float clamped_dot_product = min(max(dot_product, -1.0f), 1.0f);
        return (float) acos(clamped_dot_product);
    }

    float angleSin(Vector b)
    {
        if (this.magnitude() == 0 || b.magnitude() == 0)
            return 0;
        float cross_product = this.cross(b) / (this.magnitude() * b.magnitude());
        float clamped_cross_product = min(max(cross_product, -1.0f), 1.0f);
        return (float) asin(clamped_cross_product);
    }

    Vector rotate90clockwise()
    {
        //noinspection SuspiciousNameCombination
        return new Vector(this.y, -this.x);
    }

    // Number operations

    static int sign(float x) {
        return Float.compare(x, 0);
    }

    static float powf(float x, float exp) {
        if (exp == 0) return 1;
        if (exp == 1) return x;
        return (float) pow(x, exp);
    }

}
