package cz.kotox.securityshowcase.login.biservice

import android.content.Context
import android.content.Intent

enum class LongRunningCommand {
	INVALID,
	STOP,
	START
}

class LongRunningIntentBuilder(private val context: Context) {
	private var message: String? = null
	private var command = LongRunningCommand.INVALID

	fun setMessage(message: String): LongRunningIntentBuilder {
		this.message = message
		return this
	}

	/**
	 * @param command Don't use [Command.INVALID] as a param. If you do then this method does
	 * nothing.
	 */
	fun setCommand(command: LongRunningCommand): LongRunningIntentBuilder {
		this.command = command
		return this
	}

	fun build(): Intent {
		val intent = Intent(context, LongRunningService::class.java)
		if (command != LongRunningCommand.INVALID) {
			intent.putExtra(KEY_COMMAND, command)
		}
		if (message != null) {
			intent.putExtra(KEY_MESSAGE, message)
		}
		return intent
	}

	companion object {

		private val KEY_MESSAGE = "msg"
		private val KEY_COMMAND = "cmd"

		fun getInstance(context: Context): LongRunningIntentBuilder {
			return LongRunningIntentBuilder(context)
		}

		fun containsCommand(intent: Intent): Boolean {
			return intent.extras?.containsKey(KEY_COMMAND) ?: false
		}

		fun containsMessage(intent: Intent): Boolean {
			return intent.extras?.containsKey(KEY_MESSAGE) ?: false
		}

		fun getCommand(intent: Intent): LongRunningCommand {
			return (intent.extras?.get(KEY_COMMAND) as LongRunningCommand)
		}

		fun getMessage(intent: Intent): String? {
			return intent.extras?.getString(KEY_MESSAGE)
		}
	}
}