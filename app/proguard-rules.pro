-keep class io.github.leekin2.xsecurity.XSecurityModule { <init>(); public *; }
-dontwarn io.github.libxposed.**
-keep class io.github.libxposed.service.** { *; }
-keep class io.github.libxposed.interfaces.** { *; }
# DexKit is driven from native code.
-keep class org.luckypray.dexkit.** { *; }
-keep class io.github.leekin2.xsecurity.XSecurityApp { *; }
