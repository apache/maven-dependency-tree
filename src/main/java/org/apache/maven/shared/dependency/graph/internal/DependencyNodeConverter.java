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
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;
import org.apache.maven.shared.dependency.graph.DependencyNode;

/**
 * Converts the tree of Maven 4 {@link Node}s into {@link DependencyNode}s, which add the parent link and the
 * dependency filtering.
 */
final class DependencyNodeConverter {
    private final Predicate<Dependency> filter;

    private final boolean verbose;

    DependencyNodeConverter(Predicate<Dependency> filter, boolean verbose) {
        this.filter = filter;
        this.verbose = verbose;
    }

    /**
     * Converts the root, which carries the project artifact and not a dependency.
     */
    DependencyNode convertRoot(Node root, Project project) {
        Artifact rootArtifact = project.getMainArtifact()
                .map(Artifact.class::cast)
                .orElseGet(() -> root.getArtifact() != null ? root.getArtifact() : project.getPomArtifact());
        DefaultDependencyNode current = new DefaultDependencyNode(null, rootArtifact, null, null, null);
        current.setChildren(convertChildren(current, root));
        return current;
    }

    private DependencyNode convert(DependencyNode parent, Node node) {
        Dependency dependency = node.getDependency();
        List<Exclusion> exclusions =
                dependency != null ? new ArrayList<>(dependency.toCoordinates().getExclusions()) : null;
        Artifact artifact = dependency != null ? dependency : node.getArtifact();
        DefaultDependencyNode current =
                new DefaultDependencyNode(parent, artifact, dependency, exclusions, verbose ? node.asString() : null);
        current.setChildren(convertChildren(current, node));
        return current;
    }

    private List<DependencyNode> convertChildren(DependencyNode parent, Node node) {
        List<DependencyNode> nodes = new ArrayList<>(node.getChildren().size());
        for (Node child : node.getChildren()) {
            Dependency dependency = child.getDependency();
            if (filter == null || dependency == null || filter.test(dependency)) {
                nodes.add(convert(parent, child));
            }
        }
        return Collections.unmodifiableList(nodes);
    }
}
