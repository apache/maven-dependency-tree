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

import org.apache.maven.api.PathScope;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverException;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilder;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyCollectorRequest;
import org.apache.maven.shared.dependency.graph.DependencyNode;

/**
 * Project dependency raw dependency collector API, using the verbose mode of the Maven 4 {@link DependencyResolver}:
 * the returned tree keeps the nodes omitted for duplicate or conflict.
 *
 * @author Gabriel Belingueres
 * @since 3.1.0
 */
@Named
@Singleton
public class DefaultDependencyCollectorBuilder implements DependencyCollectorBuilder {

    @Override
    public DependencyNode collectDependencyGraph(DependencyCollectorRequest request)
            throws DependencyCollectorBuilderException {
        DependencyResolverResult result;
        try {
            result = request.getSession()
                    .getService(DependencyResolver.class)
                    .collect(DependencyResolverRequest.builder()
                            .session(request.getSession())
                            .requestType(DependencyResolverRequest.RequestType.COLLECT)
                            .project(request.getProject())
                            .pathScope(PathScope.TEST_RUNTIME)
                            .verbose(true)
                            .build());
        } catch (DependencyResolverException e) {
            throw new DependencyCollectorBuilderException("Could not collect dependencies: " + e.getMessage(), e);
        }

        return new DependencyNodeConverter(request.getFilter(), true)
                .convertRoot(result.getRoot(), request.getProject());
    }
}
