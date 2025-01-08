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

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Exclusion;
import org.apache.maven.shared.dependency.graph.ConflictData;
import org.apache.maven.shared.dependency.graph.DependencyNode;

class VerboseDependencyNode extends DefaultDependencyNode {

    private final ConflictData data;

    VerboseDependencyNode(
            DependencyNode parent,
            Artifact artifact,
            String premanagedVersion,
            String premanagedScope,
            String versionConstraint,
            Boolean optional,
            List<Exclusion> exclusions,
            ConflictData data) {
        super(parent, artifact, premanagedVersion, premanagedScope, versionConstraint, optional, exclusions);

        this.data = data;
    }

    @Override
    public String toNodeString() {
        StringBuilder buffer = new StringBuilder();

        boolean included = (data.getWinnerVersion() == null);

        if (!included) {
            buffer.append('(');
        }

        buffer.append(getArtifact());

        ItemAppender appender = new ItemAppender(buffer, included ? " (" : " - ", "; ", included ? ")" : "");

        if (getPremanagedVersion() != null) {
            appender.append("version managed from ", getPremanagedVersion());
        }

        if (getPremanagedScope() != null) {
            appender.append("scope managed from ", getPremanagedScope());
        }

        if (data.getIgnoredScope() != null) {
            appender.append("scope not updated to ", data.getIgnoredScope());
        }

        //        if ( getVersionSelectedFromRange() != null )
        //        {
        //            appender.append( "version selected from range ", getVersionSelectedFromRange().toString() );
        //            appender.append( "available versions ", getAvailableVersions().toString() );
        //        }

        if (!included) {
            String winnerVersion = data.getWinnerVersion();
            if (winnerVersion.equals(getArtifact().getVersion())) {
                appender.append("omitted for duplicate");
            } else {
                appender.append("omitted for conflict with ", winnerVersion);
            }
        }

        appender.flush();

        if (!included) {
            buffer.append(')');
        }

        return buffer.toString();
    }

    @Override
    public ConflictData getConflictData() {
        return data;
    }

    /**
     * Utility class to concatenate a number of parameters with separator tokens.
     */
    private static class ItemAppender {
        private StringBuilder buffer;

        private String startToken;

        private String separatorToken;

        private String endToken;

        private boolean appended;

        ItemAppender(StringBuilder buffer, String startToken, String separatorToken, String endToken) {
            this.buffer = buffer;
            this.startToken = startToken;
            this.separatorToken = separatorToken;
            this.endToken = endToken;

            appended = false;
        }

        public ItemAppender append(String item1) {
            appendToken();

            buffer.append(item1);

            return this;
        }

        public ItemAppender append(String item1, String item2) {
            appendToken();

            buffer.append(item1).append(item2);

            return this;
        }

        public void flush() {
            if (appended) {
                buffer.append(endToken);

                appended = false;
            }
        }

        private void appendToken() {
            buffer.append(appended ? separatorToken : startToken);

            appended = true;
        }
    }
}
