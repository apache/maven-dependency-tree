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
package org.apache.maven.shared.dependency.graph.filter;

import java.util.function.Predicate;

import org.apache.maven.api.Dependency;
import org.apache.maven.shared.dependency.graph.DependencyNode;

/**
 * A dependency node filter that delegates to a dependency predicate. The root node, which has no dependency, is
 * always accepted.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id$
 * @since 1.1
 */
public class ArtifactDependencyNodeFilter implements DependencyNodeFilter {
    // fields -----------------------------------------------------------------

    /**
     * The predicate this dependency node filter delegates to.
     */
    private final Predicate<Dependency> filter;

    // constructors -----------------------------------------------------------

    /**
     * Creates a dependency node filter that delegates to the specified dependency predicate.
     *
     * @param filter the predicate to delegate to
     */
    public ArtifactDependencyNodeFilter(Predicate<Dependency> filter) {
        this.filter = filter;
    }

    // DependencyNodeFilter methods -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accept(DependencyNode node) {
        Dependency dependency = node.getDependency();

        return dependency == null || filter.test(dependency);
    }

    // public methods ---------------------------------------------------------

    /**
     * Gets the predicate this dependency node filter delegates to.
     *
     * @return the predicate this dependency node filter delegates to
     */
    public Predicate<Dependency> getArtifactFilter() {
        return filter;
    }
}
