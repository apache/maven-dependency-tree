/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.shared.dependency.graph.internal;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.shared.dependency.graph.ConflictData;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VerboseDependencyNodeTest {

    private final Artifact artifact = new DefaultArtifact("group", "artifact", "1.2", "compile", "jar", "", null);

    @Test
    public void verboseDependencyNode_should_return_conflict_data() {
        ConflictData conflictData = new ConflictData("winnerVersion", "ignoredScope");
        VerboseDependencyNode verboseDependencyNode =
                new VerboseDependencyNode(null, artifact, "1.0", "compile", "1.0", false, emptyList(), conflictData);

        assertEquals("winnerVersion", verboseDependencyNode.getConflictData().getWinnerVersion());
        assertEquals("ignoredScope", verboseDependencyNode.getConflictData().getIgnoredScope());
    }
}
