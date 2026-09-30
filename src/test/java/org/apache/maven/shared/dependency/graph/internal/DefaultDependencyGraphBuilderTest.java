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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.Node;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.Type;
import org.apache.maven.api.Version;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverException;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyCollectorRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultDependencyGraphBuilderTest {

    private Session session;

    private Project project;

    private DependencyResolver resolver;

    private ProducedArtifact mainArtifact;

    @BeforeEach
    void setUp() {
        session = mock(Session.class);
        project = mock(Project.class);
        resolver = mock(DependencyResolver.class);
        when(session.getService(DependencyResolver.class)).thenReturn(resolver);
        mainArtifact = mock(ProducedArtifact.class);
        when(project.getMainArtifact()).thenReturn(Optional.of(mainArtifact));
    }

    private Version version(String v) {
        Version version = mock(Version.class);
        when(version.toString()).thenReturn(v);
        return version;
    }

    private Node node(String artifactId, DependencyScope scope, String asString, Node... children) {
        Version version = version("1.0");
        Dependency dependency = mock(Dependency.class);
        Type type = mock(Type.class);
        when(type.id()).thenReturn("jar");
        when(dependency.getGroupId()).thenReturn("g");
        when(dependency.getArtifactId()).thenReturn(artifactId);
        when(dependency.getVersion()).thenReturn(version);
        when(dependency.getClassifier()).thenReturn("");
        when(dependency.getType()).thenReturn(type);
        when(dependency.getScope()).thenReturn(scope);
        DependencyCoordinates coordinates = mock(DependencyCoordinates.class);
        Exclusion exclusion = mock(Exclusion.class);
        when(exclusion.getGroupId()).thenReturn("x");
        when(exclusion.getArtifactId()).thenReturn("y");
        when(coordinates.getExclusions()).thenReturn(Collections.singletonList(exclusion));
        when(dependency.toCoordinates()).thenReturn(coordinates);
        Node node = mock(Node.class);
        when(node.getDependency()).thenReturn(dependency);
        when(node.getArtifact()).thenReturn(dependency);
        when(node.getChildren()).thenReturn(List.of(children));
        when(node.asString()).thenReturn(asString);
        return node;
    }

    private void resolverReturns(Node... children) {
        Node rootNode = mock(Node.class);
        Artifact pom = mock(Artifact.class);
        when(rootNode.getArtifact()).thenReturn(pom);
        when(rootNode.getChildren()).thenReturn(List.of(children));
        DependencyResolverResult result = mock(DependencyResolverResult.class);
        when(result.getRoot()).thenReturn(rootNode);
        when(resolver.collect(any(DependencyResolverRequest.class))).thenReturn(result);
    }

    @Test
    void buildsTreeWithParentsAndProjectRootArtifact() throws Exception {
        Node grandChild = node("c", DependencyScope.COMPILE, "c-verbose");
        Node child = node("b", DependencyScope.COMPILE, "b-verbose", grandChild);
        resolverReturns(child);

        DependencyNode root = new DefaultDependencyGraphBuilder().buildDependencyGraph(session, project, null);

        assertSame(mainArtifact, root.getArtifact());
        assertNull(root.getDependency());
        assertNull(root.getParent());
        assertEquals(1, root.getChildren().size());
        DependencyNode b = root.getChildren().get(0);
        assertSame(root, b.getParent());
        assertEquals("g:b:jar:1.0:compile", b.toNodeString());
        assertEquals("g:c:jar:1.0:compile", b.getChildren().get(0).toNodeString());
        assertSame(b, b.getChildren().get(0).getParent());
        assertEquals(1, b.getExclusions().size());
        assertEquals("y", b.getExclusions().get(0).getArtifactId());

        ArgumentCaptor<DependencyResolverRequest> request = ArgumentCaptor.forClass(DependencyResolverRequest.class);
        verify(resolver).collect(request.capture());
        assertFalse(request.getValue().getVerbose());
        assertEquals(
                DependencyResolverRequest.RequestType.COLLECT,
                request.getValue().getRequestType());
    }

    @Test
    void filterPrunesSubtrees() throws Exception {
        Node kept = node("kept", DependencyScope.COMPILE, "kept");
        Node pruned = node("pruned", DependencyScope.TEST, "pruned", node("below", DependencyScope.COMPILE, "below"));
        resolverReturns(kept, pruned);

        DependencyNode root = new DefaultDependencyGraphBuilder()
                .buildDependencyGraph(session, project, d -> d.getScope() != DependencyScope.TEST);

        assertEquals(1, root.getChildren().size());
        assertEquals("kept", root.getChildren().get(0).getArtifact().getArtifactId());
    }

    @Test
    void resolverFailureIsWrapped() {
        when(resolver.collect(any(DependencyResolverRequest.class)))
                .thenThrow(new DependencyResolverException("boom", null));

        DependencyGraphBuilderException e = assertThrows(
                DependencyGraphBuilderException.class,
                () -> new DefaultDependencyGraphBuilder().buildDependencyGraph(session, project, null));
        assertTrue(e.getMessage().contains("boom"));
    }

    @Test
    void collectorAsksForVerboseAndUsesResolverNodeString() throws Exception {
        Node omitted = node("dup", DependencyScope.COMPILE, "(g:dup:jar:1.0:compile - omitted for duplicate)");
        resolverReturns(omitted);

        DependencyNode root = new DefaultDependencyCollectorBuilder()
                .collectDependencyGraph(new DependencyCollectorRequest(session, project));

        assertEquals(
                "(g:dup:jar:1.0:compile - omitted for duplicate)",
                root.getChildren().get(0).toNodeString());
        ArgumentCaptor<DependencyResolverRequest> request = ArgumentCaptor.forClass(DependencyResolverRequest.class);
        verify(resolver).collect(request.capture());
        assertTrue(request.getValue().getVerbose());
    }

    @Test
    void collectorFailureIsWrapped() {
        when(resolver.collect(any(DependencyResolverRequest.class)))
                .thenThrow(new DependencyResolverException("boom", null));

        assertThrows(
                DependencyCollectorBuilderException.class,
                () -> new DefaultDependencyCollectorBuilder()
                        .collectDependencyGraph(new DependencyCollectorRequest(session, project)));
    }
}
