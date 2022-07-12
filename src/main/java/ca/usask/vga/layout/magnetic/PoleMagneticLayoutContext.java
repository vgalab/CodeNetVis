package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.util.FieldType;
import ca.usask.vga.layout.magnetic.util.HierarchyForce;
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

    @Tunable(description="Pin pole positions", gravity=270.2, groups=POLE_GROUP, context="both", longDescription="TODO", exampleStringValue="true")
    public boolean pinPoles = true;

    @Tunable(description="Enable pole attraction", gravity=270.31, groups=POLE_GROUP, context="both", longDescription="TODO", exampleStringValue="true")
    public boolean usePoleAttraction = true;

    @Tunable(description="Pole attraction", format="#.##E0", gravity=270.32, groups=POLE_GROUP, dependsOn="usePoleAttraction=true", context="both", longDescription="TODO", exampleStringValue="1e-4")
    public double poleGravity = 1e-4;

    @Tunable(description="Enable central gravity", gravity=250.41, groups=EXTRA_GROUP, context="both", longDescription="TODO", exampleStringValue="true")
    public boolean useCentralGravity = true;

    @Tunable(description="Gravity constant", format="#.##E0", gravity=250.42, groups=EXTRA_GROUP, dependsOn="useCentralGravity=true", context="both", longDescription="TODO", exampleStringValue="1e-4")
    public double centralGravity = 1e-4;

    // TODO: Change into a drop down
    @Tunable(description="Circle pin", gravity=270.21, groups=POLE_GROUP, dependsOn="pinPoles=true", context="both", longDescription="TODO", exampleStringValue="true")
    public boolean useCirclePin = true;


    // Hierarchy
    protected final String HIERARCHY_GROUP = "Hierarchy";
    public HierarchyForce.Type hierarchyType;

    @Tunable(description="Enable hierarchy force", gravity=400.41, groups=HIERARCHY_GROUP, context="both", longDescription="TODO", exampleStringValue="true")
    public ListSingleSelection<HierarchyForce.Type> getHierarchyType() {
        ListSingleSelection<HierarchyForce.Type> t = new ListSingleSelection<>(HierarchyForce.Type.NONE, HierarchyForce.Type.BASED_ON_HOP_DISTANCE, HierarchyForce.Type.SINE_FUNCTION);
        t.setSelectedValue(this.hierarchyType);
        return t;
    }
    public void setHierarchyType(ListSingleSelection<HierarchyForce.Type> t) {
        this.hierarchyType = (HierarchyForce.Type) t.getSelectedValue();
    }

    @Tunable(description="Hierarchy force strength", format="#.##E0", gravity=400.42, groups=HIERARCHY_GROUP, context="both", longDescription="TODO", exampleStringValue="1e-4")
    public double hierarchyForce = 1e-4;




}
