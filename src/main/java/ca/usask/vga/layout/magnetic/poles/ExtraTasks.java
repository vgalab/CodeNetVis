
package ca.usask.vga.layout.magnetic.poles;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.*;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.work.*;
import org.cytoscape.work.undo.UndoSupport;
import org.cytoscape.work.util.ListSingleSelection;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.cytoscape.work.ServiceProperties.*;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;

/**
 * Contains extra actions for working with the poles that are
 * displayed in the application menu, and could be used as part
 * of more complex procedures.
 * @see MakeTopDegreePoles
 * @see SelectAllPoles
 * @see LegacyPoleColoring
 * @see CopyNodeStyleToEdge
 * @see MakePoleNodesLarger
 */
public class ExtraTasks {

    final static String MENU_APP_ROOT = "Apps.Magnetic Layout";

    // UTILITY METHODS

    public static TaskFactory getTaskFactory(final Task task) {
        return new TaskFactory() {
            @Override
            public TaskIterator createTaskIterator() {
                return new TaskIterator(task);
            }
            @Override
            public boolean isReady() {
                return true;
            }
        };
    }

    public static TaskMonitor getBlankTaskMonitor() {
        return new TaskMonitor() {
            public void setTitle(String title) {}
            public void setProgress(double progress) {}
            public void setStatusMessage(String statusMessage) {}
            public void showMessage(Level level, String message) {}
        };
    }

    public static int computeDegree(CyNetwork network, CyNode node, CyEdge.Type type) {
        int degree = 0;
        for(CyEdge ignored : network.getAdjacentEdgeIterable(node, type)) {
            degree++;
        }
        return degree;
    }

    public static Comparator<CyNode> getByDegreeComparator(final CyNetwork net, final CyEdge.Type edgeType) {
        return new Comparator<CyNode>() {
            @Override
            public int compare(CyNode a, CyNode b) {
                int ret = computeDegree(net, a, edgeType) - computeDegree(net, b, edgeType);
                CyTable table = net.getDefaultNodeTable();
                if (ret != 0)
                    return ret;
                // First by degree then by name
                String nameA = table.getRow(a.getSUID()).get("name", String.class);
                String nameB = table.getRow(b.getSUID()).get("name", String.class);
                return nameA.compareTo(nameB);
            }
        };
    }

    // TASK CLASSES

    /**
     * Asks the user which type of degree they want to use as poles and how many,
     * then selects top N in/out degree nodes as the poles for the current network.
     */
    public static class MakeTopDegreePoles extends AbstractTask {

        private final CyApplicationManager am;
        private final PoleManager pm;

        public MakeTopDegreePoles(CyApplicationManager am, PoleManager pm) {
            this.am = am;
            this.pm = pm;
        }

        private static final String TASK_DESCRIPTION = "Set top N degree nodes as poles";

        public Properties getDefaultProperties() {
            Properties props = new Properties();
            props.setProperty(PREFERRED_MENU, MENU_APP_ROOT);
            props.setProperty(TITLE, TASK_DESCRIPTION);
            props.setProperty(IN_MENU_BAR, "true");
            props.setProperty(MENU_GRAVITY, "5.1");
            /*props.setProperty(COMMAND, "setPolesByDegree");
            props.setProperty(COMMAND_NAMESPACE, COMMAND_NAMESPACE);
            props.setProperty(COMMAND_DESCRIPTION, TASK_DESCRIPTION);*/
            return props;
        }

        @Tunable(description="Select top # nodes:")
        public int topN = 10;

        @Tunable(description = "Degree type", exampleStringValue = "INCOMING")
        public ListSingleSelection<CyEdge.Type> getEdgeType() {
            ListSingleSelection<CyEdge.Type> t = new ListSingleSelection<>(CyEdge.Type.INCOMING, CyEdge.Type.OUTGOING);
            t.setSelectedValue(this.edgeType);
            return t;
        }
        public void setEdgeType(ListSingleSelection<CyEdge.Type> t) {
            this.edgeType = (CyEdge.Type) t.getSelectedValue();
        }
        public CyEdge.Type edgeType = CyEdge.Type.INCOMING;

