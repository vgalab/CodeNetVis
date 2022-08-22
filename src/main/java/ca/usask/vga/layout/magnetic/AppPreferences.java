package ca.usask.vga.layout.magnetic;

import org.cytoscape.property.AbstractConfigDirPropsReader;

/**
 * Saves app preferences between Cytoscape sessions.
 * May be removed in the future for brevity.
 */
public class AppPreferences extends AbstractConfigDirPropsReader {

    public AppPreferences(String appName) {
        super(appName, appName + ".props",
                SavePolicy.SESSION_FILE_AND_CONFIG_DIR);


    }

}
