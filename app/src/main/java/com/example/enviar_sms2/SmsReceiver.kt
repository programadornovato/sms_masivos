package com.example.enviar_sms2
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.widget.Toast

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras

        if (bundle != null) {
            val pdus = bundle["pdus"] as Array<*>
            val messages = arrayOfNulls<SmsMessage>(pdus.size)

            for (i in pdus.indices) {
                messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
            }

            for (message in messages) {
                val phoneNumber = message?.originatingAddress
                val smsMessage = message?.messageBody
                Toast.makeText(context, "Número: $phoneNumber, Mensaje: $smsMessage", Toast.LENGTH_LONG).show()
            }
        }
    }
}