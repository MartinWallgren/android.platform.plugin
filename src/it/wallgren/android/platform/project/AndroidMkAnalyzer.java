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
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

/**
 * Android.mk parser. This is not a complete parser, it is only intended to find
 * the information we need
 */
public class AndroidMkAnalyzer {
    private static final String PACKAGE_NAME = "LOCAL_PACKAGE_NAME";
    private static final String BUILD_INCLUDE_APPS = "include $(BUILD_PACKAGE)";
    private static final String BUILD_INCLUDE_JAVA_LIBRARY = "include $(BUILD_PACKAGE)";

    private final File makeFile;
    private String packageName;
    private final File repoRoot;
    private File outDir;

    public AndroidMkAnalyzer(File repoRoot, File makeFile) {
        this.makeFile = makeFile;
        this.repoRoot = repoRoot;
    }

    /**
     * What we need to build a PackageProject: - Java source folder(s) - Package
     * name - dependant libraries (not really needed at the moment since we add
     * all libs to "android - java dependencies"
     */

    public void parse() throws IOException {
        packageName = null;
        final LineNumberReader reader = new LineNumberReader(new FileReader(makeFile));
        String line;
        while ((line = reader.readLine()) != null) {
            final String[] tokens = line.split("\\s*[:+]=\\s*");
            if (tokens.length > 1) {
                if (PACKAGE_NAME.equals(tokens[0])) {
                    packageName = tokens[1];
                }
            } else {
                if (BUILD_INCLUDE_APPS.equals(tokens[0])) {
                    outDir = new File(repoRoot, "/out/target/common/obj/APPS/" + getPackageName()
                            + "_intermediates");
                } else if (BUILD_INCLUDE_JAVA_LIBRARY.equals(tokens[0])) {
                    outDir = new File(repoRoot, "/out/target/common/obj/JAVA_LIBRARIES"
                            + getPackageName() + "_intermediates");
                }
            }
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public File getOutDirectory() {
        return outDir;
    }
}
