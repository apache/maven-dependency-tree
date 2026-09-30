package org.apache.maven.its.deptree;

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

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.api.Project;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.internal.DefaultDependencyGraphBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Resolves all the dependencies in the project immediately after the Project has been read.
 *
 * It needs to resolve dependencies from the reactor because in a multi-module build
 * sibling modules may never have been built yet, so will not exist in any repo.
 *
 * It is crucial that reactor dependencies can be found at this point in the build because
 * this is the only time at which we can modify the classpath. And for modules that produce
 * archives (eg Android AAR) which contain the actual Java Jar dependency, we need to know
 * that they exist so that we can add a placeholder for them onto the classpath,
 * which we can replace with the real classes once they are built.
 */
@Named
@Singleton
public final class ResolveDependenciesLifecycleParticipant extends AbstractMavenLifecycleParticipant
{
    // the Maven 4 DI index of the library is not read by a core extension, so create the (stateless) builder directly
    private final DependencyGraphBuilder dependencyGraphBuilder = new DefaultDependencyGraphBuilder();

    private final Logger log = LoggerFactory.getLogger( ResolveDependenciesLifecycleParticipant.class );

    @Override
    public void afterProjectsRead( MavenSession session ) throws MavenExecutionException
    {
        log.info( "" );
        log.info( "ResolveDependenciesLifecycleParticipant#afterProjectsRead" );

        final List<Project> projects = session.getSession().getProjects();
        File basedir = new File( session.getExecutionRootDirectory() );

        for ( Project project : projects )
        {
            log.info( "building dependency graph for project " + project.getPomArtifact() );

            File resolved = new File( basedir, "resolved-" + project.getArtifactId() + ".txt" );

            try
            {
                log.info( "building without reactor projects" );
                // resolution without reactor projects, to check that it is not possible at this point
                dependencyGraphBuilder.buildDependencyGraph( session.getSession(), project, null );
            }
            catch ( DependencyGraphBuilderException e )
            {
                log.info( "unexpected resolution failure: " + e.getMessage() );

                try
                {
                    Files.writeString( resolved.toPath(), e.getMessage() );
                }
                catch ( IOException ioe )
                {
                    throw new MavenExecutionException( "Could not write " + resolved, ioe );
                }
            }
        }

        log.info( "" );
    }
}
  
