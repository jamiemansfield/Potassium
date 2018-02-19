/*
 * This file is part of Potassium, licensed under the BSD 3-Clause License.
 *
 * Copyright (c) Jamie Mansfield <https://www.jamierocks.uk/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package me.jamiemansfield.potassium.patcher;

import static org.objectweb.asm.Opcodes.ACC_ENUM;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ASM5;

import com.google.common.io.ByteStreams;
import me.jamiemansfield.lorenz.model.jar.Signature;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public class InnerClassPatcher extends ClassVisitor {

    public static final void patchJar(final Path inputJar, final Path outputJar, final Configuration configuration) {
        try (final JarFile jarFile = new JarFile(inputJar.toFile())) {
            try (final JarOutputStream jos = new JarOutputStream(Files.newOutputStream(outputJar))) {
                for (final JarEntry entry : jarFile.stream().collect(Collectors.toSet())) {
                    jos.putNextEntry(new JarEntry(entry.getName()));

                    if (entry.getName().endsWith(".class")) {
                        final ClassReader reader = new ClassReader(ByteStreams.toByteArray(jarFile.getInputStream(entry)));
                        final ClassNode newNode = new ClassNode();
                        reader.accept(new InnerClassPatcher(newNode, configuration), 0);

                        final ClassWriter writer = new ClassWriter(0);
                        newNode.accept(writer);

                        jos.write(writer.toByteArray());
                    } else {
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ByteStreams.copy(jarFile.getInputStream(entry), baos);
                        jos.write(baos.toByteArray());
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        } catch (final IOException ex) {
            System.err.println("Failed to read the jar file!");
            ex.printStackTrace(System.err);
        }
    }

    private final Configuration configuration;

    private String name = "";
    private boolean isInnerClass = false;
    private boolean isEnum = false;
    private boolean hasOuterClass = false;

    public InnerClassPatcher(final ClassVisitor cv, final Configuration configuration) {
        super(ASM5, cv);
        this.configuration = configuration;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.name = name;
        this.isInnerClass = name.contains("$");
        this.isEnum = (access & ACC_ENUM) != 0;

        // Call super
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        this.hasOuterClass = true;

        // Call super
        super.visitOuterClass(owner, name, desc);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final boolean isConstructor = Objects.equals("<init>", name);
        final boolean isSynthetic = (access & ACC_SYNTHETIC) == 0;

        // Using the synthetic <init> method, I can automatically populate
        // the nameToOuter configurations (and innerConfig.owner) using the
        // first parameter
        // This is unless the class is static or an enum
        if (this.isInnerClass && !this.isEnum && isConstructor && isSynthetic) {
            final String type = Signature.compile(desc).getParamTypes().get(0).getObfuscated();
            final String className = type.substring(1, type.length() - 1);
            this.configuration.nameToOuter.put(className, new OuterClassConfiguration() {
                {
                    this.inner = InnerClassPatcher.this.name;
                }
            });
            if (!this.configuration.nameToInner.containsKey(this.name)) {
                this.configuration.nameToInner.put(this.name, new InnerClassConfiguration());
            }
            this.configuration.nameToInner.get(this.name).owner = className;
        }

        // Call super
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        if (this.isInnerClass && !this.hasOuterClass && this.configuration.nameToInner.containsKey(this.name)) {
            final InnerClassConfiguration classConfig = this.configuration.nameToInner.get(this.name);
            this.visitOuterClass(classConfig.owner, classConfig.name, classConfig.desc);
            this.visitInnerClass(this.name, null, null, 0);
        }

        if (this.configuration.nameToOuter.containsKey(this.name)) {
            final OuterClassConfiguration classConfiguration = this.configuration.nameToOuter.get(this.name);
            this.visitInnerClass(classConfiguration.inner, null, null, 0);
        }

        // Reset values
        this.isInnerClass = false;
        this.isEnum = false;
        this.hasOuterClass = false;

        // Call super
        super.visitEnd();
    }

    public static class Configuration {

        public Map<String, OuterClassConfiguration> nameToOuter = new HashMap<>();
        public Map<String, InnerClassConfiguration> nameToInner = new HashMap<>();

    }

    public static class OuterClassConfiguration {

        public String inner;

    }

    public static class InnerClassConfiguration {

        public String owner;
        public String name;
        public String desc;

    }

}
