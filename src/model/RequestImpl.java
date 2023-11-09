package model;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import peer.Peer;
import server.Server;

@SuppressWarnings("serial")
public class RequestImpl extends UnicastRemoteObject implements Request{
	
	private Server server;
	
	// Construtor
	public RequestImpl(Server server) throws RemoteException{
		
		super();
		this.server = server;
	}

	// Adiciona um peer ao servidor e responde ao requisitante "JOIN_OK"
	@Override
	public String join(Peer peer) throws RemoteException {
		
		server.addPeer(peer);
		System.out.println("\nPeer " + peer.getPeerIP() + ":" + peer.getPeerListeningPort() + " adicionado com arquivos " + peer.getPeerFiles().stream().map(File::getName).collect(Collectors.joining(" ")));
		
		return "JOIN_OK";
	}
	
	// Faz a busca pelo arquivo e retorna uma lista com os peers que o possuem, caso haja algum
	@Override
	public List<String> search(Peer p, String fileName) throws RemoteException {
		
		List<String> result = new ArrayList<>();
		
		for (Map.Entry<String, Peer> entry : server.getPeerMap().entrySet()) {
			Peer peer = entry.getValue();
			for (File file: peer.getPeerFiles()) {
				if (file.getName().equals(fileName)) {
					String peerIP = peer.getPeerIP();
					int peerListeningPort = peer.getPeerListeningPort();
					String peerInfo = peerIP + ":" + peerListeningPort;
					result.add(peerInfo);
				}
			}
		}
		
		System.out.println("\nPeer " + p.getPeerIP() + ":" + p.getPeerListeningPort() + " solicitou o arquivo " + fileName);
		
		return result;
	}
	
	// Faz o update dos arquivos de um peer já existente e informa no console do servidor
	@Override
	public String update(Peer peer) throws RemoteException {
		
		server.updatePeer(peer);
		System.out.println("\nPeer " + peer.getPeerIP() + ":" + peer.getPeerListeningPort() + " atualizado com arquivos " + peer.getPeerFiles().stream().map(File::getName).collect(Collectors.joining(" ")));
		
		return "UPDATE_OK";
	}
}
