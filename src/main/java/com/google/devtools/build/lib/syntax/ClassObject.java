// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.syntax;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * An instance of a Skylark class.
 */
// TODO(bazel-team): type checks, immutability
public class ClassObject {

  private final ImmutableMap<String, Object> values;

  public ClassObject(Map<String, Object> values) {
    this.values = ImmutableMap.copyOf(values);
  }

  public Object getValue(String name) {
    return values.get(name);
  }
}