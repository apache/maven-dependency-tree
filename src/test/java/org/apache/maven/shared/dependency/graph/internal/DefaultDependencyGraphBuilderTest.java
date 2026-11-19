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

import java.lang.reflect.Method;
import java.util.Collections;

import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultDependencyGraphBuilderTest {

    // Reproduces GitHub #135: a child DependencyNode produced by some resolver transformers may carry no
    // Dependency, only an Artifact. getDependencyArtifact() must fall back to the node's own artifact instead
    // of unconditionally dereferencing the (possibly null) Dependency.
    @Test
    void buildDependencyNodeFallsBackToNodeArtifactWhenChildHasNoDependency() throws Exception {
        org.eclipse.aether.artifact.Artifact childAetherArtifact =
                new org.eclipse.aether.artifact.DefaultArtifact("test:child:1.0");
        org.eclipse.aether.graph.DependencyNode childNode =
                new org.eclipse.aether.graph.DefaultDependencyNode(childAetherArtifact);
        childNode.setChildren(Collections.emptyList());

        org.eclipse.aether.artifact.Artifact rootAetherArtifact =
                new org.eclipse.aether.artifact.DefaultArtifact("test:root:1.0");
        org.eclipse.aether.graph.DependencyNode rootNode =
                new org.eclipse.aether.graph.DefaultDependencyNode(new Dependency(rootAetherArtifact, "compile"));
        rootNode.setChildren(Collections.singletonList(childNode));

        org.apache.maven.artifact.Artifact rootMavenArtifact =
                new org.apache.maven.artifact.DefaultArtifact("test", "root", "1.0", "compile", "jar", "", null);

        DefaultDependencyGraphBuilder builder = new DefaultDependencyGraphBuilder(null);
        Method buildDependencyNode = DefaultDependencyGraphBuilder.class.getDeclaredMethod(
                "buildDependencyNode",
                DependencyNode.class,
                org.eclipse.aether.graph.DependencyNode.class,
                org.apache.maven.artifact.Artifact.class,
                org.apache.maven.artifact.resolver.filter.ArtifactFilter.class);
        buildDependencyNode.setAccessible(true);

        DependencyNode result = (DependencyNode)
                assertDoesNotThrow(() -> buildDependencyNode.invoke(builder, null, rootNode, rootMavenArtifact, null));

        assertEquals(1, result.getChildren().size());
        assertEquals("child", result.getChildren().get(0).getArtifact().getArtifactId());
    }
}
