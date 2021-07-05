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

def actual = new File( basedir, "target/tree-verbose.txt" ).readLines()
// omitted for cycle not supported anymore, but should probably return
// omitted for exclusion is not supported yet
def expected = new File( basedir, "expected-verbose.txt" ).readLines().findAll{!((it.contains('omitted for cycle')||it.contains('omitted for exclusion')))}

assert actual.equals( expected )

actual = new File( basedir, "target/tree-default.txt" ).readLines()
expected = new File( basedir, "expected-default.txt" ).readLines()

assert actual.equals( expected )