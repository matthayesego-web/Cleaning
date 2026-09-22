package ca.northstarappworks.cleaning.model

import java.time.Instant

enum class RewardCouponStatus {
    AVAILABLE,
    PENDING,
    USED
}

data class RewardDefinition(
    val id: String,
    val title: String,
    val description: String = "",
    val cost: Int,
    val owner: Assignee,
    val custom: Boolean = true,
    val createdAt: Instant = Instant.now()
)

data class RewardCoupon(
    val id: String,
    val rewardId: String,
    val title: String,
    val cost: Int,
    val owner: Assignee,
    val status: RewardCouponStatus = RewardCouponStatus.AVAILABLE,
    val redeemedAt: Instant = Instant.now(),
    val requestedAt: Instant? = null,
    val resolvedAt: Instant? = null
)

fun starterRewards(owner: Assignee): List<RewardDefinition> {
    if (owner == Assignee.EITHER) return emptyList()
    val prefix = owner.name.lowercase()
    return listOf(
        RewardDefinition("$prefix-pick-show", "Pick tonight’s show", "You choose what goes on the TV.", 30, owner, custom = false),
        RewardDefinition("$prefix-snack", "Pick dessert or snack", "Choose the treat for the evening.", 40, owner, custom = false),
        RewardDefinition("$prefix-drink", "Drink made for you", "Coffee, tea, or your usual drink.", 50, owner, custom = false),
        RewardDefinition("$prefix-dinner", "Choose dinner", "You pick what’s for dinner.", 60, owner, custom = false),
        RewardDefinition("$prefix-break", "30 minutes to yourself", "Thirty uninterrupted minutes of personal time.", 75, owner, custom = false),
        RewardDefinition("$prefix-skip-chore", "Skip one normal chore", "Use on one ordinary household chore.", 100, owner, custom = false),
        RewardDefinition("$prefix-movie", "Pick the movie", "Your movie choice wins tonight.", 100, owner, custom = false),
        RewardDefinition("$prefix-sleep-in", "Sleep-in coupon", "Cash this in for a slower morning.", 125, owner, custom = false),
        RewardDefinition("$prefix-treat", "$10 personal treat", "A little something just for you.", 150, owner, custom = false),
        RewardDefinition("$prefix-takeout", "Takeout night", "Trade points for an easy dinner night.", 175, owner, custom = false),
        RewardDefinition("$prefix-hour", "One hour to yourself", "One uninterrupted hour of personal time.", 200, owner, custom = false),
        RewardDefinition("$prefix-date", "Choose date night", "You choose the plan for date night.", 250, owner, custom = false),
        RewardDefinition("$prefix-royalty", "Royalty evening", "Take the evening off from the normal household routine.", 300, owner, custom = false)
    )
}
