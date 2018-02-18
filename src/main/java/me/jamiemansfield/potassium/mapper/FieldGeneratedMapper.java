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

import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;

import me.jamiemansfield.lorenz.model.ClassMapping;
import me.jamiemansfield.potassium.env.Environment;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of {@link SimpleMapper} for generating
 * mappings for fields, based on their types.
 */
public class FieldGeneratedMapper extends SimpleMapper<FieldGeneratedMapper.Configuration> {

    /**
     * Creates a new mapper, from the given environment.
     *
     * @param environment The environment
     * @param configuration The configuration
     */
    public FieldGeneratedMapper(final Environment environment, final Configuration configuration) {
        super(environment, configuration);
    }

    @Override
    public void map(final ClassNode node) {
        // Get the class mapping
        final ClassMapping classMapping = this.mappings.getOrCreateClassMapping(node.name);

        node.fields.stream()
                .filter(fieldNode -> this.configuration.descToName.containsKey(fieldNode.desc))
                .filter(fieldNode -> (fieldNode.access & ACC_SYNTHETIC) == 0)
                .forEach(fieldNode ->
                        classMapping.getOrCreateFieldMapping(fieldNode.name)
                                .setDeobfuscatedName(this.configuration.descToName.get(fieldNode.desc))
                );
    }

    public static class Configuration extends SimpleMapper.Configuration {

        public Map<String, String> descToName = new HashMap<>();

    }

}