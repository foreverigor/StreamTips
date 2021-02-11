package me.foreverigor.intellij.plugin.streamtips.settings

import com.intellij.application.options.editor.CheckboxDescriptor
import com.intellij.application.options.editor.checkBox
import com.intellij.openapi.application.ApplicationBundle.message
import com.intellij.openapi.options.BoundCompositeConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.selected
import javax.swing.JCheckBox

/**
 * Reference [com.intellij.ide.GeneralSettingsConfigurable]
 */
class StreamTipsPluginConfigurable : BoundCompositeConfigurable<Configurable>("Stream Tips", "streamtips.help"), Configurable {

    private val options = StreamTipsPluginSettings.getInstance()
    private val shouldEarlyCalculate    get() = CheckboxDescriptor("Start calculating early", PropertyBinding(options::isShouldEarlyCalculate, options::setShouldEarlyCalculate))
    private val shouldEarlyPreview      get() = CheckboxDescriptor("Start loading the preview immediately [experimental]", PropertyBinding(options::isShouldEarlyLoad, options::setShouldEarlyLoad))
    private lateinit var earlyCalculateCheckBox: JCheckBox

    override fun createPanel(): DialogPanel {
        return panel {
            row {
                cell (isFullWidth = true) {
                    label("Popup tip delay:")
                    intTextField(options::getPopupTipsDelay, options::setPopupTipsDelay, range = 1..5000, columns = 4)
                    label(message("editor.options.ms"))
                }
            }
            row {
                earlyCalculateCheckBox = checkBox(shouldEarlyCalculate).comment("Normally the required calculations start after the specified delay. " +
                        "Enabling this option lets the inspection & intention calculations start as soon as possible.").component
            }
            row { // Disabled if early calculate disabled – represents how it behaves in code
                checkBox(shouldEarlyPreview).enableIf(earlyCalculateCheckBox.selected)
                        .comment("Start the preview computation immediately after the pre-calculations have finished. " +
                                "Preview loading can take up to 2 sec. This option helps bring down the time needed to show the popup which may be important if a low delay was specified.")
            }
        }
    }

    override fun createConfigurables(): List<Configurable> = emptyList()

}
