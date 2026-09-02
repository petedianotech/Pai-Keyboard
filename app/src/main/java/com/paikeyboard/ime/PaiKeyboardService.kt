package com.paikeyboard.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import android.content.Context
import android.media.AudioManager

/**
 * Pai Keyboard - Fast, stable, power-efficient custom IME.
 *
 * Design goals:
 * - Instant response (direct commitText, minimal allocations)
 * - Low battery impact (no heavy animations, no background work, hardware accelerated)
 * - Clean professional look
 * - Robust against crashes (null-safe InputConnection handling)
 */
class PaiKeyboardService : InputMethodService() {

    private var keyboardView: View? = null
    private var symbolsView: View? = null
    private var currentView: View? = null

    private var isShifted = false
    private var isCapsLock = false
    private var isSymbols = false

    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null

    // Simple offline suggestions dictionary (tiny, zero network, power friendly)
    private val commonWords = listOf(
        "the", "to", "and", "of", "a", "in", "is", "it", "you", "that",
        "he", "was", "for", "on", "are", "with", "as", "I", "his", "they",
        "be", "at", "one", "have", "this", "from", "or", "had", "by", "hot",
        "word", "but", "what", "some", "we", "can", "out", "other", "were", "all",
        "there", "when", "up", "use", "your", "how", "said", "an", "each", "she",
        "which", "do", "their", "time", "if", "will", "way", "about", "many", "then",
        "them", "write", "would", "like", "so", "these", "her", "long", "make", "thing",
        "see", "him", "two", "has", "look", "more", "day", "could", "go", "come",
        "did", "number", "sound", "no", "most", "people", "my", "over", "know", "water",
        "than", "call", "first", "who", "may", "down", "side", "been", "now", "find"
    )

    private var currentComposing = StringBuilder()

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    override fun onCreateInputView(): View {
        // Inflate once and reuse - faster subsequent loads
        if (keyboardView == null) {
            keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null)
            setupKeyboardListeners(keyboardView!!)
        }
        if (symbolsView == null) {
            symbolsView = layoutInflater.inflate(R.layout.keyboard_symbols, null)
            setupSymbolsListeners(symbolsView!!)
        }

