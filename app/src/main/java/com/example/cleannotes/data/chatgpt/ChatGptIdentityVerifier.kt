package com.example.cleannotes.data.chatgpt

import com.example.cleannotes.ai.*
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT

internal object ChatGptIdentityVerifier {
	fun verify(
		token: String,
		jwks: String,
		issuer: String,
		clientId: String,
		nonce: String?,
		now: Long = System.currentTimeMillis(),
	): ChatGptIdentity {
		try {
			val jwt = SignedJWT.parse(token)
			val keyId =
				jwt.header.keyID?.takeIf { it.isNotBlank() } ?: error("Missing key identifier")
			val key = JWKSet.parse(jwks).getKeyByKeyId(keyId) ?: error("Unknown signing key")
			require(key.keyUse == null || key.keyUse == KeyUse.SIGNATURE)
			require(key.algorithm == null || key.algorithm == jwt.header.algorithm)
			val valid =
				when {
					jwt.header.algorithm == JWSAlgorithm.RS256 && key is RSAKey ->
						jwt.verify(RSASSAVerifier(key.toRSAPublicKey()))
					jwt.header.algorithm == JWSAlgorithm.ES256 && key is ECKey ->
						jwt.verify(ECDSAVerifier(key.toECPublicKey()))
					else -> false
				}
			require(valid)
			val claims = jwt.jwtClaimsSet
			require(claims.issuer == issuer && claims.audience.contains(clientId))
			require(claims.audience.size == 1 || claims.getStringClaim("azp") == clientId)
			require(
				claims.getStringClaim("azp") == null || claims.getStringClaim("azp") == clientId
			)
			require(claims.expirationTime != null && claims.expirationTime.time > now - 5_000)
			require(claims.issueTime != null && claims.issueTime.time <= now + 5_000)
			require(claims.notBeforeTime == null || claims.notBeforeTime.time <= now + 5_000)
			if (nonce != null) require(claims.getStringClaim("nonce") == nonce)
			val subject = claims.subject?.takeIf { it.isNotBlank() } ?: error("Missing identity")
			val email =
				if (claims.getBooleanClaim("email_verified") == true)
					claims.getStringClaim("email").orEmpty()
				else ""
			return ChatGptIdentity(subject, email)
		} catch (_: Exception) {
			throw AiException(AiError.SIGN_IN)
		}
	}
}
