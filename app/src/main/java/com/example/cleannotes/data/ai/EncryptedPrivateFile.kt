package com.example.cleannotes.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.example.cleannotes.ai.*
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Entire configuration is encrypted and excluded from cloud backup and device transfer. */
internal class EncryptedPrivateFile(context: Context, name: String, private val keyAlias: String) {
	private val file = AtomicFile(File(context.noBackupFilesDir, name))
	private val mutex = Mutex()

	suspend fun read(): String? =
		withContext(Dispatchers.IO) {
			mutex.withLock {
				try {
					val bytes =
						try {
							file.readFully()
						} catch (error: java.io.FileNotFoundException) {
							if (file.baseFile.exists()) throw error
							return@withLock null
						}
					require(bytes.size > 12)
					val cipher = Cipher.getInstance("AES/GCM/NoPadding")
					cipher.init(
						Cipher.DECRYPT_MODE,
						key(),
						GCMParameterSpec(128, bytes.copyOfRange(0, 12)),
					)
					cipher.doFinal(bytes.copyOfRange(12, bytes.size)).decodeToString()
				} catch (_: Exception) {
					throw AiException(AiError.STORAGE)
				}
			}
		}

	suspend fun write(content: String) =
		withContext(Dispatchers.IO) {
			mutex.withLock {
				try {
					val cipher = Cipher.getInstance("AES/GCM/NoPadding")
					cipher.init(Cipher.ENCRYPT_MODE, key())
					val encrypted = cipher.iv + cipher.doFinal(content.encodeToByteArray())
					val stream = file.startWrite()
					try {
						stream.write(encrypted)
						file.finishWrite(stream)
					} catch (error: Exception) {
						file.failWrite(stream)
						throw error
					}
				} catch (_: Exception) {
					throw AiException(AiError.STORAGE)
				}
			}
		}

	suspend fun delete() =
		withContext(Dispatchers.IO) {
			mutex.withLock {
				file.delete()
				if (file.baseFile.exists()) throw AiException(AiError.STORAGE)
			}
		}

	private fun key(): SecretKey {
		val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
		(store.getKey(keyAlias, null) as? SecretKey)?.let {
			return it
		}
		return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
			.apply {
				init(
					KeyGenParameterSpec.Builder(
							keyAlias,
							KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
						)
						.setBlockModes(KeyProperties.BLOCK_MODE_GCM)
						.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
						.setKeySize(256)
						.build()
				)
			}
			.generateKey()
	}
}
