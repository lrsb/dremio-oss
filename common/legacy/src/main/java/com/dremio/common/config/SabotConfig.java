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
package com.dremio.common.config;

import com.dremio.common.exceptions.UserException;
import com.dremio.common.scanner.ClassPathScanner;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.reflections.util.ClasspathHelper;

public class SabotConfig extends NestedConfig {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(SabotConfig.class);

  @VisibleForTesting
  public SabotConfig(Config config) {
    super(config);
    logger.debug("Setting up SabotConfig object.");
    if (logger.isTraceEnabled()) {
      final String configObject = config.root().render(ConfigRenderOptions.defaults());
      logger.trace("Given Config object is:\n{}", configObject);
    }
  }

  /**
   * Get an instance of the provided interface using the configuration path provided. Construct the
   * object based on the provided constructor arguments.
   *
   * @param path The configuration path to use.
   * @param iface The Interface or Superclass of the instance you requested.
   * @param constructorArgs Any arguments required for constructing the requested type.
   * @return The new Object instance that implements the provided Interface
   */
  public <T> T getInstance(String path, Class<T> iface, Object... constructorArgs) {
    try {
      String className = this.getString(path);
      Class<?> clazz = Class.forName(className);
      return instantiate(iface, clazz, constructorArgs);
    } catch (Exception e) {
      throw UserException.unsupportedError(e)
          .message(
              "Failure while attempting to load instance of the class of type %s requested at path %s.",
              iface.getName(), path)
          .build(logger);
    }
  }

  /**
   * Same as {@link #getInstance(String, Class, Object...)}, except that it handles cases where
   * objects in constructorArgs are an instance of a subclass of the constructor's formal arguments.
   * It is assumed all the formal arguments of the constructor are of one class type.
   *
   * @param path The configuration path to use.
   * @param iface The Interface or Superclass of the instance you requested.
   * @param constructorArgClass The class type of the constructor's formal argument
   * @param constructorArgs Any arguments required for constructing the requested type.
   * @return The new Object instance that implements the provided Interface
   */
  public <T> T getInstanceWithConstructorArgType(
      String path, Class<T> iface, Class<?> constructorArgClass, Object... constructorArgs) {
    try {
      String className = this.getString(path);
      Class<?> clazz = Class.forName(className);
      return instantiateWithConstructorArgType(iface, clazz, constructorArgClass, constructorArgs);
    } catch (Exception e) {
      throw UserException.unsupportedError(e)
          .message(
              "Failure while attempting to load instance of the class of type %s requested at path %s.",
              iface.getName(), path)
          .build(logger);
    }
  }

  public <T> T getInstance(String path, Class<T> iface, Class<? extends T> defaultImpl) {
    if (this.hasPath(path)) {
      return getInstance(path, iface);
    }
    try {
      return instantiate(iface, defaultImpl);
    } catch (Exception e) {
      throw UserException.unsupportedError(e)
          .message(
              "Failure while attempting to instantiate default implementation class %s for interface  %s. The sabot config key is %s ",
              defaultImpl.getName(), iface.getName(), path)
          .build(logger);
    }
  }

  public <T> T getInstance(
      String path, Class<T> iface, T defaultInstance, Object... constructorArgs) {
    if (this.hasPath(path)) {
      return getInstance(path, iface, constructorArgs);
    }
    return defaultInstance;
  }

  public <T> T getInstanceWithConstructorArgType(
      String path,
      Class<T> iface,
      T defaultInstance,
      Class<?> constructorArgClass,
      Object... constructorArgs) {
    if (this.hasPath(path)) {
      return getInstanceWithConstructorArgType(path, iface, constructorArgClass, constructorArgs);
    }
    return defaultInstance;
  }

  @SuppressWarnings("unchecked")
  public <T> Class<? extends T> getClass(
      String path, Class<T> iface, Class<? extends T> defaultImpl) {
    if (this.hasPath(path)) {
      String className = this.getString(path);

      try {
        Class<?> clazz = Class.forName(className);
        Preconditions.checkArgument(iface.isAssignableFrom(clazz));
        return (Class<? extends T>) clazz;
      } catch (ClassNotFoundException e) {
        throw UserException.unsupportedError(e)
            .message(
                "Failure while attempting to find implementation class %s for interface  %s. The sabot config key is %s ",
                defaultImpl.getName(), iface.getName(), path)
            .build(logger);
      }
    }

    return defaultImpl;
  }

