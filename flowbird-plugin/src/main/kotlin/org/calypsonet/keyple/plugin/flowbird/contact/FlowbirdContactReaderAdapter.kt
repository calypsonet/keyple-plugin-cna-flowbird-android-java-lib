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
package org.calypsonet.keyple.plugin.flowbird.contact

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.plugin.flowbird.reader.FlowbirdReader
import org.calypsonet.keyple.plugin.flowbird.utils.BackgroundThreadExecutor
import org.calypsonet.keyple.plugin.flowbird.utils.suspendCoroutineWithTimeout
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi
import org.eclipse.keyple.core.util.HexUtil
import timber.log.Timber

/** Keyple SE Reader's Implementation for the Flowbird (SAM access) reader */
@Suppress("INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_WARNING")
@ExperimentalCoroutinesApi
internal class FlowbirdContactReaderAdapter(private val samSlot: SamSlot) :
    ReaderSpi, FlowbirdContactReader {

  private var reader = FlowbirdReader.getInstance()

  var channelResponseApdu: Channel<ByteArray?>? = null

  var startApduTime: Long? = null

  /** This property is used to force 'compute protocol' by Keyple library */
  private var physicalChannelOpen = false

  /** @see ReaderSpi.transmitApdu */
  @Throws(IllegalStateException::class)
  override fun transmitApdu(apduIn: ByteArray?): ByteArray? {
    var apduResponse: ByteArray? = byteArrayOf()

    if (BackgroundThreadExecutor.isUiThread) {
      throw IllegalStateException("APDU exchange must NOT be done on main UI thread")
    }

    try {
      Timber.d("SAM-APDU - transmitApdu apduIn : ${HexUtil.toHex(apduIn)}")
      runBlocking { apduResponse = executeApduAsync(apduIn) }
    } catch (e: Exception) {
      Timber.e("SAM-APDU - transmitApdu error $e")
      throw IllegalStateException(e)
    }

    Timber.d("SAM-APDU - transmitApdu apduResponse : ${HexUtil.toHex(apduResponse)}")
    return apduResponse
  }

  private suspend fun executeApduAsync(apduIn: ByteArray?): ByteArray? {
    if (apduIn == null) {
      return ByteArray(0)
    }

    channelResponseApdu = Channel(Channel.UNLIMITED)

    return suspendCoroutineWithTimeout(APDU_TIMEOUT) { continuation ->
      val handler =
          CoroutineExceptionHandler { _, exception ->
            Timber.e("error SAM connection : $exception")
            handleResponseApduResult(
                result = null, throwable = exception, continuation = continuation)
          }

      launchApdu(apduIn = apduIn, handler = handler, continuation = continuation)
    }
  }

  private fun launchApdu(
      apduIn: ByteArray,
      handler: CoroutineExceptionHandler,
      continuation: CancellableContinuation<ByteArray>
  ) {
    GlobalScope.launch(handler) {
      withContext(Dispatchers.IO) {
        startApduTime = System.currentTimeMillis()
        reader.sendCommandToSam(
            command = apduIn, samSlot = samSlot, channelResponseApdu = channelResponseApdu)

        for (resultApdu in channelResponseApdu!!) {
          val duration = System.currentTimeMillis() - startApduTime!!
          Timber.i("FLOW-SAM-APDU - APDU duration : $duration ms")
          handleResponseApduResult(
              result = resultApdu, throwable = null, continuation = continuation)
        }
      }
    }
  }

  private fun handleResponseApduResult(
      result: ByteArray?,
      throwable: Throwable?,
      continuation: CancellableContinuation<ByteArray>
  ) {
    if (continuation.isActive) {
      channelResponseApdu?.close()
      channelResponseApdu = null

      result?.let { continuation.resume(it) }
      throwable?.let { continuation.resumeWithException(it) }
    }
  }

  /** @see ReaderSpi.getPowerOnData */
  override fun getPowerOnData(): String {
    return HexUtil.toHex(FlowbirdReader.atrContainer[samSlot])
  }

  /** @see ReaderSpi.closePhysicalChannel */
  @Throws(IllegalStateException::class)
  override fun closePhysicalChannel() {
    physicalChannelOpen = false
  }

  /** @see ReaderSpi.openPhysicalChannel */
  override fun openPhysicalChannel() {
    physicalChannelOpen = true
  }

  /** @see ReaderSpi.isPhysicalChannelOpen */
  override fun isPhysicalChannelOpen(): Boolean {
    return physicalChannelOpen
  }

  /** @see ReaderSpi.checkCardPresence */
  @Suppress("TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING")
  override fun checkCardPresence(): Boolean {
    val readerInstanceInitDone = FlowbirdReader.getInstance().readerInstance != null
    val atrExists = FlowbirdReader.atrContainer[samSlot]?.isNotEmpty() ?: false
    return atrExists && readerInstanceInitDone
  }

  /** @see ReaderSpi.isContactless */
  override fun isContactless(): Boolean {
    return false
  }

  /** @see ReaderSpi.onUnregister */
  override fun onUnregister() {
    // Do nothing
  }

  /** @see ReaderSpi.getName */
  override fun getName(): String = "${FlowbirdContactReader.READER_NAME}_${samSlot.slotId}"

  companion object {
    private const val APDU_TIMEOUT: Long = 1000
  }
}
