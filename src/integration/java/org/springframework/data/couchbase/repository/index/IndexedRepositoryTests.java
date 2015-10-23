/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.couchbase.repository.index;

import static org.junit.Assert.*;

import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.View;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.couchbase.IntegrationTestApplicationConfig;
import org.springframework.data.couchbase.core.CouchbaseOperations;
import org.springframework.data.couchbase.repository.config.RepositoryOperationsMapping;
import org.springframework.data.couchbase.repository.support.CouchbaseRepositoryFactory;
import org.springframework.data.couchbase.repository.support.IndexManager;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This tests automatic index creation features in the Couchbase connector.
 * Automatic index creation is performed before construction of the repository implementation.
 *
 * @author Simon Baslé
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestApplicationConfig.class)
@TestExecutionListeners(IndexedRepositoryTestListener.class)
public class IndexedRepositoryTests {

  public static final String SECONDARY = "autogeneratedIndexIndexedUserN1qlSecondary";
  public static final String VIEW_DOC = "autogeneratedIndex";
  public static final String VIEW_NAME = "IndexedUserView";

  public static final String IGNORED_VIEW_NAME = "AnotherIndexedUserView";
  public static final String IGNORED_SECONDARY = "AnotherIndexedUserN1qlSecondary";

  @Autowired
  private RepositoryOperationsMapping operationsMapping;

  @Autowired
  private IndexManager indexManager;

  private CouchbaseOperations template;
  private RepositoryFactorySupport factory;

  private RepositoryFactorySupport ignoringIndexFactory;
  private IndexManager ignoringIndexManager = new IndexManager(true, true, true);

  @Before
  public void setup() throws Exception {
    factory = new CouchbaseRepositoryFactory(operationsMapping, indexManager);
    template = operationsMapping.getDefault();
    ignoringIndexFactory = new CouchbaseRepositoryFactory(operationsMapping, ignoringIndexManager);
  }

  @Test
  public void shouldFindN1qlPrimaryIndex() {
    IndexedUserRepository repository = factory.getRepository(IndexedUserRepository.class);

    String bucket = template.getCouchbaseBucket().name();
    N1qlQuery existQuery = N1qlQuery.simple("SELECT COUNT(name) = 1 AS exist FROM system:indexes WHERE keyspace_id = \"" + bucket
        + "\" AND is_primary");
    N1qlQueryResult exist = template.queryN1QL(existQuery);

    assertTrue(exist.finalSuccess());
    assertEquals(1, exist.allRows().size());
    assertEquals(true, exist.allRows().get(0).value().getBoolean("exist"));
  }

  @Test
  public void shouldFindN1qlSecondaryIndex() {
    IndexedUserRepository repository = factory.getRepository(IndexedUserRepository.class);

    String bucket = template.getCouchbaseBucket().name();
    N1qlQuery existQuery = N1qlQuery.simple("SELECT COUNT(name) = 1 AS exist FROM system:indexes WHERE keyspace_id = \"" + bucket
        + "\" AND name = \"" + SECONDARY + "\"");
    N1qlQueryResult exist = template.queryN1QL(existQuery);

    assertTrue(exist.finalSuccess());
    assertEquals(1, exist.allRows().size());
    assertEquals(true, exist.allRows().get(0).value().getBoolean("exist"));
  }

  @Test
  public void shouldFindViewIndex() {
    IndexedUserRepository repository = factory.getRepository(IndexedUserRepository.class);

    DesignDocument designDoc = template.getCouchbaseBucket()
        .bucketManager()
        .getDesignDocument(VIEW_DOC);

    assertNotNull(designDoc);
    for (View view : designDoc.views()) {
      if (view.name().equals(VIEW_NAME)) return;
    }
    fail("View not found");
  }
  @Test
  public void shouldNotFindN1qlSecondaryIndexWithIgnoringIndexManager() {
    AnotherIndexedUserRepository repository = ignoringIndexFactory.getRepository(AnotherIndexedUserRepository.class);

    String bucket = template.getCouchbaseBucket().name();
    N1qlQuery existQuery = N1qlQuery.simple("SELECT COUNT(name) = 1 AS exist FROM system:indexes WHERE keyspace_id = \"" + bucket
        + "\" AND name = \"" + IGNORED_SECONDARY + "\"");
    N1qlQueryResult exist = template.queryN1QL(existQuery);

    assertTrue(exist.finalSuccess());
    assertEquals(1, exist.allRows().size());
    assertEquals(false, exist.allRows().get(0).value().getBoolean("exist"));
  }

  @Test
  public void shouldNotFindViewIndexWithIgnoringIndexManager() {
    AnotherIndexedUserRepository repository = ignoringIndexFactory.getRepository(AnotherIndexedUserRepository.class);

    DesignDocument designDoc = template.getCouchbaseBucket()
        .bucketManager()
        .getDesignDocument(VIEW_DOC);

    if (designDoc != null) {
      for (View view : designDoc.views()) {
        if (view.name().equals(IGNORED_VIEW_NAME)) fail("Found unexpected " + IGNORED_VIEW_NAME);
      }
    }
  }
}