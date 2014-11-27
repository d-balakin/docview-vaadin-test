package ru.rlisystems.docviewer;

import lombok.extern.java.Log;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.*;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;

@Log
public class ConfigurationInjector
{
	@Produces
	@ConfigurationValue
	public static Boolean getBooleanConfigurationValue (InjectionPoint injectionPoint)
	{
		String configurationParameterValue = getConfigurationParameterValue(injectionPoint);
		return configurationParameterValue == null ? null : Boolean.parseBoolean(configurationParameterValue);
	}

	@Produces
	@ConfigurationValue
	public static String getStringConfigurationValue (InjectionPoint injectionPoint)
	{
		return getConfigurationParameterValue(injectionPoint);
	}

	@Produces
	@ConfigurationValue
	public static Integer getIntegerConfigurationValue (InjectionPoint injectionPoint)
	{
		String configurationParameterValue = getConfigurationParameterValue(injectionPoint);
		return configurationParameterValue == null ? null : Integer.parseInt(configurationParameterValue);
	}

	@Produces
	@ConfigurationValue
	public static Long getLongConfigurationValue (InjectionPoint injectionPoint)
	{
		String configurationParameterValue = getConfigurationParameterValue(injectionPoint);
		return configurationParameterValue == null ? null : Long.parseLong(configurationParameterValue);
	}

	@Produces
	@ConfigurationValue
	public static Float getFloatConfigurationValue (InjectionPoint injectionPoint)
	{
		String configurationParameterValue = getConfigurationParameterValue(injectionPoint);
		return configurationParameterValue == null ? null : Float.parseFloat(configurationParameterValue);
	}

	@Produces
	@ConfigurationValue
	public static Double getDoubleConfigurationValue (InjectionPoint injectionPoint)
	{
		String configurationParameterValue = getConfigurationParameterValue(injectionPoint);
		return configurationParameterValue == null ? null : Double.parseDouble(configurationParameterValue);
	}

	private static String getConfigurationParameterValue (InjectionPoint injectionPoint)
	{
		ConfigurationValue annotation = injectionPoint.getAnnotated().getAnnotation(ConfigurationValue.class);
		if (annotation == null) {
			throw new IllegalArgumentException();
		}

		final String parameterName;
		if (!annotation.name().isEmpty()) {
			parameterName = annotation.name();
		}
		else {
			Member member = injectionPoint.getMember();
			String className = member.getDeclaringClass().getName();
			String memberName = member.getName();
			if (member instanceof Method) {
				if (memberName.length() > 3 && (memberName.startsWith("get") || memberName.startsWith("set"))) {
					memberName = Character.toLowerCase(memberName.charAt(3)) + memberName.substring(4);
				}
				else if (memberName.length() > 2 && memberName.startsWith("is")) {
					memberName = Character.toLowerCase(memberName.charAt(2)) + memberName.substring(3);
				}
			}
			parameterName = className + "." + memberName;
		}

		String value = ConfigurationValue.NO_VALUE;
		if (annotation.lookupSystemProperties()) {
			value = System.getProperty(parameterName, ConfigurationValue.NO_VALUE);
		}
		if (ConfigurationValue.NO_VALUE.equals(value)) {
			URL resource = annotation.source().holder().getResource(annotation.source().name());
			if (resource != null) {
				try (InputStream inputStream = resource.openStream()) {
					Properties properties = new Properties();
					properties.load(inputStream);
					value = properties.getProperty(parameterName, ConfigurationValue.NO_VALUE);
				}
				catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		}
		if (ConfigurationValue.NO_VALUE.equals(value)) {
			value = annotation.defaultValue();
		}
		if (ConfigurationValue.NULL.equals(value)) {
			value = null;
		}
		else if (ConfigurationValue.NO_VALUE.equals(value)) {
			throw new IllegalArgumentException("Конфигурационный параметр '" +  parameterName +
													   						"' не задан для " + injectionPoint);
		}
		return value;
	}

	@Qualifier
	@Retention (RetentionPolicy.RUNTIME)
	@Target ({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
	public static @interface ConfigurationValue
	{
		public static final String NULL = "\0";
		public static final String NO_VALUE = "\1";

		@Nonbinding String name () default "";
		@Nonbinding boolean lookupSystemProperties () default true;
		@Nonbinding String defaultValue () default NO_VALUE;
		@Nonbinding Source source () default @Source (name = "/META-INF/application.properties");

		public static @interface Source
		{
			String name ();
			Class<?> holder () default Source.class;
		}
	}
}
