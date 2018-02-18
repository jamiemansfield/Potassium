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

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import me.jamiemansfield.lorenz.io.writer.SrgWriter;
import me.jamiemansfield.potassium.env.Environment;
import me.jamiemansfield.potassium.env.MinecraftClassicEnvironment;
import me.jamiemansfield.potassium.jar.JarWalker;
import me.jamiemansfield.potassium.jar.SourceSet;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This is the user-facing program for mapping Minecraft Classic.
 */
public final class MinecraftClassicTool {

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

        final OptionSpec<Path> clientSrgPathSpec = parser.accepts("clientSrg", "The location of the client srg")
                .withRequiredArg()
                .withValuesConvertedBy(PathValueConverter.INSTANCE)
                .defaultsTo(Paths.get("client.srg"));
        final OptionSpec<Path> serverSrgPathSpec = parser.accepts("serverSrg", "The location of the server srg")
                .withRequiredArg()
                .withValuesConvertedBy(PathValueConverter.INSTANCE)
                .defaultsTo(Paths.get("server.srg"));

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
        final Path clientSrg = options.valueOf(clientSrgPathSpec);
        final Path serverSrg = options.valueOf(serverSrgPathSpec);

        if (!(Files.exists(clientJar) && Files.exists(serverJar))) {
            throw new RuntimeException("Client jar, server jar, or both do not exist!");
        }

        {
            final SourceSet clientSources = new SourceSet();
            new JarWalker(clientJar).walk(clientSources);
            final MinecraftClassicEnvironment clientEnv = new MinecraftClassicEnvironment(clientSources, Environment.Side.CLIENT);
            clientEnv.map();

            try (final SrgWriter writer = new SrgWriter(new PrintWriter(Files.newOutputStream(clientSrg)))) {
                writer.write(clientEnv.getMappings());
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
        }

        {
            final SourceSet serverSources = new SourceSet();
            new JarWalker(serverJar).walk(serverSources);
            final MinecraftClassicEnvironment serverEnv = new MinecraftClassicEnvironment(serverSources, Environment.Side.SERVER);
            serverEnv.map();

            try (final SrgWriter writer = new SrgWriter(new PrintWriter(Files.newOutputStream(serverSrg)))) {
                writer.write(serverEnv.getMappings());
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private MinecraftClassicTool() {
    }

}
