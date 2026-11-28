package com.example.cleannotes.di

import com.example.cleannotes.ai.ChatGptAccess
import com.example.cleannotes.ai.ChatGptStore
import com.example.cleannotes.data.chatgpt.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatGptModule {
	@Binds @Singleton abstract fun store(implementation: EncryptedChatGptStore): ChatGptStore

	companion object {
		@Provides
		@Singleton
		fun access(store: ChatGptStore, client: OkHttpClient): ChatGptAccess =
			ChatGptAccountService(store, ChatGptApi(client), client)
	}
}
