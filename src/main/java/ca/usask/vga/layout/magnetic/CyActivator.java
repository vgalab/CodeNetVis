package ca.usask.vga.layout.magnetic;

import ca.usask.vga.layout.magnetic.highlight.*;
import ca.usask.vga.layout.magnetic.io.JavaReader;
import ca.usask.vga.layout.magnetic.io.PajekReader;
import ca.usask.vga.layout.magnetic.poles.*;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.equations.EquationCompiler;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.session.events.SessionAboutToBeLoadedListener;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.task.TableCellTaskFactory;
import org.cytoscape.task.hide.HideTaskFactory;
import org.cytoscape.task.hide.UnHideAllTaskFactory;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.work.undo.UndoSupport;
import org.osgi.framework.BundleContext;

import java.util.Properties;

import static org.cytoscape.work.ServiceProperties.*;

public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {

		final String MENU_APP_ROOT = "Apps.Magnetic Layout";
		
		UndoSupport undo = getService(bc,UndoSupport.class);

		// App preferences
		AppPreferences preferences = new AppPreferences("magnetic-layout");
    	Properties propsReaderServiceProps = new Properties();
    	propsReaderServiceProps.setProperty("cyPropertyName", "magnetic-layout.props");
    	registerAllServices(bc, preferences, propsReaderServiceProps);

		// Simple Magnetic Layout
		SimpleMagneticLayout simpleMagneticLayout = new SimpleMagneticLayout(undo);

        Properties sLayoutProps = new Properties();
        sLayoutProps.setProperty(PREFERRED_MENU,"Layout.Magnetic Layouts");
        sLayoutProps.setProperty("preferredTaskManager","menu");  // Purpose: unknown
        sLayoutProps.setProperty(TITLE,simpleMagneticLayout.toString());
        sLayoutProps.setProperty(MENU_GRAVITY,"10.51");
		registerService(bc,simpleMagneticLayout,CyLayoutAlgorithm.class, sLayoutProps);

		// Magnetic Poles
		PoleManager poleManager = new PoleManager(getService(bc, CyNetworkManager.class), undo);
		registerService(bc, poleManager, PoleManager.class);
		registerService(bc, poleManager, NetworkAddedListener.class);
		registerService(bc, poleManager, SetCurrentNetworkListener.class);
		registerService(bc, poleManager, SessionAboutToBeLoadedListener.class);

		var am = getService(bc, CyApplicationManager.class);
		var im = getService(bc, IconManager.class);

		AddNorthPoleAction addNPole = new AddNorthPoleAction(am, im, poleManager);
		AddSouthPoleAction addSPole = new AddSouthPoleAction(am, im, poleManager);
		RemovePoleAction removePole = new RemovePoleAction(am, im, poleManager);

		for (ActionOnSelected action : new ActionOnSelected[] {addNPole, addSPole, removePole}) {
			registerService(bc, action, CyAction.class);
			registerService(bc, action, SelectedNodesAndEdgesListener.class);
			registerService(bc, action.getNetworkTaskFactory(), NetworkViewTaskFactory.class, action.getNetworkTaskProperties());
			registerService(bc, action.getNodeViewTaskFactory(), NodeViewTaskFactory.class, action.getNetworkTaskProperties());
			registerService(bc, action.getTableCellTaskFactory(), TableCellTaskFactory.class, action.getTableTaskProperties());
		}

		// Pole Magnetic Layout
		PoleMagneticLayout poleMagneticLayout = new PoleMagneticLayout(poleManager, undo);

		Properties pLayoutProps = new Properties();
		pLayoutProps.setProperty(PREFERRED_MENU,"Layout.Magnetic Layouts");
		pLayoutProps.setProperty("preferredTaskManager","menu");  // Purpose: unknown
		pLayoutProps.setProperty(TITLE,poleMagneticLayout.toString());
		pLayoutProps.setProperty(MENU_GRAVITY,"10.52");
		registerService(bc,poleMagneticLayout,CyLayoutAlgorithm.class, pLayoutProps);

		// Extra pole tasks

		ExtraTasks.MakeTopDegreePoles makeTopDegreePoles = new ExtraTasks.MakeTopDegreePoles(am, poleManager);
		registerService(bc, ExtraTasks.getTaskFactory(makeTopDegreePoles),
				TaskFactory.class, makeTopDegreePoles.getDefaultProperties());

		ExtraTasks.SelectAllPoles selectAllPoles = new ExtraTasks.SelectAllPoles(am, poleManager);
		registerService(bc, ExtraTasks.getTaskFactory(selectAllPoles),
				TaskFactory.class, selectAllPoles.getDefaultProperties());

		ExtraTasks.LegacyPoleColoring legacyPoleColoring = new ExtraTasks.LegacyPoleColoring(am, poleManager,
				getService(bc, VisualMappingManager.class),
				getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=discrete)"));
		registerService(bc, ExtraTasks.getTaskFactory(legacyPoleColoring),
				TaskFactory.class, legacyPoleColoring.getDefaultProperties());

		ExtraTasks.CopyNodeStyleToEdge copyNodeStyleToEdge = new ExtraTasks.CopyNodeStyleToEdge(am,
				getService(bc, VisualMappingManager.class),
				getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=discrete)"), undo);
		registerService(bc, ExtraTasks.getTaskFactory(copyNodeStyleToEdge),
				TaskFactory.class, copyNodeStyleToEdge.getDefaultProperties());

		ExtraTasks.MakePoleNodesLarger makePoleNodesLarger = new ExtraTasks.MakePoleNodesLarger(am,
				getService(bc, VisualMappingManager.class),
				getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=discrete)"), undo);
		registerService(bc, ExtraTasks.getTaskFactory(makePoleNodesLarger),
				TaskFactory.class, makePoleNodesLarger.getDefaultProperties());

		var networkCyAccess = new NetworkCyAccess(
				getService(bc, CyNetworkFactory.class),
				getService(bc, CyNetworkManager.class),
				getService(bc, CyNetworkViewFactory.class),
				getService(bc, CyNetworkViewManager.class),
				getService(bc, CyNetworkNaming.class),
				getService(bc, VisualMappingManager.class),
				getService(bc, CyRootNetworkManager.class),
				getService(bc, CyApplicationManager.class),
				poleManager);

		// Editor Edge Highlighting
		final EdgeHighlighting edgeHighlighting = new EdgeHighlighting(networkCyAccess, preferences);
		registerService(bc, edgeHighlighting, SelectedNodesAndEdgesListener.class);
		registerService(bc, edgeHighlighting, SetCurrentNetworkListener.class);

		registerService(bc, new ToggleHighlightAction(edgeHighlighting), CyAction.class, new Properties());
		registerService(bc, new CopyHighlightedAction(edgeHighlighting), CyAction.class, new Properties());
		registerService(bc, new ChangeHopDistanceAction(edgeHighlighting), CyAction.class, new Properties());

		// PAJEK .NET File format reading
		PajekReader pajekReader = PajekReader.create(new PajekReader.CyAccess(getService(bc, CyNetworkFactory.class),
				getService(bc, CyNetworkViewFactory.class)), getService(bc, StreamUtil.class));

		registerService(bc, pajekReader, pajekReader.getServiceClass(), pajekReader.getDefaultProperties());

		// JAR File input
		var jarReaderAccess = new JavaReader.CyAccess(getService(bc, CyNetworkFactory.class),
				getService(bc, CyNetworkViewFactory.class), getService(bc, EquationCompiler.class));

		JavaReader javaReader = JavaReader.create(jarReaderAccess, getService(bc, StreamUtil.class));

		registerService(bc, javaReader, javaReader.getServiceClass(), javaReader.getDefaultProperties());

		// Software Panel
		SoftwareLayout softwareLayout = new SoftwareLayout(poleMagneticLayout, getService(bc, TaskManager.class),
				getService(bc, CyApplicationManager.class), new CreateSubnetworkTask(networkCyAccess));

		SoftwareStyle softwareStyle = new SoftwareStyle(getService(bc, CyApplicationManager.class),
				getService(bc, TaskManager.class), getService(bc, VisualMappingManager.class),
				getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=passthrough)"),
				getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=discrete)"),
				getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=continuous)"),
				getService(bc, VisualStyleFactory.class), poleManager,
				getService(bc, AnnotationManager.class),
				getService(bc, AnnotationFactory.class, "(type=ShapeAnnotation.class)"),
				getService(bc, HideTaskFactory.class), getService(bc, UnHideAllTaskFactory.class),
				getService(bc, EquationCompiler.class));
		registerService(bc, softwareStyle, NetworkViewAboutToBeDestroyedListener.class);

		SoftwareImport softwareImport = new SoftwareImport(getService(bc, DialogTaskManager.class),
				getService(bc, FileUtil.class),
				getService(bc, LoadNetworkFileTaskFactory.class), jarReaderAccess,
				getService(bc, CyNetworkManager.class),
				getService(bc, CyNetworkViewManager.class),
				getService(bc, FileUtil.class),
				getService(bc, CySwingApplication.class));

		SoftwarePanel sPanel = new SoftwarePanel(getService(bc, CySwingApplication.class),
				getService(bc, DialogTaskManager.class),
				softwareLayout, softwareStyle, softwareImport);

		//registerService(bc, sPanel, CytoPanelComponent.class);
		//registerService(bc, sPanel, SessionLoadedListener.class);
		registerAllServices(bc, sPanel);

	}
}

