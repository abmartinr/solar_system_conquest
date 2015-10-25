package clienteyservidor;

import java.awt.Toolkit;
import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;


public class Servidor {

	private static int uniqueId;
	private ArrayList<HiloJugador> al;
	private InterfazServidor sg;
	private SimpleDateFormat sdf;
	private int port;
	private boolean keepGoing;
	public String[] planetas = { "Mercurio", "Venus", "Tierra", "Marte",
			"Jupiter", "Saturno", "Urano", "Neptuno", "Pluton" };
	public int[] lunas = { 0, 0, 1, 2, 63, 62, 27, 13, 3 };
	ArrayList<String> listaPlanetasServ = new ArrayList<String>();
	ArrayList<String> planetasLibres;
	static HashMap<String, Recolector> listaRecolectores = new HashMap<String, Recolector>();

	public Servidor(int port) {
		this(port, null);
	}

	public Servidor(int port, InterfazServidor sg) {

		this.sg = sg;
		this.port = port;
		sdf = new SimpleDateFormat("HH:mm:ss");
		al = new ArrayList<HiloJugador>();

		// crearemos todos los planetas desde 0
		inicializarPlanetas();

	}

	/**
	 * Iniciamos el juego
	 * 
	 * 1- Limpiamos la base de datos 2- La volvemos a poblar desde 0
	 * 
	 */

