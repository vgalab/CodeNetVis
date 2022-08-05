package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.poles.ExtraTasks;
import ca.usask.vga.layout.magnetic.poles.PoleManager;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.task.hide.HideTaskFactory;
import org.cytoscape.task.hide.UnHideAllTaskFactory;
import org.cytoscape.view.layout.LayoutPartition;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.TaskManager;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SoftwareStyle {

    private final CyApplicationManager am;
    private final TaskManager tm;
    private final VisualMappingManager vmm;
    private final VisualMappingFunctionFactory vmff_passthrough;
    private final VisualMappingFunctionFactory vmff_discrete;
    private final VisualMappingFunctionFactory vmff_continuous;
    private final PoleManager pm;
    private final AnnotationManager anm;
    private final AnnotationFactory<ShapeAnnotation> anf;
    private final HideTaskFactory htf;
    private final UnHideAllTaskFactory utf;

    public boolean keepPolesLarger = true;

    protected final PinRadiusAnnotation pinRadiusAnnotation;
    protected final RingsAnnotation ringsAnnotation;
    private boolean showUnique;

    public SoftwareStyle(CyApplicationManager am, TaskManager tm, VisualMappingManager vmm,
                         VisualMappingFunctionFactory vmff_passthrough,
                         VisualMappingFunctionFactory vmff_discrete,
                         VisualMappingFunctionFactory vmff_continuous, PoleManager pm,
                         AnnotationManager anm, AnnotationFactory anf,
                         HideTaskFactory htf, UnHideAllTaskFactory utf) {
        this.am = am;
        this.tm = tm;
        this.vmm = vmm;
        this.vmff_passthrough = vmff_passthrough;
        this.vmff_discrete = vmff_discrete;
        this.vmff_continuous = vmff_continuous;
        this.pm = pm;
        this.anm = anm;
        this.anf = anf;
        this.htf = htf;
        this.utf = utf;
        pinRadiusAnnotation = new PinRadiusAnnotation();
        ringsAnnotation = new RingsAnnotation();
    }

    public void setNodeSize(float value) {
        setNodeLabelSize(nodeSizeFunc(value));
    }

    private double nodeSizeFunc(double value) {
        double calculated = Math.round(Math.pow(10, 1+(value / 50)));
        if (calculated == 10) {
            calculated = 0.1;
        }
        return calculated;
    }

    private double nodeSizeFuncReverse(double size) {
        if (size == 0.1) size = 10;
        return 50*(Math.log10(size) - 1);
    }

    private void setNodeLabelSize(double size) {
        // TODO EXISTING MAPPINGS
        // TODO NON ROUND NODES

        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());

        style.setDefaultValue(BasicVisualLexicon.NODE_SIZE, size);
        style.setDefaultValue(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, (int) Math.ceil(size/2));

        if (keepPolesLarger) {
            var t = new ExtraTasks.MakePoleNodesLarger(am, vmm, vmff_discrete, null);
            t.keepIncreasing = false;
            t.run(ExtraTasks.getBlankTaskMonitor());
        }

    }

    private double getCurrentNodeSize() {
        if (am.getCurrentNetworkView() == null) return 30;
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        return style.getDefaultValue(BasicVisualLexicon.NODE_SIZE);
    }

    public float getInitialNodeSizeValue() {
        return (float) Math.max(0, Math.min(nodeSizeFuncReverse(getCurrentNodeSize()), 100));
    }

    public void setEdgeTransparency(float value) {
        int calculated = Math.round(value);

        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        style.setDefaultValue(BasicVisualLexicon.EDGE_TRANSPARENCY, calculated);
    }

    public float getInitialEdgeTransparency() {
        if (am.getCurrentNetworkView() == null) return 120;
        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        return style.getDefaultValue(BasicVisualLexicon.EDGE_TRANSPARENCY);
    }

    public void setShowUnique(boolean showUnique) {
        if (this.showUnique == showUnique)
            return;

        this.showUnique = showUnique;

        var net = am.getCurrentNetwork();
        var view = am.getCurrentNetworkView();

        if (showUnique) {
            List<CyNode> nodesToHide = new ArrayList<>();

            for (CyNode n : net.getNodeList()) {
                if (pm.isClosestToMultiple(net, n) || pm.isDisconnected(net, n)) {
                    nodesToHide.add(n);
                }
            }

            tm.execute(htf.createTaskIterator(view, nodesToHide, new ArrayList<>()));
        } else {
            tm.execute(utf.createTaskIterator(view));
        }
    }

    public PinRadiusAnnotation getRadiusAnnotation() {
        return pinRadiusAnnotation;
    }

    public RingsAnnotation getRingsAnnotation() {
        return ringsAnnotation;
    }

    public class PinRadiusAnnotation implements TooltipAnnotation {

        protected boolean visible;
        protected ShapeAnnotation annotation;
        protected float radius = 2500;

        public PinRadiusAnnotation() {
        }

        protected void init() {
            if (annotation != null) return;
            var argMap = new HashMap<String, String>();
            annotation = anf.createAnnotation(ShapeAnnotation.class, am.getCurrentNetworkView(), argMap);
            annotation.setShapeType(ShapeAnnotation.ShapeType.ELLIPSE.shapeName());
            annotation.setBorderWidth(20);
            reposition();
        }

        protected void checkNetwork() {
            if (annotation != null && annotation.getNetworkView() != am.getCurrentNetworkView()) {
                anm.removeAnnotation(annotation);
                annotation = null;
                init();
            }
        }

        public void reposition() {
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
            anm.removeAnnotation(annotation);
        }

        @Override
        public void setVisible(boolean visible) {
            this.visible = visible;
            if (visible) show();
            else hide();
        }

        @Override
        public void reset() {
            annotation = null;
        }
    }

    public class RingsAnnotation implements TooltipAnnotation {

        protected boolean visible;
        protected int maxRings = 4;

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
            for (int i = 0; i < maxRings; i++) {
                if (annotations.size() <= i) {
                    var argMap = new HashMap<String, String>();
                    var a = anf.createAnnotation(ShapeAnnotation.class, am.getCurrentNetworkView(), argMap);
                    a.setShapeType(ShapeAnnotation.ShapeType.ELLIPSE.shapeName());
                    a.setBorderWidth(10);
                    a.setBorderOpacity(50);
                    annotations.add(a);
                }
            }
            hide();
        }

        protected void checkNetwork() {
            if (annotations.size() > 0 && annotations.get(0).getNetworkView() != am.getCurrentNetworkView()) {
                anm.removeAnnotations(annotations);
                annotations.clear();
                init();
            }
        }

        public void reposition() {
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
            anm.removeAnnotations(annotations);
        }

        @Override
        public void reset() {
            annotations.clear();
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
                x += nodeV.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
                y += nodeV.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
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
            x = nodeV.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
            y = nodeV.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
        }
        return new Point2D.Double(x+offsetX, y+offsetY);
    }

    protected boolean polesPresent() {
        return am.getCurrentNetwork() != null && pm.getPoleList(am.getCurrentNetwork()).size() > 0;
    }

    public float getSuggestedRadius() {
        var view = am.getCurrentNetworkView();
        if (view == null) return 2500;
        double scale = view.getVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR);
        double width = view.getVisualProperty(BasicVisualLexicon.NETWORK_WIDTH) / scale;
        double height = view.getVisualProperty(BasicVisualLexicon.NETWORK_HEIGHT) / scale;
        return Math.min((float) Math.min(width, height) / 2, 10000);
    }


}
