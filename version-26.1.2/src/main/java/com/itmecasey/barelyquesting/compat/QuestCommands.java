package com.itmecasey.barelyquesting.compat;

import com.itmecasey.barelyquesting.core.edit.EditOps;
import com.itmecasey.barelyquesting.core.imports.Importer;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.net.QuestServer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /barelyquesting} (alias {@code /bq}): open, edit, reload, complete, reset, import and editor management. */
public final class QuestCommands {
    private QuestCommands() {
    }

    private static final SuggestionProvider<CommandSourceStack> QUESTS = (ctx, builder) -> {
        QuestServer q = ServerHooks.quests();
        if (q != null) {
            for (Quest quest : q.engine().book().allQuests()) {
                SharedSuggestionProvider.suggest(java.util.List.of(quest.id), builder);
            }
        }
        return builder.buildFuture();
    };

    private static LiteralArgumentBuilder<CommandSourceStack> admin(String name) {
        return Commands.literal(name).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("barelyquesting");
        root.then(Commands.literal("open")
                .executes(ctx -> open(ctx, null))
                .then(Commands.argument("quest", StringArgumentType.word()).suggests(QUESTS).executes(ctx -> open(ctx, StringArgumentType.getString(ctx, "quest")))));
        root.then(admin("edit").executes(ctx -> open(ctx, "!edit")));
        root.then(admin("reload").executes(ctx -> {
            QuestServer q = need(ctx);
            q.reload();
            ok(ctx, "Reloaded " + q.engine().book().allQuests().size() + " quests");
            return 1;
        }));
        root.then(admin("complete")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("quest", StringArgumentType.word()).suggests(QUESTS).executes(ctx -> {
                            QuestServer q = need(ctx);
                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                            String quest = StringArgumentType.getString(ctx, "quest");
                            boolean done = q.engine().forceComplete(q.engine().teams().teamOf(p.getUUID()).id, quest);
                            ok(ctx, done ? "Completed " + quest + " for " + p.getName().getString() : "Nothing to complete (unknown or already done)");
                            return done ? 1 : 0;
                        }))));
        root.then(admin("reset")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> reset(ctx, null))
                        .then(Commands.argument("quest", StringArgumentType.word()).suggests(QUESTS)
                                .executes(ctx -> reset(ctx, StringArgumentType.getString(ctx, "quest"))))));
        root.then(admin("import")
                .then(Commands.argument("path", StringArgumentType.greedyString()).executes(ctx -> {
                    QuestServer q = need(ctx);
                    String path = StringArgumentType.getString(ctx, "path");
                    Path p = Path.of(path);
                    if (!p.isAbsolute()) {
                        p = Mc.server.getServerDirectory().resolve(path);
                    }
                    Importer.Result r = Importer.importPath(q, p);
                    if (r.error != null) {
                        ctx.getSource().sendFailure(Component.literal("Import failed: " + r.error));
                        return 0;
                    }
                    ok(ctx, "Imported " + r.chapters + " chapters and " + r.quests + " quests from " + r.format);
                    return r.quests;
                })));
        root.then(admin("editor")
                .then(Commands.literal("add").then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> editor(ctx, true))))
                .then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> editor(ctx, false)))));
        dispatcher.register(root);
        dispatcher.register(Commands.literal("bq").redirect(dispatcher.getRoot().getChild("barelyquesting")));
    }

    private static QuestServer need(CommandContext<CommandSourceStack> ctx) {
        QuestServer q = ServerHooks.quests();
        if (q == null) {
            throw new IllegalStateException("Quests are not running");
        }
        return q;
    }

    private static void ok(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), true);
    }

    private static int open(CommandContext<CommandSourceStack> ctx, String quest) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        QuestServer q = need(ctx);
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        if ("!edit".equals(quest) && !q.canEdit(new McPlayer(p))) {
            ctx.getSource().sendFailure(Component.literal("You may not edit quests"));
            return 0;
        }
        q.open(new McPlayer(p), quest);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx, String quest) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        QuestServer q = need(ctx);
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        q.engine().reset(q.engine().teams().teamOf(p.getUUID()).id, quest);
        ok(ctx, "Reset " + (quest == null ? "all quests" : quest) + " for " + p.getName().getString() + "'s team");
        return 1;
    }

    private static int editor(CommandContext<CommandSourceStack> ctx, boolean add) {
        QuestServer q = need(ctx);
        String name = StringArgumentType.getString(ctx, "name").toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> names = new java.util.ArrayList<>(com.itmecasey.barelyquesting.core.config.Settings.list("editors"));
        names.removeIf(n -> n.equalsIgnoreCase(name));
        if (add) {
            names.add(name);
        }
        com.itmecasey.barelyquesting.core.config.Settings.set("editors", names);
        q.refreshPermissions();
        ok(ctx, (add ? "Added " : "Removed ") + name + (add ? " to" : " from") + " the editors");
        return 1;
    }
}
