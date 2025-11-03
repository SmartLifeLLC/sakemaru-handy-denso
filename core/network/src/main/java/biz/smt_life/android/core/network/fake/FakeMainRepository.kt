package biz.smt_life.android.core.network.fake

import biz.smt_life.android.core.domain.model.FeedItem
import biz.smt_life.android.core.domain.model.FeedItemType
import biz.smt_life.android.core.domain.model.Summary
import biz.smt_life.android.core.domain.repository.MainRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FakeMainRepository @Inject constructor() : MainRepository {

    private val feedItems = listOf(
        FeedItem("1", "Inbound task #1234", "Check arrival schedule", System.currentTimeMillis() - 3600000, FeedItemType.TASK),
        FeedItem("2", "Low stock alert", "Item A needs restock", System.currentTimeMillis() - 7200000, FeedItemType.ALERT),
        FeedItem("3", "Outbound request", "Prepare shipment for customer B", System.currentTimeMillis() - 10800000, FeedItemType.TASK),
        FeedItem("4", "Inventory count", "Complete zone C inventory", System.currentTimeMillis() - 14400000, FeedItemType.TASK),
        FeedItem("5", "System notification", "Maintenance scheduled tonight", System.currentTimeMillis() - 18000000, FeedItemType.NOTIFICATION),
        FeedItem("6", "Quality check", "Inspect incoming batch #5678", System.currentTimeMillis() - 21600000, FeedItemType.TASK),
        FeedItem("7", "Temperature alert", "Cold storage zone D warning", System.currentTimeMillis() - 25200000, FeedItemType.ALERT),
        FeedItem("8", "Move task", "Transfer items from A to B", System.currentTimeMillis() - 28800000, FeedItemType.TASK),
    )

    override fun summary(): Flow<Summary> = flow {
        delay(500) // Simulate network delay

        // 10% chance of error for testing
        if (Random.nextFloat() < 0.1f) {
            throw Exception("Failed to load summary")
        }

        emit(Summary(
            today = Random.nextInt(10, 50),
            week = Random.nextInt(50, 200),
            messages = Random.nextInt(0, 15),
            alerts = Random.nextInt(0, 5)
        ))
    }

    override fun feed(page: Int, pageSize: Int): Flow<List<FeedItem>> = flow {
        delay(800) // Simulate network delay

        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, feedItems.size)

        if (startIndex >= feedItems.size) {
            emit(emptyList())
        } else {
            emit(feedItems.subList(startIndex, endIndex))
        }
    }
}
