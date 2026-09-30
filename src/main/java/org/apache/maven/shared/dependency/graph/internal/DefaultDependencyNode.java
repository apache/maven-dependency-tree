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

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Exclusion;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;

/**
 * Default implementation of a DependencyNode.
 */
public class DefaultDependencyNode implements DependencyNode {
    private final Artifact artifact;

    private final Dependency dependency;

    private final DependencyNode parent;

    private final List<Exclusion> exclusions;

    private final String nodeString;

    private List<DependencyNode> children = Collections.emptyList();

    /**
     * Constructs the DefaultDependencyNode.
     *
     * @param parent     Parent node, may be {@code null}.
     * @param artifact   Artifact associated with this node.
     * @param dependency Dependency that led to this node, {@code null} for the root.
     * @param exclusions Exclusions of the dependency, may be {@code null}.
     * @param nodeString The text returned by {@link #toNodeString()}, or {@code null} to derive it from the artifact,
     *                   scope and optional flag.
     */
    public DefaultDependencyNode(
            DependencyNode parent,
            Artifact artifact,
            Dependency dependency,
            List<Exclusion> exclusions,
            String nodeString) {
        this.parent = parent;
        this.artifact = artifact;
        this.dependency = dependency;
        this.exclusions = exclusions;
        this.nodeString = nodeString;
    }

    // user to refer to winner
    public DefaultDependencyNode(Artifact artifact) {
        this(null, artifact, null, null, null);
    }

    /**
     * Applies the specified dependency node visitor to this dependency node and its children.
     *
     * @param visitor the dependency node visitor to use
     * @return the visitor result of ending the visit to this node
     * @since 1.1
     */
    @Override
    public boolean accept(DependencyNodeVisitor visitor) {
        if (visitor.visit(this)) {
            for (DependencyNode child : getChildren()) {
                if (!child.accept(visitor)) {
                    break;
                }
            }
        }

        return visitor.endVisit(this);
    }

    /**
     * @return Artifact for this DependencyNode.
     */
    @Override
    public Artifact getArtifact() {
        return artifact;
    }

    @Override
    public Dependency getDependency() {
        return dependency;
    }

    /**
     *
     * @param children  List of DependencyNode to set as child nodes.
     */
    public void setChildren(List<DependencyNode> children) {
        this.children = children;
    }

    /**
     * @return List of child nodes for this DependencyNode.
     */
    @Override
    public List<DependencyNode> getChildren() {
        return children;
    }

    /**
     * @return Parent of this DependencyNode.
     */
    @Override
    public DependencyNode getParent() {
        return parent;
    }

    @Override
    public Boolean getOptional() {
        return dependency != null ? dependency.isOptional() : null;
    }

    @Override
    public List<Exclusion> getExclusions() {
        return exclusions;
    }

    /**
     * @return Stringified representation of this DependencyNode.
     */
    @Override
    public String toNodeString() {
        if (nodeString != null) {
            return nodeString;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(artifact.getGroupId())
                .append(':')
                .append(artifact.getArtifactId())
                .append(':');
        sb.append(dependency != null ? dependency.getType().id() : artifact.getExtension());
        if (!artifact.getClassifier().isEmpty()) {
            sb.append(':').append(artifact.getClassifier());
        }
        sb.append(':').append(artifact.getVersion());
        if (dependency != null) {
            sb.append(':').append(dependency.getScope().id());
            if (dependency.isOptional()) {
                sb.append(" (optional)");
            }
        }
        return sb.toString();
    }
}
