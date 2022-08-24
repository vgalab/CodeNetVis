package ca.usask.vga.layout.magnetic.io;

import ca.usask.vga.layout.magnetic.ActionOnSelected;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.util.swing.IconManager;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Properties;

import static org.cytoscape.work.ServiceProperties.*;

/**
 * An action that opens the code files behind the selected nodes,
 * either in the default editor, or in the browser if the origin is a URL.
 * Present in the toolbar and the context menu.
 */
public class OpenSelectedFiles extends ActionOnSelected  {

    private static final String SUBMENU = "Java Code[1000]";
    private static final String TASK_DESCRIPTION = "Open selected Java files";

    public OpenSelectedFiles(CyApplicationManager am, IconManager im) {
        super(am, im, TASK_DESCRIPTION);

        ImageIcon icon = new ImageIcon(getClass().getResource("/icons/open_code.png"));
        im.addIcon(ICON_NAMESPACE+TASK_DESCRIPTION, icon);

        ImageIcon icon2 = new ImageIcon(getClass().getResource("/icons/open_code_16.png"));
        im.addIcon(ICON_NAMESPACE+TASK_DESCRIPTION+SMALL, icon2);

        putValue(LARGE_ICON_KEY, icon);
        putValue(SHORT_DESCRIPTION, TASK_DESCRIPTION);

        setToolbarGravity(20f);

        this.useToggleButton = false;
        this.inToolBar = true;
        this.insertToolbarSeparatorBefore = true;
        this.insertToolbarSeparatorAfter = true;
    }

    @Override
    public void runTask(CyNetwork network, Collection<CyNode> selectedNodes) {

        if (!isReady(network, selectedNodes)) {
            return;
        }

        String pathToFiles = network.getDefaultNetworkTable().getRow(network.getSUID())
                .get(JavaReader.PATH_TO_FILES_COLUMN, String.class);

        if (pathToFiles.startsWith("file:")) {
            //throw new IllegalArgumentException("Path must be a src folder or a URL");
            try {
                pathToFiles = pathToFiles.replace("file:/", "");
                // Select up to the last slash
                pathToFiles = pathToFiles.substring(0, pathToFiles.lastIndexOf('/'));
                System.out.println("Opening folder: " + pathToFiles);
                Desktop.getDesktop().open(new java.io.File(pathToFiles));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        if (!pathToFiles.endsWith("/"))
            pathToFiles += "/";

        for (CyNode node : selectedNodes) {
            String fullName = network.getRow(node).get(CyNetwork.NAME, String.class);
            // Replace dots with slashes, inner classes removed
            fullName = fullName.replace(".", "/").replaceAll("\\$.*", "");
            String fileName = pathToFiles + fullName + ".java";
            System.out.println("Opening file: " + fileName);

            /*try {
                Runtime.getRuntime().exec("cmd /c start " + fileName);
            } catch (Exception e) {
                e.printStackTrace();
            }*/
            if (fileName.startsWith("http")) {
                try {
                    Desktop.getDesktop().browse(java.net.URI.create(fileName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Desktop.getDesktop().open(new java.io.File(fileName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean isReady(CyNetwork network, Collection<CyNode> selectedNodes) {
        // Check that neither network nor node list is null
        if (network == null || selectedNodes == null || selectedNodes.size() == 0)
            return false;
        // Check if the path to the files is set and is a folder
        String pathToFiles = network.getDefaultNetworkTable().getRow(network.getSUID())
                .get(JavaReader.PATH_TO_FILES_COLUMN, String.class);
        return pathToFiles != null && !pathToFiles.isEmpty();
    }

    @Override
    public Properties getNetworkTaskProperties() {
        Properties props = new Properties();
        props.setProperty(IN_MENU_BAR, "false");
        props.setProperty(PREFERRED_MENU, SUBMENU);
        props.setProperty(TITLE, TASK_DESCRIPTION);
        props.setProperty(MENU_GRAVITY, "0");
        props.setProperty(SMALL_ICON_ID, ICON_NAMESPACE+TASK_DESCRIPTION);
        return props;
    }

    @Override
    public Properties getTableTaskProperties() {
        var props = getNetworkTaskProperties();
        props.setProperty(TITLE, TASK_DESCRIPTION);
        props.setProperty(SMALL_ICON_ID, ICON_NAMESPACE+TASK_DESCRIPTION+SMALL);
        props.setProperty(MENU_GRAVITY, "10");
        return props;
    }

}
