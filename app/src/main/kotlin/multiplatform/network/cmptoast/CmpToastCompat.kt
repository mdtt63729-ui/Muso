package multiplatform.network.cmptoast

import android.widget.Toast
import com.maxrave.simpmusic.ui.component.SuiteRes

/** Muso port of CMPToast's API surface, backed by the platform Toast. */
enum class ToastDuration { Short, Long }

enum class ToastGravity { Top, Center, Bottom }

fun showToast(
    message: String,
    gravity: ToastGravity = ToastGravity.Bottom,
    duration: ToastDuration = ToastDuration.Short,
) {
    val context = SuiteRes.context ?: return
    val toast = Toast.makeText(context, message, if (duration == ToastDuration.Long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
    val y = when (gravity) {
        ToastGravity.Top -> 120
        ToastGravity.Center -> 0
        ToastGravity.Bottom -> -180
    }
    toast.setGravity(android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.CENTER_VERTICAL, 0, y)
    toast.show()
}
