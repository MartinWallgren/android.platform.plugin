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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class AndroidClasspathContainerInitializer extends ClasspathContainerInitializer {

    public AndroidClasspathContainerInitializer() {
    }

    @Override
    public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
        IPath repoRoot = containerPath.removeFirstSegments(1).makeAbsolute();
        AndroidClasspathContainer container = new AndroidClasspathContainer(repoRoot);
        JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {
            project
        },
                new IClasspathContainer[] {
                    container
                }, new NullProgressMonitor());
    }

    @Override
    public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
        return true;
    }
}
