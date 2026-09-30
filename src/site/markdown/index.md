<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Maven Dependency Tree

> **Note: This library is planned for retirement.**
>
> - **New projects**: use [Maven Resolver](https://maven.apache.org/resolver/) directly instead.
> - **Existing projects**: version 3.3.0 stays on Maven Central; plan the migration to Resolver.
> - See [Issue #150](https://github.com/apache/maven-dependency-tree/issues/150) for the plan and migration pointers.

A tree-based API for resolution of Maven project dependencies.

Component entry point is [`DependencyGraphBuilder`](./apidocs/org/apache/maven/shared/dependency/graph/DependencyGraphBuilder.html).
