// Execute each production initializer/registrar against API fixtures; no Minecraft launch.
// Usage: JAVA_HOME=/path/to/jdk21 node tools/test-creative-tabs.mjs
// The fixtures model separate parent/search entries, not the rendered client search index.
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { root, variants } from './project-utils.mjs';

if (process.argv.length > 2) {
  console.error('Usage: node tools/test-creative-tabs.mjs');
  process.exit(2);
}

const temp = mkdtempSync(join(tmpdir(), 'lazoboombox-creative-tabs-'));
const java = name => process.env.JAVA_HOME
  ? join(process.env.JAVA_HOME, 'bin', `${name}${process.platform === 'win32' ? '.exe' : ''}`)
  : name;

// Visibility behavior is documented by the upstream implementations:
// https://github.com/neoforged/NeoForge/blob/1.21.11/src/main/java/net/neoforged/neoforge/event/BuildCreativeModeTabContentsEvent.java
// https://github.com/FabricMC/fabric-api/blob/1.21.1/fabric-item-group-api-v1/src/main/java/net/fabricmc/fabric/api/itemgroup/v1/FabricItemGroupEntries.java
const fixtures = {
  'com/eyecrasher/lazoboombox/block/ModBlocks.java': `package com.eyecrasher.lazoboombox.block;
    public final class ModBlocks {
      public static final net.minecraft.world.item.Item BOOMBOX_ITEM = new net.minecraft.world.item.Item();
      public static boolean initialized;
      public static void initialize() { initialized = true; }
      public static void register(net.neoforged.bus.api.IEventBus bus) { initialized = true; }
    }`,
  'com/eyecrasher/lazoboombox/config/BoomboxConfig.java': `package com.eyecrasher.lazoboombox.config;
    public final class BoomboxConfig {
      public static final Flag ALLOW_PLAYBACK_ON_SABLE_PLATFORMS = new Flag();
      public static void load() {}
      public static final class Flag { public boolean get() { return false; } }
    }`,
  'com/eyecrasher/lazoboombox/event/BoomboxEvents.java': `package com.eyecrasher.lazoboombox.event;
    public final class BoomboxEvents { public static void register() {} }`,
  'com/eyecrasher/lazoboombox/event/BoomboxSableEvents.java': `package com.eyecrasher.lazoboombox.event;
    public final class BoomboxSableEvents { public static void registerIfSablePresent() {} }`,
  'com/eyecrasher/lazoboombox/item/ModItems.java': `package com.eyecrasher.lazoboombox.item;
    public final class ModItems { public static void initialize() {} }`,
  'com/eyecrasher/lazoboombox/network/BoomboxNetworking.java': `package com.eyecrasher.lazoboombox.network;
    public final class BoomboxNetworking {
      public static void registerServer() {}
      public static void registerServer(net.neoforged.bus.api.IEventBus bus) {}
    }`,
  'com/eyecrasher/lazoboombox/voice/LazoBoomboxServerBootstrap.java': `package com.eyecrasher.lazoboombox.voice;
    public final class LazoBoomboxServerBootstrap { public static void loadPlasmoAddon() {} }`,
  'org/slf4j/Logger.java': `package org.slf4j;
    public interface Logger { default void info(String message) {} default void warn(String message) {} }`,
  'org/slf4j/LoggerFactory.java': `package org.slf4j;
    public final class LoggerFactory { public static Logger getLogger(String name) { return new Logger() {}; } }`,
  'net/minecraft/server/MinecraftServer.java': `package net.minecraft.server; public class MinecraftServer {}`,
  'net/fabricmc/api/ModInitializer.java': `package net.fabricmc.api;
    public interface ModInitializer { void onInitialize(); }`,
  'net/neoforged/fml/common/Mod.java': `package net.neoforged.fml.common;
    public @interface Mod { String value(); }`,
  'net/neoforged/fml/event/lifecycle/FMLCommonSetupEvent.java': `package net.neoforged.fml.event.lifecycle;
    public final class FMLCommonSetupEvent { public void enqueueWork(Runnable work) { work.run(); } }`,
  'net/neoforged/neoforge/common/NeoForge.java': `package net.neoforged.neoforge.common;
    public final class NeoForge {
      public static final GlobalBus EVENT_BUS = new GlobalBus();
      public static final class GlobalBus { public void register(Object listener) {} }
    }`,
  'net/minecraft/core/registries/Registries.java': `package net.minecraft.core.registries;
    public final class Registries { public static final String CREATIVE_MODE_TAB = "creative_mode_tab"; }`,
  'net/minecraft/resources/ResourceLocation.java': `package net.minecraft.resources;
    public record ResourceLocation(String namespace, String path) {
      public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        return new ResourceLocation(namespace, path);
      }
      public String toString() { return namespace + ":" + path; }
    }`,
  'net/minecraft/resources/Identifier.java': `package net.minecraft.resources;
    public record Identifier(String namespace, String path) {
      public static Identifier fromNamespaceAndPath(String namespace, String path) {
        return new Identifier(namespace, path);
      }
      public String toString() { return namespace + ":" + path; }
    }`,
  'net/minecraft/resources/ResourceKey.java': `package net.minecraft.resources;
    public final class ResourceKey<T> {
      private static final java.util.Map<String, ResourceKey<?>> KEYS = new java.util.HashMap<>();
      @SuppressWarnings("unchecked")
      public static <T> ResourceKey<T> create(Object registry, Object location) {
        return (ResourceKey<T>) KEYS.computeIfAbsent(registry + "/" + location, key -> new ResourceKey<>());
      }
    }`,
  'net/minecraft/world/item/Item.java': `package net.minecraft.world.item;
    public class Item { public Item get() { return this; } }`,
  'net/minecraft/world/item/ItemStack.java': `package net.minecraft.world.item;
    public record ItemStack(Item item) { public Item getItem() { return item; } }`,
  'net/minecraft/world/item/Items.java': `package net.minecraft.world.item;
    public final class Items {
      public static final Item JUKEBOX = new Item(), HEAD = new Item(), TAIL = new Item();
    }`,
  'net/minecraft/world/item/CreativeModeTab.java': `package net.minecraft.world.item;
    public final class CreativeModeTab {
      public enum TabVisibility { PARENT_TAB_ONLY, SEARCH_TAB_ONLY, PARENT_AND_SEARCH_TABS }
    }`,
  'net/minecraft/world/item/CreativeModeTabs.java': `package net.minecraft.world.item;
    import net.minecraft.core.registries.Registries;
    import net.minecraft.resources.ResourceKey;
    import net.minecraft.resources.ResourceLocation;
    public final class CreativeModeTabs {
      public static final ResourceKey<CreativeModeTab> FUNCTIONAL_BLOCKS = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("minecraft", "functional_blocks"));
      public static final ResourceKey<CreativeModeTab> INGREDIENTS = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("minecraft", "ingredients"));
    }`,
  'fixture/TabEntries.java': `package fixture;
    import java.util.ArrayList;
    import java.util.List;
    import net.minecraft.world.item.CreativeModeTab.TabVisibility;
    import net.minecraft.world.item.Item;
    import net.minecraft.world.item.Items;
    public class TabEntries {
      public static final List<Item> ORIGINAL = List.of(Items.HEAD, Items.JUKEBOX, Items.TAIL);
      public final List<Item> parent = new ArrayList<>(ORIGINAL);
      public final List<Item> search = new ArrayList<>(ORIGINAL);
      public void insertAfter(Item anchor, Item item, TabVisibility visibility, boolean requireAnchor) {
        if (visibility != TabVisibility.SEARCH_TAB_ONLY) insert(parent, anchor, item, requireAnchor);
        if (visibility != TabVisibility.PARENT_TAB_ONLY) insert(search, anchor, item, requireAnchor);
      }
      private static void insert(List<Item> entries, Item anchor, Item item, boolean requireAnchor) {
        if (entries.contains(item)) throw new IllegalArgumentException("duplicate creative item");
        int index = entries.lastIndexOf(anchor);
        if (index < 0 && requireAnchor) throw new IllegalArgumentException("missing anchor");
        entries.add(index < 0 ? entries.size() : index + 1, item);
      }
    }`,
  'net/fabricmc/fabric/api/itemgroup/v1/FabricItemGroupEntries.java': `package net.fabricmc.fabric.api.itemgroup.v1;
    import net.minecraft.world.item.CreativeModeTab.TabVisibility;
    import net.minecraft.world.item.Item;
    public final class FabricItemGroupEntries extends fixture.TabEntries {
      public void addAfter(Item anchor, Item... items) {
        for (Item item : items) insertAfter(anchor, item, TabVisibility.PARENT_AND_SEARCH_TABS, false);
      }
    }`,
  'net/fabricmc/fabric/api/itemgroup/v1/ItemGroupEvents.java': `package net.fabricmc.fabric.api.itemgroup.v1;
    import java.util.HashMap;
    import java.util.Map;
    import java.util.function.Consumer;
    import net.minecraft.resources.ResourceKey;
    import net.minecraft.world.item.CreativeModeTab;
    public final class ItemGroupEvents {
      private static final Map<ResourceKey<CreativeModeTab>, Event> EVENTS = new HashMap<>();
      public static Event modifyEntriesEvent(ResourceKey<CreativeModeTab> key) {
        if (!com.eyecrasher.lazoboombox.block.ModBlocks.initialized) throw new AssertionError("blocks not registered first");
        return EVENTS.computeIfAbsent(key, ignored -> new Event());
      }
      public static void fire(ResourceKey<CreativeModeTab> key, FabricItemGroupEntries entries) {
        Event event = EVENTS.get(key);
        if (event != null) event.listener.accept(entries);
      }
      public static final class Event {
        private Consumer<FabricItemGroupEntries> listener;
        public void register(Consumer<FabricItemGroupEntries> listener) {
          if (this.listener != null) throw new AssertionError("duplicate listener");
          this.listener = listener;
        }
      }
    }`,
  'net/neoforged/bus/api/IEventBus.java': `package net.neoforged.bus.api;
    public interface IEventBus {
      <T> void addListener(java.util.function.Consumer<T> listener);
    }`,
  'net/neoforged/neoforge/event/BuildCreativeModeTabContentsEvent.java': `package net.neoforged.neoforge.event;
    import net.minecraft.resources.ResourceKey;
    import net.minecraft.world.item.CreativeModeTab;
    import net.minecraft.world.item.ItemStack;
    public final class BuildCreativeModeTabContentsEvent extends fixture.TabEntries {
      private final ResourceKey<CreativeModeTab> key;
      public BuildCreativeModeTabContentsEvent(ResourceKey<CreativeModeTab> key) { this.key = key; }
      public ResourceKey<CreativeModeTab> getTabKey() { return key; }
      public void insertAfter(ItemStack anchor, ItemStack item, CreativeModeTab.TabVisibility visibility) {
        insertAfter(anchor.getItem(), item.getItem(), visibility, true);
      }
    }`,
  'fixture/EventBus.java': `package fixture;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.function.Consumer;
    import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
    public final class EventBus implements net.neoforged.bus.api.IEventBus {
      private final List<Consumer<?>> listeners = new ArrayList<>();
      public <T> void addListener(Consumer<T> listener) {
        if (!com.eyecrasher.lazoboombox.block.ModBlocks.initialized) throw new AssertionError("blocks not registered first");
        listeners.add(listener);
      }
      @SuppressWarnings("unchecked")
      public void fire(BuildCreativeModeTabContentsEvent event) {
        // Production registers creative contents first, common setup second. Other hooks are no-ops.
        if (listeners.size() != 2) throw new AssertionError("missing or duplicate initializer listeners");
        ((Consumer<BuildCreativeModeTabContentsEvent>) listeners.get(0)).accept(event);
      }
    }`,
};

