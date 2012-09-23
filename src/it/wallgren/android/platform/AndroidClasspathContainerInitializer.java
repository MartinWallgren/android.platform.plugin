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

import java.util.HashMap;
import java.util.LinkedList;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class AndroidClasspathContainerInitializer extends ClasspathContainerInitializer {
    private static final HashMap<String, AndroidClasspathContainer> CONTAINERS = new HashMap<String, AndroidClasspathContainer>();

    public AndroidClasspathContainerInitializer() {
    }

    @Override
    public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
        IClasspathContainer container = getAndroidContainer(containerPath, project);
        JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {
                project
        },
        new IClasspathContainer[] {
                container
        }, new NullProgressMonitor());
    }

    private IPath getRepoRoot(IPath containerPath) {
        return containerPath.removeFirstSegments(1).makeAbsolute();
    }

    private AndroidClasspathContainer getAndroidContainer(IPath containerPath, IJavaProject project) {
        AndroidClasspathContainer container;
        synchronized (CONTAINERS) {
            container = CONTAINERS.get(containerPath.toString());
            if (container == null) {
                container = loadClasspathContainer(project, getRepoRoot(containerPath));
                CONTAINERS.put(containerPath.toString(), container);
            }
        }
        return container;
    }

    @Override
    public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
        return true;
    }

    @Override
    public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project,
            IClasspathContainer containerSuggestion) throws CoreException {
        AndroidClasspathContainer container = getAndroidContainer(containerPath, project);
        container.setEntries(containerSuggestion.getClasspathEntries());
        JavaCore.setClasspathContainer(
                containerPath,
                new IJavaProject[] {
                        project
                }, new IClasspathContainer[] {
                        containerSuggestion
                }, new NullProgressMonitor());
        try {
            persistClasspathContainer(project, container);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Persist relevant parts of the container for future sessions
     * 
     * @param containerPath
     * @param container
     * @throws CoreException
     */
    private void persistClasspathContainer(IJavaProject project, IClasspathContainer container)
            throws CoreException {
        IClasspathEntry[] entries = container.getClasspathEntries();
        for (int i = 0; i < entries.length; i++) {
            IClasspathEntry entry = entries[i];
            QualifiedName qname = new QualifiedName(container.getPath().toString(),
                    "IClasspathEntry." + i);
            String encodedClasspathEntry = project.encodeClasspathEntry(entry);
            ResourcesPlugin.getWorkspace().getRoot()
            .setPersistentProperty(qname, encodedClasspathEntry);
        }
    }

    private AndroidClasspathContainer loadClasspathContainer(IJavaProject project,
            IPath containerPath) {
        AndroidClasspathContainer container = new AndroidClasspathContainer(containerPath);
        LinkedList<IClasspathEntry> entries = new LinkedList<IClasspathEntry>();
        IClasspathEntry entry = null;
        int i = 0;
        try {
            do {
                QualifiedName qname = new QualifiedName(container.getPath().toString(),
                        "IClasspathEntry." + i);
                entry = project.decodeClasspathEntry(ResourcesPlugin.getWorkspace().getRoot()
                        .getPersistentProperty(qname));
                if (entry != null) {
                    entries.add(entry);
                }
                i++;
            } while (entry != null);
            if (entries.size() > 0) {
                container.setEntries(entries.toArray(new IClasspathEntry[entries.size()]));
            }
        } catch (CoreException e) {
            // We'll recreate the paths later, but manual classpath changes will
            // be lost (like source and javadoc attachments)
            e.printStackTrace();
        }
        return container;
    }
}
