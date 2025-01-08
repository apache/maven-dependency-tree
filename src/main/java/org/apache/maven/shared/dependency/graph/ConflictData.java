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
package org.apache.maven.shared.dependency.graph;

/**
 * Explicit subset of Aether's DependencyNode.getData().
 *
 * @author Robert Scholte
 */
public class ConflictData {
    private String winnerVersion;

    private String originalScope;

    private String ignoredScope;

    private Boolean originalOptionality;

    /**
     * Construct ConflictData. Containing information about conflicts during dependency resolution.
     * Either this node lost the conflict and winnerVersion is set with the version of the winnig node,
     * or this node won and winnerVersion is @code{null}.
     * If this node won, ignoredScope can contain potential scopes that were ignored during conflict resolution.
     *
     * @param winnerVersion the version of the dependency that was selected
     * @param ignoredScope  the scope of the dependency that was ignored and not updated to
     */
    public ConflictData(String winnerVersion, String ignoredScope) {
        this.winnerVersion = winnerVersion;
        this.ignoredScope = ignoredScope;
    }

    /**
     * In case of a conflict, the version of the dependency that was selected.
     *
     * @return the version of the dependency node that was selected.
     */
    public String getWinnerVersion() {
        return winnerVersion;
    }

    /**
     * Original scope of a rejected dependency due to a conflict.
     *
     * @return the original scope of the dependency that was updated from
     */
    public String getOriginalScope() {
        return originalScope;
    }

    /**
     * Set original scope of a rejected dependency due to a conflict.
     *
     * @param originalScope the original scope of the dependency that was updated from
     */
    public void setOriginalScope(String originalScope) {
        this.originalScope = originalScope;
    }

    /**
     * Original optionality of a rejected dependency due to a conflict.
     *
     * @return the original optionality of the dependency
     */
    public Boolean getOriginalOptionality() {
        return originalOptionality;
    }

    /**
     * Set original optionality of a rejected dependency due to a conflict.
     *
     * @param originalOptionality the original optionality of the dependency
     */
    public void setOriginalOptionality(Boolean originalOptionality) {
        this.originalOptionality = originalOptionality;
    }

    /**
     * The scope of the dependency that was not updated to during dependency resolution.
     *
     * @return the scope of the dependency that was ignored and not updated to
     */
    public String getIgnoredScope() {
        return ignoredScope;
    }
}