        public List<CyNode> getTopNodes(final CyNetwork net, int N, final CyEdge.Type edgeType) {

            final CyTable table = net.getDefaultNodeTable();

            Comparator<CyNode> byDegree = getByDegreeComparator(net, edgeType);

            PriorityQueue<CyNode> maxHeap = new PriorityQueue<>(N+1, byDegree);

            for (CyNode node : net.getNodeList()) {
                maxHeap.add(node);

                if (maxHeap.size() > N) {
                    maxHeap.poll();
                }
            }

            List<CyNode> topKList = new ArrayList<>(maxHeap);
            Collections.sort(topKList, byDegree);
            Collections.reverse(topKList);

            return topKList;
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {

            taskMonitor.setTitle(TASK_DESCRIPTION);

            CyNetwork currentNet = am.getCurrentNetwork();

            if (currentNet != null) {
                List<CyNode> topNodes = getTopNodes(currentNet, topN, edgeType);
                pm.beginEdit(TASK_DESCRIPTION, currentNet);
                pm.removeAllPoles(currentNet);
                pm.addPole(currentNet, topNodes);
                pm.setPoleDirection(currentNet, topNodes, edgeType == CyEdge.Type.OUTGOING);
                pm.updateTables(currentNet);
                pm.completeEdit();
                taskMonitor.showMessage(TaskMonitor.Level.INFO, "Successfully selected " + topNodes.size() + " poles");
                taskMonitor.showMessage(TaskMonitor.Level.INFO, pm.getPoleNameList(currentNet).toString());
            } else {
                taskMonitor.showMessage(TaskMonitor.Level.WARN, "No network has been selected");
            }

        }
    }

    /**
     * Selects all the nodes on the pole list on the network view.
     * Further actions may be performed on the selected nodes.
     */
    public static class SelectAllPoles extends AbstractTask {

        private final CyApplicationManager am;
        private final PoleManager pm;

        public SelectAllPoles(CyApplicationManager am, PoleManager pm) {
            this.am = am;
            this.pm = pm;
        }

        private static final String TASK_DESCRIPTION = "Select all poles";

        public Properties getDefaultProperties() {
            Properties props = new Properties();
            props.setProperty(PREFERRED_MENU, MENU_APP_ROOT);
            props.setProperty(TITLE, TASK_DESCRIPTION);
            props.setProperty(IN_MENU_BAR, "true");
            props.setProperty(MENU_GRAVITY, "2.1");
            // Commands here
            return props;
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {

            taskMonitor.setTitle(TASK_DESCRIPTION);

            CyNetwork net = am.getCurrentNetwork();
            if (net == null) return;

            List<CyNode> poles = pm.getPoleList(net);

            CyTable table = net.getDefaultNodeTable();

            // Unselect old selection
            List<CyNode> oldSelected = CyTableUtil.getNodesInState(net, CyNetwork.SELECTED, true);
            for (CyNode node : oldSelected) {
                table.getRow(node.getSUID()).set(CyNetwork.SELECTED, false);
            }

            // Select poles only
            for (CyNode node : poles) {
                table.getRow(node.getSUID()).set(CyNetwork.SELECTED, true);
            }

        }
    }

    /**
     * Applies the original coloring (orange, blue, pink ...) from the
     * first implementation of the magnetic layout in C++.
     * Colors both nodes and edges.
     */
    public static class LegacyPoleColoring extends AbstractTask {

        private final CyApplicationManager am;
        private final PoleManager pm;
        private final VisualMappingManager vmm;
        private final VisualMappingFunctionFactory vmff;

        public LegacyPoleColoring(CyApplicationManager am, PoleManager pm, VisualMappingManager vmm, VisualMappingFunctionFactory vmff_discrete) {
            this.am = am;
            this.pm = pm;
            this.vmm = vmm;
            this.vmff = vmff_discrete;
        }

        private static final String TASK_DESCRIPTION = "Apply legacy pole coloring";

        public Properties getDefaultProperties() {
            Properties props = new Properties();
            props.setProperty(PREFERRED_MENU, MENU_APP_ROOT);
            props.setProperty(TITLE, TASK_DESCRIPTION);
            props.setProperty(IN_MENU_BAR, "true");
            props.setProperty(MENU_GRAVITY, "10.1");
            // Commands here
            return props;
        }

