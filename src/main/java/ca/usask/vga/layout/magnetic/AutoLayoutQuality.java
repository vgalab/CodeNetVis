package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.util.ErrorCalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutoLayoutQuality {

    private final ForceDirectedLayoutContext context;
    public final List<QualityMeasure> measures;

    public AutoLayoutQuality(ForceDirectedLayoutContext context) {
        this.context = context;
        measures = Collections.unmodifiableList(newQualityMeasureList());
    }

    protected List<QualityMeasure> newQualityMeasureList() {

        List<QualityMeasure> list = new ArrayList<>();

        if (context instanceof SimpleMagneticLayoutContext) {
            var cxt = (SimpleMagneticLayoutContext) context;
            if (cxt.magnetEnabled)
                list.add(new EdgeAlignment());
        }

        list.add(new ForceStrength());
        list.add(new NodeLocationSD());
        list.add(new NodeDensity());

        return list;
    }

    public float calculateScore(ErrorCalculator errorCalculator) {
        errorCalculator.recalculate();
        float score = 0;
        for (QualityMeasure measure : measures) {
            if (measure.isSatisfied(errorCalculator))
                score += measure.getPriority();
        }
        return score;
    }

    public String qualityToString(ErrorCalculator errorCalculator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < measures.size(); i++) {
            QualityMeasure measure = measures.get(i);
            String name = measure.getClass().getSimpleName();
            boolean satisfied = measure.isSatisfied(errorCalculator);
            sb.append(name).append(": ").append(satisfied);
            if (i < measures.size() - 1) sb.append(", \n");
        }
        return sb.toString();
    }

    protected static class EdgeAlignment extends QualityMeasure {
        boolean isSatisfied(ErrorCalculator ec) {
            return ec.degrees(ec.misalignmentMean()) < 45;
        }
        float getPriority() {
            return 1;
        }
    }

    protected static class ForceStrength extends QualityMeasure {
        boolean isSatisfied(ErrorCalculator ec) {
            return ec.forceMean() < 1e-1;
        }
        float getPriority() {
            return 0.001f;
        }
    }

    protected static class NodeDensity extends QualityMeasure {
        boolean isSatisfied(ErrorCalculator ec) {
            float density = ec.density();
            return density < 5e-5f && density > 8e-6f;
        }
        float getPriority() {
            return 0.1f;
        }
    }

    protected static class AspectRatio extends QualityMeasure {
        boolean isSatisfied(ErrorCalculator ec) {
            return ec.aspectRatioError(2) < 1.5;
        }
        float getPriority() {
            return 0.01f;
        }
    }

    protected static class NodeLocationSD extends QualityMeasure {
        boolean isSatisfied(ErrorCalculator ec) {
            return ec.sdXY() >= ec.uniformSD(0, 1)*0.7;
        }
        float getPriority() {
            return 0.01f;
        }
    }

    public abstract static class QualityMeasure {
        abstract boolean isSatisfied(ErrorCalculator ec);
        abstract float getPriority();
    }

}
