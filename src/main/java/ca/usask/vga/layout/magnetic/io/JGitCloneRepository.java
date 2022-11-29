package ca.usask.vga.layout.magnetic.io;

import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.EmptyProgressMonitor;

import javax.swing.*;
import java.io.File;

/**
 * Class for cloning a Git repository to a local directory.
 * Does not create a Cytoscape network from the repository.
 * A network could be created with loadFromSrcFolder() in SoftwareImport.
 */
public class JGitCloneRepository implements AutoCloseable {

    private final CloneCommand cloneCommand;
    private Git gitCall;

    private boolean cancelled = false;

    /**
     * Creates a new JGitCloneRepository object from a git url and a path.
     * Does not start the clone process until the execute() method is called.
     * @param gitUrl the url of the git repository to clone.
     * @param path the local directory to clone the repository to.
     */
    public JGitCloneRepository(String gitUrl, String path) {
        this.cloneCommand = Git.cloneRepository()
                .setURI(gitUrl)
                .setDirectory(new File(path))
                .setProgressMonitor(new EmptyProgressMonitor(){
                    // The default progress monitor can be cancelled but does not provide updates.
                    @Override
                    public boolean isCancelled() {
                        return cancelled;
                    }
                });
    }

    /**
     * Sets the internal git clone progress monitor to the provided
     * TaskMonitor. This allows the clone process to be cancelled as
     * well as provide progress updates in the UI.
     * @param taskMonitor the TaskMonitor to use for the clone process.
     */
    public void setTaskMonitor(TaskMonitor taskMonitor) {
        cloneCommand.setProgressMonitor(new EmptyProgressMonitor() {
            int totalTasks = 1;
            @Override
            public void start(int totalTasks) {
                this.totalTasks = totalTasks;
                taskMonitor.setProgress(0);
            }

            @Override
            public void beginTask(String title, int totalWork) {
                taskMonitor.setStatusMessage(title);
            }

            @Override
            public void update(int completed) {
                taskMonitor.setProgress((float) completed / totalTasks);
            }

            @Override
            public void endTask() {
                taskMonitor.setProgress(1);
            }
            @Override
            public boolean isCancelled() {
                return cancelled;
            }
        });
    }

    /**
     * Executes the clone process. This method will block until the clone is complete.
     */
    public void execute() throws GitAPIException {
        gitCall = cloneCommand.call();
    }

    /**
     * Creates a TaskIterator cloning a repository given the gitUrl and directory path.
     * Does not block the current thread when executed with {@link TaskManager#execute(TaskIterator)}.
     * Updates the progress bar in the Cytoscape GUI to show the download progress.
     */
    public static TaskIterator cloneGitTaskIterator(String gitUrl, String path) {
        return new TaskIterator(new Task() {
            JGitCloneRepository clone;
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                taskMonitor.setTitle("Cloning repository from " + gitUrl + "...");
                // Close the clone object when the task is finished using try-with-resources
                try (JGitCloneRepository clone = new JGitCloneRepository(gitUrl, path)) {
                    this.clone = clone;
                    clone.setTaskMonitor(taskMonitor);
                    clone.execute();
                }
                // Report success or failure
                if (!clone.cancelled) {
                    taskMonitor.setStatusMessage("Git clone completed successfully.");
                } else {
                    taskMonitor.setStatusMessage("Git clone was cancelled.");
                }
            }
            @Override
            public void cancel() {
                clone.cancelled = true;
            }
        });
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.*/
    @Override
    public void close() {
        gitCall.close();
    }

    // TODO: Remove. Testing only.
    public static void cloneTest(TaskManager tm) {
        String gitUrl = JOptionPane.showInputDialog("Enter git url:");
        String path = JOptionPane.showInputDialog("Enter path:");
        tm.execute(cloneGitTaskIterator(gitUrl, path));
    }
}
