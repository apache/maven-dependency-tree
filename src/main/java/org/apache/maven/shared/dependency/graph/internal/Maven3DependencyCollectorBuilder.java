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
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.ArtifactTypeRegistry;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.graph.CloningDependencyVisitor;
import org.sonatype.aether.util.graph.TreeDependencyVisitor;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector;
import org.sonatype.aether.util.graph.selector.OptionalDependencySelector;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;
import org.sonatype.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.sonatype.aether.util.graph.transformer.ConflictMarker;
import org.sonatype.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.sonatype.aether.util.graph.transformer.JavaEffectiveScopeCalculator;
import org.sonatype.aether.version.VersionConstraint;

/**
 * Project dependency raw dependency collector API, abstracting Maven 3's Aether implementation.
 * 
 * @author Gabriel Belingueres
 * @since 3.0.2
 */
@Component( role = DependencyCollectorBuilder.class, hint = "maven3" )
public class Maven3DependencyCollectorBuilder
    extends AbstractLogEnabled
    implements DependencyCollectorBuilder
{
    @Requirement
    private RepositorySystem repositorySystem;

    @Override
    public DependencyNode collectDependencyGraph( ArtifactRepository localRepository,
                                                ProjectBuildingRequest buildingRequest, ArtifactFilter filter )
        throws DependencyGraphBuilderException
    {
        ClassLoader prevClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            MavenProject project = buildingRequest.getProject();

            Artifact projectArtifact = project.getArtifact();
            List<ArtifactRepository> remoteArtifactRepositories = project.getRemoteArtifactRepositories();

            // throws ClassCastException (classloading issues?)
            // DefaultRepositorySystemSession repositorySystemSession =
            // (DefaultRepositorySystemSession) Invoker.invoke( buildingRequest, "getRepositorySession" );
            RepositorySystemSession repositorySystemSession = buildingRequest.getRepositorySession();

            DefaultRepositorySystemSession session = new DefaultRepositorySystemSession( repositorySystemSession );

            DependencyGraphTransformer transformer =
                new ChainedDependencyGraphTransformer( new ConflictMarker(), new JavaEffectiveScopeCalculator(),
                                                       new JavaDependencyContextRefiner() );
            session.setDependencyGraphTransformer( transformer );

            DependencySelector depFilter =
                new AndDependencySelector( new ScopeDependencySelector(), new OptionalDependencySelector(),
                                           new ExclusionDependencySelector() );
            session.setDependencySelector( depFilter );

            org.sonatype.aether.artifact.Artifact aetherArtifact =
                (org.sonatype.aether.artifact.Artifact) Invoker.invoke( RepositoryUtils.class, "toArtifact",
                                                                       Artifact.class, projectArtifact );

            @SuppressWarnings( "unchecked" )
            List<org.sonatype.aether.repository.RemoteRepository> aetherRepos =
                (List<org.sonatype.aether.repository.RemoteRepository>) Invoker.invoke( RepositoryUtils.class,
                                                                                        "toRepos", List.class,
                                                                                        remoteArtifactRepositories );

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot( new org.sonatype.aether.graph.Dependency( aetherArtifact, "" ) );
            collectRequest.setRepositories( aetherRepos );

            org.sonatype.aether.artifact.ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();
            collectDependencyList( collectRequest, project, stereotypes );
            collectManagedDependencyList( collectRequest, project, stereotypes );

            CollectResult collectResult = repositorySystem.collectDependencies( session, collectRequest );

            org.sonatype.aether.graph.DependencyNode rootNode = collectResult.getRoot();

            CloningDependencyVisitor cloner = new CloningDependencyVisitor();
            TreeDependencyVisitor treeVisitor = new TreeDependencyVisitor( cloner );
            rootNode.accept( treeVisitor );

            rootNode = cloner.getRootNode();

            if ( getLogger().isDebugEnabled() )
            {
                logTree( rootNode );
            }

            return buildDependencyNode( null, rootNode, projectArtifact, filter );
        }
        catch ( DependencyCollectionException e )
        {
            throw new DependencyGraphBuilderException( "Could not collect dependencies: " + e.getResult(), e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( prevClassLoader );
        }
    }

    private void logTree( org.sonatype.aether.graph.DependencyNode rootNode )
    {
        // print the node tree with its associated data Map
        rootNode.accept( new TreeDependencyVisitor( new DependencyVisitor()
        {
            String indent = "";

            @Override
            public boolean visitEnter( org.sonatype.aether.graph.DependencyNode dependencyNode )
            {
                StringBuilder sb = new StringBuilder();
                sb.append( indent ).append( "Aether node: " ).append( dependencyNode );
                if ( !dependencyNode.getData().isEmpty() )
                {
                    sb.append( "data map: " ).append( dependencyNode.getData() );
                }
                if ( dependencyNode.getPremanagedVersion() != null && !dependencyNode.getPremanagedVersion().isEmpty() )
                {
                    sb.append( "Premanaged.version: " ).append( dependencyNode.getPremanagedVersion() );
                }
                if ( dependencyNode.getPremanagedScope() != null && !dependencyNode.getPremanagedScope().isEmpty() )
                {
                    sb.append( "Premanaged.scope: " ).append( dependencyNode.getPremanagedScope() );
                }
                getLogger().debug( sb.toString() );
                indent += "    ";
                return true;
            }

            @Override
            public boolean visitLeave( org.sonatype.aether.graph.DependencyNode dependencyNode )
            {
                indent = indent.substring( 0, indent.length() - 4 );
                return true;
            }
        } ) );
    }

    private void collectManagedDependencyList( CollectRequest collectRequest, MavenProject project,
                                               ArtifactTypeRegistry stereotypes )
        throws DependencyGraphBuilderException
    {
        if ( project.getDependencyManagement() != null )
        {
            for ( Dependency dependency : project.getDependencyManagement().getDependencies() )
            {
                org.sonatype.aether.graph.Dependency aetherDep = toAetherDependency( stereotypes, dependency );
                collectRequest.addManagedDependency( aetherDep );
            }
        }
    }

    private void collectDependencyList( CollectRequest collectRequest, MavenProject project,
                                        org.sonatype.aether.artifact.ArtifactTypeRegistry stereotypes )
        throws DependencyGraphBuilderException
    {
        for ( Dependency dependency : project.getDependencies() )
        {
            org.sonatype.aether.graph.Dependency aetherDep = toAetherDependency( stereotypes, dependency );
            collectRequest.addDependency( aetherDep );
        }
    }

    // CHECKSTYLE_OFF: LineLength
    private org.sonatype.aether.graph.Dependency toAetherDependency( org.sonatype.aether.artifact.ArtifactTypeRegistry stereotypes,
                                                                    Dependency dependency )
        throws DependencyGraphBuilderException
    {
        org.sonatype.aether.graph.Dependency aetherDep =
            (org.sonatype.aether.graph.Dependency) Invoker.invoke( RepositoryUtils.class, "toDependency",
                                                                  Dependency.class,
                                                                   org.sonatype.aether.artifact.ArtifactTypeRegistry.class,
                                                                  dependency, stereotypes );
        return aetherDep;
    }
    // CHECKSTYLE_ON: LineLength

    private Artifact getDependencyArtifact( org.sonatype.aether.graph.Dependency dep )
    {
        org.sonatype.aether.artifact.Artifact artifact = dep.getArtifact();

        try
        {
            Artifact mavenArtifact = (Artifact) Invoker.invoke( RepositoryUtils.class, "toArtifact",
                                                                org.sonatype.aether.artifact.Artifact.class, artifact );

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

    private DependencyNode buildDependencyNode( DependencyNode parent, org.sonatype.aether.graph.DependencyNode node,
                                                Artifact artifact, ArtifactFilter filter )
    {
        String premanagedVersion = node.getPremanagedVersion();
        String premanagedScope = node.getPremanagedScope();

        Boolean optional = null;
        if ( node.getDependency() != null )
        {
            optional = node.getDependency().isOptional();
        }

        DefaultDependencyNode current =
            new DefaultDependencyNode( parent, artifact, premanagedVersion, premanagedScope,
                                       getVersionSelectedFromRange( node.getVersionConstraint() ), optional );

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
        if ( ( constraint == null ) || ( constraint.getVersion() != null ) || ( constraint.getRanges().isEmpty() ) )
        {
            return null;
        }

        return constraint.getRanges().iterator().next().toString();
    }

}
