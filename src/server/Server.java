package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import model.Request;
import model.RequestImpl;
import peer.Peer;

public class Server {

	// Utiliza um hash map para armazenar os dados dos peers
	private Map<String, Peer> peerMap;
	
	// Construtor
	private Server() {
		
		peerMap = new ConcurrentHashMap<>();
	}
	
	// Getter
	public Map<String, Peer> getPeerMap() {
		
		return peerMap;
	}
	
	// Insere os dados do peer no hash map
	public void addPeer(Peer peer) {
		
		String peerKey = peer.getPeerIP() + "-" + peer.getPeerListeningPort();
		peerMap.putIfAbsent(peerKey, peer);
	}
	
	// Atualiza os dados do peer no hash map
	public void updatePeer(Peer peer) {

		String peerKey = peer.getPeerIP() + "-" + peer.getPeerListeningPort();
		peerMap.replace(peerKey, peer);
	}

	public static void main(String[] args) throws Exception{
		
		// Cria uma instância do servidor
		Server server = new Server();
		
		// Cria uma instância do objeto remoto
		Request request = new RequestImpl(server);
		
		// Cria um registro RMI
		LocateRegistry.createRegistry(1099);
		
		// Obtém o registro RMI
		Registry registry = LocateRegistry.getRegistry();
		
		// Associa um nome ao objeto remoto no registro RMI
		registry.bind("rmi://127.0.0.1/request", request);
		
		System.out.print("\033[H\033[2J");  
		System.out.flush();
		System.out.println("=== SERVER ===");
	}
}
