/**
 * Copyright 2012 Martin Wallgren
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.wallgren.android.platform.gui;

import it.wallgren.android.platform.ProjectCreationState;
import it.wallgren.android.platform.project.AndroidMkAnalyzer;
import it.wallgren.android.platform.project.AndroidPlatformProject;
import it.wallgren.android.platform.project.AndroidProject;
import it.wallgren.android.platform.project.PackagesProject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;

public class ProjectSelectionPage extends WizardPage {

    private final List<AndroidProject> projects = new LinkedList<AndroidProject>();
    private final ProjectCreationState state;
    private ProjectSelectionComposite composite;

    public ProjectSelectionPage(ProjectCreationState state, String pageName) {
        super(pageName);
        this.state = state;
    }

    @Override
    public void createControl(Composite parent) {
        composite = new ProjectSelectionComposite(parent, SWT.NULL);

        composite.addListener(new CompositeListener() {
            @Override
            public void onCompositeChanged() {
                setPageComplete(validatePage());
            }
        });

        initializeDialogUnits(parent);
        setPageComplete(validatePage());

        // Show description on opening
        setErrorMessage(null);
        setMessage(null);
        setControl(composite);
        Dialog.applyDialogFont(composite);
    }

    private boolean validatePage() {
        TableItem[] items = composite.getItems();
        for (TableItem tableItem : items) {
            // We need at least one selected project
            if (tableItem.getChecked()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            updateProjectList();
        }
    }

    private void updateProjectList() {
        try {
            getContainer().run(true, false, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException,
                        InterruptedException {
                    synchronized (projects) {
                        // monitor.beginTask("Creating projects",
                        // packagesProjects.size() + 1);
                        monitor.beginTask("Creating projects", 1);

                        monitor.worked(1);

                        // Do we want to support packages/apps projects? Some
                        // problems still exists
                        // for some of the genrated projects. Probably not worth
                        // it
                        final List<IPath> packagesProjects = findPackagesProjects();
                        for (IPath path : packagesProjects) {
                            monitor.subTask(path.lastSegment());
                            AndroidMkAnalyzer analyzer = new AndroidMkAnalyzer(state.getRepoPath()
                                    .toFile(), new File(path.toFile(), "Android.mk"));
                            try {
                                analyzer.parse();
                            } catch (IOException e) {
                                e.printStackTrace();
                                continue; // Skip the failing package
                            }
                            String packageName = analyzer.getPackageName();
                            File outDirectory = analyzer.getOutDirectory();
                            if (outDirectory == null || packageName == null) {
                                continue; // Skip this package, it does not seem
                                          // complete
                            }
                            projects.add(new PackagesProject(path, state.getRepoPath(),
                                    packageName, outDirectory));
                            monitor.worked(1);
                        }
                        Collections.sort(projects);
                        // We want the platform project on top
                        projects.add(0, new AndroidPlatformProject(state.getRepoPath()));
                    }
                    monitor.done();

                    // Redraw must be called on the UI thread
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (projects) {
                                for (AndroidProject project : projects) {
                                    TableItem item = composite.createTableItem();
                                    item.setText(project.getName());
                                    item.setChecked(project.preSelected());
                                }
                                getShell().pack();
                            }
                        }
                    });
                }
            });
        } catch (Exception e1) {
            // TODO: Better error handling if project creation fail
            e1.printStackTrace();
        }
    }

    private List<IPath> findPackagesProjects() {
        LinkedList<IPath> folders = new LinkedList<IPath>();
        File packages = new File(state.getRepoPath().toFile(), "packages");
        for (File folder : packages.listFiles()) {
            // TODO: experimental is usually not built when building the rest of
            // the platorm. How
            // should we treat them? Don't include them for now

            // folder = apps experimental inputmethods providers wallpapers...
            if (folder.isDirectory() && !"experimental".equals(folder.getName())) {
                for (File projectFolder : folder.listFiles()) {
                    // projectFolder = .../packages/apps/* ...
                    if (projectFolder.isDirectory()
                            && new File(projectFolder, "Android.mk").exists()) {
                        folders.add(new Path(projectFolder.getAbsolutePath()));
                    }
                }
            }
        }
        return folders;
    }

    public List<AndroidProject> getSelectedProjects() {
        synchronized (projects) {
            ArrayList<AndroidProject> selectedProjects = new ArrayList<AndroidProject>(
                    projects.size());
            TableItem[] items = composite.getItems();
            for (int i = 0; i < items.length; i++) {
                if (items[i].getChecked()) {
                    selectedProjects.add(projects.get(i));
                }
            }
            return selectedProjects;
        }
    }
}
