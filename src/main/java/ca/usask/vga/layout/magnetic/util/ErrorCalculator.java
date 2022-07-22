package ca.usask.vga.layout.magnetic.util;

import ca.usask.vga.layout.magnetic.force.MagneticForce;
import org.cytoscape.work.TaskMonitor;
import org.jetbrains.annotations.Nullable;
import prefuse.util.force.ForceItem;
import prefuse.util.force.ForceSimulator;
import prefuse.util.force.Spring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Used to calculate alignment statistics and error for a {@link ForceSimulator} layout.
 * Optionally needs the {@link MagneticForce} to calculate directed edge misalignment.
 */
public class ErrorCalculator {

    private final ForceSimulator m_fsim;
    private final MagneticForce magneticForce;

    private static final int NODE_X = 0, NODE_Y = 1, NODE_FORCE = 2, NODE_VELOCITY = 3;
    private static final int EDGE_MISALIGNMENT = 0;

    private Statistics<ForceItem> nodeStat;
    private Statistics<Spring> edgeStat;

    public ErrorCalculator(ForceSimulator m_fsim, @Nullable MagneticForce magneticForce) {
        this.m_fsim = m_fsim;
        this.magneticForce = magneticForce;
        nodeStat = new Statistics<>(4);
        edgeStat = new Statistics<>(1);
    }

    protected static class Statistics<T> {
        public Statistics(int count, float[] min, float[] max, float[] mean, float[] deviation) {
            this.count = count;
            this.min = min;
            this.max = max;
            this.mean = mean;
            this.deviation = deviation;
        }
        public Statistics(int measure_total) {
            this.count = 0;
            min = new float[measure_total];
            max = new float[measure_total];
            mean = new float[measure_total];
            deviation = new float[measure_total];
        }
        int count;
        float[] min, max, mean, deviation;
    }

    public void recalculate() {
        List<Function<ForceItem, Float>> nodeFunc = new ArrayList<>();
        nodeFunc.add(item -> item.location[0]);
        nodeFunc.add(item -> item.location[1]);
        nodeFunc.add(item -> Vector.convert(item.force).magnitude());
        nodeFunc.add(item -> Vector.convert(item.velocity).magnitude());
        nodeStat = calculateStatistics(m_fsim.getItems(), nodeFunc);

        List<Function<Spring, Float>> edgeFunc = new ArrayList<>();
        if (magneticForce != null)
            edgeFunc.add(magneticForce::getEdgeMisalignment);
        else
            edgeFunc.add(s -> 0f);
        edgeStat = calculateStatistics(m_fsim.getSprings(), edgeFunc);
    }

    public long totalEdges() {
        return edgeStat.count;
    }

    public long totalNodes() {
        return nodeStat.count;
    }

    public long misalignedEdges(float threshold) {
        if (magneticForce == null) return 0;
        return countIf(m_fsim.getSprings(), (s) -> magneticForce.getEdgeMisalignment(s) >= threshold);
    }

    public float percentOfMisaligned(float threshold) {
        return 100f * misalignedEdges(threshold) / totalEdges();
    }

    public float misalignmentMean() {
        return edgeStat.mean[EDGE_MISALIGNMENT];
    }

    public float misalignmentSD() {
        return edgeStat.deviation[EDGE_MISALIGNMENT];
    }

    public float forceMean() {
        return nodeStat.mean[NODE_FORCE];
    }

    public float forceSD() {
        return nodeStat.deviation[NODE_FORCE];
    }

    public float velocityMean() {
        return nodeStat.mean[NODE_VELOCITY];
    }

    public float velocitySD() {
        return nodeStat.deviation[NODE_VELOCITY];
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
        return nodeStat.min[NODE_X];
    }
    public float minY() {
        return nodeStat.min[NODE_Y];
    }
    public float maxX() {
        return nodeStat.max[NODE_X];
    }
    public float maxY() {
        return nodeStat.max[NODE_Y];
    }

    public float meanX() {
        return nodeStat.mean[NODE_X];
    }
    public float meanY() {
        return nodeStat.mean[NODE_Y];
    }

    public float sdX() {
        return nodeStat.deviation[NODE_X];
    }
    public float sdY() {
        return nodeStat.deviation[NODE_Y];
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

    public <T> Statistics<T> calculateStatistics(Iterator<T> iterator, List<Function<T, Float>> measures) {
        int measures_length = measures.size();
        int count = 0;
        float[] total = new float[measures_length];
        float[] total_squared = new float[measures_length];
        float[] min = new float[measures_length], max = new float[measures_length];
        while (iterator.hasNext()) {
            T t = iterator.next();
            count++;
            for (int i = 0; i < measures_length; i++) {
                float val = measures.get(i).apply(t);
                total[i] += val;
                total_squared[i] += val*val;
                min[i] = Math.min(min[i], val);
                max[i] = Math.max(max[i], val);
                if (count == 1) {
                    min[i] = val;
                    max[i] = val;
                }
            }
        }
        float[] mean = new float[measures_length];
        float[] deviation = new float[measures_length];
        for (int i = 0; i < measures_length; i++) {
            mean[i] = total[i] / count;
            deviation[i] = (float) Math.sqrt(total_squared[i] / count - mean[i] * mean[i]);
        }
        return new Statistics<>(count, min, max, mean, deviation);
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
