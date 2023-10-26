/*
package com.example.enviar_sms2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
*/
package com.example.enviar_sms2
import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Random
import android.app.Application
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
class MainActivity : AppCompatActivity() {
    lateinit var evMuestraTels: EditText
    lateinit var phoneNumberEditText:EditText
    lateinit var messageEditText:EditText
    lateinit var sendButton:Button
    lateinit var txtLimite:EditText
    lateinit var txtOffset:EditText
    lateinit var txtHoraIni:EditText
    lateinit var txtHoraFin:EditText
    lateinit var SimTel1:EditText
    lateinit var SimTel2:EditText
    lateinit var etNombreEquipo:EditText
    lateinit var edNumeroNotificar:EditText
    lateinit var radioGroupSim: RadioGroup
    lateinit var radioButtonSim1: RadioButton
    lateinit var radioButtonSim2: RadioButton
    private lateinit var txtTiempoReset: EditText
    private lateinit var txtTeimpoEspera:EditText
    private lateinit var activaReg: CheckBox
    private lateinit var chEnviaSMS: CheckBox
    private lateinit var tvMuestraReg: TextView
    private lateinit var spListaMensajes: Spinner
    private lateinit var tvNotificaHora: TextView
    private lateinit var tvNotifica: TextView
    private var timer: CountDownTimer? = null
    private var contadorReinicia=0;
    private var contador=0;
    private var currentRingtone: Ringtone? = null
    //private lateinit var sharedPref: SharedPreferences

