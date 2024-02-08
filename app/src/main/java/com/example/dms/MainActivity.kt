package com.example.dms

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MainActivity : AppCompatActivity() {
    private lateinit var editTextServer: EditText
    private lateinit var editTextDomain: EditText
    private lateinit var textViewResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextServer = findViewById(R.id.editTextServer)
        editTextDomain = findViewById(R.id.editTextDomain)
        textViewResult = findViewById(R.id.textViewResult)

//        if (editTextServer.text.isBlank()) {
//            editTextServer.setText("8.8.8.8")
//        }

        val buttonLookup = findViewById<Button>(R.id.buttonLookup)
        buttonLookup.setOnClickListener {
            val domain = editTextDomain.text.toString()
            val server = editTextServer.text.toString()
            if (server.isBlank()) {
                editTextServer.setText("8.8.8.8")
            }

            performDnsLookup(domain, server)
        }
    }

    private fun performDnsLookup(
        domain: String,
        server: String,
    ) {
        Thread {
            try {
                val ips = mutableListOf<String>()
                val query = buildDnsQuery(domain)
                val response = sendDnsQuery(query, server, 53)
                parseDnsResponse(response, ips)

                runOnUiThread {
                    Log.wtf("MyLog", "$ips")
                    textViewResult.text = ips.joinToString("\n")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Log.wtf("MyLog", "${e.message}")
                    textViewResult.text = "Error: ${e.message}"
                }
            }
        }.start()
    }

    private fun buildDnsQuery(domain: String): ByteArray {
        val header =
            byteArrayOf(
                0, 0,
                1, 0,
                0, 1,
                0, 0,
                0, 0,
                0, 0,
            )
        val question =
            byteArrayOf(
                *domain.split(".")
                    .flatMap { listOf(it.length.toByte()) + it.toByteArray().toList() }
                    .toByteArray(),
                0,
                0,
                1,
                0,
                1,
            )
        return header + question
    }

    private fun sendDnsQuery(
        query: ByteArray,
        dnsServerIp: String,
        dnsServerPort: Int,
    ): ByteArray {
        val socket = DatagramSocket()
        val dnsServerAddress = InetAddress.getByName(dnsServerIp)
        val packet = DatagramPacket(query, query.size, dnsServerAddress, dnsServerPort)
        val response = ByteArray(1024)
        val responsePacket = DatagramPacket(response, response.size)
        socket.send(packet)
        socket.receive(responsePacket)
        Log.wtf("MyLog", "$responsePacket")
        socket.close()
        return response
    }

    private fun parseDnsResponse(
        response: ByteArray,
        ips: MutableList<String>,
    ) {
        var index = 12
        while (response[index] != 0.toByte()) index++
        index += 5
        repeat(response[7].toInt()) {
            while (response[index] != 0.toByte()) index++
            index += 10
            ips.add((0 until response[index].toInt()).joinToString(".") { (response[++index].toInt() and 0xFF).toString() })
            index++
        }
    }
}