        public static Color getColorByIndex(int index) {
            double r, g, b;
            switch (index) {
                case 0: r = 1.0; g = 0.5; b = 0.0; break; // orange
                case 1: r = 0.0; g = 0.65; b = 0.93; break; // sky blue
                case 2: r = 0.85; g = 0.39; b = 0.6; break; // pink
                case 3: r = 0.5; g = 0.72; b = 0; break; // lime
                case 4: r = 0.14; g = 0; b = 0.68; break; // navy
                case 5: r = 0.53; g = 0.36; b = 0.0; break; // brown
                case 6: r = 0.2; g = 0.75; b = 0.58; break; // teal
                case 7: r = 0.65; g = 0; b = 0; break; // dark red
                case 8: r = 0; g = 0.41; b = 0.07; break; // dark green
                case 9: r = 0.67; g = 0; b = 0.64; break; // purple
                default: r = 1.0; g = 0.0; b = 0.0; break; // red for all others
            }
            return new Color((float) r, (float) g, (float) b);
        }

        public static Color getMultipleColor() {
            return new Color( 0.5f, 0.5f, 0.5f);
        }

        public static Color getDisconnectedColor() {
            return new Color( 0.8f, 0.8f, 0.8f);
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {

            taskMonitor.setTitle(TASK_DESCRIPTION);

            CyNetwork net = am.getCurrentNetwork();
            if (net == null) return;

            List<CyNode> poles = pm.getPoleListSorted(net, ExtraTasks.getByDegreeComparator(net, CyEdge.Type.INCOMING));

            DiscreteMapping<String, Paint> func = (DiscreteMapping<String, Paint>)
                    vmff.createVisualMappingFunction(PoleManager.NAMESPACE + "::" + PoleManager.CLOSEST_POLE,
                            String.class, BasicVisualLexicon.NODE_FILL_COLOR);

            for (int i = 0; i < poles.size(); i++) {
                CyNode pole = poles.get(i);
                String name = pm.getPoleName(net, pole);
                Color color = getColorByIndex(i);
                func.putMapValue(name, color);
            }

            func.putMapValue(PoleManager.MULTIPLE_POLES_NAME, getMultipleColor());
            func.putMapValue(PoleManager.DISCONNECTED_NAME, getDisconnectedColor());

            VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());

            var edit = new VizMapEdit(TASK_DESCRIPTION, style, BasicVisualLexicon.NODE_FILL_COLOR,
                    BasicVisualLexicon.EDGE_UNSELECTED_PAINT, BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);

            style.addVisualMappingFunction(func);

            CopyNodeStyleToEdge copying = new CopyNodeStyleToEdge(am, vmm, vmff, pm.undoSupport);
            String columnName = PoleManager.EDGE_TARGET_NODE_POLE;

            copying.copyPoleVisualMap(BasicVisualLexicon.EDGE_UNSELECTED_PAINT, columnName);
            copying.copyPoleVisualMap(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, columnName);

            edit.completeEdit(pm.undoSupport);
        }
    }

    /**
     * Copies the color mapping from the nodes to the edges by a desired
     * edge table column. Commonly used to color edges the same color as the nodes.
     */
    public static class CopyNodeStyleToEdge extends AbstractTask {

        private final CyApplicationManager am;
        private final VisualMappingManager vmm;
        private final VisualMappingFunctionFactory vmff;
        private final UndoSupport undoSupport;

        public CopyNodeStyleToEdge(CyApplicationManager am, VisualMappingManager vmm, VisualMappingFunctionFactory vmff_discrete, UndoSupport undoSupport) {
            this.am = am;
            this.vmm = vmm;
            this.vmff = vmff_discrete;
            this.undoSupport = undoSupport;
        }

        private static final String TASK_DESCRIPTION = "Copy node colors to edge colors";

        public Properties getDefaultProperties() {
            Properties props = new Properties();
            props.setProperty(PREFERRED_MENU, MENU_APP_ROOT);
            props.setProperty(TITLE, TASK_DESCRIPTION);
            props.setProperty(IN_MENU_BAR, "true");
            props.setProperty(MENU_GRAVITY, "9.1");
            // Commands here
            return props;
        }

