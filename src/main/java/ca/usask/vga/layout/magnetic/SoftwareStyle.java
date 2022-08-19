package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.poles.ExtraTasks;
import ca.usask.vga.layout.magnetic.poles.PoleManager;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.equations.EquationCompiler;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;
import org.cytoscape.task.hide.HideTaskFactory;
import org.cytoscape.task.hide.UnHideAllTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.*;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.cytoscape.work.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

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

    public boolean keepPolesLarger = true;

    protected final PinRadiusAnnotation pinRadiusAnnotation;
    protected final RingsAnnotation ringsAnnotation;

    private boolean showUnique = false;
    private String filterPrefix = "";

    public static String INDEGREE = "Indegree", OUTDEGREE = "Outdegree", DEGREE = "Degree";

    private SizeEquation currentSizeEquation = SizeEquation.FIXED;
    private int hideLabelsLessThan = 10;
    private double lastSetNodeSize = 30;

    private boolean usePoleColors = true;
    private Coloring currentColoring = Coloring.NONE;

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

    private void setShowPoleColors(boolean value) {
        usePoleColors = value;
        updatePoleColors();
    }

    private void updatePoleColors() {
        if (usePoleColors && pm.getPoleCount(am.getCurrentNetwork()) > 0) {
            var coloring = new ExtraTasks.LegacyPoleColoring(am, pm, vmm, vmff_discrete);
            tm.execute(new TaskIterator(coloring));
        }
    }

    private void clearNodeColorMappings() {
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        style.removeVisualMappingFunction(NODE_FILL_COLOR);
        style.removeVisualMappingFunction(EDGE_UNSELECTED_PAINT);
        style.removeVisualMappingFunction(EDGE_STROKE_UNSELECTED_PAINT);
    }

    public void setTopNasPoles(int n, boolean incoming) {
        var makeTop = new ExtraTasks.MakeTopDegreePoles(am, pm);
        makeTop.topN = n;
        makeTop.edgeType = incoming ? CyEdge.Type.INCOMING : CyEdge.Type.OUTGOING;
        tm.execute(new TaskIterator(makeTop));
    }

    public void setNodeSize(float value) {
        setNodeLabelSize(nodeSizeFunc(value));
    }

    private double nodeSizeFunc(double value) {
        double calculated = Math.round(Math.pow(10, 1+(value / 50))/2);
        if (value == 0) {
            calculated = 0.1;
        }
        return calculated;
    }

    private double nodeSizeFuncReverse(double size) {
        if (size == 0.1) return 0;
        return 50*(Math.log10(size*2) - 1);
    }

    private void setNodeLabelSize(double size) {
        // TODO EXISTING MAPPINGS
        // TODO NON ROUND NODES
        lastSetNodeSize = size;
        updateSizeMapping();

        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());

        style.setDefaultValue(NODE_SIZE, size);
        style.setDefaultValue(NODE_LABEL_FONT_SIZE, (int) Math.ceil(size/2));
    }

    private double getCurrentNodeSize() {
        if (am.getCurrentNetworkView() == null) return 30;
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        var value = style.getDefaultValue(NODE_SIZE);
        lastSetNodeSize = value;
        return value;
    }

    public float getInitialNodeSizeValue() {
        return (float) Math.max(0, Math.min(nodeSizeFuncReverse(getCurrentNodeSize()), 100));
    }

    public void setEdgeTransparency(float value) {
        int calculated = Math.round(value);

        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        style.setDefaultValue(EDGE_TRANSPARENCY, calculated);
    }

    public float getInitialEdgeTransparency() {
        if (am.getCurrentNetworkView() == null) return 120;
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        return style.getDefaultValue(EDGE_TRANSPARENCY);
    }

    public void setShowUnique(boolean showUnique) {
        if (this.showUnique == showUnique)
            return;
        this.showUnique = showUnique;
        reapplyFilters();
    }

    public void setFilterPrefix(String filterPrefix) {
        if (this.filterPrefix.equals(filterPrefix))
            return;
        this.filterPrefix = filterPrefix;
        reapplyFilters();
    }

    public void reapplyFilters() {

        var net = am.getCurrentNetwork();
        var view = am.getCurrentNetworkView();
        var table = net.getDefaultNodeTable();

        Set<CyNode> nodesToHide = new HashSet<>();

        // Hide nodes without prefix and not unique
        for (CyNode n : net.getNodeList()) {
            clearNodeVisible(n);
            String name = table.getRow(n.getSUID()).get("name", String.class);
            if (showUnique && (pm.isClosestToMultiple(net, n) || pm.isDisconnected(net, n))) {
                setNodeVisible(n, false);
            } else if (!filterPrefix.isEmpty() && (!name.startsWith(filterPrefix)) || name.length() < filterPrefix.length()) {
                setNodeVisible(n, false);
            } else {
                clearNodeVisible(n);
            }
        }
    }

    protected void setNodeVisible(CyNode node, boolean visible) {
        var view = am.getCurrentNetworkView();
        view.getNodeView(node).setLockedValue(NODE_VISIBLE, visible);
    }

    protected void clearNodeVisible(CyNode node) {
        var view = am.getCurrentNetworkView();
        view.getNodeView(node).clearValueLock(NODE_VISIBLE);
    }

    public PinRadiusAnnotation getRadiusAnnotation() {
        return pinRadiusAnnotation;
    }

    public RingsAnnotation getRingsAnnotation() {
        return ringsAnnotation;
    }

    @Override
    public void handleEvent(NetworkViewAboutToBeDestroyedEvent e) {
        try {pinRadiusAnnotation.onViewDestroyed(e);} catch (Exception ignored) {};
        try {ringsAnnotation.onViewDestroyed(e);} catch (Exception ignored) {};
    }

    public class PinRadiusAnnotation implements TooltipAnnotation {

        protected boolean visible;
        protected ShapeAnnotation annotation;
        protected float radius = 2500;
        protected CyNetworkView lastView;

        public PinRadiusAnnotation() {
        }

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
            return (i+1) * (getPinRadius() / (maxRings + 1));
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
            checkNetwork();
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

    protected boolean polesPresent() {
        return am.getCurrentNetwork() != null && pm.getPoleList(am.getCurrentNetwork()).size() > 0;
    }

    public float getSuggestedRadius() {
        var view = am.getCurrentNetworkView();
        if (view == null) return 2500;
        double scale = view.getVisualProperty(NETWORK_SCALE_FACTOR);
        double width = view.getVisualProperty(NETWORK_WIDTH) / scale;
        double height = view.getVisualProperty(NETWORK_HEIGHT) / scale;
        return Math.min((float) Math.min(width, height) / 2, 10000);
    }

    public enum SizeEquation {
        FIXED, INDEGREE, OUTDEGREE, DEGREE;
        @Override
        public String toString() {
            return name().charAt(0) + name().toLowerCase().substring(1);
        }
        public String getColumnName() {
            if (equals(FIXED)) return null;
            return toString();
        }
        public static SizeEquation[] getAllowedList() {
            return new SizeEquation[] {FIXED, INDEGREE, OUTDEGREE, DEGREE};
        }
    }

    public void setSizeEquation(SizeEquation sizeEquation) {
        if (sizeEquation == this.currentSizeEquation) return;
        currentSizeEquation = sizeEquation;
        updateSizeMapping();
    }

    private void updateSizeMapping() {
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        if (currentSizeEquation == SizeEquation.FIXED) {
            style.removeVisualMappingFunction(NODE_SIZE);
            style.removeVisualMappingFunction(NODE_LABEL_FONT_SIZE);
            if (keepPolesLarger) {
                var t = new ExtraTasks.MakePoleNodesLarger(am, vmm, vmff_discrete, null);
                t.keepIncreasing = false;
                t.run(ExtraTasks.getBlankTaskMonitor());
            }
        } else {
            initColumn(currentSizeEquation);

            var nodeSizeFunc = (ContinuousMapping<Integer, Double>) vmff_continuous.createVisualMappingFunction(currentSizeEquation.getColumnName(), Integer.class, NODE_SIZE);
            var fontSizeFunc = (ContinuousMapping<Integer, Integer>) vmff_continuous.createVisualMappingFunction(currentSizeEquation.getColumnName(), Integer.class, NODE_LABEL_FONT_SIZE);

            double s = lastSetNodeSize/2;
            int i = (int) Math.max(1, Math.ceil(s)/2);

            setPoints(nodeSizeFunc, s, 5*s, 10*s, 0, 100, 1000);
            style.addVisualMappingFunction(nodeSizeFunc);

            setPoints(fontSizeFunc, i, 5*i, 10*i, hideLabelsLessThan, 100, 1000);
            style.addVisualMappingFunction(fontSizeFunc);
        }
    }

    private void setPoints(ContinuousMapping<Integer, Double> map, double lower, double mid, double upper, int min, int handle, int max) {
        map.addPoint(min, new BoundaryRangeValues<>(0d, lower, lower));
        map.addPoint(handle, new BoundaryRangeValues<>(mid, mid, mid));
        map.addPoint(max, new BoundaryRangeValues<>(upper, upper, upper));
    }

    private void setPoints(ContinuousMapping<Integer, Integer> map, int lower, int mid, int upper, int min, int handle, int max) {
        map.addPoint(min, new BoundaryRangeValues<>(0, lower, lower));
        map.addPoint(handle, new BoundaryRangeValues<>(mid, mid, mid));
        map.addPoint(max, new BoundaryRangeValues<>(upper, upper, upper));
    }

    private void initColumn(SizeEquation s) {
        if (s == SizeEquation.FIXED) return;

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

    public VisualStyle applyDirectedStyle() {
        if (am.getCurrentNetworkView() == null) return null;
        var style = vsf.createVisualStyle(vmm.getDefaultVisualStyle());

        style.setDefaultValue(NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);
        style.setDefaultValue(EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.ARROW);
        style.setDefaultValue(EDGE_TARGET_ARROW_SIZE, 12d);

        style.setDefaultValue(NODE_SIZE, 30d);
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

    public void setLabelsPassthrough(String column) {
        PassthroughMapping<String, String> func = (PassthroughMapping<String, String>)
                vmff_passthrough.createVisualMappingFunction(column, String.class, NODE_LABEL);
        vmm.getVisualStyle(am.getCurrentNetworkView()).addVisualMappingFunction(func);
    }

    public void setTooltipsPassthrough(String column) {
        PassthroughMapping<String, String> func = (PassthroughMapping<String, String>)
                vmff_passthrough.createVisualMappingFunction(column, String.class, NODE_TOOLTIP);
        vmm.getVisualStyle(am.getCurrentNetworkView()).addVisualMappingFunction(func);
    }

    public enum Coloring {
        NONE, PACKAGE, CLOSEST_POLE;
        public void apply(SoftwareStyle s) {
            s.setShowPoleColors(false);
            switch (this) {
                case PACKAGE:
                    s.applyDiscreteColoring("Package");
                    return;
                case CLOSEST_POLE:
                    s.setShowPoleColors(true);
                    return;
                default:
                    s.clearNodeColorMappings();
            }
        }
    }

    public void setCurrentColoring(Coloring c) {
        if (currentColoring != c) {
            currentColoring = c;
            c.apply(this);
        }
    }

    protected void applyDiscreteColoring(String column) {
        // Allows for parallel computation
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        style.removeVisualMappingFunction(EDGE_UNSELECTED_PAINT);
        style.removeVisualMappingFunction(EDGE_STROKE_UNSELECTED_PAINT);
        tm.execute(new TaskIterator(new ApplyDiscreteColoringTask(column, String.class)));
    }

    private <T> void applyDiscreteColoring(String column, Class<T> type) {
        var view = am.getCurrentNetworkView();
        var net = am.getCurrentNetwork();
        if (view == null || net == null) return;

        var func = (DiscreteMapping<T, Paint>)
                vmff_discrete.createVisualMappingFunction(column, type, NODE_FILL_COLOR);

        var table = net.getDefaultNodeTable();
        var values = new TreeSet<>(table.getColumn(column).getValues(type));

        int i = 0;
        for (var v : values) {
            func.putMapValue(v, COLOR_BREWER_SET3[i%12]);
            i++;
        }

        vmm.getVisualStyle(view).addVisualMappingFunction(func);
    }

    public void onFileLoaded(String fileFormat) {
        if (am.getCurrentNetworkView() == null) return;

        var style = applyDirectedStyle();

        if (fileFormat.equals("jar") || fileFormat.equals("java")) {
            // Set class as the labels
            setLabelsPassthrough("Class");
            setTooltipsPassthrough("Package");
            applyDiscreteColoring("Package");
        }

    }

    // From https://colorbrewer2.org/
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
            new Color(255,237,111)
    };

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
            applyDiscreteColoring(column, type);
        }
    }

    protected TaskObserver afterFinished(Runnable r) {
        return new TaskObserver() {
            public void taskFinished(ObservableTask task) {}
            @Override
            public void allFinished(FinishStatus finishStatus) {
                r.run();
            }
        };
    }

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

}
