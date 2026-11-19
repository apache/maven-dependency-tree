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

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.shared.dependency.graph.internal.DefaultDependencyNode;
import org.junit.jupiter.api.Test;

import static org.apache.maven.shared.dependency.graph.traversal.SerializingDependencyNodeVisitor.STANDARD_TOKENS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SerializingDependencyNodeVisitorTest {

    @Test
    void serializesTreeWithExpectedIndentation() {
        DefaultDependencyNode root = node("root", null);
        DefaultDependencyNode firstChild = node("first-child", root);
        DefaultDependencyNode grandchild = node("grandchild", firstChild);
        DefaultDependencyNode secondChild = node("second-child", root);
        root.setChildren(Arrays.asList(firstChild, secondChild));
        firstChild.setChildren(Collections.singletonList(grandchild));
        grandchild.setChildren(Collections.emptyList());
        secondChild.setChildren(Collections.emptyList());

        StringWriter output = new StringWriter();
        root.accept(new SerializingDependencyNodeVisitor(output, STANDARD_TOKENS));

        assertEquals(
                lines(
                        "group:root:jar:1.0:compile",
                        "+- group:first-child:jar:1.0:compile",
                        "|  \\- group:grandchild:jar:1.0:compile",
                        "\\- group:second-child:jar:1.0:compile"),
                output.toString());
    }

    @Test
    void interleavedTraversalsDoNotShareDepth() {
        DefaultDependencyNode firstRoot = node("first", null);
        DefaultDependencyNode secondRoot = node("second", null);
        StringWriter output = new StringWriter();
        SerializingDependencyNodeVisitor visitor = new SerializingDependencyNodeVisitor(output, STANDARD_TOKENS);

        visitor.visit(firstRoot);
        visitor.visit(secondRoot);
        visitor.endVisit(secondRoot);
        visitor.endVisit(firstRoot);

        assertEquals(lines("group:first:jar:1.0:compile", "group:second:jar:1.0:compile"), output.toString());
    }

    private static DefaultDependencyNode node(String artifactId, DefaultDependencyNode parent) {
        Artifact artifact = new DefaultArtifact("group", artifactId, "1.0", "compile", "jar", "", null);
        return new DefaultDependencyNode(parent, artifact, null, null, null);
    }

    private static String lines(String... lines) {
        return String.join("\n", lines) + "\n";
    }
}
