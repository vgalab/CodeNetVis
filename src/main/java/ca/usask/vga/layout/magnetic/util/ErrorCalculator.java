package ca.usask.vga.layout.magnetic.util;

import ca.usask.vga.layout.magnetic.force.MagneticForce;
import org.cytoscape.work.TaskMonitor;
import prefuse.util.force.ForceSimulator;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Used to calculate alignment statistics and error for a {@link ForceSimulator} layout.
 */
public class ErrorCalculator {

    private final ForceSimulator m_fsim;
    private final MagneticForce magneticForce;

    public ErrorCalculator(ForceSimulator m_fsim, MagneticForce magneticForce) {
        this.m_fsim = m_fsim;
        this.magneticForce = magneticForce;
    }

    public long totalEdges() {
        return count(m_fsim.getSprings());
    }

    public long totalNodes() {
        return count(m_fsim.getItems());
    }

    public long misalignedEdges(float threshold) {
        if (magneticForce == null) return 0;
        return countIf(m_fsim.getSprings(), (s) -> magneticForce.getEdgeMisalignment(s) >= threshold);
    }

    public float percentOfMisaligned(float threshold) {
        return 100f * misalignedEdges(threshold) / totalEdges();
    }

    public float misalignmentMean() {
        if (magneticForce == null) return 0;
        return mean(m_fsim.getSprings(), magneticForce::getEdgeMisalignment);
    }

    public float misalignmentSD() {
        if (magneticForce == null) return 0;
        return sd(m_fsim.getSprings(), magneticForce::getEdgeMisalignment, misalignmentMean());
    }

    public float forceMean() {
        return mean(m_fsim.getItems(), (item) -> Vector.convert(item.force).magnitude());
    }

    public float forceSD() {
        return sd(m_fsim.getItems(), (item) -> Vector.convert(item.force).magnitude(), forceMean());
    }

    public float velocityMean() {
        return mean(m_fsim.getItems(), (item) -> Vector.convert(item.velocity).magnitude());
    }

    public float velocitySD() {
        return sd(m_fsim.getItems(), (item) -> Vector.convert(item.velocity).magnitude(), velocityMean());
    }

    public float degrees(float radians) {
        return (float) Math.toDegrees(radians);
    }

    protected String formatFloat(float x) {
        return String.format("%.2f", x);
    }

    protected String formatExp(float x) {
        return String.format("%.1e", x);
    }


    public void displayResults(TaskMonitor taskMonitor) {
        float M_percent = percentOfMisaligned((float) Math.PI / 3);
        float M_mean = degrees(misalignmentMean());
        float M_sd = degrees(misalignmentSD());
        float F_mean = forceMean();
        float F_sd = forceSD();
        float V_mean = velocityMean();
        float V_sd = velocitySD();
        char deg = '\u00B0';
        // TODO: Remove excessive information
        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Total edges: " + totalEdges());
        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Misaligned edges (> 60"+deg+"): " + formatFloat(M_percent) + "%");
        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Angle mean = " + formatFloat(M_mean) + deg + ", sd = " + formatFloat(M_sd) + deg);
        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Force mean = " + formatExp(F_mean) + ", sd = " + formatFloat(F_sd));
        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Speed mean = " + formatExp(V_mean) + ", sd = " + formatFloat(V_sd));
        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Aspect ratio = " + formatFloat(aspectRatio()));
        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Area = " + formatExp(area()) + ", frame area = " + formatExp(frameArea()));
        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Density = " + formatExp(density()));
        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "XY SD = " + sdXY() + ", desired = " + uniformSD(0, 1));

    }

    public float minX() {
        return min(m_fsim.getItems(), item -> item.location[0]);
    }
    public float minY() {
        return min(m_fsim.getItems(), item -> item.location[1]);
    }
    public float maxX() {
        return max(m_fsim.getItems(), item -> item.location[0]);
    }
    public float maxY() {
        return max(m_fsim.getItems(), item -> item.location[1]);
    }

    public float meanX() {
        return mean(m_fsim.getItems(), item -> item.location[0]);
    }
    public float meanY() {
        return mean(m_fsim.getItems(), item -> item.location[1]);
    }

    public float sdX() {
        return sd(m_fsim.getItems(), item -> item.location[0], meanX());
    }
    public float sdY() {
        return sd(m_fsim.getItems(), item -> item.location[1], meanY());
    }

    public float sdXY() {
        return Math.max(sdX() / frameWidth(), sdY() / frameHeight());
    }

    public float uniformSD(float min, float max) {
        return (max - min) / (float) Math.sqrt(12f);
    }

    public float uniformMean(float min, float max) {
        return (max + min) / 2;
    }

    public float frameWidth() {
        return maxX()-minX();
    }
    public float frameHeight() {
        return maxY()-minY();
    }
    public float frameSize() {
        return Math.max(frameWidth(), frameHeight());
    }

    public float aspectRatio() {
        return frameWidth() / frameHeight();
    }
    public float area() { return frameWidth() * frameHeight(); }
    public float frameArea() { return (float) Math.pow(frameSize(), 2); }
    public float density() { return totalNodes() / frameArea(); }

    public float aspectRatioError(float desiredAR) {
        return ratioError(aspectRatio(), desiredAR);
    }

    public float densityError(float desiredDensity) {
        return ratioError(density(), desiredDensity);
    }

    public float ratioError(float current, float desired) {
        return current < desired ? desired / current : current / desired; //-1
        // TODO: Check implementation
        //return (1 / (current * desired)) *  (current - desired) * (current - desired);
    }

    public <T> long count(Iterator<T> iterator) {
        int total = 0;
        while (iterator.hasNext()) {
            iterator.next();
            total++;
        }
        return total;
    }

    public <T> long countIf(Iterator<T> iterator, Function<T, Boolean> condition) {
        int total = 0;
        while (iterator.hasNext()) {
            if (condition.apply(iterator.next()))
                total++;
        }
        return total;
    }

    public <T> float mean(Iterator<T> iterator, Function<T, Float> valueOf) {
        float sum = 0;
        int total = 0;
        while (iterator.hasNext()) {
            sum += valueOf.apply(iterator.next());
            total++;
        }
        return sum / total;
    }

    public <T> float sd(Iterator<T> iterator, Function<T, Float> valueOf, float mean) {
        float sd = 0;
        int total = 0;
        while (iterator.hasNext()) {
            float val = valueOf.apply(iterator.next());
            sd += (val - mean) * (val - mean);
            total++;
        }
        return (float) Math.sqrt(sd / total);
    }

    public <T> float min(Iterator<T> iterator, Function<T, Float> valueOf) {
        float minVal = Float.POSITIVE_INFINITY;
        while (iterator.hasNext()) {
            minVal = Math.min(minVal, valueOf.apply(iterator.next()));
        }
        return minVal;
    }

    public <T> float max(Iterator<T> iterator, Function<T, Float> valueOf) {
        float maxVal = Float.NEGATIVE_INFINITY;
        while (iterator.hasNext()) {
            maxVal = Math.max(maxVal, valueOf.apply(iterator.next()));
        }
        return maxVal;
    }

}
