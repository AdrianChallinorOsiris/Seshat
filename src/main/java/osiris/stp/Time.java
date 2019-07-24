package osiris.stp;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import java.util.Scanner;

public class Time extends Transition {
	static final private Pattern PATTERN = Pattern.compile("^([0-9]|0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$");

	public Time() {
		super();
		name = "<TIME (HH:MM)>";
	}

	@Override
	public Token match(Scanner sc) {
		Token t = null;
		if (sc.hasNext(PATTERN)) {
			String s = sc.next(PATTERN);

			try {
				DateTimeFormatter formatter = new DateTimeFormatterBuilder().parseCaseInsensitive()
						.appendPattern("HH:mm").toFormatter();

				LocalTime time = LocalTime.parse(s, formatter);
				t = new Token(time);
				t.sval = s;
			} catch (DateTimeParseException e) {
			}
		}
		return t;

	}
}