function regressionSource(loader) {
  const initialize = loader === 'fabric' ? 'new LazoBoombox().onInitialize();'
    : 'var bus = new fixture.EventBus(); new LazoBoombox(bus);';
  const dispatch = loader === 'fabric'
    ? 'var entries = new net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries(); net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.fire(key, entries);'
    : 'var entries = new net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent(key); bus.fire(entries);';
  return `package fixture;
    import com.eyecrasher.lazoboombox.block.ModBlocks;
    import com.eyecrasher.lazoboombox.LazoBoombox;
    import java.util.List;
    import net.minecraft.world.item.CreativeModeTabs;
    import net.minecraft.world.item.Items;
    public final class CreativeTabRegression {
      private static int assertions;
      private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
      }
      public static void main(String[] args) {
        ${initialize}
        // Fresh entries also model vanilla tab rebuilds after permissions/feature flags change.
        for (var key : List.of(CreativeModeTabs.FUNCTIONAL_BLOCKS,
            CreativeModeTabs.INGREDIENTS, CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
          ${dispatch}
          if (key == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            var expected = List.of(Items.HEAD, Items.JUKEBOX, ModBlocks.BOOMBOX_ITEM, Items.TAIL);
            check(entries.parent.equals(expected), "boombox missing or misplaced in functional tab");
            check(entries.search.equals(expected), "boombox missing or misplaced in creative search");
          } else {
            check(entries.parent.equals(TabEntries.ORIGINAL), "unrelated parent tab changed");
            check(entries.search.equals(TabEntries.ORIGINAL), "unrelated search entries changed");
          }
        }
        System.out.println("PASS: " + assertions + " assertions (initializer, tab, search, order, scope, rebuild)");
      }
    }`;
}

