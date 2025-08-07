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

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Exclusion;
import org.apache.maven.shared.dependency.graph.ConflictData;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;

/**
 * Default implementation of a DependencyNode.
 */
public class DefaultDependencyNode implements DependencyNode {
    private final Artifact artifact;

    private final DependencyNode parent;

    private final String premanagedVersion;

    private final String premanagedScope;

    private final String versionConstraint;

    private List<DependencyNode> children;

    private Boolean optional;

    private List<Exclusion> exclusions;

    /**
     * Constructs the DefaultDependencyNode.
     *
     * @param parent            Parent node, may be {@code null}.
     * @param artifact          Artifact associated with this dependency.
     * @param premanagedVersion the premanaged version, may be {@code null}.
     * @param premanagedScope   the premanaged scope, may be {@code null}.
     * @param versionConstraint the version constraint, may be {@code null.}
     */
    public DefaultDependencyNode(
            DependencyNode parent,
            Artifact artifact,
            String premanagedVersion,
            String premanagedScope,
            String versionConstraint) {
        this.parent = parent;
        this.artifact = artifact;
        this.premanagedVersion = premanagedVersion;
        this.premanagedScope = premanagedScope;
        this.versionConstraint = versionConstraint;
    }

    public DefaultDependencyNode(
            DependencyNode parent,
            Artifact artifact,
            String premanagedVersion,
            String premanagedScope,
            String versionConstraint,
            Boolean optional,
            List<Exclusion> exclusions) {
        this.parent = parent;
        this.artifact = artifact;
        this.premanagedVersion = premanagedVersion;
        this.premanagedScope = premanagedScope;
        this.versionConstraint = versionConstraint;
        this.optional = optional;
        this.exclusions = exclusions;
    }

    // user to refer to winner
    public DefaultDependencyNode(Artifact artifact) {
        this.artifact = artifact;
        this.parent = null;
        this.premanagedScope = null;
        this.premanagedVersion = null;
        this.versionConstraint = null;
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
    public String getPremanagedVersion() {
        return premanagedVersion;
    }

    @Override
    public String getPremanagedScope() {
        return premanagedScope;
    }

    @Override
    public String getVersionConstraint() {
        return versionConstraint;
    }

    @Override
    public Boolean getOptional() {
        return optional;
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
        return artifact + (Boolean.TRUE.equals(optional) ? " (optional)" : "");
    }

    @Override
    public ConflictData getConflictData() {
        return null;
    }
}