    private var simSubIds = IntArray(2) { -1 }
    companion object {
        const val SMS_PERMISSION_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //MANTENER ACTIVA LA PANTALLA
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        evMuestraTels = findViewById<EditText>(R.id.evMuestraTels)
        evMuestraTels.movementMethod = ScrollingMovementMethod()
        txtLimite = findViewById<EditText>(R.id.txtLimite)
        txtOffset = findViewById<EditText>(R.id.txtOffset)
        txtHoraIni = findViewById<EditText>(R.id.txtHoraIni)
        txtHoraFin = findViewById<EditText>(R.id.txtHoraFin)
        txtTiempoReset = findViewById(R.id.txtTiempoReset)
        txtTeimpoEspera = findViewById<EditText>(R.id.txtTeimpoEspera)
        activaReg = findViewById(R.id.activaReg)
        chEnviaSMS = findViewById(R.id.chEnviaSMS)
        tvMuestraReg = findViewById(R.id.tvMuestraReg)
        SimTel1 = findViewById<EditText>(R.id.SimTel1)
        SimTel2 = findViewById<EditText>(R.id.SimTel2)
        etNombreEquipo = findViewById<EditText>(R.id.etNombreEquipo)
        edNumeroNotificar = findViewById<EditText>(R.id.edNumeroNotificar)
        spListaMensajes = findViewById(R.id.spListaMensajes)
        radioGroupSim = findViewById(R.id.radioGroupSim)
        radioButtonSim1 = findViewById(R.id.radioButtonSim1)
        radioButtonSim2 = findViewById(R.id.radioButtonSim2)
        tvNotificaHora = findViewById<TextView>(R.id.tvNotificaHora)
        tvNotifica = findViewById<TextView>(R.id.tvNotifica)
        llenaSpin()
        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        // 120 es el valor predeterminado en caso de que no haya nada guardado.
        val savedTime = sharedPref.getString("timeReset", "120")
        txtTiempoReset.setText(savedTime)
        val txtTeimpoEsperaShare = sharedPref.getString("txtTeimpoEspera", "10")
        txtTeimpoEspera.setText(txtTeimpoEsperaShare)
        //AL HACER CLICK EN EL CHECK DE LA CUENTA REGRESIVA INICIA LA CUENTA ATRAS PARA ENVIAR MENSAJES
        activaReg.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("timeReset", txtTiempoReset.text.toString())
                    putString("txtTeimpoEspera", txtTeimpoEspera.text.toString())
                    apply()
                }
                startCountDown()
            } else {
                timer?.cancel()
            }
        }
        simSubIds  = checkSIMs()
        if (simSubIds[0] != -1) {
            Toast.makeText(this, "SIM 1 presente con subId ${simSubIds[0]}", Toast.LENGTH_SHORT).show()
        }
        if (simSubIds[1] != -1) {
            Toast.makeText(this, "SIM 2 presente con subId ${simSubIds[1]}", Toast.LENGTH_SHORT).show()
        }
        radioButtonSim1.text = simSubIds[0].toString()
        radioButtonSim2.text = simSubIds[1].toString()
        if (simSubIds[0] != -1) {
            radioButtonSim1.isChecked = true
        } else if (simSubIds[1] != -1) {
            radioButtonSim2.isChecked = true
        } else {
            radioButtonSim1.isEnabled = false
            radioButtonSim2.isEnabled = false
        }
        cargarValoresDeSharedPreferences()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), 1001)
        }
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_CODE)
        }
        val sharedPrefTel = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val savedPhoneNumber = sharedPrefTel.getString("phoneNumber", "")
        cargarValoresSimTel()
        cargarDatosEquipo()
        phoneNumberEditText.setText(savedPhoneNumber)
        spListaMensajes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()

                // Llama a la función que deseas ejecutar
                btnColocarMensajes()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Aquí puedes manejar el caso en el que no se selecciona ningún elemento, si es necesario.
            }
        }
    }
    fun fetchCsvUsingVolley() {
        val url = "https://pastebin.com/raw/tmjCa6gn"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                // Procesar el contenido CSV
                val lines = response.split("\n")
                for (line in lines) {
                    val columns = line.split(",")
                    val id = columns[0]
                    val name = columns[1]
                    val message = columns[2]
                    println("ID: $id, Name: $name, Message: $message")
                }
            },
            { error ->
                println("Error al obtener el CSV: ${error.message}")
            })

        // Añadir la solicitud a la cola
        (applicationContext as MyApp).requestQueue.add(stringRequest)
    }
    private fun startCountDown() {
        val timeMillis = txtTiempoReset.text.toString().toLongOrNull() ?: return
        val timeInMilliSeconds = timeMillis * 1000

        timer = object : CountDownTimer(timeInMilliSeconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                tvMuestraReg.text = "$secondsRemaining"
            }
            override fun onFinish() {
                tvMuestraReg.text = "Tiempo terminado"
                // Reiniciar la cuenta regresiva si el CheckBox aún está marcado
                if (activaReg.isChecked) {
                    //EXTRAEMOS LA HORA ACTUAL
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    //OBTENEMOS LA HORA EN LA QUE ESTA PERMITIDI ENVIAR SMS
                    val horaIni = txtHoraIni.text.toString().toIntOrNull() ?: 0
                    val horaFin = txtHoraFin.text.toString().toIntOrNull() ?: 0
                    //SI LA HORA ACTUAL ESTA FUERA DEL RANGO PERMITIDO DESACTIVAMOS EL CHECK Y DETENEMOS LA CUENTA REGRESIVA
                    if (currentHour !in horaIni..(horaFin-1)) {
                        chEnviaSMS.isChecked = false
                        chEnviaSMS.setBackgroundColor(Color.parseColor("#FF0000"))
                        tvNotificaHora.text="SON LAS $currentHour Y SOLO SE PUEDEN ENVIAR SMS ENTRE $horaIni Y $horaFin"
                        //timer?.cancel()
                        //showAlertError("SON LAS $currentHour Y SOLO SE PUEDEN ENVIAR SMS ENTRE $horaIni Y $horaFin");
                        //return@onFinish
                    }
                    //SI LA HORA ACTUAL ESTA DENTRO DEL RANGO PERMITIDO ACTIVAMOS EL ENVIO DE SMS
                    else{
                        chEnviaSMS.isChecked = true
                        chEnviaSMS.setBackgroundColor(Color.parseColor("#00FF00"))
                        tvNotificaHora.text="SON LAS $currentHour ESTA DENTRO DEL HORARIO $horaIni Y $horaFin"
                    }
                    evMuestraTels.setText("")
                    //SI ESTA ACTIVO EL ENVIAR SMS SE ENVIA EL SMS
                    if(chEnviaSMS.isChecked==true){
                        //ENVIAMOS EL SMS
                        enviaSmsServidor(txtLimite.text.toString(),txtOffset.text.toString());
                        //CADA x CICLOS NOTIFICARNOS CON UN SMS
                        var numeroNotificar= edNumeroNotificar.text.toString().toInt()
                        if(contadorReinicia>=numeroNotificar){
                            var txtTeimpoEsperaL=txtTeimpoEspera.text.toString().toLong()
                            txtTeimpoEsperaL *= 1000L
                            pausa(txtTeimpoEsperaL)
                            val tel =phoneNumberEditText.text.toString()
                            if (tel.trim().isNotEmpty()) {
                                val etNombreEquipoS=etNombreEquipo.text.toString()
                                val sim1=simSubIds[0]
                                val sim2=simSubIds[1]
                                if (sim1 != -1) {
                                    sendSms(tel, "Enviado desde \"$etNombreEquipoS\" $contador",sim1){enviado1 ->
                                        if (enviado1) {
                                        }
                                        else {
                                            activaReg.isChecked = false
                                            timer?.cancel()
                                            showAlertError("NO SE PUDO ENVIAR EL MENSAJE HACIA $tel DESDE LA SIM $sim1");
                                            playDefaultNotificationSound(RingtoneManager.TYPE_ALARM)
                                            return@sendSms
                                        }
                                    }
                                }
                                pausa(4000L)
                                if (sim2 != -1) {
                                    sendSms(tel, "Enviado desde \"$etNombreEquipoS\" $contador",sim2){enviado2 ->
                                        if (enviado2) {
                                        }
                                        else {
                                            activaReg.isChecked = false
                                            timer?.cancel()
                                            showAlertError("NO SE PUDO ENVIAR EL MENSAJE HACIA $tel DESDE LA SIM $sim2");
                                            playDefaultNotificationSound(RingtoneManager.TYPE_ALARM)
                                            return@sendSms
                                        }
                                    }
                                }

                            }
                            contadorReinicia=0
                        }
                        contadorReinicia++
                        contador++
                        tvNotifica.text="ciclos $contador"
                    }
                    startCountDown()
                }
            }
        }.start()
    }
    fun enviarUnMensaje(view: View){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_CODE)
        } else {
            val selectedId = radioGroupSim.checkedRadioButtonId

            val selectedValue: String? = when (selectedId) {
                R.id.radioButtonSim1 -> radioButtonSim1.text.toString()
                R.id.radioButtonSim2 -> radioButtonSim2.text.toString()
                else -> null
            }
            val numSim =selectedValue.toString().toInt()
            sendSms(phoneNumberEditText.text.toString(), messageEditText.text.toString(),numSim){ success ->
                if (success) {
                    Toast.makeText(this, "SMS enviado exitosamente", Toast.LENGTH_SHORT).show()
                    view.setBackgroundColor(Color.parseColor("#4CAF50")) // Verde
                } else {
                    Toast.makeText(this, "Error al enviar SMS", Toast.LENGTH_SHORT).show()
                    view.setBackgroundColor(Color.parseColor("#F44336")) // Rojo
                }
            }
        }
    }
    fun btnguardarTelefono(view: View){
        val phoneNumber = phoneNumberEditText.text.toString()
        val sharedPrefTel = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefTel.edit()
        editor.putString("phoneNumber", phoneNumber)
        editor.apply()
        Toast.makeText(this, "Numero guardado exitosamente", Toast.LENGTH_SHORT).show()
    }
    fun btnGuardaEquipo(view: View){
        val sharedPreferences = getSharedPreferences("datosEquipoPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Obtener el estado del CheckBox
        val isChEnviaSMSChecked = chEnviaSMS.isChecked

        editor.putString("etNombreEquipo", etNombreEquipo.text.toString())
        editor.putString("edNumeroNotificar", edNumeroNotificar.text.toString())
        editor.putBoolean("chEnviaSMS", isChEnviaSMSChecked)

        editor.apply()

        Toast.makeText(this, "Valores guardados correctamente", Toast.LENGTH_SHORT).show()
    }
    fun cargarDatosEquipo() {
        val sharedPreferences = getSharedPreferences("datosEquipoPreferences", Context.MODE_PRIVATE)
        val randomNumber = Random().nextInt( 200)
        val etNombreEquipoS = sharedPreferences.getString("etNombreEquipo", "Equipo $randomNumber")
        val edNumeroNotificarS = sharedPreferences.getString("edNumeroNotificar", "5")
        val isChEnviaSMSChecked = sharedPreferences.getBoolean("chEnviaSMS", true)
        chEnviaSMS.isChecked = isChEnviaSMSChecked
        colocreaChEnviaSMS()
        etNombreEquipo.setText(etNombreEquipoS)
        edNumeroNotificar.setText(edNumeroNotificarS)
    }
    fun clickChEnviaSMS(view: View){
        colocreaChEnviaSMS()
    }
    fun colocreaChEnviaSMS(){
        if(chEnviaSMS.isChecked){
            chEnviaSMS.setBackgroundColor(Color.parseColor("#00FF00"))
        }else{
            chEnviaSMS.setBackgroundColor(Color.parseColor("#FF0000"))
        }
    }
    fun sendSms(phoneNumber: String, message: String, subId: Int, callback: (Boolean) -> Unit) {
        val intent = Intent("SMS_SENT")
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        val sentPI = PendingIntent.getBroadcast(this, 0, intent, flags)

        val smsManager = SmsManager.getSmsManagerForSubscriptionId(subId)
        smsManager.sendTextMessage(phoneNumber, null, message, sentPI, null)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                unregisterReceiver(this)  // Desregistrar el receiver
                when (resultCode) {
                    Activity.RESULT_OK -> callback(true)
                    else -> callback(false)
                }
            }
        }

        registerReceiver(receiver, IntentFilter("SMS_SENT"))
        Toast.makeText(this, "Mensaje enviado desde $subId", Toast.LENGTH_SHORT).show()
    }
    fun checkSIMs(): IntArray  {
        val simSubIds = IntArray(2) { -1 }  // Por defecto ambos tienen valor -1, indicando que no hay SIM presente

        // Comprobar permisos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permiso y regresar valores por defecto
            // Es importante manejar la solicitud de permisos de forma asincrónica
            val REQUEST_CODE = 1234
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_CODE)
            return simSubIds
        }

        val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList

        if (subscriptionInfoList != null) {
            for (info in subscriptionInfoList) {
                when (info.simSlotIndex) {
                    0 -> simSubIds[0] = info.subscriptionId  // Guardar subId de la SIM 1
                    1 -> simSubIds[1] = info.subscriptionId  // Guardar subId de la SIM 2
                }
            }
        }

        return simSubIds
    }
    fun enviaSmsServidor(limite: String, offset: String) {
        val SimTel1Value = SimTel1.text.toString()
        val SimTel2Value = SimTel2.text.toString()
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        // Mostrar el ProgressBar
        progressBar.visibility = View.VISIBLE

        val queue = Volley.newRequestQueue(this)
        val url = "https://chiapasmerecemas.com/wp-json/mycustomnamespace/v1/myendpoint/?password=chiapas22&limite=$limite&offset=$offset"
        val idUsers = mutableListOf<String>()
        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                val totalMensajes = response.length()

                val mensajesSIM1: Int
                val mensajesSIM2: Int

                when {
                    simSubIds[0] != -1 && simSubIds[1] != -1 -> {
                        // Ambas SIMs presentes
                        mensajesSIM1 = totalMensajes / 2
                        mensajesSIM2 = totalMensajes - mensajesSIM1
                    }
                    simSubIds[0] != -1 -> {
                        // Solo SIM 1 presente
                        mensajesSIM1 = totalMensajes
                        mensajesSIM2 = 0
                    }
                    simSubIds[1] != -1 -> {
                        // Solo SIM 2 presente
                        mensajesSIM1 = 0
                        mensajesSIM2 = totalMensajes
                    }
                    else -> {
                        // Ninguna SIM presente
                        mensajesSIM1 = 0
                        mensajesSIM2 = 0
                    }
                }
                if (mensajesSIM1 == 0 && mensajesSIM2 == 0) {
                    showAlertError("No hay tarjetas SIM disponibles.")
                    return@JsonArrayRequest
                }
                val userRecords = mutableListOf<JSONObject>()
                for (i in 0 until totalMensajes) {
                    val jsonObject = response.getJSONObject(i)

                    val descripcionPlantilla = jsonObject.getString("descripcion_plantilla")
                    val partes = descripcionPlantilla.split("|")
                    val indiceAleatorio = Random().nextInt(partes.size)
                    var descripcionPlantillaSeleccionada = partes[indiceAleatorio]

                    val idUser = jsonObject.getString("ID")
                    idUsers.add(idUser)
                    val displayName = jsonObject.getString("display_name")
                    var telefono = jsonObject.getString("user_login")


                    if (telefono.startsWith("52")) {
                        telefono = telefono.substring(2)
                    }
                    descripcionPlantillaSeleccionada = descripcionPlantillaSeleccionada.replace("{nombre_cliente}", displayName)

                    var estado = "enviado"
                    try {
                        var subIdToSend = -1
                        if (i < mensajesSIM1) {
                            subIdToSend = simSubIds[0]
                        } else {
                            subIdToSend = simSubIds[1]
                        }
                        sendSms(telefono, descripcionPlantillaSeleccionada, subIdToSend)
                        evMuestraTels.setText("${evMuestraTels.text}$telefono | $displayName | $subIdToSend\n")
                        val userRecord = JSONObject().apply {
                            put("ID", idUser)
                            if (subIdToSend == simSubIds[0]) {
                                put("phoneNumber", SimTel1Value)
                            } else {
                                put("phoneNumber", SimTel2Value)
                            }
                        }
                        userRecords.add(userRecord)

                    } catch (e: Exception) {
                        estado = "error"
                        showAlertError(e.message)
                    }
                    var txtTeimpoEsperaL=txtTeimpoEspera.text.toString().toLong()
                    txtTeimpoEsperaL *= 1000L
                    pausa(txtTeimpoEsperaL)
                }
                // Convertir userRecords a JSON y enviar via POST
                val jsonBody = JSONObject().apply {
                    put("userRecords", JSONArray(userRecords))
                }
                val postUrl = "https://chiapasmerecemas.com/wp-json/mycustomnamespace/v1/update_sms_status/?password=chiapas22"
                val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, postUrl, jsonBody,
                    { response ->
                        Toast.makeText(this, "SUBIDO Y ACTUALIZADO", Toast.LENGTH_SHORT).show()
                    },
                    { error ->
                        Toast.makeText(this, "ERROR "+error.toString(), Toast.LENGTH_SHORT).show()
                        error.printStackTrace()
                    })

                Volley.newRequestQueue(this).add(jsonObjectRequest)
                // Ocultar el ProgressBar
                progressBar.visibility = View.GONE
            },
            { error ->
                error.printStackTrace()
            }
        )
        queue.add(jsonArrayRequest)
        //EMITIR UN SONIDO
        playDefaultNotificationSound(RingtoneManager.TYPE_NOTIFICATION)
    }
    fun playDefaultNotificationSound(tipoTono:Int) {
        val notification = RingtoneManager.getDefaultUri(tipoTono)
        currentRingtone = RingtoneManager.getRingtone(applicationContext, notification)
        currentRingtone?.play()
    }
    fun clickEnviaSms(view:View){
        evMuestraTels.setText("")
        enviaSmsServidor(txtLimite.text.toString(),txtOffset.text.toString());
    }


    // Función de pausa
    fun pausa(sleeptime: Long) {
        try {
            Thread.sleep(sleeptime)
        } catch (e: InterruptedException) {
        }
    }
    fun sendSms(phoneNumber: String, message: String, subId: Int) {
        val smsManager = SmsManager.getSmsManagerForSubscriptionId(subId)
        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        Toast.makeText(this, "Mensaje enviado desde $subId", Toast.LENGTH_SHORT).show()
    }

    fun btnGuardaLim(view: View){
        val txtLimiteS=txtLimite.text.toString()
        val txtOffsetS=txtOffset.text.toString()
        val txtHoraIniS=txtHoraIni.text.toString()
        val txtHoraFinS=txtHoraFin.text.toString()
        guardarEnSharedPreferences(txtHoraIniS,txtHoraFinS,txtLimiteS, txtOffsetS)
    }
    fun guardarEnSharedPreferences(horaIni: String,horaFin: String, limite: String, offset: String) {
        val sharedPreferences = getSharedPreferences("misPreferencias", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("LIMITE", limite)
        editor.putString("OFFSET", offset)
        editor.putString("horaIni", horaIni)
        editor.putString("horaFin", horaFin)
        editor.apply()
        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("timeReset", txtTiempoReset.text.toString())
            putString("txtTeimpoEspera", txtTeimpoEspera.text.toString())
            apply()
        }

        Toast.makeText(this, "Datos guardados con éxito", Toast.LENGTH_SHORT).show()
    }
    fun cargarValoresDeSharedPreferences() {
        val sharedPreferences = getSharedPreferences("misPreferencias", Context.MODE_PRIVATE)
        val limiteGuardado = sharedPreferences.getString("LIMITE", "0")  // El segundo parámetro es un valor por defecto en caso de que no exista.
        val offsetGuardado = sharedPreferences.getString("OFFSET", "10")  // El segundo parámetro es un valor por defecto en caso de que no exista.
        val txtHoraIniGuardado = sharedPreferences.getString("horaIni", "8")  // El segundo parámetro es un valor por defecto en caso de que no exista.
        val txtHoraFinGuardado = sharedPreferences.getString("horaFin", "20")  // El segundo parámetro es un valor por defecto en caso de que no exista.

        txtLimite.setText(limiteGuardado)
        txtOffset.setText(offsetGuardado)
        txtHoraIni.setText(txtHoraIniGuardado)
        txtHoraFin.setText(txtHoraFinGuardado)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
        }
        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permiso leer mensajes concedido", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permiso leer mensajes denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    private fun showAlertError(message: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message ?: "Un error desconocido ha ocurrido.")
        builder.setPositiveButton("OK") { dialog, _ ->
            currentRingtone?.stop()
            dialog.dismiss()
        }
        builder.show()
    }
    fun GuardarSimTel(view: View) {
        val sharedPreferences = getSharedPreferences("simTelPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putString("simTel1Value", SimTel1.text.toString())
        editor.putString("simTel2Value", SimTel2.text.toString())

        editor.apply()

        Toast.makeText(this, "Valores guardados correctamente", Toast.LENGTH_SHORT).show()
    }
    fun cargarValoresSimTel() {
        val sharedPreferences = getSharedPreferences("simTelPreferences", Context.MODE_PRIVATE)
        val simTel1Value = sharedPreferences.getString("simTel1Value", "")
        val simTel2Value = sharedPreferences.getString("simTel2Value", "")

        SimTel1.setText(simTel1Value)
        SimTel2.setText(simTel2Value)
    }
    //al hacer click en el boton btnColocarMensajes tomar el item del spListaMensajes que esta seleccionado y colocarlo en  el edittext messageEditText
    fun btnColocarMensajes(){
        // Obtener una referencia al Spinner y al EditText
        val spListaMensajes: Spinner = findViewById(R.id.spListaMensajes)
        val messageEditText: EditText = findViewById(R.id.messageEditText)
        // Obtener el ítem seleccionado del Spinner
        val selectedItem = spListaMensajes.selectedItem as String
        // Establecer el valor en el EditText
        messageEditText.setText(selectedItem)
    }
    fun llenaSpin(){
        // Request a JSONArray response from the provided URL.
        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET,
            "https://chiapasmerecemas.com/wp-json/mycustomnamespace/v1/myendpoint/?password=chiapas22&limite=0&offset=1",
            null,
            { response ->
                // Parse the result
                val jsonObject = response.getJSONObject(0)
                val descripcionPlantilla = jsonObject.getString("descripcion_plantilla")
                val partes = descripcionPlantilla.split("|").toTypedArray()

                // Populate the spinner
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, partes)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spListaMensajes.adapter = adapter
            },
            { error ->
                // TODO: Handle error
            }
        )

        // Add the request to the RequestQueue.
        Volley.newRequestQueue(this).add(jsonArrayRequest)
    }
}
