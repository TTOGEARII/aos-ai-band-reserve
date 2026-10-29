package com.enezzang.yatte.di

import com.enezzang.yatte.data.EnezzangYatteApi
import com.enezzang.yatte.data.EnezzangYatteNetwork
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 생성자 주입이 안 되는 것(Retrofit 인터페이스)만 모듈로 제공한다.
 * Repository·Store는 각자 @Inject 생성자를 갖는다 — 배선이 클래스 선언에 보이도록.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideEnezzangYatteApi(): EnezzangYatteApi = EnezzangYatteNetwork.createApi()
}
