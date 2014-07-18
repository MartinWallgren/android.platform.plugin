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

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

public class AndroidPlatformProject extends AndroidProject {
    private static String FILE_FILTER_ID = "org.eclipse.ui.ide.patternFilterMatcher";
    private static final String[] BROKEN_CLASSPATH_ENTRIES = new String[]{"frameworks/ex/carousel/java"};

    private final IPath repoPath;

    private final String projectName;

    public AndroidPlatformProject(IPath repoPath) {
        this.repoPath = repoPath;
        this.projectName = getProjectName(repoPath);
    }

    public static String getProjectName(IPath repoPath) {
        return repoPath.lastSegment() + "-platform";
    }

    @Override
    public String getName() {
        return projectName;
    }

    @Override
    public IPath getPath() {
        return repoPath;
    }

    @Override
    public void doCreate(IProgressMonitor monitor) throws CoreException {
        final IProject project = createProject(projectName, monitor);
        addJavaNature(project, monitor);
    }

    private void addJavaNature(IProject project, IProgressMonitor monitor) throws CoreException {
        if (project == null) {
            throw new IllegalStateException(
                    "Project must be created before giving it a Java nature");
        }
        final IFolder repoLink = createRepoLink(monitor, project, repoPath);
        IFile classpath = repoLink.getFile("development/ide/eclipse/.classpath");
        IFile classpathDestination = project.getFile(".classpath");
        if (classpathDestination.exists()) {
            classpathDestination.delete(true, monitor);
        }
        classpath.copy(classpathDestination.getFullPath(), true, monitor);
        final IProjectDescription description = project.getDescription();
        final String[] natures = description.getNatureIds();
        final String[] newNatures = Arrays.copyOf(natures, natures.length + 1);
        newNatures[natures.length] = JavaCore.NATURE_ID;
        description.setNatureIds(newNatures);
        project.setDescription(description, null);
        final IJavaProject javaProject = JavaCore.create(project);
        @SuppressWarnings("rawtypes")
        final Map options = javaProject.getOptions(true);
        // Compliance level need to be 1.6
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
        javaProject.setOptions(options);

        IClasspathEntry[] classPath = mangleClasspath(javaProject.getRawClasspath(), project,
                repoLink);
        javaProject.setRawClasspath(classPath, monitor);
        javaProject.setOutputLocation(javaProject.getPath().append("out"), monitor);
    }

    private IClasspathEntry[] mangleClasspath(IClasspathEntry[] rawClasspath, IProject project,
            IFolder repoLink) {
        LinkedList<IClasspathEntry> entries = new LinkedList<IClasspathEntry>();

        // Filter out anything that is not framworks, packages, libcore or R
        IFile frameworks = project.getFile("frameworks");
        IFile packages = project.getFile("packages");
        IFile libcore = project.getFile("libcore");
        for (IClasspathEntry entry : rawClasspath) {
            if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                // is frameworks source folder or R source folder
                if (frameworks.getFullPath().isPrefixOf(entry.getPath())
                        || libcore.getFullPath().isPrefixOf(entry.getPath())
                        || packages.getFullPath().isPrefixOf(entry.getPath())
                        || entry.getPath().lastSegment().equals("R")) {
                    IPath path = entry.getPath().removeFirstSegments(1);
                    IFolder entryFolder = repoLink.getFolder(path);
                    if (!isBroken(entryFolder)) {
                        entries.add(JavaCore.newSourceEntry(entryFolder.getFullPath()));
                    }
                }
            }
        }

        // Add the special platform libs container
        entries.add(getAndroidDependenceis(repoPath));
        return entries.toArray(new IClasspathEntry[0]);
    }

    /**
     * Returns true if the classpath entry is non existant or have an
     * compile/build error
     *
     * @param classpathEntry
     * @return
     */
    private boolean isBroken(IFolder classpathEntry) {
        for (String src : BROKEN_CLASSPATH_ENTRIES) {
            if (classpathEntry.getFullPath().toString().endsWith(src)) {
                return true;
            }
        }
        return !classpathEntry.exists();
    }

    private IFolder createRepoLink(IProgressMonitor monitor, IProject project, IPath repoPath)
            throws CoreException {
        final IFolder repoLink = project.getFolder(repoPath.lastSegment());
        if (repoLink.exists()) {
            // This is an existing project the link already exists
            return repoLink;
        }

        // List of root folders we want to keep, all else is hidden. This is for
        // performance reasons. Indexing the entire Android repo is expensive as
        // shit
        String[] resources = new String[] {
                "frameworks", "out", "libcore", "development"
        };

        for (String res : resources) {
            repoLink.createFilter(IResourceFilterDescription.INCLUDE_ONLY
                    | IResourceFilterDescription.FOLDERS, new FileInfoMatcherDescription(
                    FILE_FILTER_ID, res), 0, monitor);
        }

        // Let's filter out some content we don't need. To avoid it being
        // indexed
        repoLink.createFilter(IResourceFilterDescription.EXCLUDE_ALL
                | IResourceFilterDescription.FOLDERS | IResourceFilterDescription.INHERITABLE,
                new FileInfoMatcherDescription(FILE_FILTER_ID, "bin"), 0, monitor);
        repoLink.createFilter(IResourceFilterDescription.EXCLUDE_ALL
                | IResourceFilterDescription.FOLDERS, new FileInfoMatcherDescription(
                FILE_FILTER_ID, ".repo"), 0, monitor);
        repoLink.createFilter(IResourceFilterDescription.EXCLUDE_ALL
                | IResourceFilterDescription.FOLDERS | IResourceFilterDescription.INHERITABLE,
                new FileInfoMatcherDescription(FILE_FILTER_ID, ".git"), 0, monitor);
        // repoLink.createFilter(IResourceFilterDescription.EXCLUDE_ALL
        // | IResourceFilterDescription.FOLDERS, new FileInfoMatcherDescription(
        // FILE_FILTER_ID, "out"), 0, monitor);
        final IFolder out = repoLink.getFolder("out");
        // Only allow target/common/R in the out folder
        final int filterFlags = IResourceFilterDescription.INCLUDE_ONLY
                | IResourceFilterDescription.FOLDERS | IResourceFilterDescription.FILES;
        out.createFilter(filterFlags, new FileInfoMatcherDescription(FILE_FILTER_ID, "target"), 0,
                monitor);

        final IFolder target = out.getFolder("target");
        target.createFilter(filterFlags, new FileInfoMatcherDescription(FILE_FILTER_ID, "common"),
                0, monitor);

        final IFolder common = target.getFolder("common");
        common.createFilter(filterFlags, new FileInfoMatcherDescription(FILE_FILTER_ID, "R"), 0,
                monitor);
        repoLink.createLink(repoPath, 0, monitor);

        return repoLink;
    }

    private IClasspathEntry getAndroidDependenceis(IPath repoPath) {
        return JavaCore.newContainerEntry(new Path(
                "it.wallgren.android.platform.classpathContainerInitializer"
                        + repoPath.makeAbsolute()));
    }

    @Override
    public boolean preSelected() {
        return true;
    }
}
