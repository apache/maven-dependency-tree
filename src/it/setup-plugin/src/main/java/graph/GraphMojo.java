package graph;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.Mojo;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Parameter;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.SerializingDependencyNodeVisitor;

@org.apache.maven.api.plugin.annotations.Mojo( name = "graph" )
public class GraphMojo
    implements Mojo
{

    @Inject
    private Session session;

    @Inject
    private Project project;

    @Parameter
    private Path outputFile;

    @Parameter
    private boolean verbose;

    @Inject
    private DependencyGraphBuilder graphBuilder;

    @Inject
    private DependencyCollectorBuilder collectorBuilder;

    @Override
    public void execute() throws MojoException
    {
        try
        {
            DependencyNode node;
            if ( verbose )
            {
                node = collectorBuilder.collectDependencyGraph( session, project, null );
            }
            else
            {
                node = graphBuilder.buildDependencyGraph( session, project, null );
            }

            if ( outputFile != null )
            {
                Path output = project.getBasedir().resolve( outputFile );
                Files.createDirectories( output.getParent() );

                try ( java.io.Writer writer = Files.newBufferedWriter( output ) )
                {
                    node.accept( new SerializingDependencyNodeVisitor( writer,
                                                                       SerializingDependencyNodeVisitor.STANDARD_TOKENS ) );
                }
            }
        }
        catch ( Exception e ) // Catch all is good enough for IT
        {
            throw new MojoException( "Failed to build dependency graph", e );
        }
    }

}
