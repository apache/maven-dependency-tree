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

String actual = new File(basedir, "module-z-deps-y/target/tree.txt").getText('UTF-8')
String expected = new File(basedir, "expected-tree.txt").getText('UTF-8')

actual = actual.replaceAll("[\n\r]+", "\n")
expected = expected.replaceAll("[\n\r]+", "\n")

System.out.println("Checking module-z-deps-y dependency tree...")

assert actual.equals(expected) : "Unexpected dependency tree"

File resolved = new File( basedir, "resolved-module-z-deps-y.txt" )

assert !resolved.exists()
