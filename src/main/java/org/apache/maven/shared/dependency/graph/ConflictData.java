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

    private Boolean originaOptionality;

    public ConflictData(String winnerVersion, String ignoredScope) {
        this.winnerVersion = winnerVersion;
        this.ignoredScope = ignoredScope;
    }

    public String getWinnerVersion() {
        return winnerVersion;
    }

    public String getOriginalScope() {
        return originalScope;
    }

    public void setOriginalScope(String originalScope) {
        this.originalScope = originalScope;
    }

    public Boolean getOriginaOptionality() {
        return originaOptionality;
    }

    public void setOriginaOptionality(Boolean originaOptionality) {
        this.originaOptionality = originaOptionality;
    }

    public String getIgnoredScope() {
        return ignoredScope;
    }
}
