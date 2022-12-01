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
package org.calypsonet.keyple.plugin.flowbird.state

import android.os.Bundle
import android.os.RemoteException
import com.parkeon.services.hunt.HuntConstants
import com.parkeon.services.hunt.HuntEventListener
import com.parkeon.services.hunt.HuntInterface
import com.parkeon.utils.BundleUtils
import com.parkeon.utils.Log
import com.parkeon.utils.StringUtils
import org.calypsonet.keyple.plugin.flowbird.reader.CardRemoved
import org.calypsonet.keyple.plugin.flowbird.reader.HuntEventResult
import org.calypsonet.keyple.plugin.flowbird.reader.Tag
import org.calypsonet.keyple.plugin.flowbird.reader.TagResultSuccess
import timber.log.Timber

class MyHuntEventListener : HuntEventListener.Stub() {

  private var mListener: ((HuntEventResult<Tag>) -> Unit)? = null

  private var id = javaClass::class.java.simpleName

  var usePeripheralResult = false
  fun add(hunt: HuntInterface) {
    try {
      hunt.addEventListener(this)
    } catch (ex: Exception) {
      Log.e("Failure:", ex)
    }
  }

  private fun dumpBundle(bundle: Bundle?) {
    Timber.d("MyHuntEventListener.dumpBundle")
    if (bundle != null) {
      if (bundle.size() > 0) {
        val keys = bundle.keySet()
        val it: Iterator<*> = keys.iterator()
        while (it.hasNext()) {
          val key = it.next() as String
          if (bundle[key] != null) {
            if (bundle[key] is Bundle) {
              Timber.d(
                  String.format("    [%s=%s]", key, BundleUtils.toString(bundle[key] as Bundle?)))
            } else {
              Timber.d(String.format("    [%s=%s]", key, bundle[key].toString()))
            }
          } else {
            Timber.d(String.format("    [%s=nulls]", key))
          }
        }
      } else {
        Timber.e("Empty bundle.")
        Log.e("BundleUtils", "Empty bundle.")
      }
    } else {
      Timber.e("Null bundle.")
      Log.e("BundleUtils", "Null bundle.")
    }
  }

  override fun onDetected(data: Bundle) {
    Timber.i("Got onDetected.")
    dumpBundle(data)
    var atr: String? = ""
    val readableData: String?
    val customerType = data.getString(HuntConstants.TAG_TYPE)
    val peripheral = data.getString(HuntConstants.TAG_PERIPHERAL)
    usePeripheralResult = false
    var cardId: Long? = null
    if (HuntConstants.TYPE_CLESS == customerType) {
      atr = data.getString("atr")
      cardId = data.getLong("id")
      readableData = atr!!.substring(14)
    } else if (HuntConstants.TYPE_BARCODE == customerType) {
      atr = data.getString("data")
      readableData = atr
    } else if (HuntConstants.TYPE_MAGNETIC == customerType) {
      if (HuntConstants.PERIPHERAL_DDM == peripheral) {
        atr = StringUtils.bytesToHexString(data.getByteArray("track"))
        readableData = atr
      } else {
        atr = data.getString("track")
        readableData = atr
      }
    } else if ("OPENPAY" == customerType) {
      usePeripheralResult = true
      atr = "*****"
      readableData = "0123456789ABCD0123456789ABC"
    } else {
      readableData = ""
    }

    mListener?.let {
      val tag =
          Tag(
              cardId = cardId,
              customerType = customerType,
              atr = atr,
              peripheral = peripheral,
              readableData = readableData)
      it(TagResultSuccess(tag))
    }
  }

  override fun onError(data: Bundle) {
    Timber.i("Got onError, wait a bit an restart.")
    try {
      Thread.sleep(500)
    } catch (ex: Exception) {}
  }

  override fun onRemoved(data: Bundle) {
    Timber.i("Got onRemoved.")
    mListener?.let { it(CardRemoved()) }
  }

  @Throws(RemoteException::class)
  override fun getListenerId(): String {
    return id
  }

  fun setListener(listener: ((HuntEventResult<Tag>) -> Unit)?) {
    mListener = listener
  }

  companion object {
    const val HUNT_MODE_COMPETITION = "COMPETITION"
    const val HUNT_MODE_ALTERNATE = "ALTERNATE"
  }
}