        public boolean copyPoleVisualMap(VisualProperty<Paint> visualProperty, String columnName) {
            VisualMappingFunction<?, Paint> from = vmm.getVisualStyle(am.getCurrentNetworkView())
                    .getVisualMappingFunction(BasicVisualLexicon.NODE_FILL_COLOR);
            DiscreteMapping<String, Paint> to = (DiscreteMapping<String, Paint>)
                    vmff.createVisualMappingFunction(PoleManager.NAMESPACE + "::" + columnName,
                            String.class, visualProperty);
            if (!(from instanceof DiscreteMapping))
                return false;
            if (!from.getMappingColumnName().equals(PoleManager.NAMESPACE + "::" + PoleManager.CLOSEST_POLE))
                return false;
            to.putAll(((DiscreteMapping) from).getAll());
            vmm.getVisualStyle(am.getCurrentNetworkView()).addVisualMappingFunction(to);
            return true;
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws IllegalArgumentException {

            taskMonitor.setTitle(TASK_DESCRIPTION);

            CyNetwork net = am.getCurrentNetwork();
            if (net == null) return;

            var edit = new VizMapEdit(TASK_DESCRIPTION, vmm.getVisualStyle(am.getCurrentNetworkView()),
                    BasicVisualLexicon.EDGE_UNSELECTED_PAINT, BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);

            String columnName = PoleManager.EDGE_TARGET_NODE_POLE;

            boolean copySuccess = copyPoleVisualMap(BasicVisualLexicon.EDGE_UNSELECTED_PAINT, columnName);
            copyPoleVisualMap(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, columnName);

            if (!copySuccess) {
                throw new IllegalArgumentException("Incompatible visual styles, edges were left unchanged.");
            } else {
                edit.completeEdit(undoSupport);
            }

        }
    }

    /**
     * Makes poles 2x as large on the graph, while the rest of the nodes
     * are all the same size. Incompatible with changing size by the degree.
     */
    public static class MakePoleNodesLarger extends AbstractTask {

        private final CyApplicationManager am;
        private final VisualMappingManager vmm;
        private final VisualMappingFunctionFactory vmff;
        private final UndoSupport undoSupport;

        public boolean keepIncreasing = true;

        public MakePoleNodesLarger(CyApplicationManager am, VisualMappingManager vmm, VisualMappingFunctionFactory vmff_discrete, UndoSupport undoSupport) {
            this.am = am;
            this.vmm = vmm;
            this.vmff = vmff_discrete;
            this.undoSupport = undoSupport;
        }

        private static final String TASK_DESCRIPTION = "Make pole nodes larger";

        public Properties getDefaultProperties() {
            Properties props = new Properties();
            props.setProperty(PREFERRED_MENU, MENU_APP_ROOT);
            props.setProperty(TITLE, TASK_DESCRIPTION);
            props.setProperty(IN_MENU_BAR, "true");
            props.setProperty(MENU_GRAVITY, "9.3");
            // Commands here
            return props;
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws IllegalArgumentException {

            taskMonitor.setTitle(TASK_DESCRIPTION);

            CyNetwork net = am.getCurrentNetwork();
            if (net == null) return;

            String columnName = PoleManager.NAMESPACE + "::" + PoleManager.IS_POLE;

            DiscreteMapping<Boolean, Double> func = (DiscreteMapping<Boolean, Double>)
                    vmff.createVisualMappingFunction(columnName,
                            Boolean.class, BasicVisualLexicon.NODE_SIZE);

            VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());

            var edit = new VizMapEdit(TASK_DESCRIPTION, style, BasicVisualLexicon.NODE_SIZE);

            double defaultSize = style.getDefaultValue(BasicVisualLexicon.NODE_SIZE);

            VisualMappingFunction<?, Double> oldFunc = style.getVisualMappingFunction(BasicVisualLexicon.NODE_SIZE);

            if (keepIncreasing && oldFunc instanceof DiscreteMapping && oldFunc.getMappingColumnName().equals(columnName)) {
                defaultSize = ((DiscreteMapping<Boolean, Double>) oldFunc).getMapValue(true);
            }

            func.putMapValue(true, defaultSize*2);

            style.addVisualMappingFunction(func);

            if (undoSupport != null)
                edit.completeEdit(undoSupport);
        }
    }

}

