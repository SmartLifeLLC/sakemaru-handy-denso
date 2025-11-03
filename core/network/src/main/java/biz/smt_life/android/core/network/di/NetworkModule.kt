package biz.smt_life.android.core.network.di

import biz.smt_life.android.core.domain.repository.AuthRepository
import biz.smt_life.android.core.domain.repository.InboundRepository
import biz.smt_life.android.core.domain.repository.MainRepository
import biz.smt_life.android.core.domain.repository.OutboundRepository
import biz.smt_life.android.core.domain.repository.OutboundCourseRepository
import biz.smt_life.android.core.domain.repository.ProfileRepository
import biz.smt_life.android.core.network.fake.FakeAuthRepository
import biz.smt_life.android.core.network.fake.FakeInboundRepository
import biz.smt_life.android.core.network.fake.FakeMainRepository
import biz.smt_life.android.core.network.fake.FakeOutboundRepository
import biz.smt_life.android.core.network.fake.FakeOutboundCourseRepository
import biz.smt_life.android.core.network.fake.FakeProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        fakeAuthRepository: FakeAuthRepository
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
}
