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

import java.util.Objects;
import java.util.function.Predicate;

import org.apache.maven.api.Dependency;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;

/**
 * This class carries the options of
 * {@link DependencyCollectorBuilder#collectDependencyGraph(DependencyCollectorRequest)}.
 * <p>
 * Unlike the Maven 3 based version, it can no longer carry a Resolver {@code DependencySelector},
 * {@code DependencyGraphTransformer} or configuration properties: the Maven 4 {@code DependencyResolverRequest}
 * only knows a {@code verbose} switch, and the collection is done with the selectors and transformers of the session.
 * </p>
 *
 * @since 3.2.1
 */
public class DependencyCollectorRequest {

    private final Session session;

    private final Project project;

    private final Predicate<Dependency> filter;

    public DependencyCollectorRequest(Session session, Project project) {
        this(session, project, null);
    }

    public DependencyCollectorRequest(Session session, Project project, Predicate<Dependency> filter) {
        this.session = Objects.requireNonNull(session, "session cannot be null");
        this.project = Objects.requireNonNull(project, "project cannot be null");
        this.filter = filter;
    }

    public Session getSession() {
        return session;
    }

    public Project getProject() {
        return project;
    }

    public Predicate<Dependency> getFilter() {
        return filter;
    }
}
