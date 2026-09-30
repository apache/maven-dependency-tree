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

import java.util.function.Predicate;

import org.apache.maven.api.Dependency;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;

/**
 * Maven project dependency graph builder API, on top of the Maven 4 {@code DependencyResolver} service.
 *
 * @author Hervé Boutemy
 * @since 2.0
 */
public interface DependencyGraphBuilder {
    /**
     * Build the dependency graph.
     *
     * @param session the Maven session
     * @param project the project to process the dependencies of
     * @param filter dependency filter (can be <code>null</code>)
     * @return the dependency graph
     * @throws DependencyGraphBuilderException if some of the dependencies could not be resolved.
     */
    DependencyNode buildDependencyGraph(Session session, Project project, Predicate<Dependency> filter)
            throws DependencyGraphBuilderException;
}
