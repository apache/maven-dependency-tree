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

import static org.eclipse.aether.util.graph.manager.DependencyManagerUtils.NODE_DATA_PREMANAGED_VERSION;

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
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.version.VersionConstraint;

/**
 * Wrapper around Eclipse Aether dependency resolver, used in Maven 3.1.
 *
 * @see ProjectDependenciesResolver
 * @author Hervé Boutemy
 * @since 2.1
 */
@Component( role = DependencyGraphBuilder.class, hint = "maven31" )
public class Maven31DependencyGraphBuilder
    extends AbstractLogEnabled
    implements DependencyGraphBuilder
{
    @Requirement
    private ProjectDependenciesResolver resolver;

    private final ExceptionHandler<DependencyGraphBuilderException> exceptionHandler;
    
    public Maven31DependencyGraphBuilder()
    {
        this.exceptionHandler = DependencyGraphBuilderException::new;
    }

    /**
     * Builds the dependency graph for Maven 3.1+.
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

        RepositorySystemSession session =
            (RepositorySystemSession) Invoker.invoke( buildingRequest, "getRepositorySession", exceptionHandler );

        
        if ( Boolean.TRUE != session.getConfigProperties().get( NODE_DATA_PREMANAGED_VERSION ) )
        {
            DefaultRepositorySystemSession newSession = new DefaultRepositorySystemSession( session );
            newSession.setConfigProperty( NODE_DATA_PREMANAGED_VERSION, true );
            session = newSession;
        }         

        final DependencyResolutionRequest request = new DefaultDependencyResolutionRequest();
        request.setMavenProject( project );
        Invoker.invoke( request, "setRepositorySession", RepositorySystemSession.class, session );
        // only download the poms, not the artifacts
        DependencyFilter collectFilter = ( node, parents ) -> false;
        Invoker.invoke( request, "setResolutionFilter", DependencyFilter.class, collectFilter );

        final DependencyResolutionResult result = resolveDependencies( request );
        org.eclipse.aether.graph.DependencyNode graph =
            (org.eclipse.aether.graph.DependencyNode) Invoker.invoke( DependencyResolutionResult.class, result,
                                                                      "getDependencyGraph", exceptionHandler );

        return buildDependencyNode( null, graph, project.getArtifact(), filter );
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
        org.eclipse.aether.artifact.Artifact artifact = dep.getArtifact();

        try
        {
            Artifact mavenArtifact = (Artifact) Invoker.invoke( RepositoryUtils.class, "toArtifact",
                                              org.eclipse.aether.artifact.Artifact.class, artifact, exceptionHandler );

            mavenArtifact.setScope( dep.getScope() );
            mavenArtifact.setOptional( dep.isOptional() );

            return mavenArtifact;
        }
        catch ( DependencyGraphBuilderException e )
        {
            // ReflectionException should not happen
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    private DependencyNode buildDependencyNode( DependencyNode parent, org.eclipse.aether.graph.DependencyNode node,
                                                Artifact artifact, ArtifactFilter filter )
    {
        String premanagedVersion = DependencyManagerUtils.getPremanagedVersion( node );
        String premanagedScope = DependencyManagerUtils.getPremanagedScope( node );

        List<org.apache.maven.model.Exclusion> exclusions = null;
        Boolean optional = null;
        if ( node.getDependency() != null )
        {
            exclusions = new ArrayList<>( node.getDependency().getExclusions().size() );
            for ( Exclusion exclusion : node.getDependency().getExclusions() )
            {
                org.apache.maven.model.Exclusion modelExclusion = new org.apache.maven.model.Exclusion();
                modelExclusion.setGroupId( exclusion.getGroupId() );
                modelExclusion.setArtifactId( exclusion.getArtifactId() );
                exclusions.add( modelExclusion );
            }
        }

        DefaultDependencyNode current =
            new DefaultDependencyNode( parent, artifact, premanagedVersion, premanagedScope,
                                       getVersionSelectedFromRange( node.getVersionConstraint() ),
                                       optional, exclusions );

        List<DependencyNode> nodes = new ArrayList<>( node.getChildren().size() );
        for ( org.eclipse.aether.graph.DependencyNode child : node.getChildren() )
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

        return constraint.getRange().toString();
    }
}
