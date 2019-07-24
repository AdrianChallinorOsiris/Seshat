package osiris.stp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Data;


@Data
public class Token {
	public TokenType type;
	public String sval;
	public double dval;
	public int ival;
	public char cval;
	public LocalDate ldval;
	public LocalTime ltval;
	public LocalDateTime ldtval;
	
	public int size;
	public String fmt;
	
	public Token() { 
		type = TokenType.LAMBDA;
		size = 0;
	}
	
	public Token(LocalDateTime ldt) { 
		type = TokenType.DATETIME;
		ldtval = ldt;
		size = 1;
	}

	public Token(LocalDate ldt) { 
		type = TokenType.DATE;
		ldval = ldt;
		size = 1;
	}

	public Token(LocalTime ldt) { 
		type = TokenType.TIME;
		ltval = ldt;
		size = 1;
	}

	public Token(char c) {
		type = TokenType.CHARACTER;
		cval = c;
		sval = new String("" + c);
		size = 1;
	}
	
	public Token(int i) {
		type = TokenType.INTEGER;
		ival = i;
		dval = i;
		size = 1;
	}
	
	public Token(String s) { 
		type = TokenType.STRING;
		sval = s;
		size = 1;
	}
	
	public Token(double d) {
		type = TokenType.DOUBLE;
		dval = d ;
		ival = (int)d;
		size = 1;
	}
		
	public String toString() { 
		String v ="";
		switch (type) { 
		case STRING:  
			v = "SVAL="+sval;
			break;
		case CHARACTER: 
			v = "CVAL="+cval;
			break;
		case INTEGER:
			v = "IVAL="+ival;
			break;
		case DOUBLE: 
			v = "DVAL="+dval;
			break;
		case DATE: 
			v = "DATE="+ldval;
		case TIME: 
			v = "DATE="+ltval;
		default: ;
		}
		String s = "Type=" + type + " " + v;

		return s ;
	}

}
