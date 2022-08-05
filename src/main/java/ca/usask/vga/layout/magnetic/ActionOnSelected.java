package ca.usask.vga.layout.magnetic;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.*;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.task.TableCellTaskFactory;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

/**
 * Allows actions to act on the selected nodes/edges in the current view
 * TODO: Rewrite to work even without events
 */
public abstract class ActionOnSelected extends AbstractCyAction implements SelectedNodesAndEdgesListener {

    private SelectedNodesAndEdgesEvent selectionEvent;

    protected final CyApplicationManager am;
    protected final IconManager im;
    private final String taskName;

    protected final String POLE_SUBMENU = "Poles";
    protected final String ICON_NAMESPACE = "MagneticLayout::";
    protected final String SMALL = " Small";

    public ActionOnSelected(CyApplicationManager am, IconManager im, String name) {
        super(name);
        this.am = am;
        this.im = im;
        taskName = name;
        updateSelectedNodes();
    }

    public String getTaskName() {
        return taskName;
    }

    // Actions override these
    public abstract void runTask(CyNetwork network, Collection<CyNode> selectedNodes);

    public boolean isReady(CyNetwork network, Collection<CyNode> selectedNodes) { return true; }

    public abstract Properties getNetworkTaskProperties();
    public abstract Properties getTableTaskProperties();

    private boolean isNetworkViewReady() {
        return isNetworkViewSelectionActive() && isReady(getNetwork(), getSelectedNetworkViewNodes());
    }

    private TaskIterator newTaskIterator(CyNetwork network, Collection<CyNode> selectedNodes) {
        return new TaskIterator(new Task() {
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                taskMonitor.setTitle(getTaskName());
                runTask(network, selectedNodes);
            }
            @Override
            public void cancel() {}
        });
    }

    private TaskIterator networkViewTaskIterator() {
        return newTaskIterator(getNetwork(), getSelectedNetworkViewNodes());
    }

    private boolean isTableViewReady(CyColumn column, Object primaryKeyValue) {
        var node = getNetwork().getNode((long) primaryKeyValue);
        return node != null;
    }

    private TaskIterator tableViewTaskIterator(CyColumn column, Object primaryKeyValue) {
        var node = getNetwork().getNode((long) primaryKeyValue);
        if (node == null) return new TaskIterator(new Task() {
            public void run(TaskMonitor taskMonitor) throws Exception {
                taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Node not found " + primaryKeyValue);
            }
            public void cancel() {}
        });
        var collection = Collections.singleton(node);
        return newTaskIterator(getNetwork(), collection);
    }

    private void updateSelectedNodes() {
        if (am.getCurrentNetwork() == null) {
            selectionEvent = null;
            return;
        }
        selectionEvent = new SelectedNodesAndEdgesEvent(am.getCurrentNetwork(),
                true, true, true);
    }

    private CyNetwork getNetwork() {
        return selectionEvent.getNetwork();
    }

    private Collection<CyNode> getSelectedNetworkViewNodes() {
        if (!isNetworkViewSelectionActive()) return new ArrayList<>();
        return selectionEvent.getSelectedNodes();
   }

    private boolean isNetworkViewSelectionActive() {
        return selectionEvent != null && selectionEvent.isCurrentNetwork();
    }

    // AbstractCyAction
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isNetworkViewSelectionActive()) {
            runTask(getNetwork(), getSelectedNetworkViewNodes());
        }
    }

    // SelectedNodesAndEdgesListener
    @Override
    public void handleEvent(SelectedNodesAndEdgesEvent event) {
        selectionEvent = event;
    }

    public NetworkViewTaskFactory getNetworkTaskFactory() {
        return new NetworkViewTaskFactory() {
            @Override
            public boolean isReady(CyNetworkView networkView) {
                return isNetworkViewReady();
            }
            @Override
            public TaskIterator createTaskIterator(CyNetworkView networkView) {
                return networkViewTaskIterator();
            }
        };
    }

    public TableCellTaskFactory getTableCellTaskFactory() {
        return new TableCellTaskFactory() {
            @Override
            public boolean isReady(CyColumn column, Object primaryKeyValue) {
                return isTableViewReady(column, primaryKeyValue);
            }
            @Override
            public TaskIterator createTaskIterator(CyColumn column, Object primaryKeyValue) {
                return tableViewTaskIterator(column, primaryKeyValue);
            }
        };
    }

    public NodeViewTaskFactory getNodeViewTaskFactory() {
        return new NodeViewTaskFactory() {
            @Override
            public boolean isReady(View<CyNode> view, CyNetworkView cyNetworkView) {
                return isNetworkViewReady();
            }
            @Override
            public TaskIterator createTaskIterator(View<CyNode> view, CyNetworkView cyNetworkView) {
                return networkViewTaskIterator();
            }
        };
    }
}
