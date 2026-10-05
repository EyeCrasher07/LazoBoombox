// Compile production Fabric registration against controlled registry/builder fakes.
// This verifies the registered id, factory and supported blocks without starting Minecraft.
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { variants, read } from './project-utils.mjs';

assert.equal(process.argv.length, 2, 'This test accepts no arguments');
const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'lazoboombox-registration-'));
const java = name => process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', name) : name;
const fixtures = {
  'com/eyecrasher/lazoboombox/LazoBoombox.java': `package com.eyecrasher.lazoboombox;
    public final class LazoBoombox { public static final String MOD_ID="lazoboombox";
      public static final Log LOGGER=new Log(); public static class Log {public void info(String s){}} }`,
  'net/minecraft/resources/ResourceLocation.java': `package net.minecraft.resources;
    public record ResourceLocation(String value) {
      public static ResourceLocation fromNamespaceAndPath(String n,String p){return new ResourceLocation(n+":"+p);}
      public String toString(){return value;} }`,
  'net/minecraft/resources/Identifier.java': `package net.minecraft.resources;
    public record Identifier(String value) {
      public static Identifier fromNamespaceAndPath(String n,String p){return new Identifier(n+":"+p);}
      public String toString(){return value;} }`,
  'net/minecraft/resources/ResourceKey.java': `package net.minecraft.resources;
    public record ResourceKey<T>(String registry,String location) {
      public static <T> ResourceKey<T> create(Object registry,Object id){return new ResourceKey<>(registry.toString(),id.toString());} }`,
  'net/minecraft/core/Registry.java': `package net.minecraft.core;
    import java.util.*; public final class Registry<T> {
      public final Map<String,T> entries=new LinkedHashMap<>(); private final String name;
      public Registry(String n){name=n;} public String key(){return name;}
      public static <T,V extends T> V register(Registry<T> r,Object id,V value){
        if(r.entries.putIfAbsent(id.toString(),value)!=null)throw new AssertionError("Duplicate id");return value;} }`,
  'net/minecraft/core/registries/BuiltInRegistries.java': `package net.minecraft.core.registries;
    import net.minecraft.core.Registry;import net.minecraft.world.item.Item;
    import net.minecraft.world.level.block.Block;import net.minecraft.world.level.block.entity.BlockEntityType;
    public final class BuiltInRegistries { public static final Registry<Block> BLOCK=new Registry<>("block");
      public static final Registry<Item> ITEM=new Registry<>("item");
      public static final Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPE=new Registry<>("block_entity_type"); }`,
  'net/minecraft/world/level/material/MapColor.java': `package net.minecraft.world.level.material;
    public final class MapColor {public static final MapColor WOOD=new MapColor();}`,
  'net/minecraft/world/level/block/state/BlockBehaviour.java': `package net.minecraft.world.level.block.state;
    import net.minecraft.resources.ResourceKey;import net.minecraft.world.level.material.MapColor;
    public class BlockBehaviour {public static class Properties {
      public ResourceKey<?> id;public static Properties of(){return new Properties();}
      public Properties mapColor(MapColor c){return this;}public Properties strength(float a,float b){return this;}
      public Properties noOcclusion(){return this;}public Properties setId(ResourceKey<?> key){id=key;return this;} } }`,
  'net/minecraft/world/level/block/Block.java': `package net.minecraft.world.level.block;
    import net.minecraft.world.level.block.state.BlockBehaviour;
    public class Block {public final BlockBehaviour.Properties properties;
      public Block(BlockBehaviour.Properties p){properties=p;} }`,
  'net/minecraft/world/item/Item.java': `package net.minecraft.world.item;
    import net.minecraft.resources.ResourceKey;
    public class Item {public final Properties properties;public Item(Properties p){properties=p;}
      public static class Properties {public ResourceKey<?> id;public int count;
        public Properties setId(ResourceKey<?> key){id=key;return this;}
        public Properties stacksTo(int n){count=n;return this;}public Properties fireResistant(){return this;} } }`,
  'com/eyecrasher/lazoboombox/item/BoomboxItem.java': `package com.eyecrasher.lazoboombox.item;
    import net.minecraft.world.item.Item;import net.minecraft.world.level.block.Block;
    public class BoomboxItem extends Item {public final Block block;
      public BoomboxItem(Block b,Properties p){super(p);block=b;} }`,
  'com/eyecrasher/lazoboombox/block/BoomboxBlock.java': `package com.eyecrasher.lazoboombox.block;
    import net.minecraft.world.level.block.Block;import net.minecraft.world.level.block.state.BlockBehaviour;
    public class BoomboxBlock extends Block {public BoomboxBlock(BlockBehaviour.Properties p){super(p);} }`,
  'net/minecraft/core/BlockPos.java': `package net.minecraft.core;public record BlockPos(int x,int y,int z){}`,
  'net/minecraft/world/level/block/state/BlockState.java': `package net.minecraft.world.level.block.state;
    import net.minecraft.world.level.block.Block;public record BlockState(Block block){}`,
  'net/minecraft/world/level/block/entity/BlockEntity.java': `package net.minecraft.world.level.block.entity;
    import net.minecraft.core.BlockPos;import net.minecraft.world.level.block.state.BlockState;
    public class BlockEntity {public final BlockPos pos;public final BlockState state;
      public BlockEntity(BlockPos p,BlockState s){pos=p;state=s;} }`,
  'com/eyecrasher/lazoboombox/block/BoomboxBlockEntity.java': `package com.eyecrasher.lazoboombox.block;
    import net.minecraft.core.BlockPos;import net.minecraft.world.level.block.state.BlockState;
    import net.minecraft.world.level.block.entity.BlockEntity;
    public class BoomboxBlockEntity extends BlockEntity {public BoomboxBlockEntity(BlockPos p,BlockState s){super(p,s);} }`,
  'net/minecraft/world/level/block/entity/BlockEntityType.java': `package net.minecraft.world.level.block.entity;
    import java.util.*;import net.minecraft.core.BlockPos;import net.minecraft.world.level.block.Block;
    import net.minecraft.world.level.block.state.BlockState;
    public class BlockEntityType<T extends BlockEntity> {
      public interface Factory<T extends BlockEntity>{T create(BlockPos p,BlockState s);}
      private final Factory<? extends T> factory;private final Set<Block> blocks;
      public BlockEntityType(Factory<? extends T> f,Set<Block> b){factory=f;blocks=b;}
      public T create(BlockPos p,BlockState s){return factory.create(p,s);}
      public boolean isValid(BlockState s){return blocks.contains(s.block());}
      public static <T extends BlockEntity> BlockEntityType<T> register(String id,Factory<T> f,Block... b){
        throw new AssertionError("VANILLA_DFU_LOOKUP: "+id);}
      public static final class Builder<T extends BlockEntity> {
        private final Factory<T> factory;private final Block[] blocks;
        private Builder(Factory<T> f,Block[] b){factory=f;blocks=b;}
        public static <T extends BlockEntity> Builder<T> of(Factory<T> f,Block... b){return new Builder<>(f,b);}
        public BlockEntityType<T> build(Object schema){
          if(schema!=null)throw new AssertionError("Unexpected vanilla schema");
          return new BlockEntityType<>(factory,Set.of(blocks));} } }`,
  'net/fabricmc/fabric/api/object/builder/v1/block/entity/FabricBlockEntityTypeBuilder.java': `package net.fabricmc.fabric.api.object.builder.v1.block.entity;
    import java.util.*;import net.minecraft.world.level.block.Block;
    import net.minecraft.world.level.block.entity.BlockEntity;import net.minecraft.world.level.block.entity.BlockEntityType;
    public final class FabricBlockEntityTypeBuilder<T extends BlockEntity> {
      private final BlockEntityType.Factory<T> factory;private final Block[] blocks;
      private FabricBlockEntityTypeBuilder(BlockEntityType.Factory<T> f,Block[] b){factory=f;blocks=b;}
      public static <T extends BlockEntity> FabricBlockEntityTypeBuilder<T> create(BlockEntityType.Factory<T> f,Block... b){
        return new FabricBlockEntityTypeBuilder<>(f,b);}
      public BlockEntityType<T> build(){return new BlockEntityType<>(factory,Set.of(blocks));} }`,
  'RegistrationTest.java': `import com.eyecrasher.lazoboombox.block.*;
    import net.minecraft.core.*;import net.minecraft.core.registries.*;
    import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.*;
    public class RegistrationTest {
      private static int checks;private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("check "+checks);}
      public static void main(String[] args){ModBlocks.initialize();String id="lazoboombox:boombox";
        check(BuiltInRegistries.BLOCK.entries.size()==1);check(BuiltInRegistries.ITEM.entries.size()==1);
        check(BuiltInRegistries.BLOCK_ENTITY_TYPE.entries.size()==1);
        check(BuiltInRegistries.BLOCK.entries.get(id)==ModBlocks.BOOMBOX);
        check(BuiltInRegistries.ITEM.entries.get(id)==ModBlocks.BOOMBOX_ITEM);
        check(BuiltInRegistries.BLOCK_ENTITY_TYPE.entries.get(id)==ModBlocks.BOOMBOX_ENTITY);
        check(ModBlocks.BOOMBOX_ITEM.block==ModBlocks.BOOMBOX);check(ModBlocks.BOOMBOX_ITEM.properties.count==1);
        var p=new BlockPos(3,4,5);var s=new BlockState(ModBlocks.BOOMBOX);var be=ModBlocks.BOOMBOX_ENTITY.create(p,s);
        check(be instanceof BoomboxBlockEntity);check(be.pos.equals(p));check(be.state==s);
        check(ModBlocks.BOOMBOX_ENTITY.isValid(s));
        check(!ModBlocks.BOOMBOX_ENTITY.isValid(new BlockState(new Block(BlockBehaviour.Properties.of()))));
        if(!args[0].equals("1.21.1")){
          check(ModBlocks.BOOMBOX.properties.id.location().equals(id));
          check(ModBlocks.BOOMBOX_ITEM.properties.id.location().equals(id));}
        System.out.println("PASS "+args[0]+" registration: "+checks+" assertions");} }`,
};

