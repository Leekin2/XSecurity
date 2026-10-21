"""Builds disposable debuggable probes with two keys and three versions.

No APK is installed by this script. Outputs stay under build/device-probe.
"""
from pathlib import Path
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "build/device-probe"
SDK = Path((ROOT / "local.properties").read_text().split("sdk.dir=", 1)[1].strip().replace("\\\\", "\\").replace("\\:", ":"))
TOOLS = SDK / "build-tools/36.0.0"
ANDROID = SDK / "platforms/android-37.0/android.jar"
KEY_A = Path.home() / ".android/debug.keystore"


def run(*args):
    command = [str(arg) for arg in args]
    if command[0].lower().endswith(".bat"):
        command = ["cmd", "/c", *command]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(result.stdout + result.stderr)


def main():
    if not KEY_A.is_file():
        raise FileNotFoundError("The module's debug keystore is required for certificate A")
    OUT.mkdir(parents=True, exist_ok=True)
    classes = OUT / "classes"
    dex = OUT / "dex"
    classes.mkdir(exist_ok=True)
    dex.mkdir(exist_ok=True)
    run("javac", "-encoding", "UTF-8", "-source", "17", "-target", "17", "-classpath", ANDROID,
        "-d", classes, Path(__file__).with_name("ProbeActivity.java"))
    run(TOOLS / "d8.bat", "--lib", ANDROID, "--min-api", "35", "--output", dex, *classes.rglob("*.class"))
    key_b = OUT / "fixture-b.keystore"
    if not key_b.exists():
        run("keytool", "-genkeypair", "-keystore", key_b, "-storepass", "android", "-keypass", "android",
            "-alias", "androiddebugkey", "-keyalg", "RSA", "-keysize", "2048", "-validity", "10",
            "-dname", "CN=XSecurity disposable test key")
    for label, version, key in (("A-v1", 1, KEY_A), ("B-v2", 2, key_b), ("A-v3", 3, KEY_A)):
        manifest = OUT / (label + "-AndroidManifest.xml")
        manifest.write_text(f'''<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="io.github.leekin2.xsecurity.testprobe" android:versionCode="{version}" android:versionName="{version}">
    <uses-sdk android:minSdkVersion="35" android:targetSdkVersion="36"/>
    <application android:label="XSecurity Capture Test" android:debuggable="true" android:allowBackup="false"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity android:name=".ProbeActivity" android:exported="true"/>
    </application>
</manifest>''', encoding="utf-8")
        resources = OUT / (label + "-resources.apk")
        run(TOOLS / "aapt2.exe", "link", "-I", ANDROID, "--manifest", manifest, "-o", resources)
        unsigned = OUT / (label + "-unsigned.apk")
        with zipfile.ZipFile(resources) as source, zipfile.ZipFile(unsigned, "w") as target:
            for item in source.infolist():
                target.writestr(item, source.read(item.filename))
            target.write(dex / "classes.dex", "classes.dex")
        output = OUT / (label + ".apk")
        run(TOOLS / "apksigner.bat", "sign", "--ks", key, "--ks-key-alias", "androiddebugkey",
            "--ks-pass", "pass:android", "--key-pass", "pass:android", "--out", output, unsigned)
        run(TOOLS / "apksigner.bat", "verify", output)
        print(output)


if __name__ == "__main__":
    main()
