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

package it.wallgren.android.platform;

import it.wallgren.android.platform.gui.AndroidRepoPage;
import it.wallgren.android.platform.gui.ProjectSelectionPage;
import it.wallgren.android.platform.project.AndroidProject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class NewProjectWizard extends Wizard implements INewWizard {
    private final ProjectCreationState state = new ProjectCreationState();

    private AndroidRepoPage page1;
    private ProjectSelectionPage page2;

    public NewProjectWizard() {
        setWindowTitle("New Android Platform project");
        setNeedsProgressMonitor(true);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {

    }

    @Override
    public void addPages() {
        super.addPages();
        page1 = new AndroidRepoPage(state, "New Android Platform project");
        page1.setDescription("Creates a new eclipse project for working with the android platform");
        addPage(page1);

        page2 = new ProjectSelectionPage(state, "Select projects to generate");
        page2.setDescription("Select the projects you want to create");
        addPage(page2);
    }

    @Override
    public boolean performFinish() {
        try {
            getContainer().run(false, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException,
                        InterruptedException {
                    try {
                        List<AndroidProject> projects = page2.getSelectedProjects();
                        monitor.beginTask("Creating projects", projects.size());
                        for (AndroidProject androidProject : projects) {
                            monitor.subTask(androidProject.getName());
                            androidProject.create(monitor);
                            monitor.worked(1);
                        }
                        monitor.done();
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (Exception e1) {
            // TODO: Better error handling
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e1.getCause().printStackTrace(pw);
            page2.setErrorMessage(sw.toString());
            e1.printStackTrace();
            return false;
        }
        return true;
    }

}
