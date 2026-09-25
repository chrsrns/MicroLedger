package ph.chrsrns.microledger.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ph.chrsrns.microledger.data.YearMonth
import java.util.Calendar
import javax.inject.Singleton

fun interface MonthProvider {
    fun current(): YearMonth
}

@Module
@InstallIn(SingletonComponent::class)
object MonthProviderModule {
    @Provides
    @Singleton
    fun provideMonthProvider(): MonthProvider =
        MonthProvider {
            val calendar = Calendar.getInstance()
            YearMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        }
}
