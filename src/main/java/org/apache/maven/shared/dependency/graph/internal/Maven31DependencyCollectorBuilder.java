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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilder;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.internal.maven31.Maven31DirectScopeDependencySelector;
import org.apache.maven.shared.dependency.graph.internal.maven31.VerboseJavaScopeSelector;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;
import org.eclipse.aether.version.VersionConstraint;

/**
 * Project dependency raw dependency collector API, abstracting Maven 3.1+'s Aether implementation.
 * 
 * @author Gabriel Belingueres
 * @since 3.1.0
 */
@Component( role = DependencyCollectorBuilder.class, hint = "maven31" )
public class Maven31DependencyCollectorBuilder
    extends AbstractLogEnabled
    implements DependencyCollectorBuilder
{
    @Requirement
    private RepositorySystem repositorySystem;
    
    private final ExceptionHandler<DependencyCollectorBuilderException> exceptionHandler;
    
    public Maven31DependencyCollectorBuilder()
    {
        this.exceptionHandler = new ExceptionHandler<DependencyCollectorBuilderException>()
        {
            @Override
            public DependencyCollectorBuilderException create( String message, Exception exception )
            {
                return new DependencyCollectorBuilderException( message, exception );
            }
        };
    }

    @Override
    public DependencyNode collectDependencyGraph( ProjectBuildingRequest buildingRequest, ArtifactFilter filter )
        throws DependencyCollectorBuilderException
    {
        DefaultRepositorySystemSession session = null;
        try
        {
            MavenProject project = buildingRequest.getProject();

            Artifact projectArtifact = project.getArtifact();
            List<ArtifactRepository> remoteArtifactRepositories = project.getRemoteArtifactRepositories();

            DefaultRepositorySystemSession repositorySession =
                (DefaultRepositorySystemSession) Invoker.invoke( buildingRequest, "getRepositorySession",
                                                                 exceptionHandler );

            session = new DefaultRepositorySystemSession( repositorySession );

            DependencyGraphTransformer transformer =
                new ConflictResolver( new NearestVersionSelector(), new VerboseJavaScopeSelector(),
                                      new SimpleOptionalitySelector(), new JavaScopeDeriver() );
            session.setDependencyGraphTransformer( transformer );

            DependencySelector depFilter =
                new AndDependencySelector( new Maven31DirectScopeDependencySelector( JavaScopes.TEST ),
                                           new OptionalDependencySelector(),
                                           new ExclusionDependencySelector() );
            session.setDependencySelector( depFilter );

            session.setConfigProperty( ConflictResolver.CONFIG_PROP_VERBOSE, true );
            session.setConfigProperty( DependencyManagerUtils.CONFIG_PROP_VERBOSE, true );

            org.eclipse.aether.artifact.Artifact aetherArtifact =
                (org.eclipse.aether.artifact.Artifact) Invoker.invoke( RepositoryUtils.class, "toArtifact",
                                                                       Artifact.class, projectArtifact,
                                                                       exceptionHandler );

            @SuppressWarnings( "unchecked" )
            List<org.eclipse.aether.repository.RemoteRepository> aetherRepos =
                (List<org.eclipse.aether.repository.RemoteRepository>) Invoker.invoke( RepositoryUtils.class, "toRepos",
                                                                                       List.class,
                                                                                       remoteArtifactRepositories,
                                                                                       exceptionHandler );

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRootArtifact( aetherArtifact );
            collectRequest.setRepositories( aetherRepos );

            org.eclipse.aether.artifact.ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();
            collectDependencyList( collectRequest, project, stereotypes );
            collectManagedDependencyList( collectRequest, project, stereotypes );

            CollectResult collectResult = repositorySystem.collectDependencies( session, collectRequest );

            org.eclipse.aether.graph.DependencyNode rootNode = collectResult.getRoot();

            if ( getLogger().isDebugEnabled() )
            {
                logTree( rootNode );
            }

            return buildDependencyNode( null, rootNode, projectArtifact, filter );
        }
        catch ( DependencyCollectionException e )
        {
            throw new DependencyCollectorBuilderException( "Could not collect dependencies: " + e.getResult(), e );
        }
        finally
        {
            if ( session != null )
            {
                session.setReadOnly();
            }
        }
    }

    private void logTree( org.eclipse.aether.graph.DependencyNode rootNode )
    {
        // print the node tree with its associated data Map
        rootNode.accept( new TreeDependencyVisitor( new DependencyVisitor()
        {
            String indent = "";

            @Override
            public boolean visitEnter( org.eclipse.aether.graph.DependencyNode dependencyNode )
            {
                getLogger().debug( indent + "Aether node: " + dependencyNode + " data map: "
                    + dependencyNode.getData() );
                indent += "    ";
                return true;
            }

            @Override
            public boolean visitLeave( org.eclipse.aether.graph.DependencyNode dependencyNode )
            {
                indent = indent.substring( 0, indent.length() - 4 );
                return true;
            }
        } ) );
    }

    private void collectManagedDependencyList( CollectRequest collectRequest, MavenProject project,
                                               ArtifactTypeRegistry stereotypes )
        throws DependencyCollectorBuilderException
    {
        if ( project.getDependencyManagement() != null )
        {
            for ( Dependency dependency : project.getDependencyManagement().getDependencies() )
            {
                org.eclipse.aether.graph.Dependency aetherDep = toAetherDependency( stereotypes, dependency );
                collectRequest.addManagedDependency( aetherDep );
            }
        }
    }

    private void collectDependencyList( CollectRequest collectRequest, MavenProject project,
                                        org.eclipse.aether.artifact.ArtifactTypeRegistry stereotypes )
        throws DependencyCollectorBuilderException
    {
        for ( Dependency dependency : project.getDependencies() )
        {
            org.eclipse.aether.graph.Dependency aetherDep = toAetherDependency( stereotypes, dependency );
            collectRequest.addDependency( aetherDep );
        }
    }

    // CHECKSTYLE_OFF: LineLength
    private org.eclipse.aether.graph.Dependency toAetherDependency( org.eclipse.aether.artifact.ArtifactTypeRegistry stereotypes,
                                                                    Dependency dependency )
        throws DependencyCollectorBuilderException
    {
        org.eclipse.aether.graph.Dependency aetherDep =
            (org.eclipse.aether.graph.Dependency) Invoker.invoke( RepositoryUtils.class, "toDependency",
                                                                  Dependency.class,
                                                                  org.eclipse.aether.artifact.ArtifactTypeRegistry.class,
                                                                  dependency, stereotypes, exceptionHandler );
        return aetherDep;
    }
    // CHECKSTYLE_ON: LineLength

    private Artifact getDependencyArtifact( org.eclipse.aether.graph.Dependency dep )
    {
        org.eclipse.aether.artifact.Artifact artifact = dep.getArtifact();

        try
        {
            Artifact mavenArtifact =
                (Artifact) Invoker.invoke( RepositoryUtils.class, "toArtifact",
                                           org.eclipse.aether.artifact.Artifact.class, artifact, exceptionHandler );

            mavenArtifact.setScope( dep.getScope() );
            mavenArtifact.setOptional( dep.isOptional() );

            return mavenArtifact;
        }
        catch ( DependencyCollectorBuilderException e )
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

        Boolean optional = null;
        if ( node.getDependency() != null )
        {
            optional = node.getDependency().isOptional();
        }
        
        List<org.apache.maven.model.Exclusion> exclusions = null;
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

        org.eclipse.aether.graph.DependencyNode winner =
            (org.eclipse.aether.graph.DependencyNode) node.getData().get( ConflictResolver.NODE_DATA_WINNER );
        String winnerVersion = null;
        String ignoredScope = null;
        if ( winner != null )
        {
            winnerVersion = winner.getArtifact().getBaseVersion();
        }
        else
        {
            ignoredScope = (String) node.getData().get( VerboseJavaScopeSelector.REDUCED_SCOPE );
        }
        
        ConflictData data = new ConflictData( winnerVersion, ignoredScope );
        
        VerboseDependencyNode current =
            new VerboseDependencyNode( parent, artifact, premanagedVersion, premanagedScope,
                                       getVersionSelectedFromRange( node.getVersionConstraint() ), optional,
                                       exclusions, data );

        List<DependencyNode> nodes = new ArrayList<DependencyNode>( node.getChildren().size() );
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
