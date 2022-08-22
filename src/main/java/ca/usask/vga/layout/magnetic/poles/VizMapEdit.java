package ca.usask.vga.layout.magnetic.poles;

import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.undo.AbstractCyEdit;
import org.cytoscape.work.undo.UndoSupport;

/**
 * Implements the undo/redo functionality for visual style
 * changes of the graph. Describes a single edit to the visual properties.
 */
public class VizMapEdit extends AbstractCyEdit {

    private final VisualStyle style;
    private final VisualProperty<?>[] properties;

    private VisualMappingFunction<?,?>[] mappingsBefore;
    private VisualMappingFunction<?,?>[] mappingsAfter;

    private boolean editComplete;

    public VizMapEdit(String description, VisualStyle style, VisualProperty<?>... properties) {
        super(description);
        this.style = style;
        this.properties = properties;
        mappingsBefore = new VisualMappingFunction[properties.length];
        mappingsAfter = new VisualMappingFunction[properties.length];
        setMappings(mappingsBefore);
    }

    protected void setMappings(VisualMappingFunction<?,?>[] dest) {
        for (int i = 0; i < properties.length; i++) {
            dest[i] = style.getVisualMappingFunction(properties[i]);
        }
    }

    protected void restoreMappings(VisualMappingFunction<?,?>[] from) {
        for (int i = 0; i < properties.length; i++) {
            if (from[i] != null)
                style.addVisualMappingFunction(from[i]);
            else
                style.removeVisualMappingFunction(properties[i]);
        }
    }

    public void completeEdit(UndoSupport undoSupport) {
        if (editComplete) return;
        setMappings(mappingsAfter);
        editComplete = true;
        undoSupport.postEdit(this);
    }

    @Override
    public void undo() {
        if (!editComplete)
            throw new RuntimeException("VizMapEdit creation incomplete before undo call");

        restoreMappings(mappingsBefore);
    }

    @Override
    public void redo() {
        if (!editComplete)
            throw new RuntimeException("VizMapEdit creation incomplete before redo call");

        restoreMappings(mappingsAfter);
    }
}
