package osiris.stp;

import java.math.BigDecimal;
import java.util.Scanner;

public class DoubleNumber extends Transition {
	public DoubleNumber() { 
		super();
		name = "<DOUBLE>";
	}
	
	@Override
	public Token match(Scanner sc) {
		Token t = null;
		if (sc.hasNextBigDecimal()) {
			BigDecimal bd = sc.nextBigDecimal();
			t = new Token(bd.doubleValue());
		} 
		return t;
	}	
}
