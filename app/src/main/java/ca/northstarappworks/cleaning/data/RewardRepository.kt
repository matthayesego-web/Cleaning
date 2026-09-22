package ca.northstarappworks.cleaning.data

import ca.northstarappworks.cleaning.model.RewardCoupon
import ca.northstarappworks.cleaning.model.RewardDefinition
import kotlinx.coroutines.flow.StateFlow

interface RewardRepository {
    val customRewards: StateFlow<List<RewardDefinition>>
    val coupons: StateFlow<List<RewardCoupon>>

    fun addReward(reward: RewardDefinition)
    fun deleteReward(rewardId: String)
    fun addCoupon(coupon: RewardCoupon)
    fun updateCoupon(coupon: RewardCoupon)
    fun replaceRewards(rewards: List<RewardDefinition>)
    fun replaceCoupons(coupons: List<RewardCoupon>)
}
