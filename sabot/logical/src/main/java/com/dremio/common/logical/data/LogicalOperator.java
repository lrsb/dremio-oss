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
package com.dremio.common.logical.data;

import com.dremio.common.graph.GraphValue;
import com.dremio.common.logical.ValidationError;
import com.dremio.common.logical.data.visitors.LogicalVisitor;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.List;

@JsonPropertyOrder({"@id", "memo", "input"}) // op will always be first since it is wrapped.
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "op")
public interface LogicalOperator extends GraphValue<LogicalOperator> {

  public void setupAndValidate(List<LogicalOperator> operators, Collection<ValidationError> errors);

  /**
   * Provides capability to build a set of output based on traversing a query graph tree.
   *
   * @param logicalVisitor
   * @return
   */
  public <T, X, E extends Throwable> T accept(LogicalVisitor<T, X, E> logicalVisitor, X value)
      throws E;

  public void registerAsSubscriber(LogicalOperator operator);

  NodeBuilder<?> nodeBuilder();

  public interface NodeBuilder<T extends LogicalOperator> {
    ObjectNode convert(ObjectMapper mapper, T operator, Integer inputId);
  }
}
