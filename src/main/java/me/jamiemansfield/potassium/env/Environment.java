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

import me.jamiemansfield.lorenz.MappingSet;
import me.jamiemansfield.potassium.jar.SourceSet;
import me.jamiemansfield.potassium.mapper.AbstractMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an environment, of which can be mapped.
 */
public abstract class Environment extends AbstractMapper {

    protected final SourceSet sources;
    private final List<AbstractMapper> mappers = new ArrayList<>();

    /**
     * Creates a new environment, from the given source set.
     *
     * @param sources The source set
     */
    protected Environment(final SourceSet sources) {
        super(new MappingSet());
        this.sources = sources;
    }

    /**
     * Configures the environment.
     */
    public abstract void configure();

    /**
     * Registers the given {@link AbstractMapper}.
     *
     * @param mapper The mapper
     */
    protected void registerMapper(final AbstractMapper mapper) {
        this.mappers.add(mapper);
    }

    /**
     * Maps the environment.
     */
    public void map() {
        this.configure();
        this.map(this.sources);
    }

    @Override
    public void map(final SourceSet sources) {
        this.mappers.forEach(mapper -> mapper.map(sources));
    }

    public enum Side {

        CLIENT,
        SERVER,
        ;

    }

}