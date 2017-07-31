package com.feresr.allrx

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val transformer: ObservableTransformer<MainIntent, MainState> = ObservableTransformer {
        it.publish({
            //Intent -> Action
            Observable.merge(it.ofType(MainIntent.LoadInfoIntent::class.java).map { MainAction.GetDataAction(it.data) },
                    it.ofType(MainIntent.ShowToastIntent::class.java).map { MainAction.ToastAction(it.data) }
            )
        }).publish {
            //Action -> Result
            Observable.merge(it.ofType(MainAction.GetDataAction::class.java).compose(getData)
                    , it.ofType(MainAction.ToastAction::class.java).compose(logger))
        }.scan(MainState(), { oldState, result ->
            //Result --> New state
            when (result) {
                is MainResult.GetDataResult -> when (result) {
                    is MainResult.GetDataResult.Success -> oldState.copy(isProgress = false, success = true, errorMessage = "", message = result.data, initial = false)
                    is MainResult.GetDataResult.Error -> oldState.copy(isProgress = false, success = false, errorMessage = result.error, initial = false)
                    is MainResult.GetDataResult.InProgress -> oldState.copy(isProgress = true, errorMessage = "", initial = false)
                    else -> throw IllegalStateException("Unknown network result " + result)
                }
                is MainResult.ToastResult -> oldState.copy(errorMessage = result.s, initial = false)
                else -> throw IllegalStateException("Unknown result " + result)
            }
        })
    }

    val presenter: MviPresenter<MainIntent, MainState> = MviPresenter(transformer)

    //Intents
    interface MainIntent : MVI.Intent {
        data class LoadInfoIntent(val data: String) : MainIntent
        data class ShowToastIntent(val data: String) : MainIntent
    }

    //Actions
    interface MainAction : MVI.Action {
        data class GetDataAction(val data: String) : MainAction
        data class ToastAction(val data: String) : MainAction
    }

    //State
    data class MainState(val initial: Boolean = true, val isProgress: Boolean = false, val success: Boolean = false, val errorMessage: String = "", val message: String = "") : MVI.ViewState

    //Results
    interface MainResult : MVI.Result {
        data class ToastResult(val s: String) : MainResult
        open class GetDataResult : MainResult {
            class InProgress : GetDataResult()
            data class Success(val data: String) : GetDataResult()
            data class Error(val error: String) : GetDataResult()
        }
    }

    /**
     * Dummy transformers, to be replaced with a real implementation
     * **/
    val logger: ObservableTransformer<MainAction.ToastAction, MainResult.ToastResult> = ObservableTransformer {
        it.map { MainResult.ToastResult(it.data) }
    }
    val getData: ObservableTransformer<MainAction.GetDataAction, MainResult.GetDataResult> = ObservableTransformer {
        it.switchMap {
            Observable.just(it.data)
                    .map {
                        Thread.sleep(2000)
                        "$it is Sunny"
                    }.subscribeOn(Schedulers.computation())
                    .map<MainResult.GetDataResult> { MainResult.GetDataResult.Success(it) }
                    .onErrorReturn { e -> MainResult.GetDataResult.Error(e.message ?: "") }
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWith(MainResult.GetDataResult.InProgress())
        }
    }
}