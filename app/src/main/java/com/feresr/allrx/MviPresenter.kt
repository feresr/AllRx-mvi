package com.feresr.allrx

import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.Disposable

class MviPresenter<I : MVI.Intent, S : MVI.ViewState>(transformer: ObservableTransformer<I, S>) : MVI.MVIPresenter<I, S> {

    private var intents: Relay<I> = PublishRelay.create<I>().toSerialized()
    private var states: Observable<S> = intents.compose(transformer).replay(1).autoConnect()

    override fun sendIntent(intent: I) {
        intents.accept(intent)
    }

    override fun observeIntents(intentObservable: Observable<I>): Disposable {
        return intentObservable.subscribe(intents)
    }

    override fun states(): Observable<S> {
        return states
    }
}