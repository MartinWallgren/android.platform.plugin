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

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class AndroidRepoPage extends WizardPage {
    private String repoName;
    private ProjectCreationState state;
    private AndroidRepoComposite composite;

    public AndroidRepoPage(ProjectCreationState state, String pageName) {
        super(pageName);
        this.state = state;
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);
        composite = new AndroidRepoComposite(parent, SWT.NULL);
        composite.addListener(new CompositeListener() {
            @Override
            public void onCompositeChanged() {
                setPageComplete(validatePage());
            }
        });

        setPageComplete(validatePage());
        setErrorMessage(null);
        setMessage(null);
        setControl(composite);
        Dialog.applyDialogFont(composite);
    }

    @Override
    public void setPageComplete(boolean complete) {
        if (complete) {
            state.setRepoPath(getRepoPath());
        }
        super.setPageComplete(complete);
    }

    private boolean validatePage() {
        setErrorMessage(null);
        IPath repoPath = getRepoPath();
        return isAndroidRepo(repoPath) && isAndroidRepoCompiled(repoPath);
    }

    private boolean isAndroidRepoCompiled(IPath repoPath) {
        // Just check if the JAVA_LIBRARIES and the APP folder
        // exists since they are what we depend on
        String root = repoPath.toOSString();
        File javaLib = new File(root, "out/target/common/obj/JAVA_LIBRARIES");
        File appLib = new File(root, "out/target/common/obj/APPS");
        boolean valid = javaLib.isDirectory() && appLib.isDirectory();
        if (!valid) {
            setErrorMessage(repoPath
                    + " does not seem to be compiled. You need to build your android repo at least"
                    + " once before creating an eclipse project.");
        }
        return valid;
    }

    private boolean isAndroidRepo(IPath repoPath) {
        if (repoPath == null || repoPath.isEmpty()) {
            return false;
        }
        File root = repoPath.toFile();
        if (new File(root, ".repo").isDirectory()) {
            return true;
        }
        setErrorMessage(repoPath + " does not point to a valid android repo ");
        return false;
    }

    public IPath getRepoPath() {
        return new Path(composite.getRepoPath());
    }

    public String getRepoName() {
        if (repoName == null) {
            repoName = getRepoPath().lastSegment();
        }
        return repoName;
    }

    public String getProjectName() {
        return getRepoName() + "-platform";
    }
}
