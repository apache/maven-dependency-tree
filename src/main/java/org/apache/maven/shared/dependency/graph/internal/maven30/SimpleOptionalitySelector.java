package org.apache.maven.shared.dependency.graph.internal.maven30;

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

import java.util.Collection;

import org.apache.maven.shared.dependency.graph.internal.maven30.ConflictResolver.ConflictContext;
import org.apache.maven.shared.dependency.graph.internal.maven30.ConflictResolver.ConflictItem;
import org.apache.maven.shared.dependency.graph.internal.maven30.ConflictResolver.OptionalitySelector;
import org.sonatype.aether.RepositoryException;

/**
 * This class is a copy of their homonymous in the Eclipse Aether library, adapted to work with Sonatype Aether.
 * 
 * @author Gabriel Belingueres
 * @since 3.0.2
 */
public class SimpleOptionalitySelector
    extends OptionalitySelector
{

    @Override
    public void selectOptionality( ConflictContext context )
        throws RepositoryException
    {
        boolean optional = chooseEffectiveOptionality( context.getItems() );
        context.setOptional( optional );
    }

    private boolean chooseEffectiveOptionality( Collection<ConflictItem> items )
    {
        boolean optional = true;
        for ( ConflictItem item : items )
        {
            if ( item.getDepth() <= 1 )
            {
                return item.getDependency().isOptional();
            }
            if ( ( item.getOptionalities() & ConflictItem.OPTIONAL_FALSE ) != 0 )
            {
                optional = false;
            }
        }
        return optional;
    }

}