/*
 * Copyright (C) 2017-2019 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.provision;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.immutables.value.Value.Immutable;

/** List of cluster responses organized by type. */
@JsonDeserialize(builder = ImmutableClusterResponsesByType.Builder.class)
@Immutable
public interface ClusterResponsesByType {
  @NotNull
  ClusterType getClusterType();

  List<ClusterResponse> getClusterList();

  public static ImmutableClusterResponsesByType.Builder builder() {
    return new ImmutableClusterResponsesByType.Builder();
  }
}
