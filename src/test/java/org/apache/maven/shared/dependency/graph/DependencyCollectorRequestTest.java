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

import java.util.Map;

import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DependencyCollectorRequestTest {

    @Test
    void configPropertiesAreDefensivelyCopiedAndUnmodifiable() {
        DependencyCollectorRequest request =
                new DependencyCollectorRequest(new DefaultProjectBuildingRequest());
        request.addConfigProperty("mode", "safe");

        Map<String, Object> properties = request.getConfigProperties();

        assertEquals("safe", properties.get("mode"));
        assertThrows(UnsupportedOperationException.class, () -> properties.put("mode", "unsafe"));
        assertThrows(UnsupportedOperationException.class, () -> properties.remove("mode"));
        assertEquals("safe", request.getConfigProperties().get("mode"));

        request.addConfigProperty("mode", "updated");
        assertEquals("safe", properties.get("mode"),
                "A previously returned snapshot must not reflect later request changes");
        assertEquals("updated", request.getConfigProperties().get("mode"));
    }
}
