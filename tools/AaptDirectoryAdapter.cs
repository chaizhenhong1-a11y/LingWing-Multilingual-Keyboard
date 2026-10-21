// Optional Windows sandbox workaround. Resource compilation/linking still uses the official SDK AAPT2.
// The native ZIP writer crashes in this execution environment, even for an empty Android application.
using System;
using System.IO;
using System.IO.Compression;
using System.Diagnostics;
using System.Collections.Generic;

class AaptDirectoryAdapter {
    [System.Runtime.InteropServices.DllImport("kernel32.dll", CharSet = System.Runtime.InteropServices.CharSet.Unicode, SetLastError = true)]
    static extern uint GetShortPathName(string path, System.Text.StringBuilder buffer, uint length);
    static string ResponsePath(string path) {
        var buffer = new System.Text.StringBuilder(32768);
        if (GetShortPathName(path, buffer, (uint)buffer.Capacity) == 0) throw new System.ComponentModel.Win32Exception();
        string result = buffer.ToString();
        if (result.IndexOfAny(new char[] { ' ', '\t', '\r', '\n' }) >= 0) throw new InvalidOperationException("AAPT2 response inputs require a path without whitespace.");
        return result.Replace('\\', '/');
    }
    static string Quote(string value) { return "\"" + value.Replace("\"", "\\\"") + "\""; }

    static int Main(string[] args) {
        string executable = Environment.GetEnvironmentVariable("LAZYTALK_SDK_AAPT2");
        if (string.IsNullOrEmpty(executable) || !File.Exists(executable)) {
            Console.Error.WriteLine("Set LAZYTALK_SDK_AAPT2 to the official SDK aapt2.exe.");
            return 1;
        }
        Console.WriteLine("Ready"); Console.Out.Flush();
        while (true) {
            string line = Console.ReadLine();
            if (line == null || line == "quit") return 0;
            if (line.Length == 0) continue;
            var command = new List<string> { line == "l" ? "link" : line == "c" ? "compile" : line };
            while ((line = Console.ReadLine()) != null && line.Length > 0) command.Add(line);
            try {
                string archive = null, directory = null;
                if (command[0] == "link") {
                    int outputIndex = command.IndexOf("-o") + 1;
                    archive = command[outputIndex];
                    string tempRoot = Environment.GetEnvironmentVariable("LAZYTALK_BUILD_TEMP") ?? Path.GetTempPath();
                    directory = Path.Combine(tempRoot, "lt-" + Guid.NewGuid().ToString("N"));
                    Directory.CreateDirectory(directory);
                    Directory.CreateDirectory(Path.Combine(directory, "res"));
                    foreach (string item in command) {
                        if (!item.EndsWith(".flat")) continue;
                        string fileName = Path.GetFileName(item);
                        int separator = fileName.IndexOf('_');
                        if (separator > 0) Directory.CreateDirectory(Path.Combine(directory, "res", fileName.Substring(0, separator)));
                    }
                    command[outputIndex] = directory;
                    command.Add("--output-to-dir");
                    // Large dependency graphs can exceed Windows' 32767-character
                    // process command line. AAPT2 officially supports response files for -R inputs.
                    if (string.Join(" ", command.ConvertAll(Quote).ToArray()).Length > 24000) {
                        var overlays = new List<string>();
                        var compact = new List<string>();
                        int insertion = -1;
                        for (int i = 0; i < command.Count; i++) {
                            if (command[i] == "-R" && i + 1 < command.Count) {
                                if (insertion < 0) insertion = compact.Count;
                                string resource = command[++i];
                                if (resource.StartsWith("@")) throw new InvalidOperationException("Nested AAPT2 response files need explicit expansion.");
                                // AAPT2's response parser splits whitespace and does not unquote paths.
                                overlays.Add(ResponsePath(resource));
                            } else compact.Add(command[i]);
                        }
                        if (overlays.Count > 0) {
                            string responseFile = directory + ".overlays";
                            File.WriteAllLines(responseFile, overlays.ToArray(), new System.Text.UTF8Encoding(false));
                            compact.Insert(insertion, "-R"); compact.Insert(insertion + 1, "@" + responseFile);
                            command = compact;
                        }
                    }
                }
                var info = new ProcessStartInfo(executable, string.Join(" ", command.ConvertAll(Quote).ToArray())) {
                    UseShellExecute = false, RedirectStandardOutput = true,
                    RedirectStandardError = true, CreateNoWindow = true
                };
                using (var process = Process.Start(info)) {
                    var errors = process.StandardError.ReadToEndAsync();
                    var output = process.StandardOutput.ReadToEndAsync();
                    process.WaitForExit();
                    if (process.ExitCode == 0 && archive != null) {
                        StoredResourceZip.Write(directory, archive);
                    }
                    Console.Error.Write(errors.Result);
                    if (process.ExitCode != 0) {
                        Console.Error.WriteLine("SDK AAPT2 exited: " + process.ExitCode);
                        Console.Error.WriteLine("Error");
                    }
                }
            } catch (Exception error) {
                Console.Error.WriteLine("Resource adapter failed: " + error.GetType().Name + ": " + error.Message);
                Console.Error.WriteLine("Error");
            }
            // AAPT2's daemon protocol uses stderr for completion and stdout for the initial Ready.
            Console.Error.WriteLine("Done"); Console.Error.Flush();
        }
    }
}

