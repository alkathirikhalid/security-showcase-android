package cz.koto.misak.securityshowcase.storage

import cz.koto.misak.securityshowcase.model.AuthResponseSimple
import cz.koto.misak.securityshowcase.utility.Logcat


object CredentialStorage {

    private var accessToken: String? = null
    private var userName: String? = null
    private var password: String? = null

    var forceLockScreenFlag: Boolean? = true

    fun getAccessToken(): String? {
        if (accessToken != null)
            Logcat.d("getToken %s", accessToken!!)
        else
            Logcat.d("NULL token!")
        return accessToken
    }

    fun getUserName() = userName
    fun getPassword() = password

    fun storeUser(authResponse: AuthResponseSimple?, username: String, pass: String) =
            authResponse?.let {
                if (it.idToken!=null) {
                    accessToken = it.idToken
                    userName = username
                    password = pass
                }
            }

    fun performLogout() {
        accessToken = null
        userName = null
        password = null
    }

    /**
     * Set forceLockScreenFlag to avoid automatic login just after logout.
     */
    fun forceLockScreenFlag() {
        forceLockScreenFlag = true
    }

    /**
     * Dismiss requirement to display LockScreen given by application.
     * Requirement given by certificate definition remains.
     */
    fun dismissForceLockScreenFlag() {
        this.forceLockScreenFlag = false
    }

}