	private void inicializarPlanetas() {
		Connection connexio = null;
		Statement statement = null;
		ResultSet rs = null;
		String url = "jdbc:sqlite:galaxia.db";
		planetasLibres = new ArrayList<String>();
		try {
			Class.forName("org.sqlite.JDBC");
			connexio = DriverManager.getConnection(url);
			statement = connexio.createStatement();

			// Limpiamos la base de datos

			statement.executeUpdate("DELETE from planetas");
			statement.executeUpdate("DELETE from jugadores");
			statement.executeUpdate("DELETE from planeta_jugador");

			for (int i = 0; i < planetas.length; i++) {
				statement.executeUpdate("INSERT into planetas VALUES ('"
						+ planetas[i] + "', 0, 10, 50, 0 ,"
						+ ((lunas[i] * 300) + 2000) + ", 0, " + lunas[i] + ")");
				planetasLibres.add(planetas[i]);
				listaPlanetasServ.add(planetas[i]);
			}

		} catch (SQLException e) {
			System.err.println(e.getMessage());
		} catch (ClassNotFoundException e) {
			System.err.println(e.getMessage());
		} finally {
			if (connexio != null) {
				try {
					connexio.close();
				} catch (SQLException e) {
					;
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					;
				}
			}
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					;
				}
			}
		}
	}

	public void start() {
		keepGoing = true;

		try {

			ServerSocket serverSocket = new ServerSocket(port);

			while (keepGoing) {

				display("Servidor funcionando, esperando conexiones en el puerto "
						+ port + ".");

				Socket socket = serverSocket.accept();

				if (!keepGoing)
					break;
				HiloJugador t = new HiloJugador(socket);
				al.add(t);
				t.start();

				iniciaJuegoParaCliente(t);
			}

			try {
				serverSocket.close();
				for (int i = 0; i < al.size(); ++i) {
					HiloJugador tc = al.get(i);
					try {
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					} catch (IOException ioE) {

					}
				}
			} catch (Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}

		catch (IOException e) {
			String msg = sdf.format(new Date())
					+ " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}

	protected void stop() {
		keepGoing = false;

		try {
			new Socket("localhost", port);
		} catch (Exception e) {

		}
	}

	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		if (sg == null)
			System.out.println(time);
		else
			sg.appendEvent(time + "\n");
	}

	private synchronized void broadcast(String message) {

		String time = sdf.format(new Date());
		String messageLf = time + " " + message + "\n";

		if (sg == null)
			System.out.print(messageLf);
		else
			sg.appendRoom(messageLf);

		for (int i = al.size(); --i >= 0;) {
			HiloJugador ct = al.get(i);

			if (!ct.writeMsg(messageLf)) {
				al.remove(i);
				display("Cliente " + ct.username
						+ " desconectado y eliminado de la lista.");
			}
		}
	}

	synchronized void remove(int id) {

		for (int i = 0; i < al.size(); ++i) {
			HiloJugador ct = al.get(i);

			if (ct.id == id) {
				al.remove(i);
				return;
			}
		}
	}

	/**
	 * Inicia el juego para el cliente
	 * 
	 * Crea su registro en la base de datos Se le asigna un planeta Se le envía
	 * el inicio del juego Se da la orden al planeta para que empiece a
	 * recolectar.
	 * 
	 * @param cliente
	 */

	void iniciaJuegoParaCliente(HiloJugador cliente) {

		Connection connexio = null;
		Statement statement = null;
		String url = "jdbc:sqlite:galaxia.db";
		String planetaAAsignar = null;
		try {
			Class.forName("org.sqlite.JDBC");
			connexio = DriverManager.getConnection(url);
			statement = connexio.createStatement();

			// Inserto el usuario
			statement.executeUpdate("INSERT into jugadores VALUES ("
					+ cliente.id + ", '" + cliente.username + "')");

			// Miro los planetas que hay libres y busco uno aleatorio y se lo
			// asigno
			Random num = new Random();
			planetaAAsignar = planetasLibres.get(num.nextInt(planetasLibres
					.size()));

			statement.executeUpdate("INSERT into planeta_jugador VALUES ("
					+ cliente.id + ", '" + planetaAAsignar + "')");

		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			try {
				connexio.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		cliente.listaPlanetasOtros = planetasOtros(cliente);
		cliente.listaPlanetasPropios = planetasPropios(cliente);
		cliente.enviaInicioDeJuego();
		// Una vez enviado el inicio del juego, inicio la recolección para el
		// planeta:

		listaRecolectores.put(planetaAAsignar, new Recolector(planetaAAsignar));

	}

	private ArrayList<String> planetasPropios(HiloJugador hj) {
		ArrayList<String> res = new ArrayList<String>();

		Connection connexio = null;
		Statement statement = null;
		ResultSet rs = null;
		String url = "jdbc:sqlite:galaxia.db";
		try {
			Class.forName("org.sqlite.JDBC");
			connexio = DriverManager.getConnection(url);
			statement = connexio.createStatement();

			rs = statement
					.executeQuery("SELECT * from planeta_jugador where jugador_id ="
							+ hj.id);

			while (rs.next()) {
				res.add(rs.getString(2));
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			try {
				connexio.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return res;
	}

	private ArrayList<String> planetasOtros(HiloJugador hj) {
		ArrayList<String> res = (ArrayList<String>) listaPlanetasServ.clone();

		Connection connexio = null;
		Statement statement = null;
		ResultSet rs = null;
		String url = "jdbc:sqlite:galaxia.db";
		try {
			Class.forName("org.sqlite.JDBC");
			connexio = DriverManager.getConnection(url);
			statement = connexio.createStatement();

			rs = statement
					.executeQuery("SELECT * from planeta_jugador where jugador_id ="
							+ hj.id);

			while (rs.next()) {
				res.remove(rs.getString(2));
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			try {
				connexio.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return res;
	}

	public class HiloJugador extends Thread {

		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		int id;
		String username;
		Objeto cm;
		String date;
		ArrayList<String> listaPlanetasPropios, listaPlanetasOtros;

		HiloJugador(Socket socket) {

			listaPlanetasPropios = new ArrayList<String>();
			listaPlanetasOtros = new ArrayList<String>();
			id = ++uniqueId;
			this.socket = socket;

			try {

				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput = new ObjectInputStream(socket.getInputStream());

				username = (String) sInput.readObject();
				display(username + " se acaba de conectar.");

			} catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}

			catch (ClassNotFoundException e) {
			}
			date = new Date().toString() + "\n";
		}

		public void run() {

			boolean keepGoing = true;
			while (keepGoing) {

				try {
					cm = (Objeto) sInput.readObject();
				} catch (IOException e) {
					display(username + " Exception reading Streams: " + e);
					System.out.println(e);
					break;
				} catch (ClassNotFoundException e2) {
					break;
				}

				String message = cm.getMessage();

				switch (cm.getType()) {

				case Objeto.ATTACK:
					String[] aux = cm.getMessage().split(" ");
					// PLANETAQUEATACA CANTIDADTROPAS PLANETAQUESUFRE
					if (atacar(aux[0], Integer.parseInt(aux[1]), aux[2])) {

						HiloJugador duenoPlanetaAtacado = getDuenoPlaneta(aux[2]);

						if (duenoPlanetaAtacado != null) {
							desasignarPlaneta(aux[2], duenoPlanetaAtacado);
						}
						asignarPlaneta(aux[2], this);
					}else{
						writeMsg("El ataque no ha sido satisfactorio\n");
					}

					quitarTropas(aux[0], Integer.parseInt(aux[1]));
					Objeto forzar = new Objeto(Objeto.MESSAGE);
					forzar.ForceUpdate = true;
					enviaObjeto(forzar);				
					
					
					break;

				case Objeto.ACTION:

					String[] accionJugador = cm.getMessage().split(" ");

					switch (accionJugador[0]) {

					case "CONSTRUIR":
						// TROPAS TIPOTROPAS PLANETA
						boolean tropasCreadas = construyeTropas(
								Integer.parseInt(accionJugador[1]),
								Integer.parseInt(accionJugador[2]),
								accionJugador[3]);

						try {
							if (tropasCreadas) {
								String trop = "";
								switch (Integer.parseInt(accionJugador[2])) {
								case 1:
									trop = "soldados";
									break;

								case 2:
									trop = "defensores";
									break;
								case 3:
									trop = "obreros";
									break;
								}
								sOutput.writeObject(new Objeto(Objeto.MESSAGE,
										"Has contratado " + accionJugador[1]
												+ " " + trop
												+ " correctamente.", true));
							} else {
								sOutput.writeObject(new Objeto(Objeto.MESSAGE,
										"No tienes recursos suficientes para contratar estas tropas."));
							}

						} catch (IOException e) {
							display("Error enviando comunicación a " + username);
							display(e.toString());
						}

						break;
					}

					break;

				case Objeto.MESSAGE:
					broadcast(username + ": " + message);
					break;
				case Objeto.LOGOUT:
					display(username + " desconectado.");
					keepGoing = false;
					for (String s : listaPlanetasPropios) {
						Servidor.listaRecolectores.get(s).paraRecoleccion();
					}
					break;
				case Objeto.WHOISIN:
					writeMsg("Lista enviada a las " + sdf.format(new Date())
							+ "\n");

					for (int i = 0; i < al.size(); ++i) {
						HiloJugador ct = al.get(i);
						writeMsg((i + 1) + ") " + ct.username + " since "
								+ ct.date);
					}
					break;
				}
			}

			remove(id);
			close();
		}

		private void quitarTropas(String planeta, int cantidad) {
			Connection connexio = null;
			boolean res = false;
			Statement statement = null;
			ResultSet rs = null;
			String url = "jdbc:sqlite:galaxia.db";
			try {
				Class.forName("org.sqlite.JDBC");
				connexio = DriverManager.getConnection(url);
				statement = connexio.createStatement();

				statement
						.executeUpdate("UPDATE planetas set planeta_soldados = planeta_soldados-"
								+ cantidad +
								" where planeta_nombre ='"
								+ planeta + "'");

			} catch (Exception e) {
				System.err.println(e.getMessage());
			} finally {
				try {
					connexio.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}

		private HiloJugador getDuenoPlaneta(String planeta) {
			Connection connexio = null;
			boolean res = false;
			Statement statement = null;
			ResultSet rs = null;
			String url = "jdbc:sqlite:galaxia.db";
			HiloJugador hj = null;
			int id = 0;

			try {
				Class.forName("org.sqlite.JDBC");
				connexio = DriverManager.getConnection(url);
				statement = connexio.createStatement();

				rs = statement
						.executeQuery("SELECT jugador_id from planeta_jugador where planeta_id ='"
								+ planeta + "'");

				while (rs.next()) {
					id = rs.getInt(1);
				}
				for (HiloJugador hj2 : al) {
					if (hj2.getId() == id) {
						hj = hj2;
						break;
					}
				}

			} catch (Exception e) {
				System.err.println(e.getMessage());
			} finally {
				try {
					connexio.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return hj;
		}

		private boolean construyeTropas(int cantidad, int tipo, String planeta) {
			Connection connexio = null;
			boolean res = false;
			Statement statement = null;
			ResultSet rs = null;
			String url = "jdbc:sqlite:galaxia.db";
			int vRecursosRec = 0;

			try {
				Class.forName("org.sqlite.JDBC");
				connexio = DriverManager.getConnection(url);
				statement = connexio.createStatement();

				rs = statement
						.executeQuery("SELECT * from planetas where planeta_nombre ='"
								+ planeta + "'");

				while (rs.next()) {
					vRecursosRec = rs.getInt(5);
				}
			} catch (Exception e) {
				System.err.println(e.getMessage());
			} finally {
				try {
					connexio.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			switch (tipo) {
			case 1:
				// soldado 20
				if (vRecursosRec >= (20 * cantidad)) {

					try {
						Class.forName("org.sqlite.JDBC");
						connexio = DriverManager.getConnection(url);
						statement = connexio.createStatement();

						statement
								.executeUpdate("UPDATE planetas set planeta_soldados = planeta_soldados+"
										+ cantidad
										+ ", planeta_recursosRec = (planeta_recursosRec-(20*"
										+ cantidad
										+ ")), planeta_recursosGast = (planeta_recursosGast+(20*"
										+ cantidad
										+ ")) where planeta_nombre ='"
										+ planeta + "'");

					} catch (Exception e) {
						System.err.println(e.getMessage());
					} finally {
						try {
							connexio.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					res = true;
				}
				break;

			case 2:
				// defensor 10
				if (vRecursosRec >= (10 * cantidad)) {

					try {
						Class.forName("org.sqlite.JDBC");
						connexio = DriverManager.getConnection(url);
						statement = connexio.createStatement();

						statement
								.executeUpdate("UPDATE planetas set planeta_defensores = planeta_defensores+"
										+ cantidad
										+ ", planeta_recursosRec = (planeta_recursosRec-(10*"
										+ cantidad
										+ ")), planeta_recursosGast = (planeta_recursosGast+(10*"
										+ cantidad
										+ ")) where planeta_nombre ='"
										+ planeta + "'");

					} catch (Exception e) {
						System.err.println(e.getMessage());
					} finally {
						try {
							connexio.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					res = true;
				}
				break;

			case 3:
				// obrero 5
				if (vRecursosRec >= (5 * cantidad)) {

					try {
						Class.forName("org.sqlite.JDBC");
						connexio = DriverManager.getConnection(url);
						statement = connexio.createStatement();

						statement
								.executeUpdate("UPDATE planetas set planeta_obreros = planeta_obreros+"
										+ cantidad
										+ ", planeta_recursosRec = (planeta_recursosRec-(5*"
										+ cantidad
										+ ")), planeta_recursosGast = (planeta_recursosGast+(5*"
										+ cantidad
										+ ")) where planeta_nombre ='"
										+ planeta + "'");

					} catch (Exception e) {
						System.err.println(e.getMessage());
					} finally {
						try {
							connexio.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					res = true;
				}
				break;
			}

			return res;
		}

		private void close() {

			try {
				if (sOutput != null)
					sOutput.close();
			} catch (Exception e) {
			}
			try {
				if (sInput != null)
					sInput.close();
			} catch (Exception e) {
			}
			;
			try {
				if (socket != null)
					socket.close();
			} catch (Exception e) {
			}
		}

		private boolean enviaInicioDeJuego() {
			if (!socket.isConnected()) {
				close();
				return false;
			}
			try {

				Objeto o = new Objeto(Objeto.ACTION, "inicioJuego");
				o.listaPlanetasPropios = listaPlanetasPropios;
				o.listaPlanetasOtros = listaPlanetasOtros;
				sOutput.writeObject(o);

			} catch (IOException e) {
				display("Error enviando comunicación a " + username);
				display(e.toString());
			}
			return true;
		}

		private boolean enviaObjeto(Objeto o) {
			if (!socket.isConnected()) {
				close();
				return false;
			}
			try {
				sOutput.writeObject(o);
			} catch (IOException e) {
				display("Error enviando comunicación a " + username);
				display(e.toString());
			}
			return true;
		}

		private boolean writeMsg(String msg) {

			if (!socket.isConnected()) {
				close();
				return false;
			}

			try {

				sOutput.writeObject(new Objeto(Objeto.MESSAGE, msg));
			}

			catch (IOException e) {
				display("Error enviando comunicación a " + username);
				display(e.toString());
			}
			return true;
		}
	}

	public void desasignarPlaneta(String planeta, HiloJugador c) {

		// Si desasignamos un planeta hay que hacer lo siguiente:
		// 1 - Parar el hilo de ese planeta
		// 2 - Quitar el planeta de las variables del Hilo del jugador.
		// 3 - Informar a ese jugador que le han quitado el planeta.

		listaRecolectores.get(planeta).paraRecoleccion();
		quitarPlanetaJugadorDB(planeta, c.id);
		c.listaPlanetasPropios.remove(planeta);
		c.listaPlanetasOtros.add(planeta);
		Objeto o = new Objeto(Objeto.ACTION, "DESASIGNARPLANETA");
		o.planetaName = planeta;
		c.enviaObjeto(o);

	}

	public void asignarPlaneta(String planeta, HiloJugador c) {

		// Si asignamos un planeta a un jugador hacemos lo siguiete
		// 1 - Iniciamos el hilo de recoleccion
		// 2 - Asignamos ese planeta a la variable del jugador.
		// 3 - Generamos el registro en la base de datos.
		// 4 - Informamos al jugador.

		listaRecolectores.put(planeta, new Recolector(planeta));

		c.listaPlanetasPropios.add(planeta);
		c.listaPlanetasOtros.remove(planeta);
		insertarPlanetaJugadorDB(planeta, c.id);
		Objeto o = new Objeto(Objeto.ACTION, "ASIGNARPLANETA");
		o.planetaName = planeta;
		c.enviaObjeto(o);
	}

	public void quitarPlanetaJugadorDB(String planeta, int jugador) {
		Connection connexio = null;
		Statement statement = null;
		String url = "jdbc:sqlite:galaxia.db";
		try {
			Class.forName("org.sqlite.JDBC");
			connexio = DriverManager.getConnection(url);
			statement = connexio.createStatement();

			// Inserto el usuario
			statement
					.executeUpdate("DELETE FROM planeta_jugador where jugador_id="
							+ jugador + " and planeta_id='" + planeta + "'");

		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			try {
				connexio.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void insertarPlanetaJugadorDB(String planeta, int jugador) {
		Connection connexio = null;
		Statement statement = null;
		String url = "jdbc:sqlite:galaxia.db";
		try {
			Class.forName("org.sqlite.JDBC");
			connexio = DriverManager.getConnection(url);
			statement = connexio.createStatement();

			// Inserto el usuario
			statement.executeUpdate("INSERT into planeta_jugador VALUES ("
					+ jugador + ", '" + planeta + "')");

		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			try {
				connexio.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Clase que generará 1 hilo que enviará una consulta de recolección a la
	 * base de datos cada 5 segundos una vez el planeta esté asignado. Cuando el
	 * planeta se desasigne de un jugador hay que matar este hilo.
	 */

	public class Recolector {

		Toolkit toolkit;
		Timer timer;
		String planeta;

		public Recolector(String planeta) {
			toolkit = Toolkit.getDefaultToolkit();
			timer = new Timer();
			timer.schedule(new Tarea(planeta), 0, 5 * 1000);
			this.planeta = planeta;

		}

		public void paraRecoleccion() {
			timer.cancel();
			timer.purge();
		}

		public boolean planetaSeco(String planeta) {
			Connection connexio = null;
			Statement statement = null;
			ResultSet rs = null;
			boolean res = false;
			String url = "jdbc:sqlite:galaxia.db";
			try {
				Class.forName("org.sqlite.JDBC");
				connexio = DriverManager.getConnection(url);
				statement = connexio.createStatement();

				rs = statement
						.executeQuery("SELECT planeta_recursosMax, (planeta_recursosGast+planeta_recursosRec) from planetas WHERE planeta_nombre='"
								+ planeta + "'");

			} catch (Exception e) {
				System.err.println(e.getMessage());
			} finally {
				try {
					connexio.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return res;
		}

		class Tarea extends TimerTask {

			String planeta;

			public Tarea(String planeta) {
				this.planeta = planeta;

			}

			public void run() {
				Connection connexio = null;
				Statement statement = null;
				ResultSet rs = null;
				Random dado = new Random();
				String url = "jdbc:sqlite:galaxia.db";
				try {
					Class.forName("org.sqlite.JDBC");
					connexio = DriverManager.getConnection(url);
					statement = connexio.createStatement();

					statement
							.executeUpdate("UPDATE planetas set planeta_recursosRec = (planeta_recursosRec+1+planeta_obreros) WHERE planeta_nombre='"
									+ planeta + "'");

					// int num = dado.nextInt(100) + 1;
					// if (num >= 95 && num <= 100) {
					// HiloJugador atacado = al.get(dado.nextInt(al.size()));
					// String planetaObjetivo = atacado.listaPlanetasPropios
					// .get(dado.nextInt(atacado.listaPlanetasPropios
					// .size()));
					//
					// boolean res = Servidor.atacar("Servidor",
					// dado.nextInt(50) + 1, planetaObjetivo);
					//
					// if (res) {
					// desasignarPlaneta(planetaObjetivo, atacado);
					// } else {
					// atacado.writeMsg("El servidor del mal te ha atacado pero ha fracasado en su intento.\n¡Buen Trabajo!");
					// }
					//
					// }

				} catch (Exception e) {
					System.err.println(e.getMessage());
				} finally {
					try {
						connexio.close();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (planetaSeco(planeta)) {
					timer.cancel();
					timer.purge();
				}
			}
		}

	}

	/**
	 * 
	 * Ataque Cada Soldado suma 1 punto de ataque. Cada defensor suma 0 puntos
	 * de ataque. Cada obrero suma 0 puntos de ataque. Defensa Cada soldado suma
	 * 0 puntos de defensa. Cada defensor suma 1 puntos de defensa. Cada obrero
	 * suma 0 puntos de defensa. si se gana el combate, se eliminan todos los
	 * soldados y defensores y se le asigna el planeta al atacante. Si se pierde
	 * el combate se pierden las tropas enviadas.
	 * 
	 * 
	 * @param planetaAtacante
	 * @param planetaObjetivo
	 */

	public static boolean atacar(String planetaAtacante, int cantidadSoldados,
			String planetaObjetivo) {

		boolean resultado = false;
		HashMap<String, Integer> tPlanetaDefiende = tropasDelPlaneta(planetaObjetivo);
		// Miramos las tropas que tiene cada uno en el momento del ataque
		if (!planetaAtacante.equals("Servidor")) {
			HashMap<String, Integer> tPlanetaAtaca = tropasDelPlaneta(planetaAtacante);

			// int puntosDeAtaque = tPlanetaAtaca.get("Soldados");
			int puntosDeAtaque = cantidadSoldados;
			int puntosDeDefensa = tPlanetaDefiende.get("Defensores");

			if (puntosDeAtaque > puntosDeDefensa) {
				resultado = true;
			}
		} else {

			int puntosDeDefensa = tPlanetaDefiende.get("Defensores");
			int puntosDeAtaque = cantidadSoldados;

			if (puntosDeAtaque > puntosDeDefensa) {
				resultado = true;
			}

		}
		return resultado;

	}

	public static HashMap<String, Integer> tropasDelPlaneta(String planeta) {

		HashMap<String, Integer> tropasQueTiene = new HashMap<String, Integer>();

		Connection connexio = null;
		Statement statement = null;
		ResultSet rs = null;
		String url = "jdbc:sqlite:galaxia.db";
		try {
			Class.forName("org.sqlite.JDBC");
			connexio = DriverManager.getConnection(url);
			statement = connexio.createStatement();

			rs = statement
					.executeQuery("SELECT * from planetas WHERE planeta_nombre='"
							+ planeta + "'");

			while (rs.next()) {
				tropasQueTiene.put("Obreros", rs.getInt(2));
				tropasQueTiene.put("Soldados", rs.getInt(4));
				tropasQueTiene.put("Defensores", rs.getInt(3));
			}

		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			try {
				connexio.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return tropasQueTiene;

	}

}
