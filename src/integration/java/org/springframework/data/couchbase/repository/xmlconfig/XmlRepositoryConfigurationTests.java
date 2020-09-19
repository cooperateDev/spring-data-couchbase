package org.springframework.data.couchbase.repository.xmlconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Simon Baslé
 */
public class XmlRepositoryConfigurationTests {

  DefaultListableBeanFactory factory;
  BeanDefinitionReader reader;

  @Before
  public void setUp() {
    factory = new DefaultListableBeanFactory();
    reader = new XmlBeanDefinitionReader(factory);
  }

  @Test
  public void testInstantiateRepositoryFromXml() {
    reader.loadBeanDefinitions(new ClassPathResource("configurations/couchbase-repository-bean.xml"));

    BeanDefinition definition = factory.getBeanDefinition("xmlItemRepository");
    assertEquals("org.springframework.data.couchbase.repository.support.CouchbaseRepositoryFactoryBean", definition.getBeanClassName());
    assertDefinitionProperty(definition, "couchbaseOperations");

    Object bean = factory.getBean("xmlItemRepository");
    assertTrue(bean instanceof XmlItemRepository);
  }

  private void assertDefinitionProperty(BeanDefinition definition, String property) {
    assertTrue("bean definition properties don't include " + property, definition.getPropertyValues().contains(property));
  }
}
