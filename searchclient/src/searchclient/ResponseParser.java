package searchclient;

/**
 * @author KaZ
 * Parses the response from the server into a boolean array
 */
public class ResponseParser {
	int agentCount;
	boolean[] parsedResponse;
	
	public ResponseParser(int agentCount) {
		this.agentCount = agentCount;
		parsedResponse = new boolean[agentCount];
	}
	
	public boolean[] parseResponse(String response) {
		int position = 1;
		for(int agentNo = 0; agentNo < this.agentCount; agentNo++) {
			if(response.charAt(position) == 't') {
				position += 6;	// +6 due to 6 characters in "true, "
				parsedResponse[agentNo] = true;
			}
			else {
				position += 7;	// +7 due to 7 characters in "false, "
				parsedResponse[agentNo] = false;
			}
		}
		
		return parsedResponse;
	}
}
