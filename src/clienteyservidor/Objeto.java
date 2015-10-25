package clienteyservidor;

import java.io.*;
import java.util.ArrayList;

public class Objeto implements Serializable {

	protected static final long serialVersionUID = 1112122200L;


	static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2, ACTION = 3, ATTACK=4;
	private int type;
	private String message;
	ArrayList<String> listaPlanetasPropios;
	ArrayList<String> listaPlanetasOtros;
	String planetaName;
	boolean ForceUpdate = false;
	

	Objeto(int type, String message) {
		this.type = type;
		this.message = message;
	}
	Objeto(int type, String message, boolean force) {
		this.type = type;
		this.message = message;
		this.ForceUpdate = force;
	}
	Objeto(int type){
		this.type = type;
	}
	public Objeto(int type, String message, String name) {
		this.type = type;
		this.message = message;
	}
	int getType() {
		return type;
	}
	String getMessage() {
		return message;
	}
	
	
}

