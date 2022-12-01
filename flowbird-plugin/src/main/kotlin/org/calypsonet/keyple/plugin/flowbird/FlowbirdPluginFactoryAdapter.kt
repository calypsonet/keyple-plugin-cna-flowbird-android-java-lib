/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.plugin.flowbird

import android.app.Activity
import org.calypsonet.keyple.plugin.flowbird.reader.FlowbirdReader
import org.eclipse.keyple.core.common.CommonApiProperties
import org.eclipse.keyple.core.plugin.PluginApiProperties
import org.eclipse.keyple.core.plugin.ReaderIOException
import org.eclipse.keyple.core.plugin.spi.PluginFactorySpi
import org.eclipse.keyple.core.plugin.spi.PluginSpi

internal class FlowbirdPluginFactoryAdapter internal constructor() :
    FlowbirdPluginFactory, PluginFactorySpi {

  private lateinit var pluginAdapter: FlowbirdPluginAdapter

  @Throws(ReaderIOException::class)
  suspend fun init(
      activity: Activity,
      mediaFiles: List<String>,
      situationFiles: List<String>,
      translationFiles: List<String>
  ): FlowbirdPluginFactoryAdapter {
    val started =
        FlowbirdReader.initReader(
            context = activity,
            mediaFiles = mediaFiles,
            situationFiles = situationFiles,
            translationFiles = translationFiles)

    this.pluginAdapter = FlowbirdPluginAdapter(activity)

    return if (started) {
      this
    } else {
      throw ReaderIOException("Could not init Flowbird Adapter")
    }
  }

  override fun getPluginName(): String = FlowbirdPlugin.PLUGIN_NAME

  override fun getPlugin(): PluginSpi = pluginAdapter

  override fun getCommonApiVersion(): String = CommonApiProperties.VERSION

  override fun getPluginApiVersion(): String = PluginApiProperties.VERSION
}
