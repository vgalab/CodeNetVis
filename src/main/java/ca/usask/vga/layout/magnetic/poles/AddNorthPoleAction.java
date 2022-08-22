package ca.usask.vga.layout.magnetic.poles;

import ca.usask.vga.layout.magnetic.ActionOnSelected;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.util.swing.IconManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Properties;

import static org.cytoscape.work.ServiceProperties.*;

/**
 * An action that makes new North (Outward) poles from selected nodes,
 * and updates the screen to change the displayed colors.
 * Present in the toolbar and the context menu.
 */
public class AddNorthPoleAction  extends ActionOnSelected {

    private final PoleManager poleManager;

    private static final String TASK_DESCRIPTION = "Add new outward poles";

    public AddNorthPoleAction(CyApplicationManager am, IconManager im, PoleManager poleManager) {

        super(am, im, TASK_DESCRIPTION);

        this.poleManager = poleManager;

        ImageIcon icon = new ImageIcon(getClass().getResource("/icons/add_pole_N_icon.png"));
        im.addIcon(ICON_NAMESPACE+TASK_DESCRIPTION, icon);

        ImageIcon icon2 = new ImageIcon(getClass().getResource("/icons/add_pole_N_icon_16.png"));
        im.addIcon(ICON_NAMESPACE+TASK_DESCRIPTION+SMALL, icon2);

        putValue(LARGE_ICON_KEY, icon);
        putValue(SHORT_DESCRIPTION, "Make new North (Outward) poles from selected nodes");

        setToolbarGravity(16.11f);

        this.useToggleButton = false;
        this.inToolBar = true;
        this.insertToolbarSeparatorBefore = true;
    }

    @Override
    public void runTask(CyNetwork network, Collection<CyNode> selectedNodes) {
        poleManager.beginEdit(TASK_DESCRIPTION, network);

        poleManager.addPole(network, selectedNodes);
        poleManager.setPoleDirection(network, selectedNodes, true);
        poleManager.updateTables(network);

        poleManager.completeEdit();
    }

    @Override
    public boolean isReady(CyNetwork network, Collection<CyNode> selectedNodes) {
        return selectedNodes != null && selectedNodes.size() > 0;
    }

    @Override
    public Properties getNetworkTaskProperties() {
        Properties props = new Properties();
        props.setProperty(IN_MENU_BAR, "false");
        props.setProperty(PREFERRED_MENU, POLE_SUBMENU);
        props.setProperty(TITLE, "Set selected as outward poles");
        props.setProperty(MENU_GRAVITY, "98");
        props.setProperty(SMALL_ICON_ID, ICON_NAMESPACE+TASK_DESCRIPTION);
        return props;
    }

    @Override
    public Properties getTableTaskProperties() {
        var props = getNetworkTaskProperties();
        props.setProperty(TITLE, "Set selected as an outward pole");
        props.setProperty(SMALL_ICON_ID, ICON_NAMESPACE+TASK_DESCRIPTION+SMALL);
        return props;
    }
}