package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.io.JGitMetadataInput;
import ca.usask.vga.layout.magnetic.poles.ExtraTasks;
import ca.usask.vga.layout.magnetic.poles.PoleManager;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.equations.EquationCompiler;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.task.hide.HideTaskFactory;
import org.cytoscape.task.hide.UnHideAllTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.LineTypeVisualProperty;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.LineType;
import org.cytoscape.view.vizmap.*;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.cytoscape.work.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.function.Function;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

/**
 * Used for changing the style of the displayed graph, such as node size and color,
 * filtering out certain nodes,as well as adding annotations on top of the graph view.
 */
public class SoftwareStyle implements NetworkViewAboutToBeDestroyedListener {

    protected final CyApplicationManager am;
    private final TaskManager tm;
    private final VisualMappingManager vmm;
    private final VisualMappingFunctionFactory vmff_passthrough;
    private final VisualMappingFunctionFactory vmff_discrete;
    private final VisualMappingFunctionFactory vmff_continuous;
    private final VisualStyleFactory vsf;
    protected final PoleManager pm;
    private final AnnotationManager anm;
    private final AnnotationFactory<ShapeAnnotation> anf;
    private final HideTaskFactory htf;
    private final UnHideAllTaskFactory utf;
    private final EquationCompiler eq;

    protected final PinRadiusAnnotation pinRadiusAnnotation;
    protected final RingsAnnotation ringsAnnotation;

    private boolean showUnique = false;
    private String filterPrefix = "";

    public static String INDEGREE = "Indegree", OUTDEGREE = "Outdegree", DEGREE = "Degree";

    private SizeEquation currentSizeEquation = SizeEquation.FIXED;
    private int hideLabelsLessThan = -1; // Turned off
    private double lastSetNodeSize = 30;

    private boolean usePoleColors = true;
    private Coloring currentColoring = Coloring.PACKAGE;

    private int currentGitHistoryCutoff = 0;

    private CyNetwork cachedLastCommitDateNetwork = null;
    private TreeSet<String> cachedLastCommitDateOptions = null;
    private CyNetwork cachedCommitAuthorsNetwork = null;
    private TreeSet<String> cachedCommitAuthors = null;


    public final int MAX_DISCRETE_COLORS = 12*2;

    /**
     * Initializes the parameters for the software style functionality.
     * Note that many services are necessary to update the style of the graph.
     */
    public SoftwareStyle(CyApplicationManager am, TaskManager tm, VisualMappingManager vmm,
                         VisualMappingFunctionFactory vmff_passthrough,
                         VisualMappingFunctionFactory vmff_discrete,
                         VisualMappingFunctionFactory vmff_continuous, VisualStyleFactory vsf, PoleManager pm,
                         AnnotationManager anm, AnnotationFactory anf,
                         HideTaskFactory htf, UnHideAllTaskFactory utf,
                         EquationCompiler eq) {
        this.am = am;
        this.tm = tm;
        this.vmm = vmm;
        this.vmff_passthrough = vmff_passthrough;
        this.vmff_discrete = vmff_discrete;
        this.vmff_continuous = vmff_continuous;
        this.vsf = vsf;
        this.pm = pm;
        this.anm = anm;
        this.anf = anf;
        this.htf = htf;
        this.utf = utf;
        this.eq = eq;
        pinRadiusAnnotation = new PinRadiusAnnotation();
        ringsAnnotation = new RingsAnnotation();
        lastSetNodeSize = 30;
        usePoleColors = true;
        pm.addChangeListener(this::updatePoleColors);
    }

    /**
     * Set whether to continuously update the node colors, applying coloring by pole.
     */
    private void setShowPoleColors(boolean value) {
        usePoleColors = value;
        updatePoleColors();
    }

    /**
     * Update the pole color mapping in a separate thread.
     */
    private void updatePoleColors() {
        if (usePoleColors) {
            var coloring = new ExtraTasks.LegacyPoleColoring(am, pm, vmm, vmff_discrete);
            tm.execute(new TaskIterator(coloring));
        }
    }

    /**
     * Remove the node and edge color mappings from the current style.
     */
    private void clearNodeColorMappings() {
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        style.removeVisualMappingFunction(NODE_FILL_COLOR);
        style.removeVisualMappingFunction(EDGE_UNSELECTED_PAINT);
        style.removeVisualMappingFunction(EDGE_STROKE_UNSELECTED_PAINT);
    }

    /**
     * Set the top n poles for the current network. To prompt the user instead,
     * use {@link ExtraTasks.MakeTopDegreePoles} with a dialog task manager.
     */
    public void setTopNasPoles(int n, boolean incoming) {
        var makeTop = new ExtraTasks.MakeTopDegreePoles(am, pm);
        makeTop.topN = n;
        makeTop.edgeType = incoming ? CyEdge.Type.INCOMING : CyEdge.Type.OUTGOING;
        tm.execute(new TaskIterator(makeTop));
    }

    /**
     * Convert the given slider value to node size and set the node and label size to that value.
     */
    public void setNodeSize(float value) {
        setNodeLabelSize(nodeSizeFunc(value));
    }

    /**
     * Calculate the node size based on the slider value.
     * Uses a logarithmic scale, to cover a range of sizes from 5 to 500.
     * The slider value of 0 results in a size of 0.1 (practically invisible).
     */
    private double nodeSizeFunc(double value) {
        double calculated = Math.round(Math.pow(10, 1+(value / 50))/2);
        if (value == 0) {
            calculated = 0.1;
        }
        return calculated;
    }

    /**
     * Calculate the slider value based on the node size.
     * Reverse of the function {@link #nodeSizeFunc(double)}
     */
    private double nodeSizeFuncReverse(double size) {
        if (size == 0.1) return 0;
        return 50*(Math.log10(size*2) - 1);
    }

    /**
     * Set the node size and label size to the given value.
     */
    private void setNodeLabelSize(double size) {
        lastSetNodeSize = size;
        updateSizeMapping();

        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());

