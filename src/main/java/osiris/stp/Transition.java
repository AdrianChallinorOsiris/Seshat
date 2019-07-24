package osiris.stp;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;
import java.util.Scanner;

@Data
public abstract class Transition {
	private State successState;
	private boolean complete;
	private String cbMethod;
	private Grammar grammar;
	protected String name;

	private static final String newline = System.getProperty("line.separator");
	private static final String padding = "                                 ";

	public Transition() {
		successState = null;
		complete = false;
		cbMethod = null;
	}
		
	
	public Transition Complete() {
		this.complete = true;
		this.successState = null;
		return this;
	}


	public Transition setSuccess(State successState) {
		this.successState = successState;
		return this;
	}
		
	public Transition callback(String method) {
		this.cbMethod = method;
		return this;
	}
	
	public abstract Token match(Scanner sc) ;
	
	protected void print(Writer w, int level) throws IOException {
		w.write(StringUtils.left(padding, level*2) + name);
		if (complete)  w.write(" Complete" );
		// if (cbMethod != null)  w.write(" Callback=" + cbMethod + "()");
		w.write(newline);
		w.flush();
		if (successState != null) {
			successState.print(w, level+1);
		}
	}	
	
	public void help() { 
		System.out.println("    " + name);
	}
}
