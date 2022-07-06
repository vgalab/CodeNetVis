package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.util.FieldType;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

public class PoleMagneticLayoutContext extends SimpleMagneticLayoutContext {

    protected final String POLE_GROUP = "Selected poles";

    // TODO: Write descriptions, change to floats (?), add validation states

    @Tunable(
            description = "Field type",
            groups = MAGNET_GROUP,
            context = "both",
            longDescription = "TODO",
            exampleStringValue = "Linear (horizontal)",
            dependsOn="useMagneticPoles=false",
            gravity=300.05
    )
    @Override
    public ListSingleSelection<FieldType> getFieldType() {
        return super.getFieldType();
    }

    @Tunable(description="Use magnetic poles", gravity=300.09, groups=MAGNET_GROUP, dependsOn="magnetEnabled=true", context="both", longDescription="TODO", exampleStringValue="true")
    public boolean useMagneticPoles = true;

    @Tunable(description="Pin pole positions", gravity=700.2, groups=POLE_GROUP, context="both", longDescription="TODO", exampleStringValue="true")
    public boolean pinPoles = true;

    @Tunable(description="Enable pole attraction", gravity=700.31, groups=POLE_GROUP, context="both", longDescription="TODO", exampleStringValue="true")
    public boolean usePoleAttraction = true;

    @Tunable(description="Pole attraction", format="#.##E0", groups=POLE_GROUP, dependsOn="usePoleAttraction=true", gravity=700.32, context="both", longDescription="TODO", exampleStringValue="1e-4")
    public double poleGravity = 1e-4;

}
