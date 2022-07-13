package ca.usask.vga.layout.magnetic.util;

import ca.usask.vga.layout.magnetic.force.MagneticForce;
import org.cytoscape.work.TaskMonitor;
import prefuse.util.force.ForceSimulator;
import prefuse.util.force.Spring;

import java.util.Iterator;

/**
 * Used to calculate alignment statistics and error for a {@link ForceSimulator} layout.
 */
public class ErrorCalculator {

    private final ForceSimulator m_fsim;
    private final MagneticForce magneticForce;

    private int totalCount = -1;

    public ErrorCalculator(ForceSimulator m_fsim, MagneticForce magneticForce) {
        this.m_fsim = m_fsim;
        this.magneticForce = magneticForce;
    }

    public int totalEdges() {
        if (totalCount >= 0) return totalCount;
        totalCount = 0;
        Iterator<Spring> springs = m_fsim.getSprings();
        while (springs.hasNext()) {
            springs.next();
            totalCount++;
        }
        return totalCount;
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

    public float averageMisalignment() {
        float total = 0;
        Iterator<Spring> springs = m_fsim.getSprings();
        while (springs.hasNext()) {
            Spring s = springs.next();
            total += magneticForce.getEdgeMisalignment(s);
        }
        return total / totalEdges();
    }

    public float standardDeviation() {
        float sd = 0;
        float mean = averageMisalignment();
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

    public void displayResults(TaskMonitor taskMonitor) {
        float percent = percentOfMisaligned((float) Math.PI / 3);
        float average = degrees(averageMisalignment());
        float sd = degrees(standardDeviation());
        percent = Math.round(percent * 100) / 100f;
        average = Math.round(average * 100) / 100f;
        sd = Math.round(sd * 100) / 100f;
        char deg = '\u00B0';
        taskMonitor.showMessage(TaskMonitor.Level.INFO, "Total edges: " + totalEdges());
        taskMonitor.showMessage(TaskMonitor.Level.INFO, "Misaligned edges (> 60"+deg+"): " + percent + "%");
        taskMonitor.showMessage(TaskMonitor.Level.INFO, "Angle mean = " + average + deg + ", sd = " + sd + deg);
    }


}
