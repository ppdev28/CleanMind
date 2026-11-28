package com.example.cleannotes.di

import com.example.cleannotes.ai.AiGateway
import com.example.cleannotes.ai.AiSettingsRepository
import com.example.cleannotes.data.ai.EncryptedAiSettingsRepository
import com.example.cleannotes.data.ai.OpenAiCompatibleGateway
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
	@Binds
	@Singleton
	abstract fun settings(implementation: EncryptedAiSettingsRepository): AiSettingsRepository

	@Binds @Singleton abstract fun gateway(implementation: OpenAiCompatibleGateway): AiGateway

	companion object {
		@Provides
		@Singleton
		fun httpClient(): OkHttpClient =
			OkHttpClient.Builder()
				.connectTimeout(15, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS)
				.callTimeout(75, TimeUnit.SECONDS)
				.followRedirects(false)
				.followSslRedirects(false)
				.retryOnConnectionFailure(false)
				.build()
	}
}
