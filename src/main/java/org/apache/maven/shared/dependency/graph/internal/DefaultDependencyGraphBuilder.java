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

import java.util.function.Predicate;

import org.apache.maven.api.Dependency;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverException;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;

/**
 * Wrapper around the Maven 4 {@link DependencyResolver}: collects the dependencies of the project, without
 * downloading the artifacts.
 *
 * @author Hervé Boutemy
 * @since 2.1
 */
@Named
@Singleton
public class DefaultDependencyGraphBuilder implements DependencyGraphBuilder {

    @Override
    public DependencyNode buildDependencyGraph(Session session, Project project, Predicate<Dependency> filter)
            throws DependencyGraphBuilderException {
        DependencyResolverResult result;
        try {
            result = session.getService(DependencyResolver.class)
                    .collect(DependencyResolverRequest.builder()
                            .session(session)
                            .requestType(DependencyResolverRequest.RequestType.COLLECT)
                            .project(project)
                            .pathScope(PathScope.TEST_RUNTIME)
                            .build());
        } catch (DependencyResolverException e) {
            throw new DependencyGraphBuilderException("Could not resolve following dependencies: " + e.getMessage(), e);
        }

        return new DependencyNodeConverter(filter, false).convertRoot(result.getRoot(), project);
    }
}
