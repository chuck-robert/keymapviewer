package com.keymapviewer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.keymapviewer.hotkey.KeyBindingInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 快捷键导入/导出（纯逻辑）：
 * 导出 → <gameDir>/kmv/output/yyyyMMdd-HHmmss.json
 * 导入 → <gameDir>/kmv/input/ 下时间戳最新的文件
 * （gameDir 由 config 目录回退两级得到，即与 mods 同级）。
 */
public final class BindingsIo {
    public record ExportEntry(String source, String mod, String category, String action, int[] codes) {
    }

    private BindingsIo() {
    }

    /** <gameDir>/kmv */
    public static Path baseDir() {
        return ModConfig.configPath().getParent().getParent().resolve("kmv");
    }

    public static Path exportDir() {
        return baseDir().resolve("output");
    }

    public static Path importDir() {
        return baseDir().resolve("input");
    }

    /** 本次导出的文件名（时间戳）。 */
    public static Path exportFile() {
        return exportDir().resolve(timestamp() + ".json");
    }

    private static String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    /** import 目录里最新（文件名最大）的备份文件。 */
    public static Optional<Path> latestImport() {
        Path dir = importDir();
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .max(Comparator.comparing(p -> p.getFileName().toString()));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /** 最近一次导出的文件（作为“打开备份文件”的目标）。 */
    public static Optional<Path> latestExport() {
        Path dir = exportDir();
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .max(Comparator.comparing(p -> p.getFileName().toString()));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static String exportJson(List<KeyBindingInfo> bindings) {
        JsonArray arr = new JsonArray();
        for (KeyBindingInfo info : bindings) {
            JsonObject o = new JsonObject();
            o.addProperty("source", info.source.name());
            o.addProperty("mod", info.modName);
            o.addProperty("category", info.category);
            o.addProperty("action", info.actionName);
            JsonArray codes = new JsonArray();
            for (int c : info.keyCodes) {
                codes.add(c);
            }
            o.add("codes", codes);
            arr.add(o);
        }
        return new GsonBuilder().setPrettyPrinting().create().toJson(arr);
    }

    public static List<ExportEntry> parse(String json) {
        List<ExportEntry> out = new ArrayList<>();
        JsonArray arr = new Gson().fromJson(json, JsonArray.class);
        if (arr == null) {
            return out;
        }
        for (JsonElement eIn : arr) {
            if (!eIn.isJsonObject()) {
                continue;
            }
            JsonObject o = eIn.getAsJsonObject();
            try {
                String source = o.get("source").getAsString();
                String mod = o.get("mod").getAsString();
                String category = o.get("category").getAsString();
                String action = o.get("action").getAsString();
                List<Integer> codes = new ArrayList<>();
                for (JsonElement c : o.getAsJsonArray("codes")) {
                    codes.add(c.getAsInt());
                }
                int[] arr2 = new int[codes.size()];
                for (int i = 0; i < codes.size(); i++) {
                    arr2[i] = codes.get(i);
                }
                out.add(new ExportEntry(source, mod, category, action, arr2));
            } catch (Exception ignored) {
                // 单条损坏跳过
            }
        }
        return out;
    }

    /** 导出并返回写出的文件路径。 */
    public static Path writeToFile(List<KeyBindingInfo> bindings) throws IOException {
        Path p = exportFile();
        if (p.getParent() != null) {
            Files.createDirectories(p.getParent());
        }
        Files.writeString(p, exportJson(bindings));
        return p;
    }

    /** 读取 import 目录里最新的备份；没有则抛 IO 异常。 */
    public static List<ExportEntry> readFromFile() throws IOException {
        Path p = latestImport().orElseThrow(() -> new IOException("no import file"));
        return parse(Files.readString(p));
    }
}