package net.leanix.githubagent.shared

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun hmacSHA256(secret: String, data: String): String {
    val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(secretKey)
    val hmacData = mac.doFinal(data.toByteArray())
    return hmacData.joinToString("") { "%02x".format(it) }
}

fun timingSafeEqual(a: String, b: String): Boolean {
    val aBytes = a.toByteArray()
    val bBytes = b.toByteArray()
    if (aBytes.size != bBytes.size) return false

    var diff = 0
    for (i in aBytes.indices) {
        diff = diff or (aBytes[i].toInt() xor bBytes[i].toInt())
    }
    return diff == 0
}
