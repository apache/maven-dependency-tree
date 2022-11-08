package org.apache.maven.shared.dependency.graph;

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
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.internal.DirectScopeDependencySelector;
import org.apache.maven.shared.dependency.graph.internal.VerboseJavaScopeSelector;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <div>
 * This class will carry various options used by
 * {@link DependencyCollectorBuilder#collectDependencyGraph(DependencyCollectorRequest)}
 * </div>
 * <div>
 * There is a set of default values such:
 * </div>
 * <div>
 * DependencySelector
 * <pre>
 *  new AndDependencySelector(
 *             new DirectScopeDependencySelector( JavaScopes.TEST ),
 *             new DirectScopeDependencySelector( JavaScopes.PROVIDED ),
 *             new OptionalDependencySelector(),
 *             new ExclusionDependencySelector() );
 * </pre>
 * </div>
 * <div>
 * DependencyGraphTransformer
 * <pre>
 * new ConflictResolver(
 *             new NearestVersionSelector(),
 *             new VerboseJavaScopeSelector(),
 *             new SimpleOptionalitySelector(),
 *             new JavaScopeDeriver() );
 * </pre>
 * </div>
 * <div>
 * configProperties have 2 default values
 * <pre>
 *   ConflictResolver.CONFIG_PROP_VERBOSE, true
 *   DependencyManagerUtils.CONFIG_PROP_VERBOSE, true
 * </pre>
 * <a href="https://maven.apache.org/resolver/configuration.html">Move Resolver configuration properties</a>.
 * </div>
 * @since 3.2.1
 */
public class DependencyCollectorRequest
{

    private final ProjectBuildingRequest buildingRequest;

    private ArtifactFilter filter;

    private Map<String, Object> configProperties = new HashMap<>();

    private DependencySelector dependencySelector = new AndDependencySelector(
            new DirectScopeDependencySelector( JavaScopes.TEST ),
            new DirectScopeDependencySelector( JavaScopes.PROVIDED ),
            new OptionalDependencySelector(),
            new ExclusionDependencySelector() );

    private DependencyGraphTransformer dependencyGraphTransformer = new ConflictResolver(
            new NearestVersionSelector(),
            new VerboseJavaScopeSelector(),
            new SimpleOptionalitySelector(),
            new JavaScopeDeriver() );

    public DependencyCollectorRequest( ProjectBuildingRequest buildingRequest )
    {
        this( buildingRequest, null );
    }

    public DependencyCollectorRequest( ProjectBuildingRequest buildingRequest, ArtifactFilter filter )
    {
        Objects.requireNonNull( buildingRequest, "ProjectBuildingRequest cannot be null" );
        this.buildingRequest = buildingRequest;
        this.filter = filter;
        configProperties.put( ConflictResolver.CONFIG_PROP_VERBOSE, true );
        configProperties.put( DependencyManagerUtils.CONFIG_PROP_VERBOSE, true );
    }

    public ProjectBuildingRequest getBuildingRequest()
    {
        return buildingRequest;
    }

    public ArtifactFilter getFilter()
    {
        return filter;
    }

    public DependencySelector getDependencySelector()
    {
        return dependencySelector;
    }

    public DependencyCollectorRequest dependencySelector( DependencySelector dependencySelector )
    {
        this.dependencySelector = dependencySelector;
        return this;
    }

    public DependencyGraphTransformer getDependencyGraphTransformer()
    {
        return dependencyGraphTransformer;
    }

    public DependencyCollectorRequest dependencyGraphTransformer(
            DependencyGraphTransformer dependencyGraphTransformer )
    {
        this.dependencyGraphTransformer = dependencyGraphTransformer;
        return this;
    }

    public Map<String, Object> getConfigProperties()
    {
        return this.configProperties;
    }

    public void addConfigProperty( String key, Object value )
    {
        this.configProperties.put( key, value );
    }

    public void removeConfigProperty( String key )
    {
        this.configProperties.remove( key );
    }
}
