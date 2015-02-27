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
package com.google.devtools.build.lib.runtime;

import java.util.Date;

import com.google.devtools.build.lib.util.BlazeClock;

/**
 * Base class for Command events that includes some resource fields.
 */
public abstract class CommandEvent {

  private final long eventTimeInNanos;
  private final long eventTimeInEpochTime;

  protected CommandEvent() {
    eventTimeInNanos = BlazeClock.nanoTime();
    eventTimeInEpochTime = new Date().getTime();
  }

  /**
   * Get the time-stamp in ns for the event.
   */
  public long getEventTimeInNanos() {
    return eventTimeInNanos;
  }

  /**
   * Get the time-stamp as epoch-time for the event.
   */
  public long getEventTimeInEpochTime() {
    return eventTimeInEpochTime;
  }
}
