package org.apache.maven.shared.dependency.graph.internal.maven30;

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

import org.sonatype.aether.collection.DependencyCollectionContext;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.graph.Dependency;

/**
 * A dependency selector that excludes dependencies of an specific Scope which occur beyond level one of the dependency
 * graph.
 * 
 * @see {@link Dependency#getScope()}
 * @author Gabriel Belingueres
 * @since 3.1.0
 */
public class Maven3DirectScopeDependencySelector
    implements DependencySelector
{

    private final String scope;

    private final int depth;

    public Maven3DirectScopeDependencySelector( String scope )
    {
        this( scope, 0 );
    }

    private Maven3DirectScopeDependencySelector( String scope, int depth )
    {
        if ( scope == null )
        {
            throw new IllegalArgumentException( "scope is null!" );
        }
        this.scope = scope;
        this.depth = depth;
    }

    /**
     * Decides whether the specified dependency should be included in the dependency graph.
     * 
     * @param dependency The dependency to check, must not be {@code null}.
     * @return {@code false} if the dependency should be excluded from the children of the current node, {@code true}
     *         otherwise.
     */
    @Override
    public boolean selectDependency( Dependency dependency )
    {
        return depth < 2 || !scope.equals( dependency.getScope() );
    }

    /**
     * Derives a dependency selector for the specified collection context. When calculating the child selector,
     * implementors are strongly advised to simply return the current instance if nothing changed to help save memory.
     * 
     * @param context The dependency collection context, must not be {@code null}.
     * @return The dependency selector for the target node, must not be {@code null}.
     */
    @Override
    public DependencySelector deriveChildSelector( DependencyCollectionContext context )
    {
        if ( depth >= 2 )
        {
            return this;
        }

        return new Maven3DirectScopeDependencySelector( scope, depth + 1 );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + depth;
        result = prime * result + ( ( scope == null ) ? 0 : scope.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        Maven3DirectScopeDependencySelector other = (Maven3DirectScopeDependencySelector) obj;
        if ( depth != other.depth )
        {
            return false;
        }
        if ( scope == null )
        {
            if ( other.scope != null )
            {
                return false;
            }
        }
        else if ( !scope.equals( other.scope ) )
        {
            return false;
        }
        return true;
    }

}
