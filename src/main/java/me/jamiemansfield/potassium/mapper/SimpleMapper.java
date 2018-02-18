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

package me.jamiemansfield.potassium.mapper;

import com.google.common.collect.Sets;
import me.jamiemansfield.potassium.env.Environment;
import me.jamiemansfield.potassium.jar.SourceSet;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.Set;

/**
 * An abstract implementation of {@link AbstractMapper}, that provides
 * a simple API.
 *
 * @param <C> The type of the mapper's configuration
 */
public abstract class SimpleMapper<C extends SimpleMapper.Configuration> extends AbstractMapper {

    private static final Set<String> BLACKLIST = Sets.newHashSet(
            "net/minecraft/client/ClientBrandRetriever",
            "net/minecraft/realms/",
            "com/google/",
            "paulscode/sound/",
            "com/jcraft/",
            "de/jarnbjo"
    );

    protected final C configuration;

    /**
     * Creates a new mapper, from the given environment.
     *
     * @param environment The environment
     */
    protected SimpleMapper(final Environment environment, final C configuration) {
        super(environment.mappings);
        this.configuration = configuration;
    }

    /**
     * Maps the given {@link ClassNode}.
     *
     * @param node The class node
     */
    public abstract void map(final ClassNode node);

    @Override
    public void map(final SourceSet sources) {
        sources.getClasses().stream()
                .filter(node -> this.configuration.packageBlacklist.stream().noneMatch(partial -> node.name.startsWith(partial)))
                .filter(node -> !this.configuration.classBlacklist.contains(node.name))
                .forEach(node -> {
                    System.out.println("Processing " + node.name);
                    this.map(node);
                });
    }

    public static class Configuration {

        public Set<String> packageBlacklist = new HashSet<>();
        public Set<String> classBlacklist = new HashSet<>();

    }

}