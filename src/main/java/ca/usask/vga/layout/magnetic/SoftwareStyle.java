package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.poles.ExtraTasks;
import ca.usask.vga.layout.magnetic.poles.PoleManager;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

public class SoftwareStyle {

    private final CyApplicationManager am;
    private final TaskManager tm;
    private final VisualMappingManager vmm;
    private final VisualMappingFunctionFactory vmff_passthrough;
    private final VisualMappingFunctionFactory vmff_discrete;
    private final VisualMappingFunctionFactory vmff_continuous;
    private final PoleManager pm;

    public boolean keepPolesLarger = true;

    public SoftwareStyle(CyApplicationManager am, TaskManager tm, VisualMappingManager vmm,
                         VisualMappingFunctionFactory vmff_passthrough,
                         VisualMappingFunctionFactory vmff_discrete,
                         VisualMappingFunctionFactory vmff_continuous, PoleManager pm) {
        this.am = am;
        this.tm = tm;
        this.vmm = vmm;
        this.vmff_passthrough = vmff_passthrough;
        this.vmff_discrete = vmff_discrete;
        this.vmff_continuous = vmff_continuous;
        this.pm = pm;
    }

    public void setNodeSize(float size) {
        double calculated = Math.round(Math.pow(10, 1+(size / 50)));

        if (calculated == 10) {
            calculated = 0.1;
        }

        setNodeLabelSize(calculated);
    }

    public void setEdgeTransparency(float value) {
        int calculated = Math.round(value);

        VisualStyle style = vmm.getVisualStyle(am.getCurrentNetworkView());
        style.setDefaultValue(BasicVisualLexicon.EDGE_TRANSPARENCY, calculated);
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


}
