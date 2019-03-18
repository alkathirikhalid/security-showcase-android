package cz.kotox.securityshowcase.login

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import cz.kotox.securityshowcase.core.arch.BaseFragmentViewModel
import cz.kotox.securityshowcase.login.biservice.LongRunningService
import cz.kotox.securityshowcase.login.databinding.FragmentLoginBinding
import timber.log.Timber

class LoginFragment : BaseFragmentViewModel<LoginViewModel, FragmentLoginBinding>() {
//	override val baseActivity: BaseActivity
//		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

	// Don't attempt to unbind from the service unless the client has received some
	// information about the service's state.
	private var shouldUnbind: Boolean = false

	var localService: LongRunningService? = null
	var isBound = false

	override fun setupViewModel() = findViewModel<LoginViewModel>()

	override fun inflateBindingLayout(inflater: LayoutInflater) = FragmentLoginBinding.inflate(inflater)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		LongRunningService.moveToStartedState(this.context)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = super.onCreateView(inflater, container, savedInstanceState)
		doBindService()
		binding.floatingActionButton.setOnClickListener {
			val isRunning = localService?.toggleService()
			showSnackbar(binding.root, "service running = [$isRunning]")
		}


		return view
	}

	private val localServiceConnection = object : ServiceConnection {
		override fun onServiceConnected(className: ComponentName, service: IBinder) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			localService = (service as LongRunningService.LocalBinder).service
			isBound = true
			// Tell the user about this for our demo.
			Toast.makeText(this@LoginFragment.context, R.string.local_service_connected,
				Toast.LENGTH_SHORT).show()
		}

		override fun onServiceDisconnected(className: ComponentName) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			localService = null
			isBound = false
			Toast.makeText(this@LoginFragment.context, R.string.local_service_disconnected,
				Toast.LENGTH_SHORT).show()
		}
	}

	fun doBindService() {
		// Attempts to establish a connection with the service.  We use an
		// explicit class name because we want a specific service
		// implementation that we know will be running in our own process
		// (and thus won't be supporting component replacement by other
		// applications).

		if (baseActivity.bindService(Intent(this@LoginFragment.context, LongRunningService::class.java),
				localServiceConnection, Context.BIND_AUTO_CREATE)) {
			shouldUnbind = true
		} else {
			Timber.e("Error: The requested service doesn't " + "exist, or this client isn't allowed access to it.")
		}
	}

	private fun doUnbindService() {
		if (shouldUnbind) {
			// Release information about the service's state.
			baseActivity.unbindService(localServiceConnection)
			shouldUnbind = false
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		doUnbindService()
	}

}