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
package org.calypsonet.keyple.plugin.flowbird.utils

import android.content.Context
import com.parkeon.app.data.ApplicationResources
import com.parkeon.utils.FileUtils
import com.parkeon.utils.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import timber.log.Timber

internal object FileUtils {

  /** Deploy media resources from app's assets to device internal memory */
  internal fun deployResourcesFiles(
      context: Context,
      mediaFiles: List<String>,
      situationFiles: List<String>,
      translationFiles: List<String>
  ) {
    val mediaSubDir = ApplicationResources.MEDIA_RESOURCES_ASCENDING_ORDER[0]
    val situationSubDir = ApplicationResources.SITUATION_RESOURCES_ASCENDING_ORDER[0]
    val translationSubDir = ApplicationResources.TRANSLATION_RESOURCES_ASCENDING_ORDER[0]

    var root = File(context.filesDir, mediaSubDir)
    var list = root.list()
    if (list == null || list.isEmpty()) {
      Timber.d("Deploy 'Media' resources from assets")
      mediaFiles.forEach {
        deployFileFromAssetsToDirectory(context, "$mediaSubDir/$it", mediaSubDir)
      }
    }

    root = File(context.filesDir, situationSubDir)
    list = root.list()
    if (list == null || list.isEmpty()) {
      Timber.d("Deploy 'Situation' resources from assets")
      situationFiles.forEach {
        deployFileFromAssetsToDirectory(context, "$situationSubDir/$it", situationSubDir)
      }
    }

    root = File(context.filesDir, translationSubDir)
    list = root.list()
    if (list == null || list.isEmpty()) {
      Timber.d("Deploy 'Translation' resources from assets")
      translationFiles.forEach {
        deployFileFromAssetsToDirectory(context, "$translationSubDir/$it", translationSubDir)
      }
    }
  }

  private fun deployFileFromAssetsToDirectory(context: Context, file: String, directory: String) {
    val assets = context.resources.assets
    val baseDir = context.filesDir
    val targetDir = File(baseDir, directory)
    targetDir.mkdirs()

    try {
      val f = File(file)
      var fileDir = f.parent
      if (fileDir == null) {
        fileDir = ""
      }
      val filename = f.name
      val files = assets.list(fileDir)
      for (i in files!!.indices) {
        if (files[i] == filename) {
          Log.d("FileUtils.TAG", "Deploying file " + files[i] + " to " + targetDir)
          val newFile = File(targetDir, files[i])
          val out = FileOutputStream(newFile)
          val filePath = fileDir + "/" + files[i]
          val `in` = assets.open(filePath, 3)
          FileUtils.copyFile(`in`, out as OutputStream)
          out.flush()
          out.close()
          `in`.close()
        }
      }
    } catch (e: IOException) {
      Log.e("Exception while listing files", e)
    }
  }
}
