package ca.usask.vga.layout.magnetic.poles;

import ca.usask.vga.layout.magnetic.ActionOnSelected;
import org.cytoscape.model.CyNetwork;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class RemovePoleAction  extends ActionOnSelected {

    private final PoleManager poleManager;

    public RemovePoleAction(PoleManager poleManager) {

        super("RemovePoleAction");

        this.poleManager = poleManager;

        ImageIcon icon = new ImageIcon(getClass().getResource("/remove_pole_icon.png"));
        putValue(LARGE_ICON_KEY, icon);

        putValue(SHORT_DESCRIPTION, "Remove selected poles");

        setToolbarGravity(16.2f);

        this.useToggleButton = false;
        this.inToolBar = true;
        this.insertSeparatorAfter = true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isSelectionActive()) {
            CyNetwork network = getNetwork();
            poleManager.removePole(network, getSelectedNodes());
            poleManager.updateTables(network);
        }
    }

}
