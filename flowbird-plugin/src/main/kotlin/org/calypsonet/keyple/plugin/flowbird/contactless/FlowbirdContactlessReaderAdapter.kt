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
package org.calypsonet.keyple.plugin.flowbird.contactless

import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import com.parkeon.data.ConfigurationHelper
import com.parkeon.data.StateHelper
import com.parkeon.data.StateObserver
import com.parkeon.services.hunt.AlternateHunter
import com.parkeon.services.hunt.CompetitionHunter
import com.parkeon.services.hunt.HuntInterface
import com.parkeon.utils.Log
import com.parkeon.utils.StringUtils
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.plugin.flowbird.Constants.HUNTER_NAME
import org.calypsonet.keyple.plugin.flowbird.FlowbirdUiManager
import org.calypsonet.keyple.plugin.flowbird.reader.CardRemoved
import org.calypsonet.keyple.plugin.flowbird.reader.FlowbirdReader
import org.calypsonet.keyple.plugin.flowbird.reader.HuntEventResult
import org.calypsonet.keyple.plugin.flowbird.reader.Tag
import org.calypsonet.keyple.plugin.flowbird.reader.TagResultError
import org.calypsonet.keyple.plugin.flowbird.reader.TagResultSuccess
import org.calypsonet.keyple.plugin.flowbird.state.MyHuntEventListener
import org.calypsonet.keyple.plugin.flowbird.utils.BackgroundThreadExecutor
import org.calypsonet.keyple.plugin.flowbird.utils.suspendCoroutineWithTimeout
import org.eclipse.keyple.core.plugin.ReaderIOException
import org.eclipse.keyple.core.plugin.WaitForCardInsertionAutonomousReaderApi
import org.eclipse.keyple.core.plugin.WaitForCardRemovalAutonomousReaderApi
import org.eclipse.keyple.core.plugin.spi.reader.ConfigurableReaderSpi
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.WaitForCardInsertionAutonomousSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.DontWaitForCardRemovalDuringProcessingSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.WaitForCardRemovalAutonomousSpi
import org.eclipse.keyple.core.util.HexUtil
import timber.log.Timber

