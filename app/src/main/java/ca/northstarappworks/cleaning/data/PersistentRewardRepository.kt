package ca.northstarappworks.cleaning.data

import android.content.Context
import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.model.RewardCoupon
import ca.northstarappworks.cleaning.model.RewardCouponStatus
import ca.northstarappworks.cleaning.model.RewardDefinition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class PersistentRewardRepository(context: Context) : RewardRepository {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val mutableRewards = MutableStateFlow(loadRewards())
    override val customRewards: StateFlow<List<RewardDefinition>> = mutableRewards.asStateFlow()

    private val mutableCoupons = MutableStateFlow(loadCoupons())
    override val coupons: StateFlow<List<RewardCoupon>> = mutableCoupons.asStateFlow()

    override fun addReward(reward: RewardDefinition) {
        mutableRewards.value = listOf(reward) + mutableRewards.value.filterNot { it.id == reward.id }
        persistRewards()
    }

    override fun deleteReward(rewardId: String) {
        mutableRewards.value = mutableRewards.value.filterNot { it.id == rewardId }
        persistRewards()
    }

    override fun addCoupon(coupon: RewardCoupon) {
        mutableCoupons.value = listOf(coupon) + mutableCoupons.value.filterNot { it.id == coupon.id }
        persistCoupons()
    }

    override fun updateCoupon(coupon: RewardCoupon) {
        mutableCoupons.value = mutableCoupons.value.map { if (it.id == coupon.id) coupon else it }
        persistCoupons()
    }

    override fun replaceRewards(rewards: List<RewardDefinition>) {
        mutableRewards.value = rewards.sortedBy { it.createdAt }
        persistRewards()
    }

    override fun replaceCoupons(coupons: List<RewardCoupon>) {
        mutableCoupons.value = coupons.sortedByDescending { it.redeemedAt }
        persistCoupons()
    }

    private fun persistRewards() {
        val array = JSONArray()
        mutableRewards.value.forEach { reward ->
            array.put(JSONObject().apply {
                put("id", reward.id)
                put("title", reward.title)
                put("description", reward.description)
                put("cost", reward.cost)
                put("owner", reward.owner.name)
                put("custom", reward.custom)
                put("createdAt", reward.createdAt.toString())
            })
        }
        preferences.edit().putString(KEY_REWARDS, array.toString()).apply()
    }

    private fun persistCoupons() {
        val array = JSONArray()
        mutableCoupons.value.forEach { coupon ->
            array.put(JSONObject().apply {
                put("id", coupon.id)
                put("rewardId", coupon.rewardId)
                put("title", coupon.title)
                put("cost", coupon.cost)
                put("owner", coupon.owner.name)
                put("status", coupon.status.name)
                put("redeemedAt", coupon.redeemedAt.toString())
                put("requestedAt", coupon.requestedAt?.toString() ?: JSONObject.NULL)
                put("resolvedAt", coupon.resolvedAt?.toString() ?: JSONObject.NULL)
            })
        }
        preferences.edit().putString(KEY_COUPONS, array.toString()).apply()
    }

    private fun loadRewards(): List<RewardDefinition> = runCatching {
        val raw = preferences.getString(KEY_REWARDS, null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RewardDefinition(
                        id = item.getString("id"),
                        title = item.optString("title", "Reward"),
                        description = item.optString("description", ""),
                        cost = item.optInt("cost", 10).coerceAtLeast(1),
                        owner = enumValueOrDefault(item.optString("owner"), Assignee.MATT),
                        custom = item.optBoolean("custom", true),
                        createdAt = item.optNullableInstant("createdAt") ?: Instant.now()
                    )
                )
            }
        }
    }.getOrElse { emptyList() }

    private fun loadCoupons(): List<RewardCoupon> = runCatching {
        val raw = preferences.getString(KEY_COUPONS, null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RewardCoupon(
                        id = item.getString("id"),
                        rewardId = item.optString("rewardId", ""),
                        title = item.optString("title", "Reward"),
                        cost = item.optInt("cost", 10).coerceAtLeast(1),
                        owner = enumValueOrDefault(item.optString("owner"), Assignee.MATT),
                        status = enumValueOrDefault(item.optString("status"), RewardCouponStatus.AVAILABLE),
                        redeemedAt = item.optNullableInstant("redeemedAt") ?: Instant.now(),
                        requestedAt = item.optNullableInstant("requestedAt"),
                        resolvedAt = item.optNullableInstant("resolvedAt")
                    )
                )
            }
        }
    }.getOrElse { emptyList() }

    private fun JSONObject.optNullableInstant(key: String): Instant? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback

    private companion object {
        const val PREFS_NAME = "our_home_rewards"
        const val KEY_REWARDS = "custom_rewards_json"
        const val KEY_COUPONS = "reward_coupons_json"
    }
}
