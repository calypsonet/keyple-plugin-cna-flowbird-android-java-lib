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

object FlowbirdPluginFactoryProvider {
  suspend fun getFactory(
      activity: Activity,
      mediaFiles: List<String>,
      situationFiles: List<String>,
      translationFiles: List<String>
  ): FlowbirdPluginFactory {
    val pluginFactory = FlowbirdPluginFactoryAdapter()
    pluginFactory.init(
        activity = activity,
        mediaFiles = mediaFiles,
        situationFiles = situationFiles,
        translationFiles = translationFiles)

    return pluginFactory
  }
}
