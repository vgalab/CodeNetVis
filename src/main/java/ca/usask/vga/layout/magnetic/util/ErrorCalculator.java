package ca.usask.vga.layout.magnetic.util;

import ca.usask.vga.layout.magnetic.force.MagneticForce;
import org.cytoscape.work.TaskMonitor;
import prefuse.util.force.ForceItem;
import prefuse.util.force.ForceSimulator;
import prefuse.util.force.Spring;

import java.util.Iterator;

/**
 * Used to calculate alignment statistics and error for a {@link ForceSimulator} layout.
 */
public class ErrorCalculator {

    private final ForceSimulator m_fsim;
    private final MagneticForce magneticForce;

    private int _totalEdges = -1;
    private int _totalNodes = -1;

    public ErrorCalculator(ForceSimulator m_fsim, MagneticForce magneticForce) {
        this.m_fsim = m_fsim;
        this.magneticForce = magneticForce;
    }

    public int totalEdges() {
        if (_totalEdges >= 0) return _totalEdges;
        _totalEdges = 0;
        Iterator<Spring> springs = m_fsim.getSprings();
        while (springs.hasNext()) {
            springs.next();
            _totalEdges++;
        }
        return _totalEdges;
    }

    public int totalNodes() {
        if (_totalNodes >= 0) return _totalNodes;
        _totalNodes = 0;
        Iterator<Spring> springs = m_fsim.getSprings();
        while (springs.hasNext()) {
            springs.next();
            _totalNodes++;
        }
        return _totalNodes;
    }

    public int misalignedEdges(float threshold) {
        int count = 0;
        Iterator<Spring> springs = m_fsim.getSprings();
        while (springs.hasNext()) {
            Spring s = springs.next();
            if (magneticForce.getEdgeMisalignment(s) >= threshold)
                count++;
        }
        return count;
    }

    public float percentOfMisaligned(float threshold) {
        return 100f * misalignedEdges(threshold) / totalEdges();
    }

    public float misalignmentMean() {
        float total = 0;
        Iterator<Spring> springs = m_fsim.getSprings();
        while (springs.hasNext()) {
            Spring s = springs.next();
            total += magneticForce.getEdgeMisalignment(s);
        }
        return total / totalEdges();
    }

    public float misalignmentSD() {
        float sd = 0;
        float mean = misalignmentMean();
        Iterator<Spring> springs = m_fsim.getSprings();
        while (springs.hasNext()) {
            float val = magneticForce.getEdgeMisalignment(springs.next());
            sd += (val - mean) * (val - mean);
        }
        return (float) Math.sqrt(sd / totalEdges());
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
        float F_mean = degrees(forceMean());
        float F_sd = degrees(forceSD());
        char deg = '\u00B0';
        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Total edges: " + totalEdges());
        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Misaligned edges (> 60"+deg+"): " + formatFloat(M_percent) + "%");
        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Angle mean = " + formatFloat(M_mean) + deg + ", sd = " + formatFloat(M_sd) + deg);
        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Force mean = " + formatFloat(F_mean) + ", sd = " + formatFloat(F_sd));
    }

    public float forceMean() {
        float totalForce = 0;
        Iterator<ForceItem> items = m_fsim.getItems();
        while (items.hasNext()) {
            totalForce += Vector.convert(items.next().force).magnitude();
        }
        return totalForce / totalNodes();
    }

    public float forceSD() {
        float sd = 0;
        float mean = forceMean();
        Iterator<ForceItem> items = m_fsim.getItems();
        while (items.hasNext()) {
            float val = Vector.convert(items.next().force).magnitude();
            sd += (val - mean) * (val - mean);
        }
        return (float) Math.sqrt(sd / totalNodes());
    }


}
