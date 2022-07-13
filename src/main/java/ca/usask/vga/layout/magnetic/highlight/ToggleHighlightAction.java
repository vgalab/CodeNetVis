package ca.usask.vga.layout.magnetic.highlight;

import org.cytoscape.application.swing.AbstractCyAction;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ToggleHighlightAction extends AbstractCyAction {

    private EdgeHighlighting edgeHighlighting;

    private final ImageIcon onIcon, offIcon;

    public ToggleHighlightAction(EdgeHighlighting edgeHighlighting) {
        super("DirectedEdgeColor");

        onIcon = new ImageIcon(getClass().getResource("/icons/edge_highlighting_on_icon.png"));
        offIcon = new ImageIcon(getClass().getResource("/icons/edge_highlighting_off_icon.png"));

        this.edgeHighlighting = edgeHighlighting;

        updateIcon();

        putValue(SHORT_DESCRIPTION, "Toggle directed edge highlighting");

        setToolbarGravity(15.1f);

        this.useToggleButton = true;
        this.inToolBar = true;
        this.insertToolbarSeparatorBefore = true;
    }

    protected void updateIcon() {
        putValue(LARGE_ICON_KEY, edgeHighlighting.getEnabled() ? onIcon : offIcon);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        edgeHighlighting.toggleFeature();
        updateIcon();
    }

}