function compileAndRun(variant, source, suffix = '') {
  const directory = path.join(temp, variant.minecraft + suffix);
  const sources = [];
  for (const [name, text] of Object.entries({ ...fixtures,
    'com/eyecrasher/lazoboombox/block/ModBlocks.java': source })) {
    const file = path.join(directory, name);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, text);
    sources.push(file);
  }
  const classes = path.join(directory, 'classes');
  const compilation = spawnSync(java('javac'), ['--release', '21', '-d', classes, ...sources],
    { encoding: 'utf8', timeout: 60_000 });
  assert.equal(compilation.error, undefined, compilation.error?.message);
  assert.equal(compilation.status, 0, compilation.stdout + compilation.stderr);
  const result = spawnSync(java('java'), ['-cp', classes, 'RegistrationTest', variant.minecraft],
    { encoding: 'utf8', timeout: 30_000 });
  assert.equal(result.error, undefined, result.error?.message);
  return result;
}

try {
  for (const variant of variants().filter(variant => variant.loader === 'fabric')) {
    const source = read(path.join(variant.directory,
      'src/main/java/com/eyecrasher/lazoboombox/block/ModBlocks.java'));
    const result = compileAndRun(variant, source);
    assert.equal(result.status, 0, result.stdout + result.stderr);
    process.stdout.write(result.stdout);
    if (variant.minecraft === '1.21.2' || variant.minecraft === '1.21.11') {
      // Restore the original helper in memory, keeping the real production registration code.
      const original = source.replace(/Registry\.register\(\s*BuiltInRegistries\.BLOCK_ENTITY_TYPE,\s*id,\s*FabricBlockEntityTypeBuilder\.create\(BoomboxBlockEntity::new, BOOMBOX\)\s*\.build\(\)\)/,
        'BlockEntityType.register(LazoBoombox.MOD_ID + ":boombox", BoomboxBlockEntity::new, BOOMBOX)');
      assert.notEqual(original, source, 'Negative control did not restore the original helper');
      const negative = compileAndRun(variant, original, '-negative');
      assert.notEqual(negative.status, 0, 'Original registration unexpectedly passed');
      assert.match(negative.stderr, /VANILLA_DFU_LOOKUP: lazoboombox:boombox/);
      console.log(`PASS ${variant.minecraft} negative control rejects the vanilla-only DFU lookup`);
    }
  }
} finally {
  fs.rmSync(temp, { recursive: true, force: true });
}
