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
package com.dremio.service.namespace;

import com.dremio.datastore.SearchTypes;
import com.dremio.datastore.api.Document;
import com.dremio.datastore.api.FindByCondition;
import com.dremio.service.namespace.proto.EntityId;
import com.dremio.service.namespace.proto.NameSpaceContainer;
import java.util.List;
import java.util.Optional;

/**
 * Namespace operations for generic entities. If you are operating on a specific entity, use that
 * entity's NamespaceService.For example, if getting a function, use {@link
 * com.dremio.service.namespace.function.FunctionNamespaceService}.
 */
public interface EntityNamespaceService {
  //// READ
  boolean exists(NamespaceKey key, NameSpaceContainer.Type type);

  boolean exists(NamespaceKey key);

  boolean hasChildren(NamespaceKey key);

  /**
   * Returns entity id by path
   *
   * @param entityPath
   * @return a data set entity id or null, if there is no entityPath by provided path
   */
  String getEntityIdByPath(NamespaceKey entityPath) throws NamespaceNotFoundException;

  Optional<NameSpaceContainer> getEntityById(EntityId id);

  /**
   * Returns entities by Id
   *
   * @param ids - A list of EntityIds
   * @return A list of NamespaceContainers. The container will be null if the NamespaceContainer
   *     cannot be found.
   */
  List<NameSpaceContainer> getEntitiesByIds(List<EntityId> ids);

  /**
   * Returns an entity given its path.
   *
   * @param entityPath namespace key
   * @return entity associated with this path or null, if there is no entity.
   */
  NameSpaceContainer getEntityByPath(NamespaceKey entityPath) throws NamespaceException;

  /**
   * Get multiple entities of given type
   *
   * @param lookupKeys namespace keys
   * @return list of namespace containers with null if no value found for a key. Order of returned
   *     list matches with order of lookupKeys.
   */
  List<NameSpaceContainer> getEntities(List<NamespaceKey> lookupKeys);

  /**
   * Return list of counts matching each query
   *
   * @param queries list of queries to perform search on
   * @return list of counts. Order of returned counts is same as order of queries.
   * @throws NamespaceException
   */
  List<Integer> getCounts(SearchTypes.SearchQuery... queries) throws NamespaceException;

  List<NameSpaceContainer> list(NamespaceKey entityPath, String startChildName, int maxResults)
      throws NamespaceException;

  Iterable<NameSpaceContainer> getAllDescendants(final NamespaceKey root);

  /** Find entries by condition. If condition is not provided, returns all items. */
  Iterable<Document<NamespaceKey, NameSpaceContainer>> find(
      FindByCondition condition, EntityNamespaceFindOption... options);

  //// DELETE
  /** Do not use. Leverage an entity-specific deletion. */
  @Deprecated
  void deleteEntity(NamespaceKey entityPath) throws NamespaceException;

  //// OTHER
  default void invalidateNamespaceCache(final NamespaceKey key) {}
}
