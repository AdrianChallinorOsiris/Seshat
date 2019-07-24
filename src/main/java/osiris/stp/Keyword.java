package osiris.stp;

import java.util.regex.Pattern;
import java.util.Scanner;

public class Keyword extends Transition {
	private String keyword;
	private Pattern pattern;
	private static final int MINCHARS = 3;

	public Keyword() {
		super();
	}
	
	public Keyword(String k) {
		super();
		keyword = k;
		name = k;
		StringBuffer p1 = new StringBuffer();
		StringBuffer p2 = new StringBuffer();
		char c[] = k.toCharArray();
		for (int i=0; i<c.length; i++) {
			if (i >= MINCHARS) {
				p1.append('(');
				p2.append(")?");
			}
			p1.append(k.charAt(i));
		}
		String p = p1.toString() + p2.toString();
		pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
	}

/*
 * Do we nede to do something funky here. 	
  	public Token match(Scanner sc) {
		Token t = null;
		String s = sc.next();
		if  (keyword.toUpperCase().startsWith(s.toUpperCase())) {
			t = new Token(s);
		}
		return t;
	}
*/
	
	public Token match(Scanner sc) {
		Token t = null;
		if (sc.hasNext(pattern)) {
			t = new Token(sc.next(pattern));
		}
		return t;
	}
	
	public Transition auto() { 
		setSuccess(new State(keyword));
		return this;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setValue(String keyword) {
		this.keyword = keyword;
	}	
}