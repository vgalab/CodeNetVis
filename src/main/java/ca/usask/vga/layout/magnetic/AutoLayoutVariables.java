package ca.usask.vga.layout.magnetic;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Contains the ranges of possible input parameter adjustments,
 * as well as a way to iterate over all possible combinations of them.
 */
public class AutoLayoutVariables {

    private final ForceDirectedLayoutContext context;
    public final List<IndependentVar> variables;

    public AutoLayoutVariables(ForceDirectedLayoutContext context) {
        this.context = context;
        variables = Collections.unmodifiableList(newIndependentVarList());
    }

    protected List<IndependentVar> newIndependentVarList() {
        List<IndependentVar> vars = new ArrayList<>();

        vars.add(new RepulsionCoefficient());
        vars.add(new SpringCoefficient());

        if (context instanceof SimpleMagneticLayoutContext) {
            var cxt = (SimpleMagneticLayoutContext) context;
            if (cxt.magnetEnabled)
                vars.add(new MagneticField());
        }

        if (context instanceof PoleMagneticLayoutContext) {
            var cxt = (PoleMagneticLayoutContext) context;
            if (cxt.useHierarchyForce)
                vars.add(new HierarchyForce());
        }

        return vars;
    }

    public void setAll(int[] combination) {
        for (int i = 0; i < variables.size(); i++) {
            var parameter = variables.get(i);
            parameter.set(parameter.getSuggested(combination[i]));
        }
    }

    public Iterable<int[]> getAllCombinations() {
        return new CombinationsIterable();
    }

    public int getVarCount() {
        return variables.size();
    }

    public int getCombinationCount() {
        int product = 1;
        for (var v : variables) {
            product *= v.suggestedCount();
        }
        return product;
    }

    public String combinationToString(int[] combination) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < variables.size(); i++) {
            var parameter = variables.get(i);
            float value = parameter.getSuggested(combination[i]);
            String name = parameter.getClass().getSimpleName();
            sb.append(name).append(": ").append(value);
            if (i < variables.size() - 1) sb.append(", \n");
        }
        return sb.toString();
    }

    protected ForceDirectedLayoutContext getPrefuseContext() {
        return context;
    }

    protected SimpleMagneticLayoutContext getSimpleMagneticContext() {
        return (SimpleMagneticLayoutContext) context;
    }

    protected PoleMagneticLayoutContext getPoleMagneticContext() {
        return (PoleMagneticLayoutContext) context;
    }

    public class MagneticField extends IndependentVar {

        protected MagneticField() {
            super(new float[] {1e-4f, 1e-3f, 1e-5f, 1e-2f});
        }

        public float get() {
            return (float) getSimpleMagneticContext().magneticFieldStrength;
        }

        public void set(float val) {
            getSimpleMagneticContext().magneticFieldStrength = val;
        }
    }


    public class RepulsionCoefficient extends IndependentVar {

        protected RepulsionCoefficient() {
            super(new float[] {1, 2, 5, 10});
        }

        public float get() {
            return (float) getPrefuseContext().repulsionCoefficient;
        }

        public void set(float val) {
            getPrefuseContext().repulsionCoefficient = val;
        }
    }

    public class SpringLength extends IndependentVar {

        protected SpringLength() {
            super(new float[] {50, 100, 200});
        }

        public float get() {
            return (float) getPrefuseContext().defaultSpringLength;
        }

        public void set(float val) {
            getPrefuseContext().defaultSpringLength = val;
        }
    }

    public class SpringCoefficient extends IndependentVar {

        protected SpringCoefficient() {
            super(new float[] {1e-4f, 1e-5f, 1e-3f});
        }

        public float get() {
            return (float) getPrefuseContext().defaultSpringCoefficient;
        }

        public void set(float val) {
            getPrefuseContext().defaultSpringCoefficient = val;
        }
    }

    public class HierarchyForce extends IndependentVar {

        protected HierarchyForce() {
            super(new float[] {1e0f, 1e1f, 1e2f});
        }

        public float get() {
            return (float) getPoleMagneticContext().hierarchyForce;
        }

        public void set(float val) {
            getPoleMagneticContext().hierarchyForce = val;
        }
    }

    public abstract static class IndependentVar {

        protected final float[] suggestedValues;
        protected IndependentVar(float[] suggestedValues) {
            this.suggestedValues = suggestedValues;
        }

        public abstract float get();
        public abstract void set(float val);

        public float getDefault() {
            return suggestedValues[0];
        }
        public float getSuggested(int i) {
            return suggestedValues[i % suggestedCount()];
        }
        public int suggestedCount() {
            return suggestedValues.length;
        }

    }

    public class CombinationsIterable implements Iterable<int[]> {

        private final List<IndependentVar> vars;

        public CombinationsIterable() {
            this.vars = variables;
        }

        protected class CIterator implements Iterator<int[]> {
            int[] combination;
            boolean started = false;

            public CIterator() {
                this.combination = new int[vars.size()];
            }

            @Override
            public boolean hasNext() {
                for (int i = 0; i < combination.length; i++) {
                    if (combination[i] < vars.get(i).suggestedCount() - 1)
                        return true;
                }
                return false;
            }

            @Override
            public int[] next() {
                if (!hasNext())
                    return null;
                if (!started) {
                    started = true;
                } else {
                    int valueSet = 0;
                    for (int i = 0; valueSet == 0 && i < combination.length; i++) {
                        combination[i] = (combination[i] + 1) % vars.get(i).suggestedCount();
                        valueSet = combination[i];
                    }
                }
                return Arrays.copyOf(combination, combination.length);
            }
        }

        @NotNull
        @Override
        public Iterator<int[]> iterator() {
            return new CIterator();
        }
    }

}
