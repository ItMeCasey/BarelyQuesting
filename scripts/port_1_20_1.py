"""One-off: derive version-1.20.1 from version-1.21.1 (items carry NBT instead of components, a few renamed calls)."""
import re

BASE = 'version-1.20.1/src/main/java/com/itmecasey/barelyquesting/compat/'


def edit(name, fn, base=BASE):
    p = base + name
    s = open(p, encoding='utf8').read()
    s = fn(s)
    open(p, 'w', encoding='utf8').write(s)


def mc(s):
    s = s.replace('import net.minecraft.core.component.DataComponentPatch;\n', '')
    s = s.replace('ResourceLocation.parse(s.contains(":") ? s : "minecraft:" + s)', 'new ResourceLocation(s.contains(":") ? s : "minecraft:" + s)')
    s = s.replace('ResourceLocation.fromNamespaceAndPath(ns, path)', 'new ResourceLocation(ns, path)')
    a = s.index('    /** The item data (component patch)')
    b = s.index('    public static ItemSpec specOf')
    s = s[:a] + '''    /** The item's NBT as SNBT text, or null when there is none. */
    public static String dataOf(ItemStack stack, RegistryAccess access) {
        CompoundTag tag = stack.getTag();
        return tag == null || tag.isEmpty() ? null : tag.toString();
    }

''' + s[b:]
    a = s.index('        if (spec.data() != null && access != null) {')
    b = s.index('        return stack;\n    }\n\n    /** Whether a stack satisfies')
    s = s[:a] + '''        if (spec.data() != null) {
            try {
                stack.setTag(TagParser.parseTag(spec.data()));
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                // keep the plain stack
            }
        }
''' + s[b:]
    s = s.replace('TagParser.parseTag(wanted)', 'TagParser.parseTag(wanted)')
    s = s.replace('item data is a component patch', 'item data is NBT')
    s = s.replace('Minecraft 1.21.1', 'Minecraft 1.20.1')
    return s


def player(s):
    s = s.replace('stack.copyWithCount(n)', 'copyOf(stack, n)')
    s = s.replace('    @Override\n    public void addExperiencePoints', '''    private static ItemStack copyOf(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    @Override
    public void addExperiencePoints''', 1)
    s = s.replace('import net.minecraft.advancements.AdvancementHolder;', 'import net.minecraft.advancements.Advancement;')
    s = s.replace('AdvancementHolder holder = level().getServer().getAdvancements().get(Mc.id(id));',
                  'Advancement holder = level().getServer().getAdvancements().getAdvancement(Mc.id(id));')
    return s


def gfx(s):
    s = s.replace('stack.copyWithCount(item.count())', 'copyOf(stack, item.count())')
    s = s.replace('    @Override\n    public int width()', '''    private static ItemStack copyOf(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    @Override
    public int width()''', 1)
    s = s.replace('Minecraft 1.21.1', 'Minecraft 1.20.1')
    return s


def screen(s):
    s = s.replace('''    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return ui.mouseScrolled((int) x, (int) y, scrollY) || super.mouseScrolled(x, y, scrollX, scrollY);''',
                  '''    public boolean mouseScrolled(double x, double y, double delta) {
        return ui.mouseScrolled((int) x, (int) y, delta) || super.mouseScrolled(x, y, delta);''')
    s = s.replace('''    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {''',
                  '''    public void renderBackground(GuiGraphics graphics) {''')
    s = s.replace('Minecraft 1.21.1', 'Minecraft 1.20.1')
    return s


def hooks(s):
    return s.replace('1.21.1', '1.20.1')


edit('Mc.java', mc)
edit('McPlayer.java', player)
edit('GfxImpl.java', gfx)
edit('QuestScreen.java', screen)
edit('ClientHooks.java', hooks)
edit('McEnv.java', hooks)
edit('ServerHooks.java', hooks)

# self test: NBT name instead of a component
p = 'version-1.20.1/src/dev/java/com/itmecasey/barelyquesting/dev/SelfTest.java'
s = open(p, encoding='utf8').read()
s = s.replace('import net.minecraft.core.component.DataComponents;\n', '')
s = s.replace('named.set(DataComponents.CUSTOM_NAME, Component.literal("Quest Blade"));',
              'named.setHoverName(Component.literal("Quest Blade"));')
s = s.replace('new ServerPlayer(server, server.overworld(), new GameProfile(UUID.nameUUIDFromBytes("selftest".getBytes()), "selftest"),\n                ClientInformation.createDefault())',
              'new ServerPlayer(server, server.overworld(), new GameProfile(UUID.nameUUIDFromBytes("selftest".getBytes()), "selftest"))')
s = s.replace('import net.minecraft.server.level.ClientInformation;\n', '')
open(p, 'w', encoding='utf8').write(s)
print('done')