// .NET Framework's CompressionLevel.NoCompression still writes ZIP method 8 (DEFLATE).
// Android R+ requires resources.arsc to use method 0 (STORED), so write real stored entries.
static class StoredResourceZip {
    class Entry { public byte[] Name; public uint Crc, Size, Offset; }
    static uint Crc32(byte[] data) {
        uint value = 0xffffffff;
        foreach (byte item in data) {
            value ^= item;
            for (int i = 0; i < 8; i++) value = (value >> 1) ^ ((value & 1) == 0 ? 0u : 0xedb88320u);
        }
        return ~value;
    }
    public static void Write(string directory, string archive) {
        var entries = new List<Entry>();
        string[] files = Directory.GetFiles(directory, "*", SearchOption.AllDirectories);
        Array.Sort(files, StringComparer.Ordinal);
        using (var writer = new BinaryWriter(File.Create(archive))) {
            foreach (string file in files) {
                byte[] data = File.ReadAllBytes(file);
                var entry = new Entry {
                    Name = System.Text.Encoding.UTF8.GetBytes(file.Substring(directory.Length + 1).Replace('\\', '/')),
                    Crc = Crc32(data), Size = checked((uint)data.Length), Offset = checked((uint)writer.BaseStream.Position)
                };
                int padding = (int)((4 - ((writer.BaseStream.Position + 30 + entry.Name.Length + 4) % 4)) % 4);
                writer.Write(0x04034b50u); writer.Write((ushort)20); writer.Write((ushort)0x800);
                writer.Write((ushort)0); writer.Write((ushort)0); writer.Write((ushort)33);
                writer.Write(entry.Crc); writer.Write(entry.Size); writer.Write(entry.Size);
                writer.Write(checked((ushort)entry.Name.Length)); writer.Write((ushort)(4 + padding)); writer.Write(entry.Name);
                writer.Write((ushort)0xffff); writer.Write((ushort)padding); writer.Write(new byte[padding]);
                writer.Write(data); entries.Add(entry);
            }
            uint centralOffset = checked((uint)writer.BaseStream.Position);
            foreach (Entry entry in entries) {
                writer.Write(0x02014b50u); writer.Write((ushort)20); writer.Write((ushort)20);
                writer.Write((ushort)0x800); writer.Write((ushort)0); writer.Write((ushort)0); writer.Write((ushort)33);
                writer.Write(entry.Crc); writer.Write(entry.Size); writer.Write(entry.Size);
                writer.Write(checked((ushort)entry.Name.Length)); writer.Write((ushort)0); writer.Write((ushort)0);
                writer.Write((ushort)0); writer.Write((ushort)0); writer.Write(0u); writer.Write(entry.Offset); writer.Write(entry.Name);
            }
            uint centralSize = checked((uint)writer.BaseStream.Position - centralOffset);
            writer.Write(0x06054b50u); writer.Write((ushort)0); writer.Write((ushort)0);
            writer.Write(checked((ushort)entries.Count)); writer.Write(checked((ushort)entries.Count));
            writer.Write(centralSize); writer.Write(centralOffset); writer.Write((ushort)0);
        }
    }
}
