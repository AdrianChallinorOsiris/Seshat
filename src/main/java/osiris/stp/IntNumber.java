package osiris.stp;
import java.util.Scanner;

public class IntNumber extends Transition {
	public IntNumber() { 
		super();
		name = "<Integer>";
	}

	@Override
	public Token match(Scanner sc) {
		Token t = null;
		if (sc.hasNextInt()) { 
			t = new Token(sc.nextInt());
		} 
		return t;
	}
	
}
