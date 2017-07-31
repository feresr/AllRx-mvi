package com.feresr.allrx

import io.reactivex.Observable
import io.reactivex.disposables.Disposable

interface MVI {

    interface MVIPresenter<I : Intent, S : ViewState> {
        fun sendIntent(intent: I)
        fun observeIntents(intentObservable: Observable<I>): Disposable
        fun states(): Observable<S>
    }

    interface Intent
    interface Action
    interface Result
    interface ViewState
}