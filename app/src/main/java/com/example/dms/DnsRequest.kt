package com.example.dms

import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

fun dnsRequest(domain: String): ByteArray{

    val transactionId = byteArrayOf(0,1)
    val flags = byteArrayOf(1,0)
    val questions = byteArrayOf(1,0)
    val answer = byteArrayOf(0,0)
    val aithoriry = byteArrayOf(0,0)
    val additional = byteArrayOf(0,0)
    val domainPart = domain.split(".")
    val requestData = ByteArray(domain.length + 6)

    var index = 0
    for(part in domainPart){
        requestData[index++] = part.length.toByte()
        for (char in part){
            requestData[index++] = char.code.toByte()
        }
    }

    requestData[index++] = 0
    requestData[index++] = 0
    requestData[index++] = 1
    requestData[index] = 0

    return transactionId + flags + questions + answer + aithoriry + additional + requestData
}

fun sendDnsRequest(requestData: ByteArray, dnsServerAddress: InetAddress, dnsPort: Int){
    try {
        val socket = DatagramSocket()
        val sendPacket = DatagramPacket(requestData, requestData.size, dnsServerAddress, dnsPort)

        socket.send(sendPacket)
        socket.close()
    } catch (e: Exception) {
        println(e.message)
    }
}

fun receiveDnsResponse(): ByteArray?{
    val receiveData = ByteArray(1024)
    try {
        val socket = DatagramSocket()
        val receivePacket = DatagramPacket(receiveData, receiveData.size)

        socket.receive(receivePacket)
        socket.close()
        return receivePacket.data
    } catch (e: Exception) {
        println(e.message)
    }
    return null
}

fun parseDnsResponse(responseData: ByteArray): String?{
    try {
        val answerCount = (responseData[6].toInt() shl 8) or responseData[7].toInt()
        var index = 12

        while (index < responseData.size && responseData[index] != 0.toByte()){
            index++
        }
        index += 5

        for (i in 0 until answerCount) {
            if (responseData[index] == 0.toByte()) {
                val type = (responseData[index + 2].toInt() shl 8) or responseData[index + 3].toInt()
                if (type == 1) {
                    val ipAddress = "${responseData[index + 12].toInt() and 0xFF}" +
                            ".${responseData[index + 13].toInt() and 0xFF}" +
                            ".${responseData[index + 14].toInt() and 0xFF}" +
                            ".${responseData[index + 15].toInt() and 0xFF}"
                    return ipAddress
                }
            }
            index += 16
        }
    } catch (e:Exception) {
        println(e.message)
    }
    return null
}
