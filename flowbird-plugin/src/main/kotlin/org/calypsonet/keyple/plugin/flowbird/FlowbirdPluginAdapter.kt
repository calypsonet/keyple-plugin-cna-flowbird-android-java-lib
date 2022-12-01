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

import android.content.Context
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.calypsonet.keyple.plugin.flowbird.contact.FlowbirdContactReaderAdapter
import org.calypsonet.keyple.plugin.flowbird.contact.SamSlot
import org.calypsonet.keyple.plugin.flowbird.contactless.FlowbirdContactlessReaderAdapter
import org.calypsonet.keyple.plugin.flowbird.reader.FlowbirdReader
import org.eclipse.keyple.core.plugin.spi.ObservablePluginSpi
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi

/** Handle native Readers mapped for Keyple */
internal class FlowbirdPluginAdapter(context: Context) : ObservablePluginSpi, FlowbirdPlugin {

  companion object {
    private const val MONITORING_CYCLE_DURATION_MS = 1000
  }

  private var weakReferenceContext: WeakReference<Context?>

  init {
    weakReferenceContext = WeakReference(context)
  }

  private lateinit var seReaders: ConcurrentHashMap<String, ReaderSpi>

  @ExperimentalCoroutinesApi
  override fun searchAvailableReaders(): MutableSet<ReaderSpi> {

    seReaders = ConcurrentHashMap()

    val sam1 = FlowbirdContactReaderAdapter(SamSlot.ONE)
    seReaders[sam1.name] = sam1

    val sam2 = FlowbirdContactReaderAdapter(SamSlot.TWO)
    seReaders[sam2.name] = sam2

    val sam3 = FlowbirdContactReaderAdapter(SamSlot.THREE)
    seReaders[sam3.name] = sam3

    val sam4 = FlowbirdContactReaderAdapter(SamSlot.FOUR)
    seReaders[sam4.name] = sam4

    val nfc = FlowbirdContactlessReaderAdapter(weakReferenceContext.get()!!)
    seReaders[nfc.name] = nfc

    return seReaders.map { it.value }.toMutableSet()
  }

  override fun searchReader(readerName: String?): ReaderSpi? {
    return if (seReaders.containsKey(readerName)) {
      seReaders[readerName]!!
    } else {
      null
    }
  }

  override fun searchAvailableReaderNames(): MutableSet<String> {
    return seReaders.map { it.key }.toMutableSet()
  }

  override fun getMonitoringCycleDuration(): Int {
    return MONITORING_CYCLE_DURATION_MS
  }

  override fun getName(): String = FlowbirdPlugin.PLUGIN_NAME

  override fun onUnregister() {
    FlowbirdReader.clearInstance()
    weakReferenceContext = WeakReference(null)
  }
}
