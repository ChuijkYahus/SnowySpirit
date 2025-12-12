package net.mehvahdjukaar.snowyspirit.reg;

import com.google.common.base.Preconditions;
import net.mehvahdjukaar.moonlight.api.misc.RegSupplier;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.moonlight.api.set.wood.VanillaWoodTypes;
import net.mehvahdjukaar.snowyspirit.SnowySpirit;
import net.mehvahdjukaar.snowyspirit.configs.CommonConfigs;
import net.mehvahdjukaar.snowyspirit.integration.supp.SuppCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ModCreativeTabs {

    public static final RegSupplier<CreativeModeTab> MOD_TAB = !CommonConfigs.MOD_TAB.get() ? null :
            RegHelper.registerCreativeModeTab(SnowySpirit.res(SnowySpirit.MOD_ID), builder ->
                    builder.title(Component.translatable("tab.snowyspirit")).icon(
                            () -> ModRegistry.SLED_ITEMS.get(VanillaWoodTypes.OAK).getDefaultInstance()));


    public static void init() {
        RegHelper.addItemsToTabsRegistration(ModCreativeTabs::registerItemsToTabs);
    }

    public static void registerItemsToTabs(RegHelper.ItemToTabEvent e) {
        TabAdder adder = new TabAdder(e);

        adder.after(i -> i.getItem().components().get(DataComponents.JUKEBOX_PLAYABLE) != null, CreativeModeTabs.TOOLS_AND_UTILITIES,
                ModRegistry.WINTER_DISC_NAME,
                ModRegistry.WINTER_DISC); // TODO

        adder.before(Items.HONEY_BOTTLE, CreativeModeTabs.FOOD_AND_DRINKS,
                ModRegistry.EGGNOG_NAME,
                ModRegistry.EGGNOG);

        if (SnowySpirit.SUPPLEMENTARIES_INSTALLED) {
            adder.after(SuppCompat::isCandy, CreativeModeTabs.FOOD_AND_DRINKS,
                    ModRegistry.GINGER_NAME,
                    ModRegistry.GINGERBREAD_COOKIE);
            adder.after(SuppCompat::isCandy, CreativeModeTabs.FOOD_AND_DRINKS,
                    ModRegistry.CANDY_CANE_NAME,
                    ModRegistry.CANDY_CANE);

            adder.after(SuppCompat::isGlobe, CreativeModeTabs.FUNCTIONAL_BLOCKS,
                    ModRegistry.SNOW_GLOBE_NAME,
                    ModRegistry.SNOW_GLOBE);
        } else {
            adder.before(Items.ROTTEN_FLESH, CreativeModeTabs.FOOD_AND_DRINKS,
                    ModRegistry.GINGER_NAME,
                    ModRegistry.GINGERBREAD_COOKIE);
            adder.before(Items.ROTTEN_FLESH, CreativeModeTabs.FOOD_AND_DRINKS,
                    ModRegistry.CANDY_CANE_NAME,
                    ModRegistry.CANDY_CANE);

            adder.after(Items.BELL, CreativeModeTabs.FUNCTIONAL_BLOCKS,
                    ModRegistry.SNOW_GLOBE_NAME,
                    ModRegistry.SNOW_GLOBE);
        }
        adder.after(ItemTags.BOATS, CreativeModeTabs.TOOLS_AND_UTILITIES,
                ModRegistry.SLED_NAME,
                ModRegistry.SLED_ITEMS.values().stream()
                        .map(i -> (Supplier<Item>) i::asItem).toArray(Supplier[]::new));
        adder.before(ItemTags.BANNERS, CreativeModeTabs.COLORED_BLOCKS,
                ModRegistry.GLOW_LIGHTS_NAME,
                ModRegistry.GLOW_LIGHTS_ITEMS.values().toArray(Supplier[]::new));

        adder.before(ItemTags.BANNERS, CreativeModeTabs.COLORED_BLOCKS,
                ModRegistry.GUMDROP_NAME,
                ModRegistry.GUMDROPS_BUTTONS.values().toArray(Supplier[]::new));


        adder.before(ItemTags.BANNERS, CreativeModeTabs.FUNCTIONAL_BLOCKS,
                ModRegistry.GLOW_LIGHTS_NAME,
                ModRegistry.GLOW_LIGHTS_ITEMS.values().toArray(Supplier[]::new));

        adder.before(ItemTags.BANNERS, CreativeModeTabs.FUNCTIONAL_BLOCKS,
                ModRegistry.GUMDROP_NAME,
                ModRegistry.GUMDROPS_BUTTONS.values().toArray(Supplier[]::new));

        adder.before(Items.GLOW_BERRIES, CreativeModeTabs.NATURAL_BLOCKS,
                ModRegistry.GINGER_NAME,
                ModRegistry.GINGER_FLOWER);

        adder.before(Items.WHEAT, CreativeModeTabs.INGREDIENTS,
                ModRegistry.GINGER_NAME,
                ModRegistry.GINGER);

        adder.after(Items.SMALL_DRIPLEAF, CreativeModeTabs.NATURAL_BLOCKS,
                ModRegistry.GINGER_NAME,
                ModRegistry.GINGER_WILD);

        adder.add(CreativeModeTabs.BUILDING_BLOCKS,
                ModRegistry.GINGER_NAME,
                ModRegistry.GINGERBREAD_FROSTED_BLOCK,
                ModRegistry.GINGERBREAD_BLOCK,
                ModRegistry.GINGERBREAD_STAIRS,
                ModRegistry.GINGERBREAD_SLAB,
                ModRegistry.GINGERBREAD_DOOR,
                ModRegistry.GINGERBREAD_TRAPDOOR);

        adder.add(CreativeModeTabs.BUILDING_BLOCKS,
                ModRegistry.CANDY_CANE_NAME,
                ModRegistry.CANDY_CANE_BLOCK);

        adder.before(Items.BOOKSHELF, CreativeModeTabs.FUNCTIONAL_BLOCKS,
                ModRegistry.WREATH_NAME,
                ModRegistry.WREATH);

        adder.add(CreativeModeTabs.SPAWN_EGGS,
                ModRegistry.GINGERBREAD_GOLEM_NAME,
                ModRegistry.GINGERBREAD_GOLEM_EGG);
    }


    public static final class TabAdder {
        private final RegHelper.ItemToTabEvent event;

        private final List<ItemStack> uniqueStacksAdded = new ArrayList<>();

        public TabAdder(RegHelper.ItemToTabEvent event) {
            this.event = event;
        }

        private void before(ResourceKey<CreativeModeTab> tab, Predicate<ItemStack> target, ItemStack... items) {
            if (MOD_TAB != null) {
                add(tab, items);
                return;
            }
            for (ItemStack stack : items) {
                event.addBefore(tab, target, stack);
            }
        }

        private void before(ResourceKey<CreativeModeTab> tab, Predicate<ItemStack> target, ItemLike... items) {
            before(tab, target, Arrays.stream(items)
                    .map(i -> i.asItem().getDefaultInstance()).toArray(ItemStack[]::new));
        }

        private void after(ResourceKey<CreativeModeTab> tab, Predicate<ItemStack> target, ItemStack... items) {
            if (MOD_TAB != null) {
                add(tab, items);
                return;
            }
            for (ItemStack stack : items) {
                event.addAfter(tab, target, stack);
            }
        }


        private void before(TagKey<Item> target,
                            ResourceKey<CreativeModeTab> tab, String key, Supplier<?>... items) {
            before(i -> i.is(target), tab, key, items);
        }

        private void after(ResourceKey<CreativeModeTab> tab, Predicate<ItemStack> target, ItemLike... items) {
            after(tab, target, Arrays.stream(items)
                    .map(i -> i.asItem().getDefaultInstance()).toArray(ItemStack[]::new));
        }

        private void add(ResourceKey<CreativeModeTab> tab, ItemStack... items) {
            ResourceKey<CreativeModeTab> tabKey = MOD_TAB == null ? tab : (ResourceKey<CreativeModeTab>) MOD_TAB.getKey();
            for (ItemStack stack : items) {
                if (isUnique(stack)) {
                    event.add(tabKey, stack);
                }
            }
        }

        private void add(ResourceKey<CreativeModeTab> tab, ItemLike... items) {
            add(tab, Arrays.stream(items)
                    .map(i -> i.asItem().getDefaultInstance()).toArray(ItemStack[]::new));
        }

        private boolean isUnique(ItemStack stack) {
            Preconditions.checkNotNull(stack);
            Preconditions.checkNotNull(stack.getItem());
            if (MOD_TAB == null) return true;
            for (var s : uniqueStacksAdded) {
                if (s.getItem() == stack.getItem()) {
                    if (ItemStack.isSameItemSameComponents(s, stack)) {
                        return false;
                    }
                }
            }
            uniqueStacksAdded.add(stack);
            return true;
        }

        private void after(TagKey<Item> target,
                           ResourceKey<CreativeModeTab> tab, String key, Supplier<?>... items) {
            after(i -> i.is(target), tab, key, items);
        }

        private void after(ItemLike target,
                           ResourceKey<CreativeModeTab> tab, String key, Supplier<?>... items) {
            after(i -> i.is(target.asItem()), tab, key, items);
        }

        private void after(Predicate<ItemStack> targetPred,
                           ResourceKey<CreativeModeTab> tab, String key, Supplier<?>... items) {
            if (CommonConfigs.isEnabled(key)) {
                var first = items[0].get();
                if (first instanceof ItemStack) {
                    ItemStack[] entries = Arrays.stream(items).map(s -> (ItemStack) s.get()).toArray(ItemStack[]::new);
                    after(tab, targetPred, entries);
                } else if (first instanceof Collection<?>) {
                    for (Object i : items) {
                        if (!(i instanceof Collection<?> c)) continue;
                        ItemLike[] entries = c.stream().map(s -> (ItemLike) s).toArray(ItemLike[]::new);
                        after(tab, targetPred, entries);
                    }
                } else {
                    ItemLike[] entries = Arrays.stream(items).map((s -> (ItemLike) (s.get()))).toArray(ItemLike[]::new);
                    after(tab, targetPred, entries);
                }
            }
        }

        private void before(ItemLike target,
                            ResourceKey<CreativeModeTab> tab, String key, Supplier<?>... items) {
            before(i -> i.is(target.asItem()), tab, key, items);
        }

        private void before(Predicate<ItemStack> targetPred,
                            ResourceKey<CreativeModeTab> tab, String key, Supplier<?>... items) {
            if (CommonConfigs.isEnabled(key)) {
                if (items[0].get() instanceof ItemStack) {
                    ItemStack[] entries = Arrays.stream(items).map(s -> (ItemStack) s.get()).toArray(ItemStack[]::new);
                    before(tab, targetPred, entries);
                } else {
                    ItemLike[] entries = Arrays.stream(items).map(s -> (ItemLike) s.get()).toArray(ItemLike[]::new);
                    before(tab, targetPred, entries);
                }
            }
        }

        private void add(ResourceKey<CreativeModeTab> tab, String key, Supplier<?>... items) {
            if (CommonConfigs.isEnabled(key)) {
                ItemLike[] entries = Arrays.stream(items).map((s -> (ItemLike) (s.get()))).toArray(ItemLike[]::new);
                add(tab, entries);
            }
        }

        private void afterML(Item target,
                             ResourceKey<CreativeModeTab> tab, String key, String modLoaded,
                             Supplier<?>... items) {
            if (PlatHelper.isModLoaded(modLoaded)) {
                after(target, tab, key, items);
            }
        }

        private void afterML(String modTarget,
                             ResourceKey<CreativeModeTab> tab, String key,
                             Supplier<?>... items) {
            ResourceLocation id = ResourceLocation.tryParse(modTarget);
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(target -> after(target, tab, key, items));
        }

        private void afterTL(Item target,
                             ResourceKey<CreativeModeTab> tab, String key,
                             List<String> tags,
                             Supplier<?>... items) {
            if (isTagOn(tags.toArray(String[]::new))) {
                after(target, tab, key, items);
            }
        }

        private void beforeML(Item target,
                              ResourceKey<CreativeModeTab> tab,
                              String key, String modLoaded,
                              Supplier<?>... items) {
            if (PlatHelper.isModLoaded(modLoaded)) {
                before(target, tab, key, items);
            }
        }

        private void beforeTL(Item target,
                              ResourceKey<CreativeModeTab> tab, String key,
                              List<String> tags,
                              Supplier<?>... items) {
            if (isTagOn(tags.toArray(String[]::new))) {
                after(target, tab, key, items);
            }
        }

        private static boolean isTagOn(String... tags) {
            for (var t : tags)
                if (BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.parse(t))).isPresent()) {
                    return true;
                }
            return false;
        }
    }

}
