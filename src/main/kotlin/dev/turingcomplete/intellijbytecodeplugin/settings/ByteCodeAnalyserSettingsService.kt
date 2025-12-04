package dev.turingcomplete.intellijbytecodeplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.util.xmlb.annotations.Attribute
import dev.turingcomplete.intellijbytecodeplugin.bytecode.MethodDeclarationUtils.MethodDescriptorRenderMode
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TypeUtils.TypeNameRenderMode

@State(name = "ByteCodeAnalyserSettingsService", storages = [Storage("byte-code-analyser.xml")])
internal class ByteCodeAnalyserSettingsService :
  PersistentStateComponent<ByteCodeAnalyserSettingsService.State> {
  // -- Properties ---------------------------------------------------------- //

  var typeNameRenderMode by AtomicProperty(DEFAULT_TYPE_NAME_RENDER_MODE)
  var methodDescriptorRenderMode by AtomicProperty(DEFAULT_METHOD_DESCRIPTOR_RENDER_MODE)
  var showAccessAsHex by AtomicBooleanProperty(DEFAULT_SHOW_ACCESS_AS_HEX)
  var skipDebug by AtomicBooleanProperty(DEFAULT_SKIP_DEBUG)
  var skipCode by AtomicBooleanProperty(DEFAULT_SKIP_CODE)
  var skipFrame by AtomicBooleanProperty(DEFAULT_SKIP_FRAME)

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun getState() =
    State(
      typeNameRenderMode = typeNameRenderMode.name,
      methodDescriptorRenderMode = methodDescriptorRenderMode.name,
      showAccessAsHex = showAccessAsHex,
      skipDebug = skipDebug,
      skipCode = skipCode,
      skipFrame = skipFrame,
    )

  override fun loadState(state: State) {
    typeNameRenderMode =
      state.typeNameRenderMode?.let { TypeNameRenderMode.valueOf(it) }
        ?: DEFAULT_TYPE_NAME_RENDER_MODE
    methodDescriptorRenderMode =
      state.methodDescriptorRenderMode?.let { MethodDescriptorRenderMode.valueOf(it) }
        ?: DEFAULT_METHOD_DESCRIPTOR_RENDER_MODE
    showAccessAsHex = state.showAccessAsHex ?: DEFAULT_SHOW_ACCESS_AS_HEX
    skipDebug = state.skipDebug ?: DEFAULT_SKIP_DEBUG
    skipCode = state.skipCode ?: DEFAULT_SKIP_CODE
    skipFrame = state.skipFrame ?: DEFAULT_SKIP_FRAME
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  data class State(
    @get:Attribute("typeNameRenderMode") var typeNameRenderMode: String? = null,
    @get:Attribute("methodDescriptorRenderMode") var methodDescriptorRenderMode: String? = null,
    @get:Attribute("showAccessAsHex") var showAccessAsHex: Boolean? = null,
    @get:Attribute("skipDebug") var skipDebug: Boolean? = null,
    @get:Attribute("skipCode") var skipCode: Boolean? = null,
    @get:Attribute("skipFrame") var skipFrame: Boolean? = null,
  )

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val DEFAULT_TYPE_NAME_RENDER_MODE = TypeNameRenderMode.QUALIFIED
    private val DEFAULT_METHOD_DESCRIPTOR_RENDER_MODE = MethodDescriptorRenderMode.DESCRIPTOR
    private const val DEFAULT_SHOW_ACCESS_AS_HEX = true
    private const val DEFAULT_SKIP_DEBUG = false
    private const val DEFAULT_SKIP_CODE = false
    private const val DEFAULT_SKIP_FRAME = false

    val instance: ByteCodeAnalyserSettingsService
      get() =
        ApplicationManager.getApplication().getService(ByteCodeAnalyserSettingsService::class.java)
  }
}
