package ca.usask.vga.layout.magnetic.opencl;

import static org.cytoscape.work.ServiceProperties.*;

import java.util.Properties;

import org.cytoscape.opencl.cycl.CyCL;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.work.undo.UndoSupport;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CyActivator extends AbstractCyActivator {

	private final Logger logger = LoggerFactory.getLogger(CyActivator.class);
	
	public void start(BundleContext bc)  {	
		// Have to wait for the opencl-cycl bundle to start and register its CyCL service.
		registerServiceListener(bc, 
			(cycl, props) -> initialize(bc, cycl),
			(cycl, props) -> {}, 
			CyCL.class);
	}
	
	
	private void initialize(BundleContext bc, CyCL cycl) {
		new Thread(() -> {
			try {
				// Don't initialize if there are no OpenCL devices.
				if (CyCL.getDevices().size() == 0) {
					logger.error("No OpenCL compatible device found. Cannot register '" + CLLayout.ALGORITHM_DISPLAY_NAME + "'.");
					return;
				}
				
				UndoSupport undo = getService(bc, UndoSupport.class);

				CLLayout forceDirectedCLLayout = new CLLayout(undo);

		        Properties forceDirectedCLLayoutProps = new Properties();
		        forceDirectedCLLayoutProps.setProperty(PREFERRED_MENU, "Layout.Cytoscape Layouts");
		        forceDirectedCLLayoutProps.setProperty("preferredTaskManager", "menu");
		        forceDirectedCLLayoutProps.setProperty(TITLE, forceDirectedCLLayout.toString());
		        forceDirectedCLLayoutProps.setProperty(MENU_GRAVITY, "10.53");
				registerService(bc, forceDirectedCLLayout, CyLayoutAlgorithm.class, forceDirectedCLLayoutProps);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}
}

