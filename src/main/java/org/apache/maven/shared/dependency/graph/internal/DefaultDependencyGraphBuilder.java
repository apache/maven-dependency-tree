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

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * Default dependency graph builder that detects current Maven version to delegate to either Maven 3.0 or 3.1+ specific
 * code.
 *
 * @see Maven3DependencyGraphBuilder
 * @see Maven31DependencyGraphBuilder
 * @author Hervé Boutemy
 * @since 2.0
 */
@Component( role = DependencyGraphBuilder.class )
public class DefaultDependencyGraphBuilder
    extends AbstractLogEnabled
    implements DependencyGraphBuilder, Contextualizable
{
    protected PlexusContainer container;

    /**
     * Builds a dependency graph.
     *
     * @param buildingRequest the buildingRequest
     * @param filter artifact filter (can be <code>null</code>)
     * @return DependencyNode containing the dependency graph.
     * @throws DependencyGraphBuilderException if some of the dependencies could not be resolved.
     */
    @Override
    public DependencyNode buildDependencyGraph( ProjectBuildingRequest buildingRequest, ArtifactFilter filter )
        throws DependencyGraphBuilderException
    {
        try
        {
            String hint = isMaven31() ? "maven31" : "maven3";

            DependencyGraphBuilder effectiveGraphBuilder =
                (DependencyGraphBuilder) container.lookup( DependencyGraphBuilder.class.getCanonicalName(), hint );
            
            if ( getLogger().isDebugEnabled() )
            {
                MavenProject project = buildingRequest.getProject();
                
                getLogger().debug( "building " + hint + " dependency graph for " + project.getId() + " with "
                                + effectiveGraphBuilder.getClass().getSimpleName() );
            }

            return effectiveGraphBuilder.buildDependencyGraph( buildingRequest, filter );
        }
        catch ( ComponentLookupException e )
        {
            throw new DependencyGraphBuilderException( e.getMessage(), e );
        }
    }

    /**
     * @return true if the current Maven version is Maven 3.1.
     */
    protected static boolean isMaven31()
    {
        try
        {
            Class<?> repoSessionClass =  MavenSession.class.getMethod( "getRepositorySession" ).getReturnType();
            
            return "org.eclipse.aether.RepositorySystemSession".equals( repoSessionClass.getName() );
        }
        catch ( NoSuchMethodException e )
        {
            throw new IllegalStateException( "Cannot determine return type of MavenSession.getRepositorySession" );
        }
    }

    /**
     * Injects the Plexus content.
     *
     * @param context   Plexus context to inject.
     * @throws ContextException if the PlexusContainer could not be located.
     */
    @Override
    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
