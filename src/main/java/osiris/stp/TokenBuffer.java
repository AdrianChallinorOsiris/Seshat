package osiris.stp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class TokenBuffer {

	private String prompt;
	private BufferedReader in;
	private String[] buffer;
	private int position; 

	public TokenBuffer() {
		position = 0;
		in = new BufferedReader(new InputStreamReader(System.in));
	}
	
	public TokenBuffer(String s) { 
		noSpecial(s);
	}

	public void setPrompt(String s) {
		prompt = s;
	}
	
	private void prompt() throws noNextTokenException {
		while (position >= (buffer==null? 0 : buffer.length)) {
			if (in != null) { 
				try {
					
					String s = "";
					while(s.isEmpty()) {
						System.out.print(prompt + " ");
						s = in.readLine();
					}
					
					noSpecial(s);
						
				} catch (IOException e) {}				
			}
			else throw new noNextTokenException();
		}
	}
	
	public void noSpecial(String s) {
		String x = s.replace('\t', ' ');
		x = x.replace('\r', ' ');
		x = x.replace('\n', ' ');
		x = StringUtils.normalizeSpace(x);
		position = 0;
		
		
		buffer = x.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
		
		if ((buffer.length == 0) && buffer[0] ==  "") 
			buffer = null;
	}
	
	public String nextToken() throws noNextTokenException { 
		prompt();		
		String word = buffer[position];
		return 	word;
	}
	
	public String nextToken(int i)  {
		StringBuffer sb = new StringBuffer();
		String space = "";
		int l = position + i; 
		if (l > buffer.length) l = buffer.length;
		for (int j = position; j < l; j++) { 
			sb.append(space);
			sb.append(buffer[j]);
			space = " ";
		}
		return 	sb.toString();
	}
	
	
	
	public void advance(int i) {
		position+=i;
	}

	public void reset() {
		StringBuffer sb = new StringBuffer();
		for (int i=position; i<buffer.length; i++) { 
			sb.append( buffer[i] + " " );
		}
		log.info("Buffer reset: Abandoning {} ", sb.toString());
		buffer = null;
		position = 0;
	}
	
	
}
