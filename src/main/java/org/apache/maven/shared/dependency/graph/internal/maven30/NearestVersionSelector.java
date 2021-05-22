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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.shared.dependency.graph.internal.maven30.ConflictResolver.ConflictContext;
import org.apache.maven.shared.dependency.graph.internal.maven30.ConflictResolver.ConflictItem;
import org.apache.maven.shared.dependency.graph.internal.maven30.ConflictResolver.VersionSelector;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.collection.UnsolvableVersionConflictException;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.graph.PathRecordingDependencyVisitor;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionConstraint;

/**
 * This class is a copy of their homonymous in the Eclipse Aether library, adapted to work with Sonatype Aether.
 * 
 * @author Gabriel Belingueres
 * @since 3.1.0
 */
public final class NearestVersionSelector
    extends VersionSelector
{

    @Override
    public void selectVersion( ConflictContext context )
        throws RepositoryException
    {
        ConflictGroup group = new ConflictGroup();
        for ( ConflictItem item : context.getItems() )
        {
            DependencyNode node = item.getNode();
            VersionConstraint constraint = node.getVersionConstraint();

            boolean backtrack = false;
            boolean hardConstraint = !constraint.getRanges().isEmpty();
//            boolean hardConstraint = constraint.getRange() != null;

            if ( hardConstraint )
            {
                if ( group.constraints.add( constraint ) )
                {
                    if ( group.winner != null && !constraint.containsVersion( group.winner.getNode().getVersion() ) )
                    {
                        backtrack = true;
                    }
                }
            }

            if ( isAcceptable( group, node.getVersion() ) )
            {
                group.candidates.add( item );

                if ( backtrack )
                {
                    backtrack( group, context );
                }
                else if ( group.winner == null || isNearer( item, group.winner ) )
                {
                    group.winner = item;
                }
            }
            else if ( backtrack )
            {
                backtrack( group, context );
            }
        }
        context.setWinner( group.winner );
    }

    private void backtrack( ConflictGroup group, ConflictContext context )
        throws UnsolvableVersionConflictException
    {
        group.winner = null;

        for ( Iterator<ConflictItem> it = group.candidates.iterator(); it.hasNext(); )
        {
            ConflictItem candidate = it.next();

            if ( !isAcceptable( group, candidate.getNode().getVersion() ) )
            {
                it.remove();
            }
            else if ( group.winner == null || isNearer( candidate, group.winner ) )
            {
                group.winner = candidate;
            }
        }

        if ( group.winner == null )
        {
            throw newFailure( context );
        }
    }

    private boolean isAcceptable( ConflictGroup group, Version version )
    {
        for ( VersionConstraint constraint : group.constraints )
        {
            if ( !constraint.containsVersion( version ) )
            {
                return false;
            }
        }
        return true;
    }

    private boolean isNearer( ConflictItem item1, ConflictItem item2 )
    {
        if ( item1.isSibling( item2 ) )
        {
            return item1.getNode().getVersion().compareTo( item2.getNode().getVersion() ) > 0;
        }
        else
        {
            return item1.getDepth() < item2.getDepth();
        }
    }

    private UnsolvableVersionConflictException newFailure( final ConflictContext context )
    {
        DependencyFilter filter = new DependencyFilter()
        {
            public boolean accept( DependencyNode node, List<DependencyNode> parents )
            {
                return context.isIncluded( node );
            }
        };
        PathRecordingDependencyVisitor visitor = new PathRecordingDependencyVisitor( filter );
        context.getRoot().accept( visitor );
        return new UnsolvableVersionConflictException( visitor.getPaths(), context.conflictId );
//        return new UnsolvableVersionConflictException( visitor.getPaths() );
    }

    static final class ConflictGroup
    {

        final Collection<VersionConstraint> constraints;

        final Collection<ConflictItem> candidates;

        ConflictItem winner;

        ConflictGroup()
        {
            constraints = new HashSet<VersionConstraint>();
            candidates = new ArrayList<ConflictItem>( 64 );
        }

        @Override
        public String toString()
        {
            return String.valueOf( winner );
        }

    }


}
