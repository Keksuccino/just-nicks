package de.keksuccino.justnicks.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.keksuccino.justnicks.JustNicks;
import de.keksuccino.justnicks.util.AbstractOptions;
import de.keksuccino.justnicks.util.permission.Permission;
import de.keksuccino.justnicks.util.permission.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class OptionsCommand {

    private static final List<OptionEntry> OPTION_ENTRIES = new ArrayList<>();
    private static final SuggestionProvider<CommandSourceStack> OPTION_NAME_SUGGESTIONS = (ctx, builder) -> {
        refreshEntries();
        for (OptionEntry entry : OPTION_ENTRIES) {
            builder.suggest(entry.key());
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> OPTION_VALUE_SUGGESTIONS = (ctx, builder) -> {
        refreshEntries();
        String name = StringArgumentType.getString(ctx, "option").toLowerCase(Locale.ROOT);
        OptionEntry entry = findEntry(name);
        if (entry == null) {
            return builder.buildFuture();
        }
        addValueSuggestions(builder, entry.option());
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        refreshEntries();

        dispatcher.register(Commands.literal("justnicksoptions")
                .then(Commands.argument("option", StringArgumentType.word())
                        .suggests(OPTION_NAME_SUGGESTIONS)
                        .then(Commands.argument("value", StringArgumentType.string())
                                .suggests(OPTION_VALUE_SUGGESTIONS)
                                .executes(OptionsCommand::setOption))));
    }

    private static int setOption(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!PermissionUtil.hasPermission(ctx.getSource(), Permission.EDIT_OPTIONS)) {
            ctx.getSource().sendFailure(Component.translatableWithFallback("justnicks.commands.general.no_permission", "You don't have permission to use this command."));
            return 0;
        }

        String optionKey = StringArgumentType.getString(ctx, "option").toLowerCase(Locale.ROOT);
        String rawValue = StringArgumentType.getString(ctx, "value");

        refreshEntries();
        OptionEntry entry = findEntry(optionKey);
        if (entry == null) {
            ctx.getSource().sendFailure(Component.translatableWithFallback("justnicks.commands.options.unknown", "Unknown option: %s", optionKey));
            return 0;
        }

        boolean success = applyValue(entry.option(), rawValue);
        if (!success) {
            ctx.getSource().sendFailure(Component.translatableWithFallback("justnicks.commands.options.invalid_value", "Invalid value for %s: %s", optionKey, rawValue));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("justnicks.commands.options.updated", "Set %s to %s.", optionKey, rawValue), false);
        return 1;
    }

    private static void refreshEntries() {
        OPTION_ENTRIES.clear();
        for (AbstractOptions.Option<?> option : JustNicks.getOptions().getOptions()) {
            OPTION_ENTRIES.add(new OptionEntry(option.getKey(), option));
        }
    }

    @Nullable
    private static OptionEntry findEntry(String keyLower) {
        for (OptionEntry entry : OPTION_ENTRIES) {
            if (entry.key().equalsIgnoreCase(keyLower)) {
                return entry;
            }
        }
        return null;
    }

    private static void addValueSuggestions(SuggestionsBuilder builder, AbstractOptions.Option<?> option) {
        Object def = option.getDefaultValue();
        if (def instanceof Boolean) {
            builder.suggest("true");
            builder.suggest("false");
        } else if (def instanceof Integer) {
            builder.suggest("Integer value, e.g. \"" + def + "\"");
        } else if (def instanceof Long) {
            builder.suggest("Long value, e.g. \"" + def + "\"");
        } else if (def instanceof Float) {
            builder.suggest("Float value, e.g. \"" + def + "\"");
        } else if (def instanceof Double) {
            builder.suggest("Double value, e.g. \"" + def + "\"");
        } else {
            String example = String.valueOf(def);
            if (example.isEmpty()) {
                example = "example text";
            }
            builder.suggest("String value, e.g. \"" + example + "\"");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean applyValue(AbstractOptions.Option<?> option, String raw) {
        Objects.requireNonNull(raw);
        AbstractOptions.Option opt = (AbstractOptions.Option) option; // erase generic to allow setValue
        Object def = option.getDefaultValue();
        try {
            if (def instanceof Boolean) {
                if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false")) {
                    return false;
                }
                opt.setValue(Boolean.parseBoolean(raw));
                return true;
            } else if (def instanceof Integer) {
                opt.setValue(Integer.parseInt(raw));
                return true;
            } else if (def instanceof Long) {
                opt.setValue(Long.parseLong(raw));
                return true;
            } else if (def instanceof Float) {
                opt.setValue(Float.parseFloat(raw));
                return true;
            } else if (def instanceof Double) {
                opt.setValue(Double.parseDouble(raw));
                return true;
            } else {
                opt.setValue(raw);
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private record OptionEntry(String key, AbstractOptions.Option<?> option) {}

}
