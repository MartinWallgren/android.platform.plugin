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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;

public class JavaFileParser {

    private final File javaFile;
    private PackageDeclaration javaPackage;

    public JavaFileParser(File javaFile) {
        this.javaFile = javaFile;
    }

    // use ASTParse to parse string
    public void parse() throws IOException {
        final ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(readFileToString().toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        // For now we only care about the package, parse more data as need
        // arises
        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        javaPackage = cu.getPackage();
    }

    private String readFileToString() throws IOException {
        final FileInputStream in = new FileInputStream(javaFile);
        final FileChannel fc = in.getChannel();
        final ByteBuffer buf = ByteBuffer.allocate(8192);

        if (fc.size() > Integer.MAX_VALUE) {
            // This is a crazy size for a java source file, you deserve to fail.
            throw new IOException(javaFile + " to large to parse");
        }

        final StringBuilder sb = new StringBuilder((int) fc.size());
        // TODO: Make sure that the entire world settles on one encoding and
        // that every file in existance is re-encoded.
        final Charset cs = Charset.forName("UTF-8");
        while (fc.read(buf) != -1) {
            buf.rewind();
            final CharBuffer chbuf = cs.decode(buf);
            sb.append(chbuf.array());
            buf.clear();
        }

        in.close();
        return sb.toString();
    }

    public String getPackage() {
        if (javaPackage == null) {
            return "";
        }
        if (javaPackage.getName() == null) {
            return "";
        }
        return javaPackage.getName().getFullyQualifiedName();
    }
}
