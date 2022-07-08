
package ca.usask.vga.layout.magnetic.poles;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.*;
import org.cytoscape.work.util.ListSingleSelection;

import java.util.*;

import static org.cytoscape.work.ServiceProperties.*;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;

public class ExtraTasks {

    final static String MENU_APP_ROOT = "Apps.Magnetic Layout";

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

    public static class MakeTopDegreePoles extends AbstractTask {

        private final CyApplicationManager am;
        private final PoleManager pm;

        public MakeTopDegreePoles(CyApplicationManager am, PoleManager pm) {
            this.am = am;
            this.pm = pm;
        }

        private final String TASK_DESCRIPTION = "Set top N degree nodes as poles";

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

        private int computeDegree(CyNetwork network, CyNode node, CyEdge.Type type) {
            int degree = 0;
            for(CyEdge edge : network.getAdjacentEdgeIterable(node, type)) {
                degree++;
            }
            return degree;
        }

        public List<CyNode> getTopNodes(final CyNetwork net, int N, final CyEdge.Type edgeType) {

            final CyTable table = net.getDefaultNodeTable();

            Comparator<CyNode> byDegree = new Comparator<CyNode>() {
                @Override
                public int compare(CyNode a, CyNode b) {
                    int ret = computeDegree(net, a, edgeType) - computeDegree(net, b, edgeType);
                    if (ret != 0)
                        return ret;
                    String nameA = table.getRow(a.getSUID()).get("name", String.class);
                    String nameB = table.getRow(b.getSUID()).get("name", String.class);
                    return nameA.compareTo(nameB);
                }
            };

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


}

