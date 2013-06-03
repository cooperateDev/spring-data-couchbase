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

package org.springframework.data.couchbase.core.convert;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.couchbase.core.mapping.ConvertedCouchbaseDocument;
import org.springframework.data.couchbase.core.mapping.CouchbasePersistentEntity;
import org.springframework.data.couchbase.core.mapping.CouchbasePersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.util.Assert;

/**
 * @author Michael Nitschinger
 */
public class MappingCouchbaseConverter extends AbstractCouchbaseConverter
  implements ApplicationContextAware {

  protected ApplicationContext applicationContext;
  protected final MappingContext<? extends CouchbasePersistentEntity<?>,
      CouchbasePersistentProperty> mappingContext;
  protected boolean useFieldAccessOnly = true;

  @SuppressWarnings("deprecation")
  public MappingCouchbaseConverter(MappingContext<? extends CouchbasePersistentEntity<?>,
      CouchbasePersistentProperty> mappingContext) {
    super(ConversionServiceFactory.createDefaultConversionService());

    this.mappingContext = mappingContext;
  }

  @Override
  public MappingContext<? extends CouchbasePersistentEntity<?>,
    CouchbasePersistentProperty> getMappingContext() {
    return mappingContext;
  }

  
	private ParameterValueProvider<CouchbasePersistentProperty> getParameterProvider(
			CouchbasePersistentEntity<?> entity, ConvertedCouchbaseDocument source, Object parent) {
		
		CouchbasePropertyValueProvider provider = new CouchbasePropertyValueProvider(source, parent);
		PersistentEntityParameterValueProvider<CouchbasePersistentProperty> parameterProvider = 
				new PersistentEntityParameterValueProvider<CouchbasePersistentProperty>(
						entity, provider, parent);
		
		return parameterProvider;
	}
	
  @Override
  public <R> R read(Class<R> type, ConvertedCouchbaseDocument doc) {
  	return read(type, doc, null);
  }
  
  public <R> R read(Class<R> type, final ConvertedCouchbaseDocument doc, Object parent) {
  	final CouchbasePersistentEntity<R> entity  = (CouchbasePersistentEntity<R>) 
  			mappingContext.getPersistentEntity(type);
  	
  	ParameterValueProvider<CouchbasePersistentProperty> provider = 
  			getParameterProvider(entity, doc, parent);
  	EntityInstantiator instantiator = instantiators.getInstantiatorFor(entity);
  	R instance = instantiator.createInstance(entity, provider);
  	
  	final BeanWrapper<CouchbasePersistentEntity<R>, R> wrapper = 
  			BeanWrapper.create(instance, conversionService);
		final R result = wrapper.getBean();
		
		// Set properties not already set in the constructor
		entity.doWithProperties(new PropertyHandler<CouchbasePersistentProperty>() {
			public void doWithPersistentProperty(CouchbasePersistentProperty prop) {

				boolean isConstructorProperty = entity.isConstructorArgument(prop);
				boolean hasValueForProperty = doc.containsField(prop.getFieldName());

				if (!hasValueForProperty || isConstructorProperty) {
					return;
				}

				Object obj = null;
				if(prop.isIdProperty()) {
					obj = doc.getId();
				} else {
					obj = doc.get(prop.getFieldName());
				}
				wrapper.setProperty(prop, obj, useFieldAccessOnly);
			}
		});
		
		return result;
  }

  @Override
  public void write(Object source, ConvertedCouchbaseDocument target) {
    if(source == null) {
      return;
    }

    TypeInformation<? extends Object> type = ClassTypeInformation.from(source.getClass());
    try {
      writeInternal(source, target, type);
    } catch (IOException ex) {
      throw new MappingException("Could not translate to JSON while converting "
        + source.getClass().getName());
    }

  }

  protected void writeInternal(final Object source,
    ConvertedCouchbaseDocument target, TypeInformation<?> type)
    throws IOException {
    CouchbasePersistentEntity<?> entity  = mappingContext.getPersistentEntity(
      source.getClass());

    if(entity == null) {
      throw new MappingException("No mapping metadata found for entity of type "
        + source.getClass().getName());
    }

    final CouchbasePersistentProperty idProperty = entity.getIdProperty();
    if(idProperty == null) {
      throw new MappingException("ID property required for entity of type "
        + source.getClass().getName());
    }

    final BeanWrapper<CouchbasePersistentEntity<Object>, Object> wrapper =
      BeanWrapper.create(source, conversionService);

    String id = wrapper.getProperty(idProperty, String.class, false);
    target.setId(id);
    target.setExpiry(entity.getExpiry());

    JsonFactory jsonFactory = new JsonFactory();
    OutputStream jsonStream = new ByteArrayOutputStream();
    final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(
      jsonStream, JsonEncoding.UTF8);
    jsonGenerator.setCodec(new ObjectMapper());

    jsonGenerator.writeStartObject();
    entity.doWithProperties(new PropertyHandler<CouchbasePersistentProperty>() {
      @Override
      public void doWithPersistentProperty(CouchbasePersistentProperty prop) {
        if(prop.equals(idProperty)) {
          return;
        }

        Object propertyValue = wrapper.getProperty(prop, prop.getType(), false);
        if(propertyValue != null) {
          try {
            jsonGenerator.writeFieldName(prop.getFieldName());
            jsonGenerator.writeObject(propertyValue);
          } catch (IOException ex) {
            throw new MappingException("Could not translate to JSON while converting "
              + source.getClass().getName());
          }
        }

      }
    });
    jsonGenerator.writeEndObject();
    jsonGenerator.close();

    target.setRawValue(jsonStream.toString());
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext)
    throws BeansException {
    this.applicationContext = applicationContext;
  }
  
	private class CouchbasePropertyValueProvider implements PropertyValueProvider<CouchbasePersistentProperty> {

		private final ConvertedCouchbaseDocument source;
		private final Object parent;

		public CouchbasePropertyValueProvider(ConvertedCouchbaseDocument source, Object parent) {
			Assert.notNull(source);
			this.source = source;
			this.parent = parent;
		}

		public <T> T getPropertyValue(CouchbasePersistentProperty property) {
			T value = null;
			
			if(property.isIdProperty()) {
				value = (T) source.getId();
			} else {
				value = (T) source.get(property.getFieldName());
			}
			
			if (value == null) {
				return null;
			}

			return value;
		}
	}

}
