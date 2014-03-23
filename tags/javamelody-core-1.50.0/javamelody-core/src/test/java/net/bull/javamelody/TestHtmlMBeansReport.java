/*
 * Copyright 2008-2014 by Emeric Vernat
 *
 *     This file is part of Java Melody.
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
package net.bull.javamelody;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import net.bull.javamelody.TestTomcatInformations.GlobalRequestProcessor;
import net.bull.javamelody.TestTomcatInformations.ThreadPool;

import org.junit.Before;
import org.junit.Test;

/**
 * Test unitaire de la classe HtmlMBeansReport.
 * @author Emeric Vernat
 */
public class TestHtmlMBeansReport {
	/**
	 * MBean sans attribut.
	 */
	public static class EmptyAttribute implements EmptyAttributeMBean {
		// rien
	}

	/**
	 * interface sans attribut.
	 */
	public interface EmptyAttributeMBean {
		// rien
	}

	/** Check. */
	@Before
	public void setUp() {
		Utils.initialize();
	}

	private static void assertNotEmptyAndClear(StringWriter writer) {
		assertTrue("rapport vide", writer.getBuffer().length() > 0);
		writer.getBuffer().setLength(0);
	}

	/** Test.
	 * @throws IOException e
	 * @throws JMException e */
	@Test
	public void testToHtml() throws IOException, JMException {
		assertNotNull("mbeans", new MBeans());
		final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		final List<ObjectName> mBeans = new ArrayList<ObjectName>();
		try {
			final ObjectInstance mBean1 = mBeanServer.registerMBean(new ThreadPool(),
					new ObjectName("Catalina:type=ThreadPool"));
			mBeans.add(mBean1.getObjectName());

			final ObjectInstance mBean2 = mBeanServer.registerMBean(new GlobalRequestProcessor(),
					new ObjectName("Catalina:type=GlobalRequestProcessor,name=http-8080"));
			mBeans.add(mBean2.getObjectName());

			final ObjectInstance mBean3 = mBeanServer.registerMBean(new GlobalRequestProcessor(),
					new ObjectName("Catalina:type=GlobalRequestProcessor,name=http-8090"));
			mBeans.add(mBean3.getObjectName());

			final ObjectInstance mBean4 = mBeanServer.registerMBean(new EmptyAttribute(),
					new ObjectName("java.lang:type=Object"));
			mBeans.add(mBean4.getObjectName());

			// on force à null une des descriptions de bean et une des descriptions d'attribut
			final MBeanInfo mbeanInfo = mBeanServer.getMBeanInfo(mBeans.get(0));
			JdbcWrapperHelper.setFieldValue(mbeanInfo, "description", null);
			final MBeanAttributeInfo mbeanAttributeInfo0 = mbeanInfo.getAttributes()[0];
			// même description que le nom de l'attribut
			JdbcWrapperHelper.setFieldValue(mbeanAttributeInfo0, "description", "maxThreads");
			final MBeanAttributeInfo mbeanAttributeInfo1 = mbeanInfo.getAttributes()[1];
			JdbcWrapperHelper.setFieldValue(mbeanAttributeInfo1, "description", null);

			// register another mbeanServer
			MBeanServerFactory.createMBeanServer("jboss");

			final StringWriter writer = new StringWriter();
			final List<MBeanNode> mbeans = MBeans.getAllMBeanNodes();
			final HtmlMBeansReport report = new HtmlMBeansReport(mbeans, writer);
			report.toHtml();
			assertNotEmptyAndClear(writer);
		} catch (final IllegalAccessException e) {
			throw new IllegalStateException(e);
		} finally {
			for (final ObjectName registeredMBean : mBeans) {
				mBeanServer.unregisterMBean(registeredMBean);
			}
		}
	}
}