        currentView = if (isSymbols) symbolsView else keyboardView
        return currentView!!
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        updateEnterKeyLabel(info)
        currentComposing.clear()
        hideSuggestions()
    }

    private fun setupKeyboardListeners(root: View) {
        val letterIds = listOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
            R.id.key_n, R.id.key_m
        )

        letterIds.forEach { id ->
            root.findViewById<Button>(id)?.setOnClickListener { v ->
                val key = (v as Button).text.toString()
                commitKey(if (isShifted || isCapsLock) key.uppercase() else key.lowercase())
                if (isShifted && !isCapsLock) {
                    isShifted = false
                    updateShiftState()
                }
                playClick()
            }
        }

        listOf(
            R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5,
            R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9, R.id.key_0
        ).forEach { id ->
            root.findViewById<Button>(id)?.setOnClickListener { v ->
                commitKey((v as Button).text.toString())
                playClick()
            }
        }

        root.findViewById<Button>(R.id.key_space)?.setOnClickListener {
            commitKey(" ")
            currentComposing.clear()
            hideSuggestions()
            playClick()
        }

        root.findViewById<Button>(R.id.key_delete)?.setOnClickListener {
            handleDelete()
            playClick()
        }

        root.findViewById<Button>(R.id.key_enter)?.setOnClickListener {
            handleEnter()
            playClick()
        }

        root.findViewById<Button>(R.id.key_shift)?.setOnClickListener {
            if (isShifted) {
                isCapsLock = !isCapsLock
                isShifted = isCapsLock
            } else {
                isShifted = true
            }
            updateShiftState()
            playClick()
        }

        root.findViewById<Button>(R.id.key_symbols)?.setOnClickListener {
            switchToSymbols()
            playClick()
        }

        root.findViewById<Button>(R.id.key_emoji)?.setOnClickListener {
            commitKey("😊")
            playClick()
        }

        root.findViewById<Button>(R.id.key_comma)?.setOnClickListener {
            commitKey(",")
            playClick()
        }

        root.findViewById<Button>(R.id.key_period)?.setOnClickListener {
            commitKey(".")
            playClick()
        }

        root.findViewById<TextView>(R.id.suggestion1)?.setOnClickListener { applySuggestion(it as TextView) }
        root.findViewById<TextView>(R.id.suggestion2)?.setOnClickListener { applySuggestion(it as TextView) }
        root.findViewById<TextView>(R.id.suggestion3)?.setOnClickListener { applySuggestion(it as TextView) }
    }

    private fun setupSymbolsListeners(root: View) {
        val symbolIds = listOf(
            R.id.sym_1, R.id.sym_2, R.id.sym_3, R.id.sym_4, R.id.sym_5,
            R.id.sym_6, R.id.sym_7, R.id.sym_8, R.id.sym_9, R.id.sym_0,
            R.id.sym_at, R.id.sym_hash, R.id.sym_dollar, R.id.sym_percent,
            R.id.sym_amp, R.id.sym_star, R.id.sym_minus, R.id.sym_plus,
            R.id.sym_eq, R.id.sym_slash, R.id.sym_underscore, R.id.sym_lt,
            R.id.sym_gt, R.id.sym_bracket_l, R.id.sym_bracket_r, R.id.sym_brace_l,
            R.id.sym_brace_r, R.id.sym_pipe, R.id.sym_backslash, R.id.sym_quote,
            R.id.sym_apostrophe, R.id.sym_colon, R.id.sym_semicolon, R.id.sym_excl,
            R.id.sym_question, R.id.sym_comma, R.id.sym_period
        )

        symbolIds.forEach { id ->
            root.findViewById<Button>(id)?.setOnClickListener { v ->
                commitKey((v as Button).text.toString())
                playClick()
            }
        }

        root.findViewById<Button>(R.id.sym_space)?.setOnClickListener {
            commitKey(" ")
            playClick()
        }

        root.findViewById<Button>(R.id.sym_delete)?.setOnClickListener {
            handleDelete()
            playClick()
        }

        root.findViewById<Button>(R.id.sym_enter)?.setOnClickListener {
            handleEnter()
            playClick()
        }

        root.findViewById<Button>(R.id.sym_abc)?.setOnClickListener {
            switchToLetters()
            playClick()
        }

        root.findViewById<Button>(R.id.sym_emoji)?.setOnClickListener {
            commitKey("😊")
            playClick()
        }
    }

    private fun commitKey(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)

        if (text.length == 1 && text[0].isLetter()) {
            currentComposing.append(text.lowercase())
            updateSuggestions(currentComposing.toString())
        } else if (text == " ") {
            currentComposing.clear()
            hideSuggestions()
        }
    }

    private fun handleDelete() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (selected != null && selected.isNotEmpty()) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
        if (currentComposing.isNotEmpty()) {
            currentComposing.deleteCharAt(currentComposing.length - 1)
            updateSuggestions(currentComposing.toString())
        } else {
            hideSuggestions()
        }
    }

    private fun handleEnter() {
        val ic = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE

        when (action) {
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_SEARCH,
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_NEXT -> {
                ic.performEditorAction(action)
            }
            else -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
        }
        currentComposing.clear()
        hideSuggestions()
    }

    private fun updateShiftState() {
        val root = keyboardView ?: return
        val letterIds = listOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
            R.id.key_n, R.id.key_m
        )
        val upper = isShifted || isCapsLock
        letterIds.forEach { id ->
            root.findViewById<Button>(id)?.let { btn ->
                val base = btn.text.toString().lowercase()
                btn.text = if (upper) base.uppercase() else base
            }
        }
        root.findViewById<Button>(R.id.key_shift)?.text = when {
            isCapsLock -> "⇪"
            isShifted -> "⇧"
            else -> "⇧"
        }
    }

    private fun switchToSymbols() {
        isSymbols = true
        setInputView(symbolsView)
    }

    private fun switchToLetters() {
        isSymbols = false
        setInputView(keyboardView)
    }

    private fun updateEnterKeyLabel(info: EditorInfo?) {
        val label = when (info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)) {
            EditorInfo.IME_ACTION_GO -> getString(R.string.key_go)
            EditorInfo.IME_ACTION_SEARCH -> getString(R.string.key_search)
            EditorInfo.IME_ACTION_SEND -> getString(R.string.key_send)
            EditorInfo.IME_ACTION_NEXT -> getString(R.string.key_next)
            EditorInfo.IME_ACTION_DONE -> getString(R.string.key_done)
            else -> "↵"
        }
        keyboardView?.findViewById<Button>(R.id.key_enter)?.text = label
        symbolsView?.findViewById<Button>(R.id.sym_enter)?.text = label
    }

    private fun updateSuggestions(prefix: String) {
        if (prefix.length < 2) {
            hideSuggestions()
            return
        }
        val matches = commonWords
            .filter { it.startsWith(prefix) && it != prefix }
            .take(3)

        val root = keyboardView ?: return
        val bar = root.findViewById<LinearLayout>(R.id.suggestion_bar) ?: return

        if (matches.isEmpty()) {
            bar.visibility = View.GONE
            return
        }

        bar.visibility = View.VISIBLE
        root.findViewById<TextView>(R.id.suggestion1)?.text = matches.getOrNull(0) ?: ""
        root.findViewById<TextView>(R.id.suggestion2)?.text = matches.getOrNull(1) ?: ""
        root.findViewById<TextView>(R.id.suggestion3)?.text = matches.getOrNull(2) ?: ""
    }

    private fun hideSuggestions() {
        keyboardView?.findViewById<LinearLayout>(R.id.suggestion_bar)?.visibility = View.GONE
    }

    private fun applySuggestion(tv: TextView) {
        val word = tv.text?.toString() ?: return
        if (word.isEmpty()) return
        val ic = currentInputConnection ?: return

        val len = currentComposing.length
        if (len > 0) {
            ic.deleteSurroundingText(len, 0)
        }
        ic.commitText("$word ", 1)
        currentComposing.clear()
        hideSuggestions()
        playClick()
    }

    private fun playClick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(12)
            }
        } catch (_: Exception) {}
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.4f)
        } catch (_: Exception) {}
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }

    override fun onDestroy() {
        keyboardView = null
        symbolsView = null
        currentView = null
        vibrator = null
        audioManager = null
        super.onDestroy()
    }
}
