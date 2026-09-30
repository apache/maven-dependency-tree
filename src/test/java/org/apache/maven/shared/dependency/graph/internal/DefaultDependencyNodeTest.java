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

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Type;
import org.apache.maven.api.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultDependencyNodeTest {

    private Dependency dependency(boolean optional, String classifier) {
        Dependency dependency = mock(Dependency.class);
        Type type = mock(Type.class);
        when(type.id()).thenReturn("jar");
        Version version = mock(Version.class);
        when(version.toString()).thenReturn("1.2");
        when(dependency.getGroupId()).thenReturn("group");
        when(dependency.getArtifactId()).thenReturn("artifact");
        when(dependency.getVersion()).thenReturn(version);
        when(dependency.getClassifier()).thenReturn(classifier);
        when(dependency.getType()).thenReturn(type);
        when(dependency.getScope()).thenReturn(DependencyScope.COMPILE);
        when(dependency.isOptional()).thenReturn(optional);
        return dependency;
    }

    @Test
    void nodeStringShouldDisplayIfDependencyIsOptonal() {
        Dependency dependency = dependency(true, "");
        DefaultDependencyNode optionalNode = new DefaultDependencyNode(null, dependency, dependency, null, null);
        assertEquals("group:artifact:jar:1.2:compile (optional)", optionalNode.toNodeString());
        assertEquals(Boolean.TRUE, optionalNode.getOptional());
    }

    @Test
    void nodeStringForMandatoryDepenendencyDoesNotContainOptionalInformation() {
        Dependency dependency = dependency(false, "");
        DefaultDependencyNode mandatoryNode = new DefaultDependencyNode(null, dependency, dependency, null, null);
        assertEquals("group:artifact:jar:1.2:compile", mandatoryNode.toNodeString());
        assertEquals(Boolean.FALSE, mandatoryNode.getOptional());
    }

    @Test
    void nodeStringContainsClassifier() {
        Dependency dependency = dependency(false, "tests");
        DefaultDependencyNode node = new DefaultDependencyNode(null, dependency, dependency, null, null);
        assertEquals("group:artifact:jar:tests:1.2:compile", node.toNodeString());
    }

    @Test
    void rootNodeHasNoScopeAndNoOptionalFlag() {
        Artifact artifact = mock(Artifact.class);
        Version version = mock(Version.class);
        when(version.toString()).thenReturn("1.2");
        when(artifact.getGroupId()).thenReturn("group");
        when(artifact.getArtifactId()).thenReturn("artifact");
        when(artifact.getVersion()).thenReturn(version);
        when(artifact.getClassifier()).thenReturn("");
        when(artifact.getExtension()).thenReturn("jar");
        DefaultDependencyNode root = new DefaultDependencyNode(artifact);
        assertEquals("group:artifact:jar:1.2", root.toNodeString());
        assertNull(root.getOptional());
    }

    @Test
    void explicitNodeStringWins() {
        Dependency dependency = dependency(true, "");
        DefaultDependencyNode node = new DefaultDependencyNode(null, dependency, dependency, null, "custom");
        assertEquals("custom", node.toNodeString());
    }
}
