package uk.ac.ncl.openlab.ongoingness.controllers

import android.content.Context
import com.crashlytics.android.Crashlytics
import uk.ac.ncl.openlab.ongoingness.collections.AbstractContentCollection
import uk.ac.ncl.openlab.ongoingness.recognisers.AbstractRecogniser
import uk.ac.ncl.openlab.ongoingness.presenters.CoverType
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.hasConnection
import uk.ac.ncl.openlab.ongoingness.presenters.Presenter
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaAsyncTask
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaPushLogsAsyncTask

const val INVERTED_PULL_CONTENT_ON_WAKE = true

class InvertedAnewController(context: Context,
                             recogniser: AbstractRecogniser,
                             presenter: Presenter,
                             contentCollection: AbstractContentCollection,
                             val startedWitTap: Boolean,
                             val faceState: String,
                             val battery: Float)
    : AbstractController(context, recogniser, presenter, contentCollection) {

    var gotData = false
    var start = true

    override fun setStatingState() {
        updateState(ControllerState.STANDBY)
    }

    override fun onStartedEvent() {

        startKillThread(30 * 1000L, 5 * 60 * 1000L)

        if(faceState == ControllerState.CHARGING.toString()) {
            getPresenter().displayChargingCover(battery)
            updateState(ControllerState.CHARGING)
        } else {
            getPresenter().displayCover(CoverType.BLACK)
            updateState(ControllerState.STANDBY)
        }

    }

    override fun onTowardsEvent() {

        when(getCurrentState()) {

            ControllerState.STANDBY -> {

                if(start) {
                    awakeUpProcedures()
                    start = false
                } else {
                    updateState(ControllerState.READY)
                }
            }
            else -> {}
        }

    }

    override fun onAwayEvent() {
        stopKillThread()
        getPresenter().view!!.finishActivity()
    }

    override fun onTapEvent() {

        when(getCurrentState()) {

            ControllerState.ACTIVE -> {

                val content = getContentCollection().goToNextContent()
                if(content != null)
                    getPresenter().displayContentPiece(content)

            }

            else ->  {}
        }
    }

    override fun onLongPressEvent() {

        when(getCurrentState()) {

            ControllerState.READY -> awakeUpProcedures()

            ControllerState.ACTIVE -> {
                getPresenter().displayCover(CoverType.BLACK)
                getContentCollection().stop()
                Logger.log(LogType.SLEEP, listOf(), context)
                Logger.deleteLogSessionToken()
                updateState(ControllerState.READY)
            }

            else ->{}

        }

    }

    override fun onChargerConnectedEvent(battery: Float) {
        getPresenter().displayChargingCover(battery)
        updateState(ControllerState.CHARGING)
    }

    override fun onChargerDisconnectedEvent() {
        when(getCurrentState()) {
            ControllerState.CHARGING -> {
                getPresenter().displayCover(CoverType.BLACK)
                updateState(ControllerState.READY)
            }
            else -> {}
        }
    }

    override fun onBatteryChangedEvent(battery: Float) {
        when(getCurrentState()) {
           ControllerState.CHARGING -> getPresenter().displayChargingCover(battery)
            else -> {}
        }
    }

    override fun onStoppedEvent() {}

    override fun onUpEvent() {}

    override fun onDownEvent() {}

    override fun onRotateUp() {}

    override fun onRotateDown() {}

    override fun onUnknownEvent() {}

    private fun awakeUpProcedures() {

        if(INVERTED_PULL_CONTENT_ON_WAKE && !gotData && hasConnection(context)) {

            val postExecuteCallback: (result: Boolean) -> Unit = {
                gotData = it
                getContentCollection().setup()
                val content = getContentCollection().getCurrentContent()
                if(content != null) {
                    getContentCollection().startLoggingFields(content)
                    getPresenter().displayContentPiece(content)
                }
                stopKillThread()
                updateState(ControllerState.ACTIVE)
                Logger.setLogSessionToken()
                Logger.log(LogType.WAKE_UP, listOf(), context)
            }

            getPresenter().displayCover(CoverType.WHITE)
            PullMediaPushLogsAsyncTask(postExecuteCallback = postExecuteCallback).execute(context)
            updateState(ControllerState.PULLING_DATA)

        } else {
            getContentCollection().setup()
            val content = getContentCollection().getCurrentContent()
            if(content != null) {
                getContentCollection().startLoggingFields(content)
                getPresenter().displayContentPiece(content)
            }
            stopKillThread()
            updateState(ControllerState.ACTIVE)
            Logger.setLogSessionToken()
            Logger.log(LogType.WAKE_UP, listOf(), context)
        }

    }

}


