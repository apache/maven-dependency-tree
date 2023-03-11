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
package org.apache.maven.shared.dependency.graph;

import java.util.Collection;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;

/**
 * Maven project dependency graph builder API, neutral against Maven 2 or Maven 3.
 *
 * @author Hervé Boutemy
 * @since 2.0
 */
public interface DependencyGraphBuilder {
    /**
     * Build the dependency graph.
     *
     * @param buildingRequest the buildingRequest
     * @param filter artifact filter (can be <code>null</code>)
     * @return the dependency graph
     * @throws DependencyGraphBuilderException if some of the dependencies could not be resolved.
     */
    DependencyNode buildDependencyGraph(ProjectBuildingRequest buildingRequest, ArtifactFilter filter)
            throws DependencyGraphBuilderException;

    /**
     *
     * @param buildingRequest the buildingRequest
     * @param filter artifact filter (can be <code>null</code>)
     * @param reactorProjects ignored
     * @return the dependency graph
     * @throws DependencyGraphBuilderException if some of the dependencies could not be resolved.
     * @deprecated Use {@link #buildDependencyGraph(ProjectBuildingRequest, ArtifactFilter)} instead
     */
    @Deprecated
    default DependencyNode buildDependencyGraph(
            ProjectBuildingRequest buildingRequest, ArtifactFilter filter, Collection<MavenProject> reactorProjects)
            throws DependencyGraphBuilderException {
        return buildDependencyGraph(buildingRequest, filter);
    }
}
