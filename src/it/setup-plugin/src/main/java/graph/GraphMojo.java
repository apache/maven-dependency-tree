package graph;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.SerializingDependencyNodeVisitor;

@Mojo( name = "graph" )
public class GraphMojo
    extends AbstractMojo
{

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;
    
    @Parameter
    private ArtifactFilter artifactFilter;
    
    @Parameter
    private File outputFile;
    
    @Parameter
    private boolean verbose;

    @Component
    private DependencyGraphBuilder graphBuilder;

    @Component
    private DependencyCollectorBuilder collectorBuilder;
    
    @Override
    public void execute() throws MojoExecutionException
    {
        // Code currently assumes project has been set...
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest( session.getProjectBuildingRequest() );
        buildingRequest.setProject( project );
        
        try
        {
            DependencyNode node;
            if ( verbose )
            {
                node = collectorBuilder.collectDependencyGraph( buildingRequest, artifactFilter );
            }
            else
            {
                node = graphBuilder.buildDependencyGraph( buildingRequest, artifactFilter );
            }
            
            if ( outputFile != null )
            {
                outputFile.getParentFile().mkdirs();

                try ( Writer writer = new FileWriter( outputFile ) )
                {
                    node.accept( new SerializingDependencyNodeVisitor( writer,
                                                                       SerializingDependencyNodeVisitor.STANDARD_TOKENS ) );
                }
            }
        }
        catch ( Exception e ) // Catch all is good enough for IT
        {
            throw new MojoExecutionException( "Failed to build dependency graph", e );
        }
    }

    
    
}