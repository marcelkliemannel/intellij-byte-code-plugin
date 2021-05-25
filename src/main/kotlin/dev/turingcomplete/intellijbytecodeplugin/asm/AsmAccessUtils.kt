package dev.turingcomplete.intellijbytecodeplugin.asm

import com.jetbrains.rd.util.EnumSet
import dev.turingcomplete.intellijbytecodeplugin.asm.Access.*
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Opcodes.*

enum class Access(val value: Int) {
  PUBLIC(ACC_PUBLIC),
  PRIVATE(ACC_PRIVATE),
  PROTECTED(ACC_PROTECTED),
  STATIC(ACC_STATIC),
  FINAL(ACC_FINAL),
  SUPER(ACC_SUPER),
  SYNCHRONIZED(ACC_SYNCHRONIZED),
  OPEN(ACC_OPEN),
  TRANSITIVE(ACC_TRANSITIVE),
  VOLATILE(ACC_VOLATILE),
  BRIDGE(ACC_BRIDGE),
  STATIC_PHASE(ACC_STATIC_PHASE),
  VARARGS(ACC_VARARGS),
  TRANSIENT(ACC_TRANSIENT),
  NATIVE(ACC_NATIVE),
  INTERFACE(ACC_INTERFACE),
  ABSTRACT(ACC_ABSTRACT),
  STRICT(ACC_STRICT),
  SYNTHETIC(ACC_SYNTHETIC),
  ANNOTATION(ACC_ANNOTATION),
  ENUM(ACC_ENUM),
  MANDATED(ACC_MANDATED),
  MODULE(ACC_MODULE),
  RECORD(ACC_RECORD),
  DEPRECATED(ACC_DEPRECATED);

  fun check(access: Int): Boolean = (value and access) != 0
}

enum class AccessGroup(val accesses: EnumSet<Access>) {
  CLASS(EnumSet.of(PUBLIC, PRIVATE, PROTECTED, FINAL, SUPER, INTERFACE, ABSTRACT, SYNTHETIC, ANNOTATION, RECORD, DEPRECATED)),
  FIELD(EnumSet.of(PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL, VOLATILE, TRANSIENT, SYNTHETIC, DEPRECATED)),
  METHOD(EnumSet.of(PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL, SYNCHRONIZED, BRIDGE, VARARGS, NATIVE, ABSTRACT, STRICT, SYNTHETIC, DEPRECATED)),
  PARAMETER(EnumSet.of(FINAL, SYNTHETIC, MANDATED)),
  MODULE(EnumSet.of(OPEN, SYNTHETIC, MANDATED, Access.MODULE)),
  MODULE_REQUIRES(EnumSet.of(TRANSITIVE, STATIC_PHASE, SYNTHETIC, MANDATED)),
  MODULE_EXPORTS(EnumSet.of(SYNTHETIC, MANDATED)),
  MODULE_OPENS(EnumSet.of(SYNTHETIC, MANDATED));

  fun toReadableAccess(access: Int): List<String> {
    return accesses.asSequence()
            .filter { (it.value and access) != 0 }
            .map { it.name.toLowerCase() }
            .toList()
  }

  override fun toString() = name.toLowerCase().capitalize().replace("_", " ")
}