internal class FlowbirdContactlessReaderAdapter(context: Context) :
    ObservableReaderSpi,
    ConfigurableReaderSpi,
    FlowbirdContactlessReader,
    DontWaitForCardRemovalDuringProcessingSpi,
    WaitForCardRemovalAutonomousSpi,
    WaitForCardInsertionAutonomousSpi {

  private var waitForCardInsertionAutonomousApi: WaitForCardInsertionAutonomousReaderApi? = null
  private var waitForCardRemovalAutonomousReaderApi: WaitForCardRemovalAutonomousReaderApi? = null

  private var stateHelper: StateHelper
  private var configHelper: ConfigurationHelper
  private var stateObs: StateObserver? = null

  private val isCardDiscovered = AtomicBoolean(false)

  private var lastScanTimestamp: Long = 0
  private var lastTagData: String? = null

  private var flowbirdReader = FlowbirdReader.getInstance()

  var channelResponseApdu: Channel<ByteArray?>? = null

  private val protocols =
      listOf(
          FlowbirdSupportContactlessProtocols.ALL,
          FlowbirdSupportContactlessProtocols.A,
          FlowbirdSupportContactlessProtocols.B)

  private var currentTag: Tag? = null
  private var currentProtocol: FlowbirdSupportContactlessProtocols? = null
  private var isConnected = false

  private var hunter: HuntInterface? = null
  private var huntEventListener = MyHuntEventListener()

  init {
    restart(false)

    configHelper = ConfigurationHelper(context)
    stateHelper = StateHelper(context)
    stateObs =
        StateObserver(context, CONFIG_ACTIVE_PROTOCOL_KEY) { _: String?, _: String? ->
          currentProtocol?.let {
            val currentConfigHuntMode = configHelper.getAsString(CONFIG_CURRENT_PROTOCOL_KEY)
            if (it.key == currentConfigHuntMode) {
              restart(true)
            }
          }
        }

    stateObs!!.enable()
  }

  private fun handleTagResult(huntEventResult: HuntEventResult<Tag>) {
    if (huntEventResult is TagResultSuccess) {
      onTagDiscovered(huntEventResult.data)
    } else if (huntEventResult is TagResultError) {
      throw IllegalStateException(
          huntEventResult.error.message ?: "onStartDetection - Error when starting NFC detection")
    } else if (huntEventResult is CardRemoved) {
      waitForCardRemovalAutonomousReaderApi?.onCardRemoved()
      /*
       * Relaunch card detection
       */
      restart(false)
    }
  }

  /** Relaunch card detection */
  private fun restart(startCardDetection: Boolean) {
    configureService {
      Timber.d("Detected hunt result : $it")
      handleTagResult(it)
    }
    if (startCardDetection) {
      onStartDetection()
    }
  }

  private fun destroyHunter(hunter: HuntInterface?) {
    if (hunter is AlternateHunter) {
      hunter.destroy()
    }
  }

  private fun configureService(listener: ((HuntEventResult<Tag>) -> Unit)?) {
    stopHunt()
    hunter?.let {
      try {
        destroyHunter(it)
      } catch (ex: java.lang.Exception) {
        Timber.e("Stop hunter failure, ${ex.message}")
      }
    }
    hunter = CompetitionHunter()
    setEventListener(listener)
  }

  private fun startHunt() {
    try {
      Timber.d("Start Hunt")
      configureCompetitiveHunter()
      hunter!!.startDetection(Bundle())
    } catch (ex: RemoteException) {
      Timber.e("Start hunter failure, ${ex.message}")
    }
  }

  private fun stopHunt() {
    try {
      Timber.d("Stop Hunt")
      hunter?.let {
        try {
          it.stopDetection()
        } catch (ex: java.lang.Exception) {
          Timber.e("Stop hunter failure, ${ex.message}")
        }
      }
    } catch (ex: RemoteException) {
      Timber.e(ex)
    }
  }

  private fun setEventListener(listener: ((HuntEventResult<Tag>) -> Unit)?) {
    try {
      huntEventListener.setListener(listener)
      hunter?.addEventListener(huntEventListener)
    } catch (ex: java.lang.Exception) {
      Log.e("Failure", ex)
    }
  }

  /**
   * Configure an Alternate hunter. Alternate hunt between many hunters
   * @see AlternateHunter
   */
  @Suppress("unused")
  private fun configureAlternateHunter() {
    val realHunter = hunter as AlternateHunter
    realHunter.reset()
    realHunter.addHunter(
        HUNTER_NAME,
        HuntInterface.Stub.asInterface(flowbirdReader.getService(HUNTER_NAME)),
        ALTERNATE_DURATION)
    Timber.i("Configured alternate hunter: $realHunter")
  }

  /**
   * Configure an Competition hunter. Define an object CompetitionHunter to manage many hunters
   * @see CompetitionHunter
   */
  private fun configureCompetitiveHunter() {
    try {
      val cardsBinder = flowbirdReader.getService(HUNTER_NAME)
      val realHunter = hunter as CompetitionHunter
      val hunters: MutableMap<String, HuntInterface?> = HashMap()
      hunters[HUNTER_NAME] = HuntInterface.Stub.asInterface(cardsBinder)

      // Configure hunter from map
      realHunter.reset()
      realHunter.setHunters(hunters)
      Timber.i("Configured competition hunter with ${StringUtils.toString(hunters.keys)}")
    } catch (ex: java.lang.Exception) {
      Timber.e(ex)
    }
  }

  /**
   * Callback function invoked by @ [FlowbirdReader] when a @ [Tag] is discovered Do not call this
   * function directly.
   *
   * @param tag : detected tag
   */
  private fun onTagDiscovered(tag: Tag?) {
    Timber.i("Received Tag Discovered event $tag")
    tag?.let {
      try {
        Timber.i("Getting tag data")

        this.currentTag = tag
        lastScanTimestamp = System.currentTimeMillis()

        lastTagData = tag.readableData

        isCardDiscovered.set(true)
        waitForCardInsertionAutonomousApi?.onCardInserted()
      } catch (e: ReaderIOException) {
        Timber.e(e)
      } catch (e: Exception) {
        Timber.e(e)
      }
    }
  }

  /** Method called when the card detection is started by the Keyple Plugin */
  override fun onStartDetection() {
    startHunt()
    FlowbirdUiManager.displayWaiting()
  }

  /** Method called when the card detection is stopped by the Keyple Plugin */
  override fun onStopDetection() {
    stopHunt()
  }

  /** @see ReaderSpi.isCurrentProtocol */
  override fun isCurrentProtocol(readerProtocolName: String?): Boolean {
    return currentProtocol?.let { it.key == readerProtocolName } ?: false
  }

  /** @see ReaderSpi.checkCardPresence */
  override fun checkCardPresence(): Boolean {
    return currentTag != null
  }

  override fun getName(): String = FlowbirdContactlessReader.READER_NAME

  /** @see ReaderSpi.isProtocolSupported */
  override fun isProtocolSupported(readerProtocol: String?): Boolean {
    val flowbirdReaderProtocol = FlowbirdSupportContactlessProtocols.findEnumByKey(readerProtocol!!)
    return !readerProtocol.isNullOrEmpty() && protocols.contains(flowbirdReaderProtocol)
  }

  /** @see ReaderSpi.activateProtocol */
  override fun activateProtocol(readerProtocol: String?) {
    readerProtocol?.let {
      val protocol = FlowbirdSupportContactlessProtocols.findEnumByKey(it)
      if (protocols.contains(protocol)) {
        configHelper.set(CONFIG_CURRENT_PROTOCOL_KEY, protocol.key)
        currentProtocol = protocol
      } else {
        throw IllegalArgumentException(
            "activateReaderProtocol - Activate protocol error : not allowed")
      }
    }
        ?: throw IllegalArgumentException(
            "activateReaderProtocol - Activate protocol error : null protocol")
  }

  /** @see ReaderSpi.deactivateProtocol */
  override fun deactivateProtocol(readerProtocol: String?) {
    // Do nothing
  }

  /** @see ReaderSpi.transmitApdu */
  override fun transmitApdu(apduIn: ByteArray?): ByteArray {
    if (apduIn == null) {
      return ByteArray(0)
    }

    var apduResponse: ByteArray? = byteArrayOf()

    if (BackgroundThreadExecutor.isUiThread) {
      throw IllegalStateException("APDU exchange must NOT be done on main UI thread")
    }

    try {
      Timber.d("FLOW-APDU - transmitApdu apduIn : ${HexUtil.toHex(apduIn)}")
      runBlocking { apduResponse = executeApduAsync(apduIn) }
    } catch (e: Exception) {
      Timber.e("FLOW-APDU - transmitApdu error $e")
      throw IllegalStateException(e)
    }

    Timber.d("FLOW-APDU - transmitApdu apduResponse : ${HexUtil.toHex(apduResponse)}")
    return apduResponse ?: ByteArray(0)
  }

  private suspend fun executeApduAsync(apduIn: ByteArray?): ByteArray? {
    if (apduIn == null) {
      return ByteArray(0)
    }
    channelResponseApdu = Channel(Channel.UNLIMITED)

    return suspendCoroutineWithTimeout(APDU_TIMEOUT) { continuation ->
      val handler =
          CoroutineExceptionHandler { _, exception ->
            Timber.e("error APDU connection : $exception")
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
        val startApduTime = System.currentTimeMillis()
        flowbirdReader.sendCommandToCard(
            command = apduIn,
            cardId = currentTag?.cardId ?: 0,
            channelResponseApdu = channelResponseApdu)

        for (resultApdu in channelResponseApdu!!) {
          val duration = System.currentTimeMillis() - startApduTime
          Timber.i("FLOW-APDU - APDU duration : $duration ms")
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

  /** @see ReaderSpi.isContactless */
  override fun isContactless(): Boolean {
    return true
  }

  /** @see ReaderSpi.getPowerOnData */
  override fun getPowerOnData(): String {
    return ""
  }

  /** @see ReaderSpi.openPhysicalChannel */
  override fun openPhysicalChannel() {
    isConnected = true
  }

  /** @see ReaderSpi.isPhysicalChannelOpen */
  override fun isPhysicalChannelOpen(): Boolean {
    return isConnected
  }

  /** @see ReaderSpi.closePhysicalChannel */
  override fun closePhysicalChannel() {
    isConnected = false
  }

  //    override fun waitForCardPresent(): Boolean {
  //        return isWaitingForCard.get()
  //    }
  //
  //    override fun stopWaitForCard() {
  //        isWaitingForCard.set(false)
  //    }

  /** @see ReaderSpi.onUnregister */
  override fun onUnregister() {
    stateObs?.disable()
    stateObs = null
    stopHunt()
    huntEventListener.setListener(null)
    hunter?.removeEventListener(huntEventListener)
    destroyHunter(hunter)
    FlowbirdReader.clearInstance()
  }

  /** @see WaitForCardRemovalAutonomousSpi.connect */
  override fun connect(
      waitForCardRemovalAutonomousReaderApi: WaitForCardRemovalAutonomousReaderApi?
  ) {
    this.waitForCardRemovalAutonomousReaderApi = waitForCardRemovalAutonomousReaderApi
  }

  /** @see WaitForCardInsertionAutonomousSpi.connect */
  override fun connect(
      waitForCardInsertionAutonomousReaderApi: WaitForCardInsertionAutonomousReaderApi?
  ) {
    waitForCardInsertionAutonomousApi = waitForCardInsertionAutonomousReaderApi
  }

  companion object {
    private const val APDU_TIMEOUT: Long = 1000
    private const val ALTERNATE_DURATION = 1000
    private const val CONFIG_CURRENT_PROTOCOL_KEY = "/contactless/hunt/pollscript/modes/current"
    private const val CONFIG_ACTIVE_PROTOCOL_KEY = "/contactless/hunt/pollscript/modes/active"
  }
}
