/*
 * Copyright 2020 FIX Protocol Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package io.fixprotocol.interfaces2md.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public final class LogUtil {

  public static Logger initializeDefaultLogger(Level level, Class<?> clazz) {
    final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    final Configuration config = ctx.getConfiguration();
    final ConsoleAppender appender = ConsoleAppender.newBuilder().setName("Console").build();
    config.addAppender(appender);
    final AppenderRef ref = AppenderRef.createAppenderRef("Console", level, null);
    final AppenderRef[] refs = new AppenderRef[] {ref};
    final LoggerConfig loggerConfig =
        LoggerConfig.createLogger(true, level, clazz.getName(), null, refs, null, config, null);
    config.addLogger(clazz.getName(), loggerConfig);
    ctx.updateLoggers();
    return LogManager.getLogger(clazz);
  }

  public static Logger initializeFileLogger(String fileName, Level level, Class<?> clazz) {
    final ConfigurationBuilder<BuiltConfiguration> builder =
        ConfigurationBuilderFactory.newConfigurationBuilder();
    final AppenderComponentBuilder appenderBuilder =
        builder.newAppender("file", "File").addAttribute("fileName", fileName);
    builder.add(appenderBuilder);
    builder.add(
        builder.newLogger(clazz.getCanonicalName(), level).add(builder.newAppenderRef("file")));
    builder.add(builder.newRootLogger(level).add(builder.newAppenderRef("file")));
    final LoggerContext ctx = Configurator.initialize(builder.build());
    return LogManager.getLogger(clazz);
  }
}
