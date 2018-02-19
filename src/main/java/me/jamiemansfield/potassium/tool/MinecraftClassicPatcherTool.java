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

package me.jamiemansfield.potassium.tool;

import static java.util.Arrays.asList;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import me.jamiemansfield.potassium.patcher.InnerClassPatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class MinecraftClassicPatcherTool {

    public static void main(final String[] args) {
        final OptionParser parser = new OptionParser();

        final OptionSpec<Void> helpSpec = parser.acceptsAll(asList("?", "help"), "Show the help")
                .forHelp();

        final OptionSpec<Path> clientJarPathSpec = parser.accepts("clientJar", "The location of the client jar")
                .withRequiredArg()
                .withValuesConvertedBy(PathValueConverter.INSTANCE)
                .defaultsTo(Paths.get("client.jar"));
        final OptionSpec<Path> serverJarPathSpec = parser.accepts("serverJar", "The location of the server jar")
                .withRequiredArg()
                .withValuesConvertedBy(PathValueConverter.INSTANCE)
                .defaultsTo(Paths.get("server.jar"));

        final OptionSpec<Path> fixedClientJarPathSpec = parser.accepts("newClientJar", "The location of the fixed client jar")
                .withRequiredArg()
                .withValuesConvertedBy(PathValueConverter.INSTANCE)
                .defaultsTo(Paths.get("client-fixed.jar"));
        final OptionSpec<Path> fixedServerJarPathSpec = parser.accepts("newServerJar", "The location of the fixed server jar")
                .withRequiredArg()
                .withValuesConvertedBy(PathValueConverter.INSTANCE)
                .defaultsTo(Paths.get("server-fixed.jar"));

        final OptionSet options;
        try {
            options = parser.parse(args);
        } catch (final OptionException ex) {
            System.err.println("Failed to parse OptionSet! Exiting...");
            ex.printStackTrace(System.err);
            System.exit(-1);
            return;
        }

        if (options == null || options.has(helpSpec)) {
            try {
                parser.printHelpOn(System.err);
            } catch (final IOException ex) {
                System.err.println("Failed to print help information!");
                ex.printStackTrace(System.err);
            }
            System.exit(-1);
            return;
        }

        final Path clientJar = options.valueOf(clientJarPathSpec);
        final Path serverJar = options.valueOf(serverJarPathSpec);
        final Path fixedClientJar = options.valueOf(fixedClientJarPathSpec);
        final Path fixedServerJar = options.valueOf(fixedServerJarPathSpec);

        if (!(Files.exists(clientJar) && Files.exists(serverJar))) {
            throw new RuntimeException("Client jar, server jar, or both do not exist!");
        }

        InnerClassPatcher.patchJar(clientJar, fixedClientJar, new InnerClassPatcher.Configuration() {
            {
                // Minecraft Class Fixes
                {
                    // Fix Minecraft
                    {
                        final List<InnerClassPatcher.InnerClassConfiguration> minecraftClass = this.inner("com/mojang/minecraft/l");

                        minecraftClass.add(new InnerClassPatcher.InnerClassConfiguration() {
                            {
                                this.name = "com/mojang/minecraft/Minecraft$OS";
                                this.outerName = "com/mojang/minecraft/l";
                                this.innerName = "OS";
                                this.access = ACC_PUBLIC;
                            }
                        });
                        minecraftClass.add(new InnerClassPatcher.InnerClassConfiguration() {
                            {
                                this.name = "com/mojang/minecraft/f";
                                this.access = ACC_PUBLIC;
                            }
                        });
                    }

                    // Fix Minecraft$OS
                    {
                        final List<InnerClassPatcher.InnerClassConfiguration> minecraftOsClass = this.inner("com/mojang/minecraft/Minecraft$OS");

                        minecraftOsClass.add(new InnerClassPatcher.InnerClassConfiguration() {
                            {
                                this.name = "com/mojang/minecraft/Minecraft$OS";
                                this.outerName = "com/mojang/minecraft/l";
                                this.innerName = "OS";
                                this.access = ACC_PUBLIC;
                            }
                        });
                    }

                    // Fix switch statement up
                    {
                        final List<InnerClassPatcher.InnerClassConfiguration> minecraftSwitchClass = this.inner("com/mojang/minecraft/f");

                        this.outerConfig.put("com/mojang/minecraft/f", new InnerClassPatcher.OuterClassConfiguration() {
                            {
                                this.owner = "com/mojang/minecraft/l";
                            }
                        });

                        minecraftSwitchClass.add(new InnerClassPatcher.InnerClassConfiguration() {
                            {
                                this.name = "com/mojang/minecraft/f";
                                this.access = ACC_PUBLIC;
                            }
                        });
                        minecraftSwitchClass.add(new InnerClassPatcher.InnerClassConfiguration() {
                            {
                                this.name = "com/mojang/minecraft/Minecraft$OS";
                                this.outerName = "com/mojang/minecraft/l";
                                this.innerName = "OS";
                                this.access = ACC_PUBLIC;
                            }
                        });
                    }
                }

                // MinecraftApplet Class Fixes
                {
                    // Fix MinecraftApplet
                    {
                        this.inner("com/mojang/minecraft/MinecraftApplet").add(new InnerClassPatcher.InnerClassConfiguration() {
                            {
                                this.name = "com/mojang/minecraft/MinecraftApplet$1";
                            }
                        });
                    }

                    // Fix anonymous class
                    {
                        this.outerConfig.put("com/mojang/minecraft/MinecraftApplet$1", new InnerClassPatcher.OuterClassConfiguration() {
                            {
                                this.owner = "com/mojang/minecraft/MinecraftApplet";
                                this.name = "init()";
                                this.desc = "()V";
                            }
                        });
                    }
                }
            }
        });

        InnerClassPatcher.patchJar(serverJar, fixedServerJar, new InnerClassPatcher.Configuration() {
            {
                // TODO: server config
            }
        });
    }

    private MinecraftClassicPatcherTool() {
    }

}
