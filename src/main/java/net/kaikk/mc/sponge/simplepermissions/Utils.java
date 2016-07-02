package net.kaikk.mc.sponge.simplepermissions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.command.CommandMessageFormatting;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import com.google.common.collect.Iterators;
public class Utils {
	public static CommandElement buildChoices(String key, String... choices) {
		return new ChoicesCommandElement(key, choices);
	}

	private static class ChoicesCommandElement extends CommandElement {
		private final String[] choices;

		public ChoicesCommandElement(String key, String...choices) {
			super(Text.of(key));
			this.choices = choices;
		}

		@Override
		public Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
			String n = args.next();
			for (String c : choices) {
				if (c.equalsIgnoreCase(n)) {
					return c;
				}
			}
			throw args.createError(Text.of("Argument was not a valid choice. Valid choices: %s", StringUtils.join(choices, ',')));
		}

		@Override
		public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
			final String prefix = args.nextIfPresent().orElse("");

			List<String> list = new ArrayList<String>();
			for (String c : choices) {
				if (c.toLowerCase().startsWith(prefix.toLowerCase())) {
					list.add(c);
				}
			}

			return list;
		}

		@Override
		public Text getUsage(CommandSource commander) {
			final Text.Builder build = Text.builder();
			build.append(CommandMessageFormatting.LT_TEXT);
			for (Iterator<String> it = Iterators.forArray(choices); it.hasNext();) {
				build.append(Text.of(it.next()));
				if (it.hasNext()) {
					build.append(CommandMessageFormatting.PIPE_TEXT);
				}
			}
			build.append(CommandMessageFormatting.GT_TEXT);
			return build.build();
		}
	}

	/** 
	 * Returns a Tristate value from a Boolean object
	 * @param b The Boolean object.
	 * @return {@link Tristate.UNDEFINED} if b is null<br>
	 *  {@link Tristate.TRUE} if b is true<br>
	 *  {@link Tristate.FALSE} if b is false
	 */
	public static Tristate tristate(@Nullable Boolean b) {
		return b==null ? Tristate.UNDEFINED : b ? Tristate.TRUE : Tristate.FALSE;
	}
}
