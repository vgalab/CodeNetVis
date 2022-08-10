package ca.usask.vga.layout.magnetic;

import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.swing.DialogTaskManager;

import java.awt.*;
import java.io.File;
import java.util.HashSet;
import java.util.function.Consumer;

public class SoftwareImport {

    private final DialogTaskManager dtm;
    private final FileUtil fu;
    private final LoadNetworkFileTaskFactory nftf;

    public SoftwareImport(DialogTaskManager dtm, FileUtil fu, LoadNetworkFileTaskFactory nftf) {
        this.dtm = dtm;
        this.fu = fu;
        this.nftf = nftf;
    }

    public void loadFromFile(Component parent, Consumer<String> onSuccess) {

        File f = fu.getFile(parent, "New graph from file", FileUtil.LOAD, new HashSet<>());

        if (f == null) return;

        dtm.execute(nftf.createTaskIterator(f), new TaskObserver() {
            public void taskFinished(ObservableTask task) {}
            public void allFinished(FinishStatus finishStatus) {
                if (onSuccess != null && finishStatus.getType() == FinishStatus.Type.SUCCEEDED)
                    onSuccess.accept(f.getName());
            }
        });

    }


}
