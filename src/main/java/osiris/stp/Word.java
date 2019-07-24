package osiris.stp;
import java.util.Scanner;

public class Word extends Transition{
	public Word() {
		super();
		name = "<Word>";
	}

	@Override
	public Token match(Scanner sc) {
		return new Token(sc.next());
	}
}
