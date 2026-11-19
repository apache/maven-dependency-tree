/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.shared.dependency.graph.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.shared.dependency.graph.internal.VerboseJavaScopeSelector.ScopeComparator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerboseJavaScopeSelectorTest {

    private final ScopeComparator comparator = new ScopeComparator();

    @Test
    void selectsTheSameUnknownScopeRegardlessOfEncounterOrder() {
        assertEquals("system", Stream.of("system", "custom").max(comparator).get());
        assertEquals("system", Stream.of("custom", "system").max(comparator).get());
    }

    @Test
    void ranksUnknownScopesBelowKnownScopesAndPreservesKnownScopeOrder() {
        List<String> scopes =
                new ArrayList<>(Arrays.asList("provided", "system", "compile", "custom", "test", "runtime"));

        scopes.sort(comparator.reversed());

        assertEquals(Arrays.asList("compile", "runtime", "provided", "test", "system", "custom"), scopes);
    }
}
