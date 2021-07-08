package org.apache.maven.shared.dependency.graph;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.project.ProjectBuildingRequest;

/**
 * Maven project dependency raw dependency collector API, providing an abstraction layer against Maven 3 and Maven 3.1+
 * particular Aether implementations.
 * 
 * @author Gabriel Belingueres
 * @since 3.1.0
 */
public interface DependencyCollectorBuilder
{

    /**
     * collect the project's raw dependency graph, with information to allow the API client to reason on its own about
     * dependencies.
     * 
     * @param buildingRequest the request with the project to process its dependencies.
     * @param filter an artifact filter if not all dependencies are required (can be <code>null</code>)
     * @return the raw dependency tree
     * @throws DependencyCollectorBuilderException if some of the dependencies could not be collected.
     */
    DependencyNode collectDependencyGraph( ProjectBuildingRequest buildingRequest, ArtifactFilter filter )
        throws DependencyCollectorBuilderException;

}
