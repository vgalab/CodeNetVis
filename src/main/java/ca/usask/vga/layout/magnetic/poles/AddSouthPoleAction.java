package ca.usask.vga.layout.magnetic.poles;

import org.cytoscape.model.CyNetwork;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class AddSouthPoleAction extends ActionOnSelected {

    private final PoleManager poleManager;

    public AddSouthPoleAction(PoleManager poleManager) {

        super("AddPoleAction");

        this.poleManager = poleManager;

        ImageIcon icon = new ImageIcon(getClass().getResource("/add_pole_S_icon.png"));
        putValue(LARGE_ICON_KEY, icon);
        setPreferredMenu("Apps");

        putValue(SHORT_DESCRIPTION, "Make new South (Inward) poles from selected nodes");

        setToolbarGravity(16.12f);

        this.useToggleButton = false;
        this.inToolBar = true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isSelectionActive()) {
            CyNetwork network = getNetwork();
            poleManager.addPole(network, getSelectedNodes());
            poleManager.setPoleDirection(network, getSelectedNodes(), false);
            poleManager.updateTables(network);
        }
    }

}