function writeSource(relative, content) {
  const destination = join(temp, relative);
  mkdirSync(dirname(destination), { recursive: true });
  writeFileSync(destination, content);
  return destination;
}

function execute(command, args, requireSuccess = true) {
  const result = spawnSync(command, args, { cwd: root, encoding: 'utf8', timeout: 30_000 });
  if (result.error || result.signal || (requireSuccess && result.status !== 0)) {
    throw new Error(`${command} failed\n${result.stdout ?? ''}${result.stderr || result.error || result.signal || ''}`);
  }
  return result;
}

try {
  const fixtureClasses = join(temp, 'fixtures');
  mkdirSync(fixtureClasses);
  const fixtureSources = Object.entries(fixtures).map(([name, content]) => writeSource(`src/${name}`, content));
  execute(java('javac'), ['--release', '21', '-d', fixtureClasses, ...fixtureSources]);
  const regressions = Object.fromEntries(['fabric', 'neoforge'].map(loader => [
    loader, writeSource(`${loader}/fixture/CreativeTabRegression.java`, regressionSource(loader)),
  ]));
  let negativeControls = 0;
  for (const variant of variants()) {
    const relative = 'src/main/java/com/eyecrasher/lazoboombox/item/ModCreativeTabs.java';
    const production = join(variant.directory, relative);
    const initializer = join(variant.directory, 'src/main/java/com/eyecrasher/lazoboombox/LazoBoombox.java');
    const output = join(temp, 'classes', variant.relative);
    mkdirSync(output, { recursive: true });
    execute(java('javac'), ['--release', '21', '-cp', fixtureClasses, '-d', output,
      production, initializer, regressions[variant.loader]]);
    const classpath = [output, fixtureClasses].join(process.platform === 'win32' ? ';' : ':');
    const result = execute(java('java'), ['-cp', classpath, 'fixture.CreativeTabRegression']);
    process.stdout.write(`${variant.relative}: ${result.stdout}`);
    if (variant.loader !== 'neoforge') continue;

    // Reintroduce exactly the old visibility flag, keeping the rest of production unchanged.
    const current = readFileSync(production, 'utf8');
    assert.equal(current.match(/TabVisibility\.PARENT_AND_SEARCH_TABS/g)?.length, 1);
    const original = writeSource(`negative/${variant.relative}/${relative}`,
      current.replace('TabVisibility.PARENT_AND_SEARCH_TABS', 'TabVisibility.PARENT_TAB_ONLY'));
    execute(java('javac'), ['--release', '21', '-cp', fixtureClasses, '-d', output,
      original, initializer, regressions[variant.loader]]);
    const negative = execute(java('java'), ['-cp', classpath, 'fixture.CreativeTabRegression'], false);
    assert.notEqual(negative.status, 0, 'parent-only visibility unexpectedly passed');
    assert.match(negative.stderr, /boombox missing or misplaced in creative search/);
    negativeControls++;
  }
  console.log(`PASS: ${variants().length} production initializer/registrar pairs; ${negativeControls} parent-only negative controls rejected.`);
} catch (error) {
  console.error(error.message);
  process.exitCode = 1;
} finally {
  rmSync(temp, { recursive: true, force: true });
}
