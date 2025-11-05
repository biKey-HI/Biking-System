package org.example.app.pricingandpayment.api

import org.example.app.pricingandpayment.PaymentService
import org.springframework.web.bind.annotation.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.example.app.user.*
import java.util.Optional
import java.util.UUID

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/plan")
class PricingPlanController(
    private val pricingPlanService: PricingPlanService
) {
    @GetMapping
    fun changePricingPlan(@RequestParam userId: UUID, @RequestParam pricingPlan: String) = pricingPlanService.changePricingPlan(userId, pricingPlan)
}

@Service
class PricingPlanService(
    private val userRepository: UserRepository,
    private val payments: PaymentService
) {
    @Transactional(readOnly = false)
    fun changePricingPlan(userId: UUID, pricingPlan: String): Boolean {
        val userOpt: Optional<User>? = userRepository.findById(userId)
        val user: User? = userOpt?.orElse(null)
        return user?.let {
            val paymentStrategy = PaymentStrategyType.fromString(pricingPlan) ?: PaymentStrategyType.DEFAULT_PAY_NOW
            if(pricingPlan != PaymentStrategyType.DEFAULT_PAY_NOW.toString())
                payments.handlePayment(user, paymentStrategy)
            user.paymentStrategy = paymentStrategy
            userRepository.save(user)
        } != null
    }
}