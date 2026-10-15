package io.github.hello09x.fakeplayer.core.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentException;
import dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentInfo;
import dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentInfoParser;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.wrappers.CommandResult;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * @author tanyaofei
 * @since 2024/7/30
 **/
public class FakePlayerCommandArgument extends CustomArgument<CommandResult, String> implements GreedyArgument {

    private final Parser parser;

    /**
     * Constructs a {@link CommandArgument} with the given node name.
     *
     * @param nodeName the name of the node for this argument
     */
    public FakePlayerCommandArgument(String nodeName) {
        // CommandAPI 11.2 changed the signature of the Argument constructor, extending CustomArgument
        // instead of Argument keeps this class working on every CommandAPI version.
        this(nodeName, new Parser());
    }

    private FakePlayerCommandArgument(String nodeName, Parser parser) {
        super(new GreedyStringArgument(nodeName), parser);
        this.parser = parser;
        applySuggestions();
    }

    private void applySuggestions() {
        super.replaceSuggestions((info, builder) -> {
            // Extract information
            var sender = this.getTarget(info);
            if (sender == null) {
                return Suggestions.empty();
            }
            var commandMap = Bukkit.getCommandMap();
            var command = info.currentArg();

            // Setup context for errors
            var context = new StringReader(command);

            if (!command.contains(" ")) {
                // Suggesting command name
                ArgumentSuggestions<CommandSender> replacement = parser.replacements.getNextSuggestion(sender);
                if (replacement != null) {
                    return replacement.suggest(new SuggestionInfo<>(sender, new CommandArguments(new Object[0], new LinkedHashMap<>(), new String[0], new LinkedHashMap<>(), info.currentInput()), command, command), builder);
                }

                var results = commandMap.tabComplete(sender, command);
                // No applicable commands
                if (results == null) {
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(context);
                }

                // Remove / that gets prefixed to command name if the sender is a player
                if (sender instanceof Player) {
                    for (String result : results) {
                        builder.suggest(result.substring(1));
                    }
                } else {
                    for (String result : results) {
                        builder.suggest(result);
                    }
                }

                return builder.buildFuture();
            }


            // Verify commandLabel
            var commandLabel = command.substring(0, command.indexOf(" "));
            var target = commandMap.getCommand(commandLabel);
            if (target == null) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(context);
            }

            // Get arguments
            var arguments = command.split(" ");
            if (!arguments[0].isEmpty() && command.endsWith(" ")) {
                // If command ends with space add an empty argument
                arguments = Arrays.copyOf(arguments, arguments.length + 1);
                arguments[arguments.length - 1] = "";
            }

            // Build suggestion
            builder = builder.createOffset(builder.getStart() + command.lastIndexOf(" ") + 1);

            int lastIndex = arguments.length - 1;
            var previousArguments = Arrays.copyOf(arguments, lastIndex);
            ArgumentSuggestions<CommandSender> replacement = parser.replacements.getNextSuggestion(sender, previousArguments);
            if (replacement != null) {
                return replacement.suggest(new SuggestionInfo<>(sender, new CommandArguments(previousArguments, new LinkedHashMap<>(), previousArguments, new LinkedHashMap<>(), info.currentInput()), command, arguments[lastIndex]), builder);
            }

            // Remove command name from arguments for normal tab-completion
            arguments = Arrays.copyOfRange(arguments, 1, arguments.length);

            // Get location sender is looking at if they are a Player, matching vanilla behavior
            // No builtin Commands use the location parameter, but they could
            Location location = null;
            if (sender instanceof Player) {
                var player = (Player) sender;
                Block block = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
                if (block != null) {
                    location = block.getLocation();
                }
            }

            // Build suggestions for new argument
            for (var tabCompletion : target.tabComplete(sender, commandLabel, arguments, location)) {
                builder.suggest(tabCompletion);
            }
            return builder.buildFuture();
        });
    }

    /**
     * Replaces the default command suggestions provided by the server with custom
     * suggestions for each argument in the command, starting with the command's
     * name. If a suggestion is null or there isn't any suggestions given for that
     * argument, the suggestions will not be overridden.
     *
     * @param suggestions An array of {@link ArgumentSuggestions} representing the
     *                    suggestions. Use the static methods in ArgumentSuggestions
     *                    to create these.
     * @return the current argument
     */
    @SafeVarargs
    public final FakePlayerCommandArgument replaceSuggestions(ArgumentSuggestions<CommandSender>... suggestions) {
        parser.replacements = SuggestionsBranch.suggest(suggestions);
        return this;
    }

    /**
     * Replaces the default command suggestions provided by the server with custom
     * suggestions for each argument in the command, starting with the command's
     * name. If a suggestion is null or there isn't any suggestions given for that
     * argument, the suggestions will not be overridden.
     *
     * @param suggestions An array of {@link ArgumentSuggestions} representing the
     *                    suggestions. Use the static methods in ArgumentSuggestions
     *                    to create these.
     * @return the current argument
     */
    @Override
    public FakePlayerCommandArgument replaceSuggestions(ArgumentSuggestions<CommandSender> suggestions) {
        parser.replacements = SuggestionsBranch.suggest(suggestions);
        return this;
    }

    /**
     * Adds {@link SuggestionsBranch} to this CommandArgument. After going through
     * the suggestions provided by
     * {@link CommandArgument#replaceSuggestions(ArgumentSuggestions...)} the
     * suggestions of these branches will be used.
     *
     * @param branches An array of {@link SuggestionsBranch} representing the
     *                 branching suggestions. Use
     *                 {@link SuggestionsBranch#suggest(ArgumentSuggestions...)} to
     *                 start creating these.
     * @return the current argument
     */
    @SafeVarargs
    public final Argument<CommandResult> branchSuggestions(SuggestionsBranch<CommandSender>... branches) {
        parser.replacements.branch(branches);
        return this;
    }

    private @Nullable Player getTarget(@NotNull SuggestionInfo<CommandSender> info) {
        var args = info.previousArgs().fullInput().split(" ");
        if (args.length < 3) {
            return null;
        }
        var name = args[2];
        var manager = Main.getInjector().getInstance(FakeplayerManager.class);
        if (info.sender().isOp()) {
            return manager.get(name);
        }
        return manager.get(info.sender(), name);
    }

    private static final class Parser implements CustomArgumentInfoParser<CommandResult, String> {

        private SuggestionsBranch<CommandSender> replacements = SuggestionsBranch.suggest();

        @Override
        public CommandResult apply(CustomArgumentInfo<String> info) throws CustomArgumentException {
            var command = info.currentInput();
            var commandMap = Bukkit.getCommandMap();

            var sender = info.sender();
            var target = (Player) info.previousArgs().get("name");
            if (target == null) {
                throw CustomArgumentException.fromString("Incorrect argument for command");
            }

            var manager = Main.getInjector().getInstance(FakeplayerManager.class);
            if (sender.isOp()) {
                if (manager.isNotFake(target)) {
                    throw unknownCommand();
                }
            } else {
                if (manager.get(sender, target.getName()) == null) {
                    throw unknownCommand();
                }
            }

            var arguments = command.split(" ");
            var commandLabel = arguments[0];
            var cmd = commandMap.getCommand(commandLabel);
            if (cmd == null) {
                throw unknownCommand();
            }

            try {
                replacements.enforceReplacements(target, arguments);
            } catch (CommandSyntaxException e) {
                throw CustomArgumentException.fromString(e.getMessage());
            }

            return new CommandResult(cmd, Arrays.copyOfRange(arguments, 1, arguments.length));
        }

        private static @NotNull CustomArgumentException unknownCommand() {
            return CustomArgumentException.fromString("Unknown or incomplete command");
        }

    }

}