  @SuppressWarnings("unchecked")
  private <T> T instantiate(Class<T> iface, Class<?> clazz, Object... constructorArgs)
      throws ReflectiveOperationException {
    Preconditions.checkArgument(iface.isAssignableFrom(clazz));
    Class<?>[] argClasses = new Class[constructorArgs.length];
    for (int i = 0; i < constructorArgs.length; i++) {
      argClasses[i] = constructorArgs[i].getClass();
    }
    Constructor<?> constructor = clazz.getConstructor(argClasses);
    return (T) constructor.newInstance(constructorArgs);
  }

  @SuppressWarnings("unchecked")
  private <T> T instantiateWithConstructorArgType(
      Class<T> iface, Class<?> clazz, Class<?> constructorArgClass, Object... constructorArgs)
      throws ReflectiveOperationException {
    Preconditions.checkArgument(iface.isAssignableFrom(clazz));
    Class<?>[] argClasses = new Class[constructorArgs.length];
    for (int i = 0; i < constructorArgs.length; i++) {
      argClasses[i] = constructorArgClass;
    }
    Constructor<?> constructor = clazz.getConstructor(argClasses);
    return (T) constructor.newInstance(constructorArgs);
  }

  /**
   * Creates a SabotConfig object using the default config file name and with server-specific
   * configuration options enabled.
   *
   * @return The new SabotConfig object.
   */
  public static SabotConfig create() {
    return create(null, null);
  }

  /**
   * Creates a {@link SabotConfig configuration} using the default config file name and with
   * server-specific configuration options disabled.
   *
   * @return {@link SabotConfig} instance
   */
  public static SabotConfig forClient() {
    return create(null, null);
  }

  /**
   * SabotConfig loads up Sabot configuration information. It does this utilizing a combination of
   * classpath scanning and Configuration fallbacks provided by the TypeSafe configuration library.
   * The order of precedence is as follows:
   *
   * <p>Configuration values are retrieved as follows:
   *
   * <ul>
   *   <li>Check a single copy of "sabot-override.conf". If multiple copies are on the classpath,
   *       which copy is read is indeterminate. If a non-null value for overrideFileResourcePathname
   *       is provided, this is used instead of "{@code sabot-override.conf}".
   *   <li>Check all copies of "{@code sabot-module.conf}". Loading order is indeterminate.
   *   <li>Check a single copy of "{@code sabot-default.conf}". If multiple copies are on the
   *       classpath, which copy is read is indeterminate.
   * </ul>
   *
   * @param overrideFileResourcePathname the classpath resource pathname of the file to use for
   *     configuration override purposes; {@code null} specifies to use the default pathname ({@link
   *     CommonConstants.CONFIG_OVERRIDE}) (does <strong>not</strong> specify to suppress trying to
   *     load an overrides file)
   * @return A merged Config object.
   */
  public static SabotConfig create(String overrideFileResourcePathname) {
    return create(overrideFileResourcePathname, null);
  }

  /** <b><u>Do not use this method outside of test code.</u></b> */
  @VisibleForTesting
  public static SabotConfig create(Properties testConfigurations) {
    return create(null, testConfigurations);
  }

  /**
   * @param overrideFileResourcePathname see {@link #create(String)}'s {@code
   *     overrideFileResourcePathname}
   * @param overriderProps optional property map for further overriding (after override file is
   *     assimilated
   * @return
   */
  private static SabotConfig create(
      String overrideFileResourcePathname, final Properties overriderProps) {
    final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(SabotConfig.class.getClassLoader());
    try {
      return doCreate(overrideFileResourcePathname, overriderProps);
    } finally {
      // Restore to the original context classloader.
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }
  }

  private static SabotConfig createFromSavedSabotConfig() {
    final String savedSabotConfigFile = System.getProperty("com.dremio.savedSabotConfig");
    if (savedSabotConfigFile == null) {
      return null;
    }

    final File savedSabotConfig = new File(savedSabotConfigFile);
    if (savedSabotConfig.exists()) {
      try {
        Config config = ConfigFactory.parseFile(savedSabotConfig);
        return new SabotConfig(config);
      } catch (ConfigException e) {
        logger.warn(
            "Unable to read saved SabotConfig from '{}' (proceeding to slow path): {}",
            savedSabotConfigFile,
            e.toString());
      }
    }

    return null;
  }

