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
package org.apache.maven.shared.dependency.graph.filter;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.internal.DefaultDependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.BuildingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AncestorOrSelfDependencyNodeFilterTest {

    private final Artifact rootArtifact = new DefaultArtifact("g", "root", "1.0", "compile", "jar", "", null);
    private final Artifact childArtifact = new DefaultArtifact("g", "child", "1.0", "compile", "jar", "", null);

    @Test
    void acceptsAncestorAndSelf() {
        DefaultDependencyNode root =
                new DefaultDependencyNode(null, rootArtifact, null, null, null, false, Collections.emptyList());
        DefaultDependencyNode child =
                new DefaultDependencyNode(root, childArtifact, null, null, null, false, Collections.emptyList());
        root.setChildren(Collections.singletonList(child));

        AncestorOrSelfDependencyNodeFilter filter = new AncestorOrSelfDependencyNodeFilter(child);

        assertTrue(filter.accept(root));
        assertTrue(filter.accept(child));
    }

    @Test
    void acceptsAncestorAndSelfOnATreeRebuiltFromTheOriginal() {
        // BuildingDependencyNodeVisitor is a common way to produce a new tree from an existing one (used e.g. by
        // consumers combining a filter with FilteringDependencyNodeVisitor). The rebuilt tree is logically
        // identical but made up of different DependencyNode instances than the tree the filter was built against.
        DefaultDependencyNode root =
                new DefaultDependencyNode(null, rootArtifact, null, null, null, false, Collections.emptyList());
        DefaultDependencyNode child =
                new DefaultDependencyNode(root, childArtifact, null, null, null, false, Collections.emptyList());
        child.setChildren(new ArrayList<>());
        root.setChildren(new ArrayList<>(Collections.singletonList(child)));

        AncestorOrSelfDependencyNodeFilter filter = new AncestorOrSelfDependencyNodeFilter(child);

        BuildingDependencyNodeVisitor builder = new BuildingDependencyNodeVisitor();
        root.accept(new DependencyNodeVisitor() {
            @Override
            public boolean visit(DependencyNode node) {
                return builder.visit(node);
            }

            @Override
            public boolean endVisit(DependencyNode node) {
                return builder.endVisit(node);
            }
        });
        DependencyNode rebuiltRoot = builder.getDependencyTree();
        DependencyNode rebuiltChild = rebuiltRoot.getChildren().get(0);

        assertTrue(filter.accept(rebuiltRoot));
        assertTrue(filter.accept(rebuiltChild));
    }

    @Test
    void rejectsUnrelatedNode() {
        DefaultDependencyNode root =
                new DefaultDependencyNode(null, rootArtifact, null, null, null, false, Collections.emptyList());
        DefaultDependencyNode child =
                new DefaultDependencyNode(root, childArtifact, null, null, null, false, Collections.emptyList());
        root.setChildren(Collections.singletonList(child));

        Artifact unrelatedArtifact = new DefaultArtifact("g", "unrelated", "1.0", "compile", "jar", "", null);
        DefaultDependencyNode unrelated =
                new DefaultDependencyNode(null, unrelatedArtifact, null, null, null, false, Collections.emptyList());

        AncestorOrSelfDependencyNodeFilter filter = new AncestorOrSelfDependencyNodeFilter(child);

        assertFalse(filter.accept(unrelated));
    }
}
