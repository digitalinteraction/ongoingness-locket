package uk.ac.ncl.openlab.ongoingness.viewmodel

import android.content.Context
import android.util.Log
import uk.ac.ncl.openlab.ongoingness.recognisers.AbstractRecogniser
import uk.ac.ncl.openlab.ongoingness.utilities.CoverType
import uk.ac.ncl.openlab.ongoingness.utilities.LogType
import uk.ac.ncl.openlab.ongoingness.utilities.Logger
import uk.ac.ncl.openlab.ongoingness.utilities.hasConnection
import uk.ac.ncl.openlab.ongoingness.workers.PullMediaAsyncTask

const val REFIND_PULL_CONTENT_ON_WAKE = true

class RefindController(context: Context,
                     recogniser: AbstractRecogniser,
                     presenter: Presenter,
                     contentCollection: AbstractContentCollection)
    : AbstractController(context, recogniser, presenter, contentCollection) {

    var gotData = false

    override fun onRotateUp() {

        when(getCurrentState()) {

            ControllerState.ACTIVE -> {
                updateKillThread(System.currentTimeMillis())
                val content = getContentCollection().goToNextContent()
                if(content != null)
                    getPresenter().displayContentPiece(content)
            }

            else -> {}

        }
    }

    override fun onRotateDown() {

        when(getCurrentState()) {

            ControllerState.ACTIVE -> {
                updateKillThread(System.currentTimeMillis())
                val content = getContentCollection().goToPreviousContent()
                if(content != null)
                    getPresenter().displayContentPiece(content)
            }

            else -> {}

        }

    }

    override fun onPickUp() {

    }



    override fun setStatingState() {

    }

    override fun onStartedEvent() {


        if(REFIND_PULL_CONTENT_ON_WAKE && !gotData && hasConnection(context)) {

            val postExecuteCallback: (result: Boolean) -> Unit = {
                gotData = it
                getContentCollection().restartIndex()
                val content = getContentCollection().getCurrentContent()
                if(content != null)
                    getPresenter().displayContentPiece(content)
                stopKillThread()
                Logger.log(LogType.WAKE_UP, listOf(), context)

                startKillThread(1000L * 30 * 1  , 5000L)

                updateState(ControllerState.ACTIVE)
            }

            getPresenter().displayCover(CoverType.WHITE)
            PullMediaAsyncTask(postExecuteCallback = postExecuteCallback).execute(context)
            updateState(ControllerState.PULLING_DATA)

        } else {
            getContentCollection().restartIndex()
            val content = getContentCollection().getCurrentContent()
            if(content != null)
                getPresenter().displayContentPiece(content)
            startKillThread(1000L * 30 * 1  , 5000L)
            updateState(ControllerState.ACTIVE)
        }

    }

    override fun onStoppedEvent() {}

    override fun onUpEvent() {}

    override fun onDownEvent() {}

    override fun onTowardsEvent() {}

    override fun onAwayEvent() {}

    override fun onUnknownEvent() {}

    override fun onTapEvent() {}

    override fun onLongPressEvent() {}

    override fun onChargerConnectedEvent(battery: Float) {}

    override fun onChargerDisconnectedEvent() {}

    override fun onBatteryChangedEvent(battery: Float) {}

    private fun awakeUpProcedures() {
        if(PULL_CONTENT_ON_WAKE && !gotData && hasConnection(context)) {

            val postExecuteCallback: (result: Boolean) -> Unit = {
                gotData = it
                getContentCollection().restartIndex()
                val content = getContentCollection().getCurrentContent()
                if(content != null)
                    getPresenter().displayContentPiece(content)
                stopKillThread()
                updateState(ControllerState.ACTIVE)
            }

            getPresenter().displayCover(CoverType.WHITE)
            PullMediaAsyncTask(postExecuteCallback = postExecuteCallback).execute(context)
            updateState(ControllerState.PULLING_DATA)

        } else {

            getContentCollection().restartIndex()
            val content = getContentCollection().getCurrentContent()
            if(content != null)
                getPresenter().displayContentPiece(content)
            stopKillThread()
            updateState(ControllerState.ACTIVE)

        }

    }

}


