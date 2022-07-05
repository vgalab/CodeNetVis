package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.util.FieldType;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

public class PoleMagneticLayoutContext extends ForceDirectedLayoutContext {

    // TODO: Write descriptions, change to floats (?), add validation states
    @Tunable(description="Enable magnetic force", groups="Magnet", context="both", longDescription="TODO", exampleStringValue="true")
    public boolean magnetEnabled = true;

    public FieldType fieldType = FieldType.VERTICAL;

    @Tunable(
            description = "Field type",
            groups = "Magnet",
            context = "both",
            longDescription = "TODO",
            exampleStringValue = "Linear (horizontal)",
            dependsOn="usePoles=false"
    )
    public ListSingleSelection<FieldType> getFieldType() {
        ListSingleSelection<FieldType> t = new ListSingleSelection<>(FieldType.VERTICAL, FieldType.HORIZONTAL, FieldType.POLAR, FieldType.CONCENTRIC);
        t.setSelectedValue(this.fieldType);
        return t;
    }

    public void setFieldType(ListSingleSelection<FieldType> t) {
        this.fieldType = (FieldType) t.getSelectedValue();
    }

    @Tunable(description="Use poles", groups="Magnet", dependsOn="magnetEnabled=true", context="both", longDescription="TODO", exampleStringValue="true")
    public boolean usePoles = true;

    @Tunable(description="Field strength", groups="Magnet", dependsOn="magnetEnabled=true", context="both", longDescription="TODO", exampleStringValue="1e-4")
    public float magneticFieldStrength = 1e-4f;
    @Tunable(description="Alpha", groups="Magnet", dependsOn="magnetEnabled=true", context="both", longDescription="TODO", exampleStringValue="1")
    public float magneticAlpha = 1;
    @Tunable(description="Beta", groups="Magnet", dependsOn="magnetEnabled=true", context="both", longDescription="TODO", exampleStringValue="1")
    public float magneticBeta = 1;


    @Tunable(description="Pin pole positions", context="both", longDescription="TODO", exampleStringValue="true", gravity=991)
    public boolean pinPoles = true;

}
