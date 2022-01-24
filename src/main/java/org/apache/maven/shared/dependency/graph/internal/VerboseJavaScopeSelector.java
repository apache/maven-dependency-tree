package org.apache.maven.shared.dependency.graph.internal;

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

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ConflictContext;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ScopeSelector;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * A JavaScopeSelector that keeps track of reduced scopes
 * 
 */
public class VerboseJavaScopeSelector extends ScopeSelector
{
    public static final String REDUCED_SCOPE = "REDUCED_SCOPE";
    
    private final ScopeSelector scopeSelector = new JavaScopeSelector();

    @Override
    public void selectScope( ConflictContext context )
        throws RepositoryException
    {
        scopeSelector.selectScope( context );
        
        context.getItems().stream()
            .flatMap( i -> i.getScopes().stream() )
            .distinct()
            .max( new ScopeComparator() )
            .filter( s -> s != context.getScope() )
            .ifPresent( s -> context.getWinner().getNode().setData( REDUCED_SCOPE, s ) );
    }
    
    static class ScopeComparator implements Comparator<String>
    {
        List<String> orderedScopes = Arrays.asList( "compile", "runtime", "provided", "test" );

        @Override
        public int compare( String lhs, String rhs )
        {
            return orderedScopes.indexOf( rhs ) - orderedScopes.indexOf( lhs );
        }
    }    
}
