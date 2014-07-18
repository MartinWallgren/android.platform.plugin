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

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.FileInfoMatcherDescription;
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

public class PackagesProject extends AndroidProject {
    private static String FILE_FILTER_ID = "org.eclipse.ui.ide.patternFilterMatcher";
    private final IPath root;
    private final String projectName;
    private final IPath repoPath;
    private final File outDir;

    public PackagesProject(IPath root, IPath repoPath, String packageName, File outDir) {
        this.root = root;
        this.repoPath = repoPath;
        this.projectName = repoPath.lastSegment() + "-" + packageName;
        this.outDir = outDir;
    }

    @Override
    public String getName() {
        return getPath().lastSegment();
    }

    @Override
    public IPath getPath() {
        return root;
    }

    @Override
    public void doCreate(IProgressMonitor monitor) throws CoreException {
        final IProject project = createProject(projectName, monitor);
        final IFolder link = createLink(monitor, project, root);

        final IClasspathEntry[] srcFolders = getSourceFolders(project, link, monitor);
        final IClasspathEntry[] pkgLibs = getPackageDependencies();
        final IClasspathEntry platform = getPlatformDependenceis(repoPath);

        final IClasspathEntry[] classPath = new IClasspathEntry[srcFolders.length + pkgLibs.length + 1];
        System.arraycopy(srcFolders, 0, classPath, 0, srcFolders.length);
        System.arraycopy(pkgLibs, 0, classPath, srcFolders.length, pkgLibs.length);
        classPath[classPath.length - 1] = platform;

        addJavaNature(classPath, project, monitor);
    }

    private void addJavaNature(IClasspathEntry[] classPath, IProject project, IProgressMonitor monitor)
            throws CoreException {
        if (project == null) {
            throw new IllegalStateException(
                    "Project must be created before giving it a Java nature");
        }

        final IProjectDescription description = project.getDescription();
        final String[] natures = description.getNatureIds();
        final String[] newNatures = Arrays.copyOf(natures, natures.length + 1);
        newNatures[natures.length] = JavaCore.NATURE_ID;
        description.setNatureIds(newNatures);
        project.setDescription(description, null);
        final IJavaProject javaProject = JavaCore.create(project);
        @SuppressWarnings("rawtypes")
        final
        Map options = javaProject.getOptions(true);
        // Compliance level need to be 1.6
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
        javaProject.setOptions(options);
        javaProject.setRawClasspath(classPath, monitor);
        javaProject.setOutputLocation(javaProject.getPath().append("out"), monitor);
    }

    private IFolder createLink(IProgressMonitor monitor, IProject project, IPath repoPath)
            throws CoreException {
        final IFolder repoLink = project.getFolder(repoPath.lastSegment());
        if (repoLink.exists()) {
            // Our work here is already done.
            return repoLink;
        }
        // Let's filter out some content we don't need.
        repoLink.createFilter(IResourceFilterDescription.EXCLUDE_ALL
                | IResourceFilterDescription.FOLDERS | IResourceFilterDescription.INHERITABLE,
                new FileInfoMatcherDescription(FILE_FILTER_ID, ".git"), 0, monitor);

        repoLink.createLink(repoPath, 0, monitor);

        return repoLink;
    }

    private IClasspathEntry getPlatformDependenceis(IPath repoPath) {
        return JavaCore.newContainerEntry(new Path(
                "it.wallgren.android.platform.classpathContainerInitializer"
                        + repoPath.makeAbsolute()));
    }

    private IClasspathEntry[] getPackageDependencies() {
        final File[] jars = outDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getAbsolutePath().endsWith(".jar");
            }
        });
        final IClasspathEntry[] entries = new IClasspathEntry[jars.length];
        int i = 0;
        for (final File file : jars) {
            entries[i++] = JavaCore.newLibraryEntry(new Path(file.getAbsolutePath()), null, null);
        }
        return entries;
    }

    private IClasspathEntry[] getSourceFolders(IProject project, IFolder link,
            IProgressMonitor monitor) throws CoreException {
        final List<IPath> files = getJavaSourceFolders(link);
        final IClasspathEntry[] classPathEntries = new IClasspathEntry[files.size()];
        int i = 0;
        for (final IPath folder : files) {
            classPathEntries[i] = JavaCore.newSourceEntry(folder);
            i++;
        }

        return classPathEntries;
    }

    private List<IPath> getJavaSourceFolders(IFolder link) {
        // TODO: Lookup all relevant java sources automagically (via Android.mk
        // or some other way)
        final List<IPath> srcFolders = new LinkedList<IPath>();

        File srcFile = link.getRawLocation().append("src").toFile();
        if (srcFile.isDirectory()) {
            srcFolders.add(link.getFullPath().append("src"));
        }
        srcFile = link.getRawLocation().append("java/src").toFile();
        if (srcFile.isDirectory()) {
            srcFolders.add(link.getFullPath().append("java/src"));
        }
        return srcFolders;
    }

    @Override
    public boolean preSelected() {
        return false;
    }
}