        style.setDefaultValue(BasicVisualLexicon.NODE_SIZE, size);
        style.setDefaultValue(NODE_LABEL_FONT_SIZE, (int) Math.ceil(size/2));
    }

    /**
     * Get the current default node size.
     */
    private double getCurrentNodeSize() {
        if (am.getCurrentNetworkView() == null) return 30;
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        var value = style.getDefaultValue(BasicVisualLexicon.NODE_SIZE);
        lastSetNodeSize = value;
        return value;
    }

    /**
     * Get the slider value for the default node size of the current graph.
     */
    public float getInitialNodeSizeValue() {
        return (float) Math.max(0, Math.min(nodeSizeFuncReverse(getCurrentNodeSize()), 100));
    }

    /**
     * Set the edge transparency to the given value.
     */
    public void setEdgeTransparency(float value) {
        int calculated = Math.round(value);
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        style.setDefaultValue(EDGE_TRANSPARENCY, calculated);
    }

    /**
     * Get the default edge transparency value.
     */
    public float getInitialEdgeTransparency() {
        if (am.getCurrentNetworkView() == null) return 120;
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        return style.getDefaultValue(EDGE_TRANSPARENCY);
    }

    /**
     * Set whether to show only unique nodes (non-gray) in the current graph.
     * Hides all nodes that are not unique.
     */
    public void setShowUnique(boolean showUnique) {
        if (this.showUnique == showUnique)
            return;
        this.showUnique = showUnique;
        if (showUnique && !polesPresent()) {
            JOptionPane.showMessageDialog(null,
                    "Pole-specific dependencies was selected but no poles have been added yet.\n" +
                            "Please use the \"Create layout\" panel or toolbar buttons to add at least one pole.",
                    "Need at least one pole", JOptionPane.WARNING_MESSAGE);
            return; // do not make any changes in this situation
        }
        reapplyFilters();
    }

    /**
     * Set the prefix to filter nodes by, usually a package name that
     * comes before the Java class name.
     */
    public void setFilterPrefix(String filterPrefix) {
        if (this.filterPrefix.equals(filterPrefix))
            return;
        this.filterPrefix = filterPrefix;
        reapplyFilters();
    }

    /**
     * Hide and unhide nodes according to the current filter settings.
     */
    public void reapplyFilters() {
        var net = am.getCurrentNetwork();
        if (net == null) return;

        var view = am.getCurrentNetworkView();
        var table = net.getDefaultNodeTable();

        Set<CyNode> nodesToHide = new HashSet<>();

        // Hide nodes without prefix and not unique
        for (CyNode n : net.getNodeList()) {
            clearNodeVisible(n);
            String name = table.getRow(n.getSUID()).get("name", String.class);
            if (showUnique && (pm.isClosestToMultiple(net, n) || pm.isDisconnected(net, n))) {
                setNodeVisible(n, false);
            } else if (name == null) {
                setNodeVisible(n, false);
            } else if (!filterPrefix.isEmpty() && (!name.startsWith(filterPrefix)) || name.length() < filterPrefix.length()) {
                setNodeVisible(n, false);
            } else {
                clearNodeVisible(n);
            }
        }
    }

    /**
     * Set the node visible property, same as hiding but with a different name.
     */
    protected void setNodeVisible(CyNode node, boolean visible) {
        var view = am.getCurrentNetworkView();
        view.getNodeView(node).setLockedValue(NODE_VISIBLE, visible);
    }

    /**
     * Reset the node visible property to its default value.
     */
    protected void clearNodeVisible(CyNode node) {
        var view = am.getCurrentNetworkView();
        view.getNodeView(node).clearValueLock(NODE_VISIBLE);
    }

    /**
     * Get the pin radius annotation object.
     */
    public PinRadiusAnnotation getRadiusAnnotation() {
        return pinRadiusAnnotation;
    }

    /**
     * Get the rings annotation object.
     */
    public RingsAnnotation getRingsAnnotation() {
        return ringsAnnotation;
    }

    /**
     * Handle the network view being destroyed. Remove the annotations from the network view,
     * to avoid memory leaks and missing references.
     */
    @Override
    public void handleEvent(NetworkViewAboutToBeDestroyedEvent e) {
        try {pinRadiusAnnotation.onViewDestroyed(e);} catch (Exception ignored) {};
        try {ringsAnnotation.onViewDestroyed(e);} catch (Exception ignored) {};
    }

    /**
     * Used to display a circle with the same radius as the pin radius of the nodes.
     */
    public class PinRadiusAnnotation implements TooltipAnnotation {

        protected boolean visible;
        protected ShapeAnnotation annotation;
        protected float radius = 2500;
        protected CyNetworkView lastView;

        protected void init() {
            if (am.getCurrentNetworkView() == null) return;
            if (annotation != null) return;
            lastView = am.getCurrentNetworkView();
            var argMap = new HashMap<String, String>();
            annotation = anf.createAnnotation(ShapeAnnotation.class, lastView, argMap);
            annotation.setShapeType(ShapeAnnotation.ShapeType.ELLIPSE.shapeName());
            annotation.setBorderWidth(20);
            reposition();
        }

        protected void checkNetwork() {
            if (annotation != null && lastView != am.getCurrentNetworkView()) {
                try {anm.removeAnnotation(annotation);} catch (Exception ex) {ex.printStackTrace();}
                annotation = null;
                init();
            }
        }

        public void reposition() {
            if (am.getCurrentNetworkView() == null) return;
            init();
            checkNetwork();
            annotation.setSize(radius*2, radius*2);
            annotation.moveAnnotation(getAveragePolePos(-radius, -radius));
            annotation.update();
        }

        public void setRadius(float radius) {
            this.radius = radius;
            reposition();
        }

        public void show() {
            reposition();
            anm.addAnnotation(annotation);
        }

        public void hide() {
            if (annotation != null) anm.removeAnnotation(annotation);
        }

        @Override
        public void setVisible(boolean visible) {
            this.visible = visible;
            if (am.getCurrentNetworkView() == null) return;
            if (visible) show();
            else hide();
        }

        @Override
        public void reset() {
            annotation = null;
            lastView = null;
        }

        public void onViewDestroyed(NetworkViewAboutToBeDestroyedEvent e) {
            if (lastView != e.getNetworkView()) return;
            hide();
            reset();
        }
    }

    /**
     * Used to display multiple circles with the same radii as the hierarchy force rings.
     */
    public class RingsAnnotation implements TooltipAnnotation {

        protected boolean visible;
        protected int maxRings = 4;
        protected CyNetworkView lastView;

        protected List<ShapeAnnotation> annotations;

        public RingsAnnotation() {
            annotations = new ArrayList<>(maxRings);
        }

        private float getPinRadius() {
            return getRadiusAnnotation().radius;
        }

        protected float getRingRadius(int i) {
            float pinRadius = getPinRadius();
            if (pinRadius == 0)
                pinRadius = SoftwareLayout.DEFAULT_RING_CIRCLE_RADIUS;
            return (i+1) * (pinRadius / (maxRings + 1));
        }

        protected void init() {
            if (am.getCurrentNetworkView() == null) return;
            lastView = am.getCurrentNetworkView();
            for (int i = 0; i < maxRings; i++) {
                if (annotations.size() <= i) {
                    var argMap = new HashMap<String, String>();
                    var a = anf.createAnnotation(ShapeAnnotation.class, lastView, argMap);
                    a.setShapeType(ShapeAnnotation.ShapeType.ELLIPSE.shapeName());
                    a.setBorderWidth(10);
                    a.setBorderOpacity(50);
                    annotations.add(a);
                }
            }
            hide();
        }

        protected void checkNetwork() {
            if (annotations.size() > 0 && lastView != am.getCurrentNetworkView()) {
                anm.removeAnnotations(annotations);
                annotations.clear();
                init();
            }
        }

        public void reposition() {
            if (am.getCurrentNetworkView() == null) return;
            for (int i = 0; i < maxRings; i++) {
                ShapeAnnotation a = annotations.get(i);
                var radius = getRingRadius(i);
                a.setSize(radius * 2, radius * 2);
                a.moveAnnotation(getAtPolePos(-radius, -radius));
                a.update();
            }
        }

        public void setMaxRings(int maxRings) {
            this.maxRings = maxRings;
            if (visible) show();
        }

        @Override
        public void setVisible(boolean visible) {
            this.visible = visible;
            if (am.getCurrentNetworkView() == null) return;
            if (visible) show();
            else hide();
        }

        public void show() {
            if (!polesPresent()) return;
            checkNetwork();
            init();
            reposition();
            anm.addAnnotations(annotations.subList(0, maxRings));
        }

        public void hide() {
            if(annotations.size()>0) anm.removeAnnotations(annotations);
        }

        @Override
        public void reset() {
            annotations.clear();
            lastView = null;
        }

        public void onViewDestroyed(NetworkViewAboutToBeDestroyedEvent e) {
            if (lastView != e.getNetworkView()) return;
            reset();
        }
    }

    public interface TooltipAnnotation {
        void setVisible(boolean visible);
        void reset();
    }

    protected Point2D getAveragePolePos(float offsetX, float offsetY) {
        var view = am.getCurrentNetworkView();
        var list = pm.getPoleList(am.getCurrentNetwork());
        double x = 0, y = 0;
        int count = list.size();
        if (count > 0) {
            for (var node : list) {
                var nodeV = view.getNodeView(node);
                x += nodeV.getVisualProperty(NODE_X_LOCATION);
                y += nodeV.getVisualProperty(NODE_Y_LOCATION);
            }
            x /= count;
            y /= count;
        }
        return new Point2D.Double(x+offsetX, y+offsetY);
    }

    /**
     * Returns the position of the first pole in the network, or the center of the network
     * if there are no poles. Offset values are added to the resulting position.
     */
    protected Point2D getAtPolePos(float offsetX, float offsetY) {
        var view = am.getCurrentNetworkView();
        var list = pm.getPoleList(am.getCurrentNetwork());
        double x = 0, y = 0;
        if (list.size() > 0) {
            var nodeV = view.getNodeView(list.get(0));
            x = nodeV.getVisualProperty(NODE_X_LOCATION);
            y = nodeV.getVisualProperty(NODE_Y_LOCATION);
        }
        return new Point2D.Double(x+offsetX, y+offsetY);
    }

    /**
     * Returns true if there are poles in the current network.
     */
    protected boolean polesPresent() {
        return am.getCurrentNetwork() != null && pm.getPoleList(am.getCurrentNetwork()).size() > 0;
    }

    /**
     * Returns the initial suggested radius for the force layout,
     * based on the size of the network view.
     */
    public float getSuggestedRadius() {
        var view = am.getCurrentNetworkView();
        if (view == null) return 2500;
        double scale = view.getVisualProperty(NETWORK_SCALE_FACTOR);
        double width = view.getVisualProperty(NETWORK_WIDTH) / scale;
        double height = view.getVisualProperty(NETWORK_HEIGHT) / scale;
        return Math.min((float) Math.min(width, height) / 2, 10000);
    }

    /**
     * Returns the list of allowed dropdown options for the size mapping.
     */
    public enum SizeEquation {
        FIXED, BIGGER_POLES, INDEGREE, OUTDEGREE, DEGREE, COMMITS;
        @Override
        public String toString() {
            return name().charAt(0) + name().toLowerCase()
                    .replace("_", " ").substring(1);
        }
        public String getColumnName() {
            if (equals(FIXED) || equals(BIGGER_POLES)) return null;
            if (equals(COMMITS)) return JGitMetadataInput.TOTAL_COMMITS;
            return toString();
        }
        public static SizeEquation[] getAllowedList() {
            return new SizeEquation[] {FIXED, BIGGER_POLES, INDEGREE, OUTDEGREE, DEGREE};
        }
    }

    /**
     * Sets the size equation to use with the node size mapping.
     */
    public void setSizeEquation(SizeEquation sizeEquation) {
        if (sizeEquation == this.currentSizeEquation) return;
        currentSizeEquation = sizeEquation;
        updateSizeMapping();
    }

    /**
     * Updates the node size mapping based on the current size equation.
     */
    private void updateSizeMapping() {
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        if (currentSizeEquation == SizeEquation.FIXED || currentSizeEquation == SizeEquation.BIGGER_POLES) {
            style.removeVisualMappingFunction(BasicVisualLexicon.NODE_SIZE);
            style.removeVisualMappingFunction(NODE_LABEL_FONT_SIZE);
            if (currentSizeEquation == SizeEquation.BIGGER_POLES) {
                var t = new ExtraTasks.MakePoleNodesLarger(am, vmm, vmff_discrete, null);
                t.keepIncreasing = false;
                t.run(ExtraTasks.getBlankTaskMonitor());
            }
        } else {
            initColumn(currentSizeEquation);

            var nodeSizeFunc = (ContinuousMapping<Integer, Double>) vmff_continuous.createVisualMappingFunction(
                    currentSizeEquation.getColumnName(), Integer.class, BasicVisualLexicon.NODE_SIZE);
            var fontSizeFunc = (ContinuousMapping<Integer, Integer>) vmff_continuous.createVisualMappingFunction(
                    currentSizeEquation.getColumnName(), Integer.class, NODE_LABEL_FONT_SIZE);

            double s = lastSetNodeSize/2;
            int i = (int) Math.max(1, Math.ceil(s)/2);

            setPoints(nodeSizeFunc, s, 5*s, 10*s, 0, 100, 1000);
            style.addVisualMappingFunction(nodeSizeFunc);

            setPoints(fontSizeFunc, i, 5*i, 10*i, hideLabelsLessThan, 100, 1000);
            style.addVisualMappingFunction(fontSizeFunc);
        }
    }

    /**
     * Sets the typical control points for the given continuous mapping.
     */
    private void setPoints(ContinuousMapping<Integer, Double> map, double lower, double mid, double upper, int min, int handle, int max) {
        map.addPoint(min, new BoundaryRangeValues<>(0d, lower, lower));
        map.addPoint(handle, new BoundaryRangeValues<>(mid, mid, mid));
        map.addPoint(max, new BoundaryRangeValues<>(upper, upper, upper));
    }

    /**
     * Sets the typical control points for the given continuous mapping.
     */
    private void setPoints(ContinuousMapping<Integer, Integer> map, int lower, int mid, int upper, int min, int handle, int max) {
        map.addPoint(min, new BoundaryRangeValues<>(0, lower, lower));
        map.addPoint(handle, new BoundaryRangeValues<>(mid, mid, mid));
        map.addPoint(max, new BoundaryRangeValues<>(upper, upper, upper));
    }

    /**
     * Initializes the node size column if it doesn't exist.
     */
    private void initColumn(SizeEquation s) {
        if (s == SizeEquation.FIXED || s == SizeEquation.BIGGER_POLES) return;

        var net = am.getCurrentNetwork();
        var table = net.getDefaultNodeTable();

        if (table.getColumn(s.getColumnName()) == null) {
            table.createColumn(s.getColumnName(), Integer.class, false);

            var map = new HashMap<String, Class<?>>();
            map.put("SUID", Long.class);

            eq.compile("=" + s.name() + "($SUID)", map);
            table.getAllRows().forEach(r -> r.set(s.getColumnName(), eq.getEquation()));
        }
    }

    /**
     * Applies the directed software network style to the current network view.
     */
    public VisualStyle applyDirectedStyle() {
        if (am.getCurrentNetworkView() == null) return null;
        var style = vsf.createVisualStyle(vmm.getDefaultVisualStyle());

        style.setDefaultValue(NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);
        style.setDefaultValue(EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.ARROW);
        style.setDefaultValue(EDGE_TARGET_ARROW_SIZE, 12d);

        style.setDefaultValue(BasicVisualLexicon.NODE_SIZE, 30d);
        style.setDefaultValue(NODE_LABEL_FONT_SIZE, 15);

        style.removeVisualMappingFunction(EDGE_LABEL);
        style.setDefaultValue(EDGE_UNSELECTED_PAINT, Color.LIGHT_GRAY);

        for (var d : style.getAllVisualPropertyDependencies()) {
            d.setDependency(d.getIdString().equals("arrowColorMatchesEdge") ||
                    d.getIdString().equals("nodeSizeLocked"));
        }

        vmm.setVisualStyle(style, am.getCurrentNetworkView());
        return style;
    }

    /**
     * Sets the passthrough mapping for the node label to the given column.
     */
    public void setLabelsPassthrough(String column) {
        PassthroughMapping<String, String> func = (PassthroughMapping<String, String>)
                vmff_passthrough.createVisualMappingFunction(column, String.class, NODE_LABEL);
        vmm.getVisualStyle(am.getCurrentNetworkView()).addVisualMappingFunction(func);
    }

    /**
     * Sets the passthrough mapping for the node tooltip to the given column.
     */
    public void setTooltipsPassthrough(String column) {
        PassthroughMapping<String, String> func = (PassthroughMapping<String, String>)
                vmff_passthrough.createVisualMappingFunction(column, String.class, NODE_TOOLTIP);
        vmm.getVisualStyle(am.getCurrentNetworkView()).addVisualMappingFunction(func);
    }

    /**
     * Returns the list of allowed options for node coloring.
     */
    public enum Coloring {
        NONE, ROOT_PACKAGE, PACKAGE, CLOSEST_POLE;
        public void apply(SoftwareStyle s) {
            s.setShowPoleColors(false);
            switch (this) {
                case PACKAGE:
                    s.applyDiscreteColoring("Package");
                    return;
                case ROOT_PACKAGE:
                    s.applyDiscreteColoring("Root package");
                    return;
                case CLOSEST_POLE:
                    s.setShowPoleColors(true);
                    if (!s.polesPresent())
                        JOptionPane.showMessageDialog(null,
                            "Pole coloring was selected but no poles have been added yet.\n" +
                                    "Please use the \"Create layout\" panel or toolbar buttons to add at least one pole.",
                            "Need at least one pole", JOptionPane.WARNING_MESSAGE);
                    return;
                default:
                    s.clearNodeColorMappings();
            }
        }
        @Override
        public String toString() {
            return name().charAt(0) + name().toLowerCase()
                    .replace("_", " ").substring(1);
        }
        public static Coloring[] getAllowedList() {
            return new Coloring[] {NONE, ROOT_PACKAGE, PACKAGE, CLOSEST_POLE};
        }
    }

    /**
     * Sets the current coloring scheme for nodes and edges of the graph.
     */
    public void setCurrentColoring(Coloring c) {
        if (currentColoring != c) {
            currentColoring = c;
            c.apply(this);
        }
    }

    /**
     * Applies the discrete coloring to the node color, based on the given column.
     */
    protected void applyDiscreteColoring(String column) {
        // Allows for parallel computation
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        style.removeVisualMappingFunction(EDGE_UNSELECTED_PAINT);
        style.removeVisualMappingFunction(EDGE_STROKE_UNSELECTED_PAINT);
        tm.execute(new TaskIterator(new ApplyDiscreteColoringTask(column, String.class)));
    }

    /**
     * Applies the discrete coloring to the node color, based on the given column and type.
     * This is a helper method for ApplyDiscreteColoringTask.
     * Returns the number of distinct categories in the specified column.
     */
    private <T> int applyDiscreteColoring(String column, Class<T> type) {
        var view = am.getCurrentNetworkView();
        var net = am.getCurrentNetwork();
        if (view == null || net == null) return 0;

        if (net.getDefaultNodeTable().getColumn(column) == null) {
            throw new RuntimeException("The graph does not contain \"" + column + "\" property to apply coloring with.");
        }

        var func = (DiscreteMapping<T, Paint>)
                vmff_discrete.createVisualMappingFunction(column, type, NODE_FILL_COLOR);

        var table = net.getDefaultNodeTable();

        var valueList = table.getColumn(column).getValues(type);
        valueList.removeIf(Objects::isNull); // TreeSet does not support null values

        var values = new TreeSet<>(valueList);

        int i = 0;
        for (var v : values) {
            if (v == null) continue;

            // Add new colors by darkening or brightening existing colors
            Color color = COLOR_BREWER_SET3[i % 12];
            if (i / 12 % 2 == 1)
                color = color.darker();

            func.putMapValue(v, color);
            i++;
        }

        vmm.getVisualStyle(view).addVisualMappingFunction(func);
        return values.size();
    }

    /**
     * Applies the discrete mapping to the nodes, based on the given column.
     */
    private <T, S> void applyDiscreteMapping(String column, Class<T> type, Map<T, S> mapping, VisualProperty<S> vp) {
        var view = am.getCurrentNetworkView();
        var net = am.getCurrentNetwork();
        if (view == null || net == null) return;

        if (net.getDefaultNodeTable().getColumn(column) == null) {
            throw new RuntimeException("The graph does not contain \"" + column + "\" property to apply style with.");
        }

        var func = (DiscreteMapping<T, S>)
                vmff_discrete.createVisualMappingFunction(column, type, vp);
        func.putAll(mapping);
        vmm.getVisualStyle(am.getCurrentNetworkView()).addVisualMappingFunction(func);
    }

    /**
     * Applies the software network style to the current network view when a software file is loaded.
     */
    public void onFileLoaded(String fileFormat) {
        if (am.getCurrentNetworkView() == null) return;

        var style = applyDirectedStyle();

        if (fileFormat.equals("jar") || fileFormat.equals("java")) {
            // Set class as the labels
            setLabelsPassthrough("Class");
            setTooltipsPassthrough("Package");
            // Pick best coloring based on the size of the project
            if (am.getCurrentNetwork().getNodeCount() >= 100)
                applyDiscreteColoring("Root package");
            else
                applyDiscreteColoring("Package");
        }
    }

    /**
     * The color brewer set 3 color palette for package colors.
     * Imported from <a href="https://colorbrewer2.org/">colorbrewer2.org</a>
     */
    public final static Color[] COLOR_BREWER_SET3 = {
            new Color(141,211,199),
            new Color(255,255,179),
            new Color(190,186,218),
            new Color(251,128,114),
            new Color(128,177,211),
            new Color(253,180,98),
            new Color(179,222,105),
            new Color(252,205,229),
            new Color(217,217,217),
            new Color(188,128,189),
            new Color(204,235,197),
            new Color(231, 59, 125) // edited for better visibility
    };

    /**
     * A gradient from green to black. Contains 4 distinct colors.
     */
    public final static Color[] GREEN_4 = {
            new Color(0, 255, 71),
            new Color(0, 199, 62),
            new Color(0, 124, 33),
            new Color(1, 77, 23),
    };

    /**
     * A gradient from red to black. Contains 4 distinct colors.
     */
    public final static Color[] RED_4 = {
            new Color(255, 0, 0),
            new Color(182, 0, 0),
            new Color(124, 0, 0),
            new Color(82, 0, 0),
    };


    /**
     * Applies the discrete coloring to the node color, based on the given column and type.
     */
    private class ApplyDiscreteColoringTask extends AbstractTask {

        private final String column;
        private final Class<?> type;

        public ApplyDiscreteColoringTask(String column, Class<?> type) {
            this.column = column;
            this.type = type;
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            taskMonitor.setTitle("Apply coloring by " + column);
            int categories = applyDiscreteColoring(column, type);

            if (categories > MAX_DISCRETE_COLORS) {
                // Message must show in another thread as not to interrupt the Task execution
                Runnable r = () ->
                        JOptionPane.showMessageDialog(null,
                                String.format("The selected option to color by %s has %d categories, more than %d.\n " +
                                        "There are not enough colors to keep all different categories distinct.",
                                        column.toLowerCase(), categories, MAX_DISCRETE_COLORS),
                                "Not enough distinct colors to color graph",
                            JOptionPane.WARNING_MESSAGE);
                new Thread(r).start();
            }
        }
    }

    /**
     * Returns a task observer that runs the given runnable or
     * lambda expression after all tasks are finished.
     */
    protected TaskObserver afterFinished(Runnable r) {
        return new TaskObserver() {
            public void taskFinished(ObservableTask task) {}
            @Override
            public void allFinished(FinishStatus finishStatus) {
                r.run();
            }
        };
    }

    /**
     * Returns the list of all package names in the current network,
     * sorted by the number of dots in the name.
     * The first element is an empty string, an option to show all packages.
     */
    public String[] getPackageFilterOptions() {

        var net = am.getCurrentNetwork();
        if (net == null) return new String[0];

        var table = net.getDefaultNodeTable();

        // First sort by number of dots
        var packages = new TreeSet<String>(Comparator.comparingInt((String s) -> s.split("\\.").length).thenComparing(s -> s));

        var secondary = new HashSet<String>();

        packages.add("");

        for (var r : table.getAllRows()) {
            var p = r.get("Package", String.class);
            if (p != null) {
                if (packages.contains(p)) {
                    continue;
                }
                packages.add(p);
                while (p.contains(".")) {
                    p = p.substring(0, p.lastIndexOf("."));
                    if (p.contains(".")) {
                        if (!secondary.contains(p)) {
                            secondary.add(p);
                        } else {
                            packages.add(p);
                            break;
                        }
                    }
                }
            }
        }

        return packages.toArray(new String[0]);
    }

    /**
     * Returns a sorted set of options for Last Commit Date for all nodes
     * in the current network, which can be used for discrete coloring.
     */
    public TreeSet<String> getLastCommitDateOptions() {

        var net = am.getCurrentNetwork();
        if (net == null) return new TreeSet<>();

        var table = net.getDefaultNodeTable();
        var column = table.getColumn(JGitMetadataInput.LAST_COMMIT_DATE);
        if (column == null) return new TreeSet<>();

        if (cachedLastCommitDateOptions == null || cachedLastCommitDateNetwork != net) {
            // Return them sorted, using a TreeSet to remove duplicates
            var dates = column.getValues(String.class);
            dates.removeIf(Objects::isNull);
            var sortedSet = new TreeSet<>(dates);
            // Remove the empty string
            sortedSet.remove("");
            cachedLastCommitDateOptions = sortedSet;
            cachedLastCommitDateNetwork = net;
        }

        return new TreeSet<>(cachedLastCommitDateOptions);
    }

    /**
     * Returns a set of commit authors for all nodes in the current network.
     */
    public Set<String> getCommitAuthors() {

        var net = am.getCurrentNetwork();
        if (net == null) return new TreeSet<>();

        var table = net.getDefaultNodeTable();
        var column = table.getColumn(JGitMetadataInput.LAST_COMMIT_AUTHOR);
        if (column == null) return new TreeSet<>();

        if (cachedCommitAuthors == null || cachedCommitAuthorsNetwork != net) {
            // Return them sorted, using a TreeSet to remove duplicates
            var authors = column.getValues(String.class);
            authors.removeIf(Objects::isNull);
            var sortedSet = new TreeSet<>(authors);
            // Remove the empty string
            sortedSet.remove("");
            cachedCommitAuthors = sortedSet;
            cachedCommitAuthorsNetwork = net;
        }

        return new TreeSet<>(cachedCommitAuthors);
    }

    /**
     * Returns the highest number of commits for nodes in the current network.
     */
    public int getMostNodeCommits() {
        var net = am.getCurrentNetwork();
        if (net == null) return 0;

        var table = net.getDefaultNodeTable();
        var column = table.getColumn(JGitMetadataInput.TOTAL_COMMITS);
        if (column == null) return 0;

        var values = column.getValues(Integer.class);
        values.removeIf(Objects::isNull);
        return values.stream().max(Integer::compareTo).orElse(0);
    }

    /**
     * Applies node coloring by the number of commits per node.
     * Lime green = most commits, gray = least commits.
     */
    public void applyColoringByCommits() {
        // Apply continuous mapping
        var func = (ContinuousMapping<Integer, Paint>)
                vmff_continuous.createVisualMappingFunction(JGitMetadataInput.TOTAL_COMMITS, Integer.class, NODE_FILL_COLOR);

        int maxCommits = getMostNodeCommits();

        Paint minColor = new Color(66, 66, 66); // Gray
        Paint maxColor = Color.GREEN;

        func.addPoint(0, new BoundaryRangeValues<>(minColor, minColor, minColor));
        func.addPoint(maxCommits, new BoundaryRangeValues<>(maxColor, maxColor, maxColor));

        vmm.getVisualStyle(am.getCurrentNetworkView()).addVisualMappingFunction(func);
    }

    /**
     * Retrieves the "Parallel Lines" border style from the renderer. If it is not found,
     * returns EQUAL_DASH.
     */
    private LineType retrieveParallelLinesStyle() {
        return am.getCurrentRenderingEngine().getVisualLexicon().getSupportedValueRange(NODE_BORDER_LINE_TYPE)
                .stream().filter(v -> v.getSerializableString().equals("PARALLEL_LINES")).findAny()
                .orElse(LineTypeVisualProperty.EQUAL_DASH);
    }

    /**
     * Resets all Git Data styles previously applied to the network.
     * @param reapplyDefaultColors if true, reapply the default colors of the current coloring
     */
    public void resetGitDataStyle(boolean reapplyDefaultColors) {
        // Remove border mappings
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        style.removeVisualMappingFunction(NODE_BORDER_WIDTH);
        style.removeVisualMappingFunction(NODE_BORDER_TRANSPARENCY);
        style.setDefaultValue(NODE_BORDER_WIDTH, 0.0);
        style.setDefaultValue(NODE_BORDER_LINE_TYPE, LineTypeVisualProperty.SOLID);
        // Reset sizes
        if (currentSizeEquation == SizeEquation.COMMITS)
            setSizeEquation(SizeEquation.FIXED);
        // Reset the coloring
        if (reapplyDefaultColors)
            currentColoring.apply(this);
    }

    /**
     * Returns topN most recent commits. If reveseSort is true, returns topN oldest commits.
     * If topN is 0, returns all commits in the specified order.
     */
    public Map<Integer, String> getMostRecentCommits(int topN, boolean reverseSort) {
        var sortedDates = getLastCommitDateOptions();
        Map<Integer, String> mostRecentCommits = new TreeMap<>();
        // N most recent dates are added
        for (int i = 0; (i < topN || topN == 0) && !sortedDates.isEmpty(); i++) {
            if (!reverseSort)
                mostRecentCommits.put(i, sortedDates.pollLast());
            else
                mostRecentCommits.put(i, sortedDates.pollFirst());
        }
        return mostRecentCommits;
    }

    /**
     * Applies a mapping from the selected data property to a quantitative visual property.
     * @param property the data property to map
     * @param maxValue the maximum value of the visual property (e.g. 255 for transparency, 10 for width)
     * @param visualProperty the visual property to map to, must be a number
     * @param convertToT a function that converts a double to the type of the visual property (usually rounding)
     * @param <T> the type of the visual property, must be a number (extends Number)
     */
    public <T> void applyNumberVisualization(GitDataProperty property, double minValue, double maxValue, VisualProperty<T> visualProperty,
                                             Function<Double, T> convertToT) {
        double range = maxValue - minValue;
        if (property == GitDataProperty.MOST_RECENT_COMMIT_DATE) {
            Map<String, T> mapping = new HashMap<>();
            var recentCommits = getMostRecentCommits(0, true);
            recentCommits.forEach((k, v) -> {
                mapping.put(v, convertToT.apply( Math.pow(1d * k / (recentCommits.size()-1), 2) * range + minValue));
            });
            applyDiscreteMapping(JGitMetadataInput.LAST_COMMIT_DATE, String.class, mapping, visualProperty);
        } else if (property == GitDataProperty.TOTAL_NUMBER_OF_COMMITS) {
            // Continuous mapping from 0 to maxValue based on number of commits
            var func = (ContinuousMapping<Integer, T>)
                    vmff_continuous.createVisualMappingFunction(JGitMetadataInput.TOTAL_COMMITS, Integer.class, visualProperty);
            int maxCommits = getMostNodeCommits();
            T min = convertToT.apply(minValue);
            T max = convertToT.apply(maxValue);
            func.addPoint(0, new BoundaryRangeValues<T>(min, min, min));
            func.addPoint(maxCommits, new BoundaryRangeValues<T>(max, max, max));
            vmm.getVisualStyle(am.getCurrentNetworkView()).addVisualMappingFunction(func);
        } else if (property == GitDataProperty.LAST_COMMIT_AUTHOR) {
            // Discrete mapping of authors to doubles, from minValue to maxValue
            Map<String, T> mapping = new HashMap<>();
            var authors = getCommitAuthors();
            double currentValue = 0;
            if (authors.size() == 1) currentValue = 0.5; // If only one author choose value halfway
            for (var author : authors) { // Calculates width of each based on number of authors
                mapping.put(author, convertToT.apply(minValue+(currentValue++)*range/Math.max(1, authors.size()-1)));
            }
            applyDiscreteMapping(JGitMetadataInput.LAST_COMMIT_AUTHOR, String.class, mapping, visualProperty);
        }
    }

    /**
     * A list of properties that can be visualized, and their different options.
     */
    public enum GitDataProperty {
        NONE, MOST_RECENT_COMMIT_DATE, TOTAL_NUMBER_OF_COMMITS, LAST_COMMIT_AUTHOR;
        public static GitDataProperty[] getAllowedList() {
            return GitDataProperty.values();
        }
        @Override
        public String toString() {
            return name().charAt(0) + name().toLowerCase()
                    .replace("_", " ").substring(1);
        }
    }

    public enum GitDataVisualization {
        NONE, BORDER_WIDTH, CIRCLED_NODES, NODE_COLOR, NODE_SIZE;
        public static GitDataVisualization[] getAllowedList() {
            return GitDataVisualization.values();
        }
        @Override
        public String toString() {
            return name().charAt(0) + name().toLowerCase()
                    .replace("_", " ").substring(1);
        }
    }

    /**
     * Applies the selected combination of data property and visualization.
     * @param property the data property to map
     * @param vis the visualization to apply
     */
    public void applyGitDataStyle(GitDataProperty property, GitDataVisualization vis) {
        // Reset style first
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());

        style.removeVisualMappingFunction(NODE_BORDER_WIDTH);
        style.removeVisualMappingFunction(NODE_BORDER_TRANSPARENCY);
        style.setDefaultValue(NODE_BORDER_WIDTH, 0.0);
        style.setDefaultValue(NODE_BORDER_LINE_TYPE, LineTypeVisualProperty.SOLID);

        // Reset sizes
        if (vis != GitDataVisualization.NODE_SIZE || property == GitDataProperty.NONE)
            updateSizeMapping();

        // Reset the coloring
        if (vis != GitDataVisualization.NODE_COLOR || property == GitDataProperty.NONE)
            currentColoring.apply(this);

        switch (vis) {
            case BORDER_WIDTH: {
                style.setDefaultValue(NODE_BORDER_WIDTH, 0.0);
                style.setDefaultValue(NODE_BORDER_TRANSPARENCY, 255);
                style.setDefaultValue(NODE_BORDER_PAINT, Color.BLACK);  // set border color to black
                style.setDefaultValue(NODE_BORDER_LINE_TYPE, LineTypeVisualProperty.SOLID);
                applyNumberVisualization(property, 0,10, NODE_BORDER_WIDTH, (d) -> d);
                break;
            }
            case CIRCLED_NODES: {
                style.setDefaultValue(NODE_BORDER_WIDTH, 10.0);
                style.setDefaultValue(NODE_BORDER_TRANSPARENCY, 0);
                style.setDefaultValue(NODE_BORDER_PAINT, Color.BLACK);  // set border color to black
                style.setDefaultValue(NODE_BORDER_LINE_TYPE, retrieveParallelLinesStyle());
                applyNumberVisualization(property, 0,255, NODE_BORDER_TRANSPARENCY, Double::intValue);
                break;
            }
            case NODE_COLOR: {
                if (property == GitDataProperty.MOST_RECENT_COMMIT_DATE) {

                    // Coloring: Green --- Gray --- Red
                    var sortedDates = getLastCommitDateOptions();
                    Map<String, Paint> colorMapping = new HashMap<>();
                    // 4 most recent dates get green colors
                    for (int i = 0; i < 4 && !sortedDates.isEmpty(); i++) {
                        colorMapping.put(sortedDates.pollLast(), GREEN_4[i]);
                    }
                    // 4 oldest dates get red colors
                    for (int i = 0; i < 4 && !sortedDates.isEmpty(); i++) {
                        colorMapping.put(sortedDates.pollFirst(), RED_4[i]);
                    }
                    // The rest get gray
                    for (var d : sortedDates) {
                        colorMapping.put(d, Color.LIGHT_GRAY);
                    }
                    applyDiscreteMapping(JGitMetadataInput.LAST_COMMIT_DATE, String.class, colorMapping, NODE_FILL_COLOR);
                    break;

                } else if (property == GitDataProperty.LAST_COMMIT_AUTHOR) {
                    applyDiscreteColoring(JGitMetadataInput.LAST_COMMIT_AUTHOR);
                    break;
                } else if (property == GitDataProperty.TOTAL_NUMBER_OF_COMMITS) {
                    applyColoringByCommits();
                    break;
                }
            }
            case NODE_SIZE: {
                style.setDefaultValue(NODE_BORDER_WIDTH, 0.0);
                applyNumberVisualization(property, 20, 120, BasicVisualLexicon.NODE_SIZE, (d) -> d);
                applyNumberVisualization(property, 10, 60, NODE_LABEL_FONT_SIZE, Double::intValue);
                break;
            }
        }


    }

    /**
     * Sets the currentGitHistoryCutoff to the given value. The value of 0
     * means that all commits are shown. The value of 100 means that only the
     * most recent commit is shown.
     * @param value integer between 0 and 100
     * @return date of the cutoff commit or null if nothing changed
     */
    public String setGitHistoryCutoff(int value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("The value must be between 0 and 100");
        }
        if (currentGitHistoryCutoff != value) {
            currentGitHistoryCutoff = value;
            return applyGitHistoryCutoff();
        }
        return null;
    }

    /**
     * Apply currentGitHistoryCutoff to the graph. The value of 0
     * means that all commits are shown. The value of 100 means that only the
     * most recent commit is shown.
     * @return date of the cutoff commit
     */
    private String applyGitHistoryCutoff() {
        var net = am.getCurrentNetwork();
        var view = am.getCurrentNetworkView();
        if (net == null || view == null) return null;

        if (currentGitHistoryCutoff == 0) {
            // Show all commits
            for (var node : net.getNodeList()) {
                setNodeVisible(node, true);
            }
            var dates = getLastCommitDateOptions();
            return dates.isEmpty() ? null : dates.first();
        }

        // Get the cutoff commit out of the list of commits
        var dates = getLastCommitDateOptions();
        var totalCommits = dates.size();
        if (totalCommits <= 0) return null;

        var cutoffIndex = (int) Math.ceil((totalCommits-1) * (currentGitHistoryCutoff / 100.0));
        var cutoffDate = dates.toArray(new String[0])[cutoffIndex];

        // Hide all commits that are older than the cutoff commit
        for (var node : net.getNodeList()) {
            var date = net.getDefaultNodeTable().getRow(node.getSUID()).get(JGitMetadataInput.LAST_COMMIT_DATE, String.class);
            if (date == null) {
                setNodeVisible(node, false);
            } else {
                setNodeVisible(node, date.compareTo(cutoffDate) >= 0);
            }
        }

        return cutoffDate;
    }

    /**
     * Get the commit properties from the given commit date:
     * valid keys: "Author", "Message", "SHA"
     */
    public Properties getCommitPropertiesFromDate(String date) {
        var net = am.getCurrentNetwork();
        if (net == null) return null;
        for (var node : net.getNodeList()) {
            var nodeDate = net.getDefaultNodeTable().getRow(node.getSUID()).get(JGitMetadataInput.LAST_COMMIT_DATE, String.class);
            if (nodeDate != null && nodeDate.equals(date)) {
                Properties properties = new Properties();
                properties.setProperty("Author", net.getDefaultNodeTable().getRow(node.getSUID()).get(JGitMetadataInput.LAST_COMMIT_AUTHOR, String.class));
                properties.setProperty("Message", net.getDefaultNodeTable().getRow(node.getSUID()).get(JGitMetadataInput.LAST_COMMIT_MESSAGE, String.class));
                properties.setProperty("SHA", net.getDefaultNodeTable().getRow(node.getSUID()).get(JGitMetadataInput.LAST_COMMIT_SHA, String.class));
                return properties;
            }
        }
        return null;
    }

}
