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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Class for analyzing a folder and it sub-folders
 */
public class DirectoryAnalyzer {
    private final FileFilter fileFilter = new FileFilter() {

        @Override
        public boolean accept(File path) {
            return path.isDirectory() || path.getName().endsWith(".java");
        }
    };

    private final File root;

    private final Comparator<? super File> fileSortComparator = new Comparator<File>() {
        @Override
        public int compare(File o1, File o2) {
            if (o1 != null && o1.isFile()) {
                return -1;
            }
            if (o2 != null && o2.isFile()) {
                return 1;
            }
            return 0;
        }
    };

    public DirectoryAnalyzer(File root) {
        this.root = root;
    }

    public Set<File> findJavaSourceDirectories() throws IOException {
        final Set<File> sourceDirectories = new HashSet<File>();
        findJavaSourceDirectories(sourceDirectories, root);
        return sourceDirectories;
    }

    private void findJavaSourceDirectories(Set<File> out, File path) throws IOException {
        // TODO: Shortcut the search for path where source dir is already found
        // in a previous
        // iteration.
        // I.e there is no need to search
        // ...src/foo/bar/File.java
        // if
        // ...src/bar/foo/File.java
        // has found .../src already

        final File[] children = path.listFiles(fileFilter);
        Arrays.sort(children, fileSortComparator);
        for (final File file : children) {
            if (file.isFile()) {
                // File filter ensures that this is a Java file
                final File dir = getSourceDirForJavaFile(file);
                if (dir != null) {
                    if (!dir.isDirectory()) {
                        System.err.println(file + " resulted in non dir as java source: " + dir);
                        continue;
                    }
                    out.add(dir);
                    // No need to check the rest of the content in this folder
                    break;
                }
            }
            if (file.isDirectory()) {
                findJavaSourceDirectories(out, file);
            }
        }
    }

    private File getSourceDirForJavaFile(File file) throws IOException {
        final JavaFileParser parser = new JavaFileParser(file);
        parser.parse();
        final String javaPackage = parser.getPackage();
        if (javaPackage.length() == 0) {
            return file.getParentFile();
        }
        final String dir = file.getParent();
        int dirIndex = dir.length() - 1;
        int pkgIndex = javaPackage.length() - 1;
        while (dirIndex > 0 && pkgIndex > 0) {
            char dc = dir.charAt(dirIndex);
            while (dc == '.' || dc == '/') {
                // Skip / and .
                dc = dir.charAt(--dirIndex);
            }
            char pc = javaPackage.charAt(pkgIndex);
            while (pc == '.') {
                // Skip .
                pc = javaPackage.charAt(--pkgIndex);
            }
            if (pc != dc) {
                // Something is fishy, package declaration does not match
                // directory structure
                return null;
            }
            dirIndex--;
            pkgIndex--;
        }
        return new File(dir.substring(0, dirIndex));
    }
}
