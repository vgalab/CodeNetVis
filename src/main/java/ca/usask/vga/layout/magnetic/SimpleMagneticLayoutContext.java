package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.util.FieldType;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

public class SimpleMagneticLayoutContext extends ForceDirectedLayoutContext {

    protected final String MAGNET_GROUP = "Magnetic force";
    protected final String EXTRA_GROUP = "Extra forces";

    // TODO: Write descriptions, change to floats (?), add validation states
    @Tunable(description="Enable magnetic force", gravity=800.01, context="both", longDescription="TODO", exampleStringValue="true")
    public boolean magnetEnabled = true;

    public FieldType fieldType = FieldType.VERTICAL;

    @Tunable(
            description = "Field type",
            groups = MAGNET_GROUP,
            context = "both",
            longDescription = "TODO",
            exampleStringValue = "Linear (horizontal)",
            dependsOn="magnetEnabled=true",
            gravity=300.09
    )
    public ListSingleSelection<FieldType> getFieldType() {
        ListSingleSelection<FieldType> t = new ListSingleSelection<>(FieldType.VERTICAL, FieldType.HORIZONTAL, FieldType.POLAR, FieldType.CONCENTRIC);
        t.setSelectedValue(this.fieldType);
        return t;
    }

    public void setFieldType(ListSingleSelection<FieldType> t) {
        this.fieldType = (FieldType) t.getSelectedValue();
    }

    @Tunable(description="Field strength", format="#.##E0", groups=MAGNET_GROUP, gravity=300.1, dependsOn="magnetEnabled=true", context="both", longDescription="TODO", exampleStringValue="1e-4")
    public double magneticFieldStrength = 1e-4;
    @Tunable(description="Distance Alpha", format="#.##", groups=MAGNET_GROUP, gravity=300.2, dependsOn="magnetEnabled=true", context="both", longDescription="TODO", exampleStringValue="1.0")
    public double magneticAlpha = 1.0;
    @Tunable(description="Angle Beta", format="#.##", groups=MAGNET_GROUP, gravity=300.3, dependsOn="magnetEnabled=true", context="both", longDescription="TODO", exampleStringValue="1.0")
    public double magneticBeta = 1.0;

}
