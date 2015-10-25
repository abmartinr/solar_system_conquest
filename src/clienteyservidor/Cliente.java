package clienteyservidor;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import javax.swing.JComboBox;

public class Cliente implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ObjectInputStream sInput;
	private ObjectOutputStream sOutput;
	private Socket socket;
	public String[] listaTropas = {"Soldado", "Defensor", "Obrero"};
	private InterfazCliente cg;
	private String server, username;
	private int port;
	public ArrayList<String> listaPlanetas;

	Cliente(String server, int port, String username, InterfazCliente cg) {
		this.server = server;
		this.port = port;
		this.username = username;
		this.cg = cg;
	}

	public boolean start() {

		try {
			socket = new Socket(server, port);
		} catch (Exception ec) {
			display("Error connectiong to server:" + ec);
			return false;
		}

		String msg = "Conexión aceptada " + socket.getInetAddress() + ":"
				+ socket.getPort();
		display(msg);

		try {
			sInput = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException eIO) {
			display("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		new ListenFromServer().start();

		try {
			sOutput.writeObject(username);
		} catch (IOException eIO) {
			display("Exception doing login : " + eIO);
			disconnect();
			return false;
		}

		return true;
	}

	void display(String msg) {
		if (cg == null)
			System.out.println(msg);
		else
			cg.append(msg + "\n");
	}


	
	void sendMessage(Objeto msg) {
		try {
			sOutput.writeObject(msg);
		} catch (IOException e) {
			display("Exception writing to server: " + e);
		}
	}

	private void disconnect() {
		try {
			if (sInput != null)
				sInput.close();
		} catch (Exception e) {
		}
		try {
			if (sOutput != null)
				sOutput.close();
		} catch (Exception e) {
		}
		try {
			if (socket != null)
				socket.close();
		} catch (Exception e) {
		}

		if (cg != null)
			cg.connectionFailed();
	}

	class ListenFromServer extends Thread {

		public void run() {
			while (true) {
				try {
					Objeto o = (Objeto) sInput.readObject();

					switch (o.getType()) {
					case 1:
						if(o.getMessage() != null){
							cg.append(o.getMessage());
						}
							if(o.ForceUpdate){
								actualizaDatosDePlaneta();
							}
						
						break;

					case 3:
						
						switch (o.getMessage()) {
						case "inicioJuego":

							for(String s: listaTropas){
								cg.cmbTropas.addItem(s);
							}
							for(String p: o.listaPlanetasPropios){
								
								System.out.println(p);
								cg.cmbPlanetas.addItem(p);
							}
							for(String p: o.listaPlanetasOtros){
								cg.cmbAtacarPlanetas.addItem(p);
							}
							
							listaPlanetas = (ArrayList<String>) o.listaPlanetasPropios.clone();
							
							cg.cmbAtacarPlanetas.repaint();
							cg.cmbTropas.repaint();
							
							cg.cmbPlanetas.addActionListener(new ActionListener() {
								
								@Override
								public void actionPerformed(ActionEvent e) {

									
							       String planetaSeleccionado = (String) cg.cmbPlanetas.getSelectedItem();
							       
							       actualizaDatosDePlaneta(planetaSeleccionado);
								}
							});
			
							cg.cmbPlanetas.setSelectedIndex(0);
							display("Los datos del juego se han cargado correctamente.\nBienvenido a tu planeta natal, "+ listaPlanetas.get(0) +" ¡Suerte en la conquista!");
							
							//Este automatico cada 5 segundos actualiza los datos del planeta
							Automatico at = new Automatico();
							break;

						case "ASIGNARPLANETA":
							
							cg.cmbAtacarPlanetas.removeItem(o.planetaName);
							cg.cmbAtacarPlanetas.repaint();
							cg.cmbPlanetas.addItem(o.planetaName);
							cg.cmbPlanetas.repaint();
							listaPlanetas.add(o.planetaName);
							display("¡Enhorabuena! Has conquistado "+ o.planetaName);
							break;
						
						case "DESASIGNARPLANETA":
							
							
							cg.cmbAtacarPlanetas.addItem(o.planetaName);
							cg.cmbAtacarPlanetas.repaint();
							cg.cmbPlanetas.removeItem(o.planetaName);
							cg.cmbPlanetas.repaint();
							listaPlanetas.remove(o.planetaName);
							display("¡Lástima! Has perdido el poder en "+ o.planetaName + " y ya no es tuyo.");
							break;
							
						}
		
						break;
					}

				} catch (IOException e) {
					display("Server has close the connection: " + e);
					if (cg != null)
						cg.connectionFailed();
					break;
				} catch (ClassNotFoundException e2) {
				}
			}
		}
	}
	public void actualizaDatosDePlaneta(String planetaSeleccionado){

		//Consulta a la base de datos de este planeta
		Connection connexio = null;
		Statement statement = null;
		ResultSet rs = null;
		String url = "jdbc:sqlite:galaxia.db";
		try {
			Class.forName("org.sqlite.JDBC");
			connexio = DriverManager.getConnection(url);
			statement = connexio.createStatement();

			rs = statement
					.executeQuery("SELECT * from planetas where planeta_nombre ='"+planetaSeleccionado+"'");
			while (rs.next()) {
			    cg.lSoldado.setText(""+rs.getInt(4));
		        cg.lDefensor.setText(""+rs.getInt(3));
		        cg.lObrero.setText(""+rs.getInt(2));
		        cg.lRecRestantes.setText(""+(rs.getInt(6)-rs.getInt(5)-rs.getInt(7)));
		        cg.lRecDisponibles.setText(""+rs.getInt(5));
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}finally{
			try {
				connexio.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		
		
	}
	

	public void actualizaDatosDePlaneta() {
		
       actualizaDatosDePlaneta(getPlanetaSeleccionado());
		
	}
	
	public String getPlanetaSeleccionado(){
		String planetaSeleccionado = (String) cg.cmbPlanetas.getSelectedItem();
		return planetaSeleccionado;
	}
	public class Automatico {

		Toolkit toolkit;

		Timer timer;

		public Automatico() {
			toolkit = Toolkit.getDefaultToolkit();
			timer = new Timer();
			timer.schedule(new Tarea(), 0, // initial delay
					5 * 1000); // subsequent rate
			
		}

		class Tarea extends TimerTask {

			public void run() {
				actualizaDatosDePlaneta();
			}
		}
	}	
}
