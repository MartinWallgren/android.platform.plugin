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

package it.wallgren.android.platform.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public abstract class AndroidProject implements Comparable<AndroidProject> {
    private final IWorkspace workspace;
    private IProject project;

    public abstract String getName();

    public abstract IPath getPath();

    public AndroidProject() {
        workspace = ResourcesPlugin.getWorkspace();
    }

    /**
     * Create the actual project in the workspace
     */
    protected abstract void doCreate(IProgressMonitor monitor) throws CoreException;

    protected IProject createProject(String name, IProgressMonitor monitor) throws CoreException {
        project = workspace.getRoot().getProject(name);
        if (!project.exists()) {
            project.create(monitor);
        }
        project.open(monitor);

        return project;
    }

    public final void create(IProgressMonitor monitor) throws CoreException {
        doCreate(monitor);
    }

    public abstract boolean preSelected();

    @Override
    public int compareTo(AndroidProject o) {
        if (o == null) {
            return -1;
        }
        return getName().compareTo(o.getName());
    }
}