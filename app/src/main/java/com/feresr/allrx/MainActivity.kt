package com.feresr.allrx

import android.arch.lifecycle.LifecycleActivity
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : LifecycleActivity() {

    private val disposables: CompositeDisposable = CompositeDisposable()
    private var model: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        model = ViewModelProviders.of(this).get(MainViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()

        val buttonEvents: Observable<MainViewModel.MainIntent> = RxView.clicks(button)
                .map { MainViewModel.MainIntent.LoadInfoIntent(etext.text.toString()) }

        val typeEvents: Observable<MainViewModel.MainIntent> = RxTextView.textChanges(etext)
                .filter({ !it.isEmpty() })
                .skip(1)
                .debounce(300, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .map { MainViewModel.MainIntent.ShowToastIntent(it.toString()) }


        model?.let {
            //intents -->
            disposables.add(it.presenter.observeIntents(Observable.merge(buttonEvents, typeEvents)))
            //states <--
            disposables.add(it.presenter.states().subscribe(this::render)
            )
        }
    }

    fun render(state: MainViewModel.MainState) {
        label.text = state.message
        progress.apply {
            visibility = if (state.isProgress) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        if (!state.errorMessage.isEmpty()) {
            Toast.makeText(this@MainActivity, state.errorMessage, Toast.LENGTH_SHORT).show()
        }

        if (state.initial) {
            //do one off setup here (i.e fetch data from network)
        }
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }
}
