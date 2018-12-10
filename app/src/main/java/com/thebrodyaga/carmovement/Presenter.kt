package com.thebrodyaga.carmovement

import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter

/**
 * Created by admin
 *         on 10/12/2018.
 */
@InjectViewState
class Presenter : MvpPresenter<MoxyView>() {
    fun updateState(stateModel: AnimatedPathView.StateModel) {
        viewState.updateState(stateModel)
    }
}