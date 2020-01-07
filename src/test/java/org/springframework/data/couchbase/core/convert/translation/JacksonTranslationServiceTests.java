/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.couchbase.core.convert.translation;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.couchbase.core.mapping.CouchbaseDocument;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the functionality of a {@link JacksonTranslationService}.
 *
 * @author Michael Nitschinger
 */
public class JacksonTranslationServiceTests {

  private TranslationService service;

  @Before
  public void setup() {
    service = new JacksonTranslationService();
    ((JacksonTranslationService) service).afterPropertiesSet();
  }

  @Test
  public void shouldEncodeNonASCII() {
    CouchbaseDocument doc = new CouchbaseDocument("key");
    doc.put("language", "русский");
    String expected = "{\"language\":\"русский\"}";
    assertThat(service.encode(doc)).isEqualTo(expected);
  }

  @Test
  public void shouldDecodeNonASCII() {
    String source = "{\"language\":\"русский\"}";
    CouchbaseDocument target = new CouchbaseDocument();
    service.decode(source, target);
    assertThat(target.get("language")).isEqualTo("русский");
  }

  @Test
  public void shouldDecodeAdHocFragment() {
    String source = "{\"language\":\"french\"}";
    LanguageFragment f = service.decodeFragment(source, LanguageFragment.class);
    assertThat(f).isNotNull();
    assertThat(f.language).isEqualTo("french");
  }

  private static class LanguageFragment {
    public String language;
  }
}