  /**
   * @param overrideFileResourcePathname see {@link #create(String)}'s {@code
   *     overrideFileResourcePathname}
   * @param overriderProps optional property map for further overriding (after override file is
   *     assimilated
   * @return
   */
  private static SabotConfig doCreate(
      String overrideFileResourcePathname, Properties overriderProps) {
    final StringBuilder logString = new StringBuilder();
    final Stopwatch watch = Stopwatch.createStarted();
    overrideFileResourcePathname =
        overrideFileResourcePathname == null
            ? CommonConstants.CONFIG_OVERRIDE_RESOURCE_PATHNAME
            : overrideFileResourcePathname;

    final SabotConfig preCreated = createFromSavedSabotConfig();
    if (preCreated != null) {
      return preCreated;
    }

    // 1. Load defaults configuration file.
    Config fallback = null;
    final ClassLoader[] classLoaders = ClasspathHelper.classLoaders();
    for (ClassLoader classLoader : classLoaders) {
      final URL url = classLoader.getResource(CommonConstants.CONFIG_DEFAULT_RESOURCE_PATHNAME);
      if (null != url) {
        logString.append("Base Configuration:\n\t- ").append(url).append("\n");
        fallback =
            ConfigFactory.load(classLoader, CommonConstants.CONFIG_DEFAULT_RESOURCE_PATHNAME);
        break;
      }
    }

    // 2. Load per-module configuration files.
    final Collection<URL> urls = ClassPathScanner.getConfigURLs();
    logString.append("Intermediate Configuration and Plugin files, in order of precedence:");
    for (URL url : urls) {
      logString.append("\n\t- ").append(url);
      fallback = ConfigFactory.parseURL(url).withFallback(fallback);
    }

    // 3. Load any specified overrides configuration file along with any
    //    overrides from JVM system properties (e.g., {-Dname=value").

    // (Per ConfigFactory.load(...)'s mention of using Thread.getContextClassLoader():)
    final URL overrideFileUrl =
        Thread.currentThread().getContextClassLoader().getResource(overrideFileResourcePathname);
    if (null != overrideFileUrl) {
      logString.append("\nOverride File: ").append(overrideFileUrl);
    }
    Config effectiveConfig =
        ConfigFactory.load(overrideFileResourcePathname).withFallback(fallback);

    // 4. Apply any overriding properties.
    if (overriderProps != null) {
      logString.append("\nOverridden Properties:");
      for (Entry<Object, Object> entry : overriderProps.entrySet()) {
        logString.append("\n\t- ").append(entry.getKey()).append(" = ").append(entry.getValue());
      }
      effectiveConfig = ConfigFactory.parseProperties(overriderProps).withFallback(effectiveConfig);
    }

    // 5. Create SabotConfig object from Config object.
    logger.info(
        "Configuration and plugin file(s) identified in {}ms.\n{}",
        watch.elapsed(TimeUnit.MILLISECONDS),
        logString);
    return new SabotConfig(effectiveConfig.resolve());
  }

  public <T> Class<? extends T> getClassAt(String location, Class<T> clazz)
      throws SabotConfigurationException {
    Class<? extends T> c = getClassAt(location, clazz, null);

    if (c != null) {
      return c;
    }

    throw new SabotConfigurationException(
        String.format(
            "No class defined at location '%s'. Expected a definition of the class [%s]",
            location, clazz.getCanonicalName()));
  }

  public <T> Class<? extends T> getClassAt(
      String location, Class<T> clazz, Class<? extends T> defaultClazz)
      throws SabotConfigurationException {
    final String className = getString(location);
    if (className == null) {
      return defaultClazz;
    }

    try {
      final Class<?> c = Class.forName(className);
      if (clazz.isAssignableFrom(c)) {
        @SuppressWarnings("unchecked")
        final Class<T> t = (Class<T>) c;
        return t;
      }

      throw new SabotConfigurationException(
          String.format(
              "The class [%s] listed at location '%s' should be of type [%s].  It isn't.",
              className, location, clazz.getCanonicalName()));
    } catch (Exception ex) {
      if (ex instanceof SabotConfigurationException) {
        throw (SabotConfigurationException) ex;
      }
      throw new SabotConfigurationException(
          String.format(
              "Failure while initializing class [%s] described at configuration value '%s'.",
              className, location),
          ex);
    }
  }

  public <T> T getInstanceOf(String location, Class<T> clazz) throws SabotConfigurationException {
    final Class<? extends T> c = getClassAt(location, clazz);
    try {
      return c.getDeclaredConstructor().newInstance();
    } catch (Exception ex) {
      throw new SabotConfigurationException(
          String.format(
              "Failure while instantiating class [%s] located at '%s.",
              clazz.getCanonicalName(), location),
          ex);
    }
  }

  @Override
  public SabotConfig withValue(String path, ConfigValue value) {
    return new SabotConfig(getInnerConfig().withValue(path, value));
  }

  @Override
  public String toString() {
    return this.root().render();
  }
}
