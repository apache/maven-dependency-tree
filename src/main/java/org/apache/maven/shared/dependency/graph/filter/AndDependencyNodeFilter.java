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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.shared.dependency.graph.DependencyNode;

/**
 * A dependency node filter that logically ANDs together a number of other dependency node filters.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id$
 * @since 1.1
 */
public class AndDependencyNodeFilter implements DependencyNodeFilter {
    // fields -----------------------------------------------------------------

    /**
     * The dependency node filters that this filter ANDs together.
     */
    private final List<DependencyNodeFilter> filters;

    // constructors -----------------------------------------------------------

    /**
     * Creates a dependency node filter that logically ANDs together the two specified dependency node filters.
     *
     * @param filter1 the first dependency node filter to logically AND together
     * @param filter2 the second dependency node filter to logically AND together
     */
    public AndDependencyNodeFilter(DependencyNodeFilter filter1, DependencyNodeFilter filter2) {
        this(Arrays.asList(filter1, filter2));
    }

    /**
     * Creates a dependency node filter that logically ANDs together the specified dependency node filters.
     *
     * @param filters the list of dependency node filters to logically AND together
     */
    public AndDependencyNodeFilter(List<DependencyNodeFilter> filters) {
        this.filters = Collections.unmodifiableList(filters);
    }

    // DependencyNodeFilter methods -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accept(DependencyNode node) {
        for (DependencyNodeFilter filter : filters) {
            if (!filter.accept(node)) {
                return false;
            }
        }

        return true;
    }

    // public methods ---------------------------------------------------------

    /**
     * Gets the list of dependency node filters that this filter ANDs together.
     *
     * @return the dependency node filters that this filter ANDs together
     */
    public List<DependencyNodeFilter> getDependencyNodeFilters() {
        return filters;
    }
}
