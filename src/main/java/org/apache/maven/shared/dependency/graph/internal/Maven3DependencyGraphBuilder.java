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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.version.VersionConstraint;

/**
 * Wrapper around Maven 3 dependency resolver.
 *
 * @see ProjectDependenciesResolver
 * @author Hervé Boutemy
 * @since 2.0
 */
@Component( role = DependencyGraphBuilder.class, hint = "maven3" )
public class Maven3DependencyGraphBuilder
    extends AbstractLogEnabled
    implements DependencyGraphBuilder
{
    @Requirement
    private ProjectDependenciesResolver resolver;

    /**
     * Builds the dependency graph for Maven 3.
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
        MavenProject project = buildingRequest.getProject();

        DependencyResolutionRequest request =
            new DefaultDependencyResolutionRequest( project, buildingRequest.getRepositorySession() );

        // only download the poms, not the artifacts
        DependencyFilter collectFilter = new DependencyFilter()
        {
            @Override
            public boolean accept( org.sonatype.aether.graph.DependencyNode node,
                                   List<org.sonatype.aether.graph.DependencyNode> parents )
            {
                return false;
            }
        };
        request.setResolutionFilter( collectFilter );

        DependencyResolutionResult result = resolveDependencies( request );

        return buildDependencyNode( null, result.getDependencyGraph(), project.getArtifact(), filter );
    }

    private DependencyResolutionResult resolveDependencies( DependencyResolutionRequest request )
        throws DependencyGraphBuilderException
    {
        try
        {
            return resolver.resolve( request );
        }
        catch ( DependencyResolutionException e )
        {
            throw new DependencyGraphBuilderException( "Could not resolve following dependencies: "
                + e.getResult().getUnresolvedDependencies(), e );
        }
    }

    private Artifact getDependencyArtifact( Dependency dep )
    {
        Artifact mavenArtifact = RepositoryUtils.toArtifact( dep.getArtifact() );

        mavenArtifact.setScope( dep.getScope() );
        mavenArtifact.setOptional( dep.isOptional() );

        return mavenArtifact;
    }

    private DependencyNode buildDependencyNode( DependencyNode parent, org.sonatype.aether.graph.DependencyNode node,
                                                Artifact artifact, ArtifactFilter filter )
    {
        DefaultDependencyNode current =
            new DefaultDependencyNode( parent, artifact,
                                       null /* node.getPremanagedVersion() */,
                                       null /* node.getPremanagedScope() */,
                                       getVersionSelectedFromRange( node.getVersionConstraint() ) );

        List<DependencyNode> nodes = new ArrayList<DependencyNode>( node.getChildren().size() );
        for ( org.sonatype.aether.graph.DependencyNode child : node.getChildren() )
        {
            Artifact childArtifact = getDependencyArtifact( child.getDependency() );

            if ( ( filter == null ) || filter.include( childArtifact ) )
            {
                nodes.add( buildDependencyNode( current, child, childArtifact, filter ) );
            }
        }

        current.setChildren( Collections.unmodifiableList( nodes ) );

        return current;
    }

    private String getVersionSelectedFromRange( VersionConstraint constraint )
    {
        if ( ( constraint == null ) || ( constraint.getVersion() != null ) )
        {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for ( org.sonatype.aether.version.VersionRange range : constraint.getRanges() )
        {
            if ( sb.length() > 0 )
            {
                sb.append( ',' );
            }
            sb.append( range );
        }

        return sb.toString();
    }
}
