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
package org.apache.maven.shared.dependency.graph.traversal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.filter.DependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.internal.DefaultDependencyNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilteringDependencyNodeVisitorTest {

    @Test
    void evaluatesFilterOncePerNode() {
        DefaultDependencyNode root = node("root", null);
        DefaultDependencyNode child = node("child", root);
        DefaultDependencyNode grandchild = node("grandchild", child);
        root.setChildren(Collections.singletonList(child));
        child.setChildren(Collections.singletonList(grandchild));
        grandchild.setChildren(Collections.emptyList());

        AtomicInteger filterCalls = new AtomicInteger();
        DependencyNodeFilter filter = node -> {
            filterCalls.incrementAndGet();
            return !"child".equals(node.getArtifact().getArtifactId());
        };

        List<String> delegateCalls = new ArrayList<>();
        DependencyNodeVisitor delegate = new DependencyNodeVisitor() {
            @Override
            public boolean visit(DependencyNode node) {
                delegateCalls.add("visit:" + artifactId(node));
                return true;
            }

            @Override
            public boolean endVisit(DependencyNode node) {
                delegateCalls.add("endVisit:" + artifactId(node));
                return true;
            }
        };

        root.accept(new FilteringDependencyNodeVisitor(delegate, filter));

        assertEquals(3, filterCalls.get());
        assertEquals(
                Arrays.asList("visit:root", "visit:grandchild", "endVisit:grandchild", "endVisit:root"), delegateCalls);
    }

    private static DefaultDependencyNode node(String artifactId, DefaultDependencyNode parent) {
        Artifact artifact = new DefaultArtifact("group", artifactId, "1.0", "compile", "jar", "", null);
        return new DefaultDependencyNode(parent, artifact, null, null, null);
    }

    private static String artifactId(DependencyNode node) {
        return node.getArtifact().getArtifactId();
    }
}
