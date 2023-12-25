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
internal class ByteCodeAnalyserSettingsService : PersistentStateComponent<ByteCodeAnalyserSettingsService.State> {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val typeNameRenderMode = AtomicProperty(DEFAULT_TYPE_NAME_RENDER_MODE)
  val methodDescriptorRenderMode = AtomicProperty(DEFAULT_METHOD_DESCRIPTOR_RENDER_MODE)
  val showAccessAsHex = AtomicBooleanProperty(DEFAULT_SHOW_ACCESS_AS_HEX)
  val skipDebug = AtomicBooleanProperty(DEFAULT_SKIP_DEBUG)
  val skipCode = AtomicBooleanProperty(DEFAULT_SKIP_CODE)
  val skipFrame = AtomicBooleanProperty(DEFAULT_SKIP_FRAME)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun getState() = State(
    typeNameRenderMode = typeNameRenderMode.get().name,
    methodDescriptorRenderMode = methodDescriptorRenderMode.get().name,
    showAccessAsHex = showAccessAsHex.get(),
    skipDebug = skipDebug.get(),
    skipCode = skipCode.get(),
    skipFrame = skipFrame.get()
  )

  override fun loadState(state: State) {
    typeNameRenderMode.set(state.typeNameRenderMode?.let { TypeNameRenderMode.valueOf(it) } ?: DEFAULT_TYPE_NAME_RENDER_MODE)
    methodDescriptorRenderMode.set(state.methodDescriptorRenderMode?.let { MethodDescriptorRenderMode.valueOf(it) } ?: DEFAULT_METHOD_DESCRIPTOR_RENDER_MODE)
    showAccessAsHex.set(state.showAccessAsHex ?: DEFAULT_SHOW_ACCESS_AS_HEX)
    skipDebug.set(state.skipDebug ?: DEFAULT_SKIP_DEBUG)
    skipCode.set(state.skipCode ?: DEFAULT_SKIP_CODE)
    skipFrame.set(state.skipFrame ?: DEFAULT_SKIP_FRAME)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class State(
    @get:Attribute("typeNameRenderMode")
    var typeNameRenderMode: String? = null,
    @get:Attribute("methodDescriptorRenderMode")
    var methodDescriptorRenderMode: String? = null,
    @get:Attribute("showAccessAsHex")
    var showAccessAsHex: Boolean? = null,
    @get:Attribute("skipDebug")
    var skipDebug: Boolean? = null,
    @get:Attribute("skipCode")
    var skipCode: Boolean? = null,
    @get:Attribute("skipFrame")
    var skipFrame: Boolean? = null,
  )

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val DEFAULT_TYPE_NAME_RENDER_MODE = TypeNameRenderMode.QUALIFIED
    private val DEFAULT_METHOD_DESCRIPTOR_RENDER_MODE = MethodDescriptorRenderMode.DESCRIPTOR
    private const val DEFAULT_SHOW_ACCESS_AS_HEX = true
    private const val DEFAULT_SKIP_DEBUG = false
    private const val DEFAULT_SKIP_CODE = false
    private const val DEFAULT_SKIP_FRAME = false

    val instance: ByteCodeAnalyserSettingsService
      get() = ApplicationManager.getApplication().getService(ByteCodeAnalyserSettingsService::class.java)

    var typeNameRenderMode by instance.typeNameRenderMode
    var methodDescriptorRenderMode by instance.methodDescriptorRenderMode
    var showAccessAsHex by instance.showAccessAsHex
    var skipDebug by instance.skipDebug
    var skipCode by instance.skipCode
    var skipFrame by instance.skipFrame
  }
}
