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

package me.jamiemansfield.potassium.env;

import me.jamiemansfield.potassium.jar.SourceSet;
import me.jamiemansfield.potassium.mapper.FieldGeneratedMapper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.Set;

/**
 * An {@link Environment} configured for Minecraft Classic.
 */
public class MinecraftClassicEnvironment extends Environment {

    // Minecraft Classic uses ObjectOutputStream for saving levels
    // This meant some classes couldn't be obfuscated
    // Potentially changing fields could break compatibility
    // At the Potassium level, compatibility SHOULD NOT BREAK!
    private static final Set<String> CLASS_BLACKLIST = new HashSet<>();
    // The Minecraft Classic client bundles some libs, they should not
    // be touched!
    private static final Set<String> PACKAGE_BLACKLIST = new HashSet<>();

    static {
        // Common
        CLASS_BLACKLIST.add("com/mojang/minecraft/level/Level");

        // Client
        PACKAGE_BLACKLIST.add("de/jarnbjo");

        // Server
    }

    private final Side side;

    /**
     * Creates a new Minecraft Classic environment, from the given source set.
     *
     * @param sources The source set
     */
    public MinecraftClassicEnvironment(final SourceSet sources, final Side side) {
        super(sources);
        this.side = side;
    }

    @Override
    public void configure() {
        // Common mappers
        this.registerMapper(new FieldGeneratedMapper(this, new FieldGeneratedMapper.Configuration() {
            {
                // Blacklist classes that weren't obfuscated
                this.classBlacklist.addAll(CLASS_BLACKLIST);
                this.packageBlacklist.addAll(PACKAGE_BLACKLIST);

                // Field mappings
                this.descToName.put("Lcom/mojang/minecraft/level/Level;", "level");
            }
        }));

        // Map the ParticleEngine class (+ container package)
        {
            // Find the ParticleEngine class
            final FieldDescFinder descFinder = new FieldDescFinder("particleEngine");
            this.sources.get("com/mojang/minecraft/level/Level").accept(descFinder);

            if (descFinder.fieldDesc != null) {
                final String particleEngineDesc = descFinder.fieldDesc;
                final String particleEngineName = particleEngineDesc.substring(1, particleEngineDesc.length() - 1);

                // Map ParticleEngine
                this.mappings.getOrCreateClassMapping(particleEngineName)
                        .setDeobfuscatedName("com/mojang/minecraft/particle/ParticleEngine");
            }
        }

        if (this.side == Side.CLIENT) {
            // Map the Minecraft class
            {
                // Find the Minecraft class
                final FieldDescFinder descFinder = new FieldDescFinder("minecraft");
                this.sources.get("com/mojang/minecraft/MinecraftApplet").accept(descFinder);

                if (descFinder.fieldDesc != null) {
                    final String minecraftDesc = descFinder.fieldDesc;
                    final String minecraftName = minecraftDesc.substring(1, minecraftDesc.length() - 1);

                    // Map the class to com.mojang.minecraft.Minecraft
                    this.mappings.getOrCreateClassMapping(minecraftName)
                            .setDeobfuscatedName("com/mojang/minecraft/Minecraft");

                    // Map fields
                    this.registerMapper(new FieldGeneratedMapper(this, new FieldGeneratedMapper.Configuration() {
                        {
                            // Blacklist classes that weren't obfuscated
                            this.classBlacklist.addAll(CLASS_BLACKLIST);
                            this.packageBlacklist.addAll(PACKAGE_BLACKLIST);

                            // Field mappings
                            this.descToName.put(minecraftDesc, "minecraft");
                        }
                    }));
                }
            }

            // Field Generated Mapper
            this.registerMapper(new FieldGeneratedMapper(this, new FieldGeneratedMapper.Configuration() {
                {
                    // Blacklist classes that weren't obfuscated
                    this.classBlacklist.addAll(CLASS_BLACKLIST);
                    this.packageBlacklist.addAll(PACKAGE_BLACKLIST);

                    // Field mappings
                    this.descToName.put("Lcom/mojang/minecraft/MinecraftApplet;", "applet");
                    this.descToName.put("Lcom/mojang/minecraft/player/Player;", "player");
                }
            }));
        }

        if (this.side == Side.SERVER) {
            // Field Generated Mapper
            this.registerMapper(new FieldGeneratedMapper(this, new FieldGeneratedMapper.Configuration() {
                {
                    // Blacklist classes that weren't obfuscated
                    this.classBlacklist.addAll(CLASS_BLACKLIST);
                    this.packageBlacklist.addAll(PACKAGE_BLACKLIST);

                    // Field mappings
                    this.descToName.put("Lcom/mojang/minecraft/server/MinecraftServer;", "server");
                    this.descToName.put("Ljava/util/logging/Logger;", "log");
                }
            }));
        }
    }

    private static class FieldDescFinder extends ClassVisitor {

        private final String fieldName;
        private String fieldDesc;

        public FieldDescFinder(final String fieldName) {
            super(Opcodes.ASM5);
            this.fieldName = fieldName;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            // Check if this is the field
            if (name.startsWith(this.fieldName)) {
                this.fieldDesc = desc;
            }

            return super.visitField(access, name, desc, signature, value);
        }

    }

}
