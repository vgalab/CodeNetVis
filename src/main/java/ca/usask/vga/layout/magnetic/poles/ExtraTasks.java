
package ca.usask.vga.layout.magnetic.poles;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.*;
import org.cytoscape.work.*;
import org.cytoscape.work.util.ListSingleSelection;

import java.util.*;

import static org.cytoscape.work.ServiceProperties.*;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;

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
                pm.removeAllPoles(currentNet);
                pm.addPole(currentNet, topNodes);
                pm.setPoleDirection(currentNet, topNodes, edgeType == CyEdge.Type.OUTGOING);
                pm.updateTables(currentNet);
                taskMonitor.showMessage(TaskMonitor.Level.INFO, "Successfully selected " + topNodes.size() + " poles");
                taskMonitor.showMessage(TaskMonitor.Level.INFO, pm.getPoleNameList(currentNet).toString());
            } else {
                taskMonitor.showMessage(TaskMonitor.Level.WARN, "No network has been selected");
            }

        }
    }

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
            props.setProperty(MENU_GRAVITY, "6.1");
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


}

