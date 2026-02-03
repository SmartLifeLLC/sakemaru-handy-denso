package biz.smt_life.android.core.network.di

import biz.smt_life.android.core.domain.repository.AuthRepository
import biz.smt_life.android.core.domain.repository.InboundRepository
import biz.smt_life.android.core.domain.repository.IncomingRepository
import biz.smt_life.android.core.domain.repository.MainRepository
import biz.smt_life.android.core.domain.repository.OutboundRepository
import biz.smt_life.android.core.domain.repository.OutboundCourseRepository
import biz.smt_life.android.core.domain.repository.PickingTaskRepository
import biz.smt_life.android.core.domain.repository.ProfileRepository
import biz.smt_life.android.core.network.BuildConfig
import biz.smt_life.android.core.network.api.AuthService
import biz.smt_life.android.core.network.api.IncomingApi
import biz.smt_life.android.core.network.api.PickingApi
import biz.smt_life.android.core.network.fake.FakeInboundRepository
import biz.smt_life.android.core.network.fake.FakeMainRepository
import biz.smt_life.android.core.network.fake.FakeOutboundRepository
import biz.smt_life.android.core.network.fake.FakeOutboundCourseRepository
import biz.smt_life.android.core.network.fake.FakeProfileRepository
import biz.smt_life.android.core.network.interceptor.ApiKeyInterceptor
import biz.smt_life.android.core.network.interceptor.AuthInterceptor
import biz.smt_life.android.core.network.repository.AuthRepositoryImpl
import biz.smt_life.android.core.network.repository.IncomingRepositoryImpl
import biz.smt_life.android.core.network.repository.PickingTaskRepositoryImpl
import biz.smt_life.android.core.ui.HostPreferences
import biz.smt_life.android.core.ui.TokenManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiKey

@Module
@InstallIn(SingletonComponent::class)
object NetworkProviderModule {

    @Provides
    @ApiKey
    fun provideApiKey(): String {
        return BuildConfig.WMS_API_KEY
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideApiKeyInterceptor(@ApiKey apiKey: String): ApiKeyInterceptor {
        return ApiKeyInterceptor(apiKey)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor { tokenManager.getToken() }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        apiKeyInterceptor: ApiKeyInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
        hostPreferences: HostPreferences
    ): Retrofit {
        // Note: Using runBlocking here is acceptable for DI setup
        // In production, consider using a more sophisticated approach
        val baseUrl = runBlocking { hostPreferences.getBaseUrlOnce() }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    @Provides
    @Singleton
    fun providePickingApi(retrofit: Retrofit): PickingApi {
        return retrofit.create(PickingApi::class.java)
    }

    @Provides
    @Singleton
    fun provideIncomingApi(retrofit: Retrofit): IncomingApi {
        return retrofit.create(IncomingApi::class.java)
    }

    @Provides
    @Singleton
    fun provideErrorMapper(): biz.smt_life.android.core.network.ErrorMapper {
        return biz.smt_life.android.core.network.ErrorMapper
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindInboundRepository(
        fakeInboundRepository: FakeInboundRepository
    ): InboundRepository

    @Binds
    @Singleton
    abstract fun bindOutboundRepository(
        fakeOutboundRepository: FakeOutboundRepository
    ): OutboundRepository

    @Binds
    @Singleton
    abstract fun bindOutboundCourseRepository(
        fakeOutboundCourseRepository: FakeOutboundCourseRepository
    ): OutboundCourseRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        fakeProfileRepository: FakeProfileRepository
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindMainRepository(
        fakeMainRepository: FakeMainRepository
    ): MainRepository

    @Binds
    @Singleton
    abstract fun bindPickingTaskRepository(
        pickingTaskRepositoryImpl: PickingTaskRepositoryImpl
    ): PickingTaskRepository

    @Binds
    @Singleton
    abstract fun bindIncomingRepository(
        incomingRepositoryImpl: IncomingRepositoryImpl
    ): IncomingRepository
}
