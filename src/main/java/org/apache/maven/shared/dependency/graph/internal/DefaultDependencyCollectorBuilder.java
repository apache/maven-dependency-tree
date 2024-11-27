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

import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.ConflictData;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilder;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyCollectorRequest;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;
import org.eclipse.aether.version.VersionConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project dependency raw dependency collector API, abstracting Maven 3.1+'s Aether implementation.
 *
 * @author Gabriel Belingueres
 * @since 3.1.0
 */
@Named
public class DefaultDependencyCollectorBuilder implements DependencyCollectorBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDependencyCollectorBuilder.class);

    private final RepositorySystem repositorySystem;

    @Inject
    public DefaultDependencyCollectorBuilder(RepositorySystem repositorySystem) {
        this.repositorySystem = repositorySystem;
    }

    @Override
    public DependencyNode collectDependencyGraph(DependencyCollectorRequest dependencyCollectorRequest)
            throws DependencyCollectorBuilderException {
        DefaultRepositorySystemSession session = null;
        try {
            ProjectBuildingRequest buildingRequest = dependencyCollectorRequest.getBuildingRequest();
            MavenProject project = buildingRequest.getProject();

            Artifact projectArtifact = project.getArtifact();
            List<ArtifactRepository> remoteArtifactRepositories = project.getRemoteArtifactRepositories();

            RepositorySystemSession repositorySession = buildingRequest.getRepositorySession();

            session = new DefaultRepositorySystemSession(repositorySession);

            session.setDependencyGraphTransformer(dependencyCollectorRequest.getDependencyGraphTransformer());

            session.setDependencySelector(dependencyCollectorRequest.getDependencySelector());

            for (Map.Entry<String, Object> entry :
                    dependencyCollectorRequest.getConfigProperties().entrySet()) {
                session.setConfigProperty(entry.getKey(), entry.getValue());
            }

            org.eclipse.aether.artifact.Artifact aetherArtifact = RepositoryUtils.toArtifact(projectArtifact);

            List<org.eclipse.aether.repository.RemoteRepository> aetherRepos =
                    RepositoryUtils.toRepos(remoteArtifactRepositories);

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRootArtifact(aetherArtifact);
            collectRequest.setRepositories(aetherRepos);

            org.eclipse.aether.artifact.ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();
            collectDependencyList(collectRequest, project, stereotypes);
            collectManagedDependencyList(collectRequest, project, stereotypes);

            CollectResult collectResult = repositorySystem.collectDependencies(session, collectRequest);

            org.eclipse.aether.graph.DependencyNode rootNode = collectResult.getRoot();

            if (LOGGER.isDebugEnabled()) {
                logTree(rootNode);
            }

            return buildDependencyNode(null, rootNode, projectArtifact, dependencyCollectorRequest.getFilter());
        } catch (DependencyCollectionException e) {
            throw new DependencyCollectorBuilderException("Could not collect dependencies: " + e.getResult(), e);
        } finally {
            if (session != null) {
                session.setReadOnly();
            }
        }
    }

    private void logTree(org.eclipse.aether.graph.DependencyNode rootNode) {
        // print the node tree with its associated data Map
        rootNode.accept(new TreeDependencyVisitor(new DependencyVisitor() {
            String indent = "";

            @Override
            public boolean visitEnter(org.eclipse.aether.graph.DependencyNode dependencyNode) {
                LOGGER.debug("{}Aether node: {} data map: {}", indent, dependencyNode, dependencyNode.getData());
                indent += "    ";
                return true;
            }

            @Override
            public boolean visitLeave(org.eclipse.aether.graph.DependencyNode dependencyNode) {
                indent = indent.substring(0, indent.length() - 4);
                return true;
            }
        }));
    }

    private void collectManagedDependencyList(
            CollectRequest collectRequest, MavenProject project, ArtifactTypeRegistry stereotypes) {
        if (project.getDependencyManagement() != null) {
            for (Dependency dependency : project.getDependencyManagement().getDependencies()) {
                org.eclipse.aether.graph.Dependency aetherDep = RepositoryUtils.toDependency(dependency, stereotypes);
                collectRequest.addManagedDependency(aetherDep);
            }
        }
    }

    private void collectDependencyList(
            CollectRequest collectRequest,
            MavenProject project,
            org.eclipse.aether.artifact.ArtifactTypeRegistry stereotypes) {
        for (Dependency dependency : project.getDependencies()) {
            org.eclipse.aether.graph.Dependency aetherDep = RepositoryUtils.toDependency(dependency, stereotypes);
            collectRequest.addDependency(aetherDep);
        }
    }

    private Artifact getDependencyArtifact(org.eclipse.aether.graph.Dependency dep) {
        org.eclipse.aether.artifact.Artifact artifact = dep.getArtifact();

        Artifact mavenArtifact = RepositoryUtils.toArtifact(artifact);
        mavenArtifact.setScope(dep.getScope());
        mavenArtifact.setOptional(dep.isOptional());

        return mavenArtifact;
    }

    private DependencyNode buildDependencyNode(
            DependencyNode parent,
            org.eclipse.aether.graph.DependencyNode node,
            Artifact artifact,
            ArtifactFilter filter) {
        String premanagedVersion = DependencyManagerUtils.getPremanagedVersion(node);
        String premanagedScope = DependencyManagerUtils.getPremanagedScope(node);

        Boolean optional = null;
        if (node.getDependency() != null) {
            optional = node.getDependency().isOptional();
        }

        List<org.apache.maven.model.Exclusion> exclusions = null;
        if (node.getDependency() != null) {
            exclusions = new ArrayList<>(node.getDependency().getExclusions().size());
            for (Exclusion exclusion : node.getDependency().getExclusions()) {
                org.apache.maven.model.Exclusion modelExclusion = new org.apache.maven.model.Exclusion();
                modelExclusion.setGroupId(exclusion.getGroupId());
                modelExclusion.setArtifactId(exclusion.getArtifactId());
                exclusions.add(modelExclusion);
            }
        }

        org.eclipse.aether.graph.DependencyNode winner =
                (org.eclipse.aether.graph.DependencyNode) node.getData().get(ConflictResolver.NODE_DATA_WINNER);
        String winnerVersion = null;
        String ignoredScope = null;
        if (winner != null) {
            winnerVersion = winner.getArtifact().getBaseVersion();
        } else {
            ignoredScope = (String) node.getData().get(VerboseJavaScopeSelector.REDUCED_SCOPE);
        }

        ConflictData data = new ConflictData(winnerVersion, ignoredScope);

        VerboseDependencyNode current = new VerboseDependencyNode(
                parent,
                artifact,
                premanagedVersion,
                premanagedScope,
                getVersionSelectedFromRange(node.getVersionConstraint()),
                optional,
                exclusions,
                data);

        List<DependencyNode> nodes = new ArrayList<>(node.getChildren().size());
        for (org.eclipse.aether.graph.DependencyNode child : node.getChildren()) {
            Artifact childArtifact = getDependencyArtifact(child.getDependency());

            if ((filter == null) || filter.include(childArtifact)) {
                nodes.add(buildDependencyNode(current, child, childArtifact, filter));
            }
        }

        current.setChildren(Collections.unmodifiableList(nodes));

        return current;
    }

    private String getVersionSelectedFromRange(VersionConstraint constraint) {
        if ((constraint == null) || (constraint.getVersion() != null)) {
            return null;
        }

        return constraint.getRange().toString();
    }
}
