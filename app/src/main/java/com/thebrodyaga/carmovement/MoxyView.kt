package com.thebrodyaga.carmovement

import com.arellomobile.mvp.MvpView
import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType

/**
 * Created by admin
 *         on 10/12/2018.
 */
interface MoxyView : MvpView {
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun updateState(stateModel: AnimatedPathView.StateModel)
}